package cn.miozus.gulimall.order.service.impl;

import cn.miozus.common.exception.NoStockException;
import cn.miozus.common.to.SkuHasStockVo;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.Query;
import cn.miozus.common.utils.R;
import cn.miozus.common.vo.MemberRespVo;
import cn.miozus.common.constant.OrderConstant;
import cn.miozus.gulimall.order.dao.OrderDao;
import cn.miozus.gulimall.order.entity.OrderEntity;
import cn.miozus.gulimall.order.entity.OrderItemEntity;
import cn.miozus.gulimall.order.enume.OrderStatusEnum;
import cn.miozus.gulimall.order.feign.CartFeignService;
import cn.miozus.gulimall.order.feign.MemberFeignService;
import cn.miozus.gulimall.order.feign.ProductFeignService;
import cn.miozus.gulimall.order.feign.WareFeignService;
import cn.miozus.gulimall.order.interceptor.LoginUserInterceptor;
import cn.miozus.gulimall.order.service.OrderItemService;
import cn.miozus.gulimall.order.service.OrderService;
import cn.miozus.gulimall.order.to.OrderCreateTo;
import cn.miozus.gulimall.order.vo.*;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.buf.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * 订单服务impl
 *
 * @author miao
 * @date 2021/10/21
 */
@Slf4j
@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private ThreadLocal<OrderSubmitVo> orderSubmitVoThreadLocal = new ThreadLocal<>();

    @Autowired
    MemberFeignService memberFeignService;
    @Autowired
    CartFeignService cartFeignService;
    @Autowired
    ThreadPoolExecutor executor;
    @Autowired
    WareFeignService wareFeignService;
    @Autowired
    RedisTemplate<String, String> redisTemplate;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    OrderItemService orderItemService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(new Query<OrderEntity>().getPage(params), new QueryWrapper<OrderEntity>());

        return new PageUtils(page);
    }

    /**
     * 确认订单
     * <p>
     * Feign本质是Http通信，默认过滤请求头，需要配置
     * * 异步：RequestContextHolder 共享上下文
     * * 同步：不用加
     *
     * @return {@link OrderConfirmVo}
     */
    @SneakyThrows
    @Override
    public OrderConfirmVo confirmOrder() {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberRespVo loginUser = LoginUserInterceptor.loginUserThreadLocal.get();
        Long uid = loginUser.getId();
        RequestAttributes feignRequestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(feignRequestAttributes);
            List<MemberReceiveAddressVo> address = memberFeignService.queryAddressByMemberId(uid);
            confirmVo.setAddress(address);
        }, executor);

        CompletableFuture<Void> orderItemFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(feignRequestAttributes);
            List<OrderItemVo> orderItems = cartFeignService.fetchOrderCartItems();
            confirmVo.setItems(orderItems);
        }, executor).thenRunAsync(() -> {
            updateStocksWareFeignService(confirmVo);
        }, executor);

        Integer integration = loginUser.getIntegration();
        confirmVo.setIntegration(integration);

        String token = pushRedisToken(uid);
        confirmVo.setOrderToken(token);

        CompletableFuture.allOf(addressFuture, orderItemFuture).get();
        return confirmVo;
    }

    /**
     * 提交订单总流程（自定义错误码）
     * 0 默认成功
     * <p>
     * 验令牌：1 失败：脚本0失败1成功
     * 创建订单
     * 验价：2 失败：零头误差大于等于 0.01
     * 保存订单
     * 远程锁库存: 3 调用失败抛出异常
     *
     * @param orderSubmitVo 订单提交签证官
     * @return {@link OrderSubmitRespVo}
     */
    @Override
    @Transactional
    public OrderSubmitRespVo submitOrder(OrderSubmitVo orderSubmitVo) throws NoStockException {
        orderSubmitVoThreadLocal.set(orderSubmitVo);
        OrderSubmitRespVo response = new OrderSubmitRespVo();
        response.setCode(0);

        Long execute = checkAndDeleteRedisToken(orderSubmitVo);
        if (execute == 0L) {
            response.setCode(1);
            return response;
        }

        OrderCreateTo order = createOrder();

        double subtract = checkPriceSubtractAccuracy(order, orderSubmitVo);
        if (Math.abs(subtract) >= OrderConstant.FRONT_BACK_PRICE_FLOAT_THRESHOLD) {
            response.setCode(2);
            return response;
        }

        saveOrder(order);

        R r = lockStockWareFeignService(order);
        if (r.getCode() != 0) {
            String msg = r.getMsg();
            throw new NoStockException(msg);
        }

        response.setOrder(order.getOrder());
        return response;
    }

    /**
     * 验价：前端提交的静态数据 vs 数据库查询计算的数据
     *
     * @param order         订单
     * @param orderSubmitVo 订单提交签证官
     * @return double
     */
    private double checkPriceSubtractAccuracy(OrderCreateTo order, OrderSubmitVo orderSubmitVo) {
        BigDecimal payAmount = order.getOrder().getPayAmount();
        BigDecimal payPrice = orderSubmitVo.getPayPrice();
        return payAmount.subtract(payPrice).doubleValue();
    }

    /**
     * 锁定库存：提供字段给远程服务，SQL 更新被锁定库存数量
     *
     * @param order 订单
     * @return {@link R}
     */
    private R lockStockWareFeignService(OrderCreateTo order) {
        WareSkuLockVo lockVo = new WareSkuLockVo();
        lockVo.setOrderSn(order.getOrder().getOrderSn());
        List<OrderItemVo> itemVos = order.getOrderItems().stream().map(item -> {
            OrderItemVo itemVo = new OrderItemVo();
            itemVo.setSkuId(item.getSkuId());
            itemVo.setCount(item.getSkuQuantity());
            itemVo.setTitle(item.getSkuName());
            return itemVo;
        }).collect(Collectors.toList());
        lockVo.setOrderItems(itemVos);
        log.debug("lockVo {} ", lockVo);
        return wareFeignService.lockOrderStock(lockVo);
    }

    /**
     * 保存订单: 订单表格 + 商品
     *
     * 调用服务的批量保存
     *
     * @param order 订单
     */
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        save(orderEntity);
        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }


    /**
     * 创建订单总流程
     * <p>
     * IdWorker:生成订单流水号
     *
     * @return {@link OrderCreateTo}
     */
    @Override
    public OrderCreateTo createOrder() {
        OrderCreateTo to = new OrderCreateTo();
        String orderSn = IdWorker.getTimeId();

        OrderEntity order = buildOrderEntity(orderSn);
        List<OrderItemEntity> orderItems = buildOrderItems(orderSn);

        calculateAggregatePrice(order, orderItems);
        appendOrderStatusInfo(order);

        to.setOrder(order);
        to.setOrderItems(orderItems);
        return to;
    }

    /**
     * 查询订单状态
     *
     * @param orderSn 订单sn
     * @return {@link OrderEntity}
     */
    @Override
    public OrderEntity queryOrderBySn(String orderSn) {
        return this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
    }

    /**
     * 追加订单状态信息
     * 0:未删除
     *
     * @param order 订单
     */
    private void appendOrderStatusInfo(OrderEntity order) {
        order.setDeleteStatus(0);
    }

    /**
     * 计算价格：叠加统计所有商品
     * 局部变量小计：
     * 优惠：促销优化 | 积分抵扣 | 优惠券抵扣 | 后台折扣（忽略）
     * 积分：
     * 总价：
     * 运费: 本地线程变量中取出并封装过
     *
     * @param order      订单
     * @param orderItems 订单项
     * @return {@link OrderEntity}
     */
    private void calculateAggregatePrice(OrderEntity order, List<OrderItemEntity> orderItems) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal promotion = BigDecimal.ZERO;
        BigDecimal coupon = BigDecimal.ZERO;
        BigDecimal integrationAmount = BigDecimal.ZERO;
        int giftGrowth = 0, giftIntegration = 0;

        for (OrderItemEntity item : orderItems) {
            coupon = coupon.add(item.getCouponAmount());
            integrationAmount = integrationAmount.add(item.getIntegrationAmount());
            promotion = promotion.add(item.getPromotionAmount());
            total = total.add(item.getRealAmount());
            giftGrowth += item.getGiftGrowth();
            giftIntegration += item.getGiftIntegration();
        }
        BigDecimal payPrice = total.add(order.getFreightAmount());
        order.setTotalAmount(total);
        order.setPayAmount(payPrice);
        order.setCouponAmount(coupon);
        order.setPromotionAmount(promotion);
        order.setIntegrationAmount(integrationAmount);
        order.setIntegration(giftIntegration);
        order.setGrowth(giftGrowth);
    }

    /**
     * 校验并删除缓存令牌
     * ⚛️ 必须使用脚本锁定原子操作
     *
     * @param orderSubmitVo 订单提交签证官
     * @return {@link Long}
     */
    private Long checkAndDeleteRedisToken(OrderSubmitVo orderSubmitVo) {
        MemberRespVo loginUser = LoginUserInterceptor.loginUserThreadLocal.get();
        Long uid = loginUser.getId();
        String tokenKey = OrderConstant.ORDER_USER_TOKEN_PREFIX + uid;
        String token = orderSubmitVo.getOrderToken();
        String deleteKeyIfExistsLuaScript = "if redis.call('get',KEYS[1]) == ARGV[1] then " +
                "return redis.call('del',KEYS[1]); " +
                "else " +
                "return 0; " +
                "end; ";
        DefaultRedisScript<Long> action = new DefaultRedisScript<>(deleteKeyIfExistsLuaScript, Long.class);
        return redisTemplate.execute(action, Collections.singletonList(tokenKey), token);
    }

    /**
     * 封装所有购物车商品
     * 最后确定每个购物项的价格
     *
     * @param orderSn
     * @return {@link List}<{@link OrderItemEntity}>
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        List<OrderItemVo> orderItems = cartFeignService.fetchOrderCartItems();
        if (CollectionUtils.isEmpty(orderItems)) {
            return null;
        }
        return orderItems.stream().map(item -> {
            OrderItemEntity orderItemEntity = buildOrderItemSingle(item);
            orderItemEntity.setOrderSn(orderSn);
            return orderItemEntity;
        }).collect(Collectors.toList());
    }

    /**
     * 封装单个购物车商品
     * <p>
     * SKU、优惠（暂时忽略）、积分
     * SPU: 冗余字段图片和品牌名另查
     * 价格：优惠暂时设为零（应该查询再拷贝，而非重写），计算乘积
     *
     * @param item 项
     * @return {@link OrderItemEntity}
     */
    private OrderItemEntity buildOrderItemSingle(OrderItemVo item) {
        OrderItemEntity entity = new OrderItemEntity();
        Long skuId = item.getSkuId();

        R r = productFeignService.querySpuInfoBySkuId(skuId);
        SpuInfoVo spuInfo = r.getData("spuInfo", new TypeReference<SpuInfoVo>() {
        });
        entity.setSpuId(spuInfo.getId());
        entity.setSpuName(spuInfo.getSpuName());
        entity.setCategoryId(spuInfo.getCatalogId());
        entity.setSpuBrand(spuInfo.getBrandId().toString());

        String skuAttrsVals = StringUtils.join(item.getSkuAttrs(), ';');
        entity.setSkuId(skuId);
        entity.setSkuAttrsVals(skuAttrsVals);
        entity.setSkuName(item.getTitle());
        entity.setSkuPrice(item.getPrice());
        entity.setSkuQuantity(item.getCount());
        entity.setSkuPic(item.getImage());

        int giftIncrement = item.getPrice().multiply(new BigDecimal((item.getCount()))).intValue();
        entity.setGiftGrowth(giftIncrement);
        entity.setGiftIntegration(giftIncrement);

        // 假设这里是查询赋值（模拟零优惠）
        BigDecimal zero = BigDecimal.ZERO;
        entity.setIntegrationAmount(zero);
        entity.setCouponAmount(zero);
        entity.setPromotionAmount(zero);

        // 正常计算: 原始总价 - (各种优惠) = 应付总额
        BigDecimal originAmount = entity.getSkuPrice().multiply(new BigDecimal(entity.getSkuQuantity()));
        BigDecimal convenience = entity.getIntegrationAmount().add(entity.getCouponAmount()).add(entity.getPromotionAmount());
        BigDecimal realAmount = originAmount.subtract(convenience);
        entity.setRealAmount(realAmount);
        return entity;
    }

    /**
     * 构建订单
     * <p>
     * 用户信息
     * 运费和收件人
     * 订单状态：0 成功
     *
     * @param orderSn 订单sn
     * @return {@link OrderEntity}
     */
    private OrderEntity buildOrderEntity(String orderSn) {
        OrderEntity order = new OrderEntity();
        order.setOrderSn(orderSn);

        MemberRespVo loginUser = LoginUserInterceptor.loginUserThreadLocal.get();
        Long memberId = loginUser.getId();
        String nickname = loginUser.getNickname();
        order.setMemberUsername(nickname);
        order.setMemberId(memberId);

        OrderSubmitVo orderSubmitVo = orderSubmitVoThreadLocal.get();
        String addrId = orderSubmitVo.getAddrId();

        R r = wareFeignService.queryFare(Long.valueOf(addrId));
        FareVo fareVo = r.getData("fareVo", new TypeReference<FareVo>() {
        });
        MemberReceiveAddressVo addr = fareVo.getAddress();
        order.setFreightAmount(fareVo.getFare());
        order.setReceiverProvince(addr.getProvince());
        order.setReceiverPostCode(addr.getPostCode());
        order.setReceiverCity(addr.getCity());
        order.setReceiverRegion(addr.getRegion());
        order.setReceiverDetailAddress(addr.getDetailAddress());
        order.setReceiverName(addr.getName());
        order.setReceiverPhone(addr.getPhone());

        order.setCreateTime(new Date());
        order.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        order.setConfirmStatus(0);
        order.setAutoConfirmDay(7);
        return order;
    }

    private String pushRedisToken(Long uid) {
        String tokenKey = OrderConstant.ORDER_USER_TOKEN_PREFIX + uid;
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(tokenKey, token, 30, TimeUnit.MINUTES);
        return token;
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