package cn.miozus.gulimall.order.service.impl;

import cn.miozus.common.to.SkuHasStockVo;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.Query;
import cn.miozus.common.utils.R;
import cn.miozus.common.vo.MemberRespVo;
import cn.miozus.gulimall.order.dao.OrderDao;
import cn.miozus.gulimall.order.entity.OrderEntity;
import cn.miozus.gulimall.order.feign.CartFeignService;
import cn.miozus.gulimall.order.feign.MemberFeignService;
import cn.miozus.gulimall.order.feign.WareFeignService;
import cn.miozus.gulimall.order.interceptor.LoginUserInterceptor;
import cn.miozus.gulimall.order.service.OrderService;
import cn.miozus.gulimall.order.vo.MemberReceiveAddressVo;
import cn.miozus.gulimall.order.vo.OrderConfirmVo;
import cn.miozus.gulimall.order.vo.OrderItemVo;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    MemberFeignService memberFeignService;
    @Autowired
    CartFeignService cartFeignService;
    @Autowired
    ThreadPoolExecutor executor;
    @Autowired
    WareFeignService wareFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(new Query<OrderEntity>().getPage(params), new QueryWrapper<OrderEntity>());

        return new PageUtils(page);
    }

    /**
     * 确认订单
     * <p>
     * Feign本质是Http通信，默认过滤请求头，需要配置
     *
     * @return {@link OrderConfirmVo}
     */
    @SneakyThrows
    @Override
    public OrderConfirmVo confirmOrder() {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberRespVo loginUser = LoginUserInterceptor.threadLocal.get();
        RequestAttributes feignRequestAttributes = RequestContextHolder.getRequestAttributes();


        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(feignRequestAttributes);
            Long uid = loginUser.getId();
            List<MemberReceiveAddressVo> address = memberFeignService.queryAddressByMemberId(uid);
            confirmVo.setAddress(address);
        }, executor);

        CompletableFuture<Void> orderItemFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(feignRequestAttributes);
            List<OrderItemVo> orderItem = cartFeignService.fetchOrderCartItems();
            confirmVo.setItems(orderItem);
        }, executor).thenRunAsync(() -> {
            updateStocksWareFeignService(confirmVo);
        }, executor);

        Integer integration = loginUser.getIntegration();
        confirmVo.setIntegration(integration);

        CompletableFuture.allOf(addressFuture, orderItemFuture).get();

        return confirmVo;
    }

    private void updateStocksWareFeignService(OrderConfirmVo confirmVo) {
        List<OrderItemVo> items = confirmVo.getItems();
        List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
        R data = wareFeignService.querySkuHasStock(skuIds);
        if (Objects.nonNull(data)) {
            List<SkuHasStockVo> skuHasStockVo = data.getData(new TypeReference<List<SkuHasStockVo>>() {
            });
            Map<Long, Boolean> skuIdStockMap = skuHasStockVo.stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, SkuHasStockVo::getHasStock));
            confirmVo.setStocks(skuIdStockMap);
        }
    }

}