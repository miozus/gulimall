package cn.miozus.gulimall.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.miozus.gulimall.common.annotation.DeleteRedis;
import cn.miozus.gulimall.common.annotation.Idempotent;
import cn.miozus.gulimall.common.annotation.PostRabbitMq;
import cn.miozus.gulimall.common.annotation.PutRedis;
import cn.miozus.gulimall.common.constant.OrderConstant;
import cn.miozus.gulimall.common.enume.BizCodeEnum;
import cn.miozus.gulimall.common.enume.OrderStatusEnum;
import cn.miozus.gulimall.common.exception.GuliMallBindException;
import cn.miozus.gulimall.common.to.SkuHasStockVo;
import cn.miozus.gulimall.common.to.mq.SeckillOrderTo;
import cn.miozus.gulimall.common.utils.PageUtils;
import cn.miozus.gulimall.common.utils.Query;
import cn.miozus.gulimall.common.utils.R;
import cn.miozus.gulimall.common.vo.MemberRespVo;
import cn.miozus.gulimall.order.config.AlipayTemplate;
import cn.miozus.gulimall.order.dao.OrderDao;
import cn.miozus.gulimall.order.entity.OrderEntity;
import cn.miozus.gulimall.order.entity.OrderItemEntity;
import cn.miozus.gulimall.order.entity.PaymentInfoEntity;
import cn.miozus.gulimall.order.feign.CartFeignService;
import cn.miozus.gulimall.order.feign.MemberFeignService;
import cn.miozus.gulimall.order.feign.ProductFeignService;
import cn.miozus.gulimall.order.feign.WareFeignService;
import cn.miozus.gulimall.order.interceptor.LoginUserInterceptor;
import cn.miozus.gulimall.order.service.OrderItemService;
import cn.miozus.gulimall.order.service.OrderService;
import cn.miozus.gulimall.order.service.PaymentInfoService;
import cn.miozus.gulimall.order.to.OrderCreateTo;
import cn.miozus.gulimall.order.vo.*;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.common.utils.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
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

    private final ThreadLocal<OrderSubmitVo> orderSubmitVoThreadLocal = new ThreadLocal<>();

    @Autowired
    MemberFeignService memberFeignService;
    @Autowired
    CartFeignService cartFeignService;
    @Autowired
    ThreadPoolExecutor executor;
    @Autowired
    WareFeignService wareFeignService;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    OrderItemService orderItemService;
    @Autowired
    PaymentInfoService paymentInfoService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(new Query<OrderEntity>().getPage(params), new QueryWrapper<>());

        return new PageUtils(page);
    }

    /**
     * 确认订单
     * <p>
     * Feign本质是Http通信，默认过滤请求头，需要配置
     * * 异步：RequestContextHolder 共享上下文，true 开启子线程继承共享，本质threadLocal
     * * 同步：不用加
     *
     * @return {@link OrderConfirmVo}
     */
    @SneakyThrows
    @Override
    @PutRedis("提交令牌")
    public OrderConfirmVo confirmOrder() {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberRespVo loginUser = LoginUserInterceptor.threadLocal.get();
        Long uid = loginUser.getId();
        RequestAttributes feignRequestAttributes = RequestContextHolder.getRequestAttributes();
        RequestContextHolder.setRequestAttributes(feignRequestAttributes, true);

        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {
            List<MemberReceiveAddressVo> address = memberFeignService.queryAddressByMemberId(uid);
            confirmVo.setAddress(address);
        }, executor);

        CompletableFuture<Void> orderItemFuture = CompletableFuture.runAsync(() -> {
            List<OrderItemVo> orderItems = cartFeignService.fetchCheckedOrderCartItems();
            confirmVo.setItems(orderItems);
        }, executor).thenRunAsync(() -> updateStocksByWareFeignService(confirmVo), executor);

        Integer integration = loginUser.getIntegration();
        confirmVo.setIntegration(integration);

        CompletableFuture.allOf(addressFuture, orderItemFuture).get();
        return confirmVo;
    }

    /**
     * 提交订单总流程（自定义错误码）
     * <p>
     * <p>
     * 验令牌
     * 创建订单
     * 验价：前后端价格误差绝对值需小于等于 0.01
     * 保存订单
     * 远程锁库存
     *
     * @param orderSubmitVo 订单提交信息（登陆前提交:临时用户，登录后提交：未赋值？）
     * @return {@link OrderEntity}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Idempotent("令牌校验")
    @PostRabbitMq("提交订单")
    @DeleteRedis("删除提交付款的购物车商品")
    public OrderEntity submitOrder(OrderSubmitVo orderSubmitVo) {
        String addrId = orderSubmitVo.getAddrId();
        if (StringUtils.isBlank(addrId)) {
            throw new GuliMallBindException(BizCodeEnum.CART_ITEM_NOT_EXIST_EXCEPTION);
        }

        orderSubmitVoThreadLocal.set(orderSubmitVo);
        OrderCreateTo orderTo = createOrder();

        boolean isOverThreshold = comparePriceSubtractAccuracy(orderTo, orderSubmitVo);
        if (isOverThreshold) {
            throw new GuliMallBindException(BizCodeEnum.PRICE_SUBTRACT_ACCURACY_OVER_THRESHOLD);
        }

        saveOrderAndOrderItems(orderTo);

        R r = callLockOrderStockFeignMethod(orderTo);
        if (r.isNotOk()) {
            throw new GuliMallBindException(r.getMsg());
        }

        return orderTo.getOrder();
    }


    /**
     * 验价：前端提交的静态数据 vs 数据库查询计算的数据
     *
     * @param order         订单
     * @param orderSubmitVo 订单提交签证官
     * @return double
     */
    private boolean comparePriceSubtractAccuracy(OrderCreateTo order, OrderSubmitVo orderSubmitVo) {
        BigDecimal payAmount = order.getOrder().getPayAmount();
        BigDecimal payPrice = orderSubmitVo.getPayPrice();
        double subtract = payAmount.subtract(payPrice).doubleValue();
        return Math.abs(subtract) > OrderConstant.FRONT_BACK_PRICE_FLOAT_THRESHOLD;
    }

    /**
     * 锁定库存：提供字段给远程服务，SQL 更新被锁定库存数量
     *
     * @param order 订单
     * @return {@link R}
     */
    private R callLockOrderStockFeignMethod(OrderCreateTo order) {
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
        return wareFeignService.lockOrderStock(lockVo);
    }

    /**
     * 保存订单: 订单表格 + 商品
     * 调用服务的批量保存
     *
     * @param order 订单
     */
    private void saveOrderAndOrderItems(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        save(orderEntity);
        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }


    /**
     * 创建订单总流程
     * IdWorker:生成订单流水号
     * 注意两次出现 orderItems 属性
     *
     * @return {@link OrderCreateTo}
     */
    @Override
    public OrderCreateTo createOrder() {
        OrderCreateTo to = new OrderCreateTo();
        String orderSn = IdWorker.getTimeId();

        List<OrderItemEntity> orderItems = buildOrderItems(orderSn);
        OrderEntity order = buildOrderEntity(orderSn);

        calculateAggregatePrice(order, orderItems);
        appendOrderStatusInfo(order);

        order.setOrderItems(orderItems);
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
     * 关闭订单
     * <p>
     * 第二次解锁库存，推送订单给解锁库存，排除网络故障时间差影响。
     *
     * @param to 支付限时 30 分钟届满后，收到的消息携带的实体,To 为了传输规范，新建套壳踢皮球
     */
    @Override
    @PostRabbitMq("关闭订单")
    public void closeOrder(OrderEntity to) {
        OrderEntity fresh = this.getById(to.getId());
        if (Objects.isNull(fresh)) {
            throw new GuliMallBindException(to.getId() + "号订单不存在，可能网络故障导致事务未提交");
        }
        Integer orderStatus = fresh.getStatus();
        if (orderStatus != OrderStatusEnum.CREATE_NEW.getCode()) {
            return;
        }
        updateOrderStatusCanceled(fresh);
    }

    /**
     * 得到订单支付
     * <p>
     * 金额：保留两位小数，向上取整
     * 商户名称：随机查商品名称
     * 备注：商品属性
     *
     * @param orderSn 订单sn
     * @return {@link PayVo}
     */
    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();
        OrderEntity order = this.queryOrderBySn(orderSn);
        BigDecimal payAmount = order.getPayAmount().setScale(2, RoundingMode.UP);
        payVo.setOutTradeNo(orderSn);
        payVo.setTotalAmount(payAmount.toString());
        List<OrderItemEntity> orderItems = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        OrderItemEntity example = orderItems.get(0);
        payVo.setSubject(example.getSkuName());
        payVo.setBody(example.getSkuAttrsVals());
        return payVo;
    }

    /**
     * 分页查询：包含购物车项目
     * <p>
     * 本质是表单列表集合的装饰器：表名 + 装饰器
     * getRecords: 分页封装的结果，每条记录
     * <p>
     * 一次性：只有付款成功成功时调用，如果 orderSubmit: 存在即是锁
     *
     * @param params 模板参数
     * @return {@link PageUtils}
     */
    @Override
    public PageUtils queryPageWithItems(Map<String, Object> params) {
        MemberRespVo loginUser = LoginUserInterceptor.threadLocal.get();
        Long uid = loginUser.getId();
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id", uid).orderByDesc("id")
        );
        List<OrderEntity> orders = page.getRecords().stream().map(order -> {
            List<OrderItemEntity> orderItems = orderItemService.list(
                    new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setOrderItems(orderItems);
            return order;
        }).collect(Collectors.toList());
        page.setRecords(orders);
        return new PageUtils(page);
    }

    /**
     * 处理支付结果
     * <p>
     * 保存订单流水信息
     * 更新订单状态：已取消 -> 已付款
     * 清空局部购物车：删除已付款的商品
     *
     * @param vo 签证官
     * @return 返回给支付宝的信息
     */
    @Override
    public String handlePayResult(PayAsyncVo vo) {
        savePaymentInfoEntity(vo);

        String tradeStatus = vo.getTrade_status();
        if (AlipayTemplate.TRADE_SUCCESS.equals(tradeStatus) || AlipayTemplate.TRADE_FINISHED.equals(tradeStatus)) {
            updateOrderStatusPayed(vo);
        }
        return "success";
    }

    @Override
    public void createSeckillOrder(SeckillOrderTo to) {
        CompletableFuture.runAsync(() -> saveOrderFromSeckill(to), executor);
        CompletableFuture.supplyAsync(() -> buildOrderSpuInfo(to.getSkuId()), executor)
                .thenAcceptAsync(orderSpuInfo -> saveOrderItemFromSeckill(to, orderSpuInfo), executor);
    }

    private void saveOrderItemFromSeckill(SeckillOrderTo to, OrderItemEntity orderSpuInfo) {
        OrderItemEntity orderSkuInfo = buildOrderSkuInfo(to.getSkuId());
        BeanUtil.copyProperties(orderSpuInfo, orderSkuInfo, CopyOptions.create().ignoreNullValue());
        Objects.requireNonNull(orderSkuInfo).setOrderSn(to.getOrderSn());
        orderSkuInfo.setSkuQuantity(to.getNum());
        orderItemService.save(orderSkuInfo);
    }

    private void saveOrderFromSeckill(SeckillOrderTo to) {
        BigDecimal multiply = new BigDecimal(to.getSeckillPrice().toString()).multiply(new BigDecimal(to.getNum().toString()));
        OrderEntity order = OrderEntity.builder()
                .orderSn(to.getOrderSn())
                .memberId(to.getMemberId())
                .payAmount(multiply)
                .status(OrderStatusEnum.CREATE_NEW.getCode())
                .createTime(new Date())
                .build();
        this.save(order);
    }

    private OrderItemEntity buildOrderSkuInfo(Long skuId) {
        R r = productFeignService.querySkuInfo(skuId);
        if (r.isNotOk()) {
            return null;
        }
        SeckillSkuInfoVo info = r.getData("skuInfo", new TypeReference<SeckillSkuInfoVo>() {
        });
        return OrderItemEntity.builder()
                .spuId(info.getSpuId())
                .categoryId(info.getCatalogId())
                .skuId(info.getSkuId())
                .skuName(info.getSkuName())
                .skuPic(info.getSkuDefaultImg())
                .skuPrice(info.getPrice())
                .build();
    }

    private void updateOrderStatusPayed(PayAsyncVo vo) {
        String outTradeNo = vo.getOut_trade_no();
        this.baseMapper.updateOrderStatus(outTradeNo, OrderStatusEnum.PAYED.getCode());
    }

    private void savePaymentInfoEntity(PayAsyncVo vo) {
        PaymentInfoEntity info = new PaymentInfoEntity();
        info.setAlipayTradeNo(vo.getTrade_no());
        info.setOrderSn(vo.getOut_trade_no());
        info.setCallbackTime(vo.getNotify_time());
        info.setPaymentStatus(vo.getTrade_status());
        info.setCreateTime(new Date());
        info.setSubject(vo.getSubject());
        paymentInfoService.save(info);
    }

    private void updateOrderStatusCanceled(OrderEntity fresh) {
        OrderEntity update = new OrderEntity();
        update.setId(fresh.getId());
        update.setStatus(OrderStatusEnum.CANCELED.getCode());
        this.updateById(update);
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
     * 计算总价格：叠加统计所有商品
     * 局部变量小计：
     * 优惠：促销优化 | 积分抵扣 | 优惠券抵扣 | 后台折扣（忽略）
     * 积分：
     * 总价：
     * 运费: 本地线程变量中取出并封装过
     *
     * @param order      订单
     * @param orderItems 订单项
     */
    private void calculateAggregatePrice(OrderEntity order, List<OrderItemEntity> orderItems) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal promotion = BigDecimal.ZERO;
        BigDecimal coupon = BigDecimal.ZERO;
        BigDecimal integrationAmount = BigDecimal.ZERO;
        int giftGrowth = 0;
        int giftIntegration = 0;
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
     * 封装所有购物车商品
     * 最后确定每个购物项的价格
     *
     * @param orderSn 订单sn
     * @return {@link List}<{@link OrderItemEntity}> 空购物车不让通过，前端也拦截了
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        List<OrderItemVo> orderItems = cartFeignService.fetchCheckedOrderCartItems();
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
        OrderItemEntity entity;
        Long skuId = item.getSkuId();

        entity = buildOrderSpuInfo(skuId);

        if (Objects.isNull(entity)) {
            throw new GuliMallBindException(BizCodeEnum.FEIGN_READ_TIMEOUT_EXCEPTION);
        }

        String skuAttrsVals = StringUtils.join(item.getSkuAttrs(), ";");
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

    private OrderItemEntity buildOrderSpuInfo(Long skuId) {
        R r = productFeignService.querySpuInfo(skuId);
        if (r.isNotOk()) {
            return null;
        }
        SpuInfoVo info = r.getData("spuInfo", new TypeReference<SpuInfoVo>() {
        });
        return OrderItemEntity.builder()
                .spuId(info.getId())
                .spuName(info.getSpuName())
                .categoryId(info.getCatalogId())
                .spuBrand(info.getBrandId().toString())
                .build();
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

        MemberRespVo loginUser = LoginUserInterceptor.threadLocal.get();
        Long memberId = loginUser.getId();
        String nickname = loginUser.getNickname();
        order.setMemberUsername(nickname);
        order.setMemberId(memberId);

        OrderSubmitVo orderSubmitVo = orderSubmitVoThreadLocal.get();
        String addrId = orderSubmitVo.getAddrId();

        if (StringUtils.isEmpty(addrId)) {
            throw new GuliMallBindException(memberId.toString(), BizCodeEnum.RECEIVER_NEEDED_EXCEPTION);
        }
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
        // 已经使用完毕，手动释放，否则无法回收导致 OOM
        orderSubmitVoThreadLocal.remove();
        return order;
    }

    private void updateStocksByWareFeignService(OrderConfirmVo confirmVo) {
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