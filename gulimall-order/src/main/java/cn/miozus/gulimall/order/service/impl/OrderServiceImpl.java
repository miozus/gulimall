package cn.miozus.gulimall.order.service.impl;

import cn.miozus.common.annotation.DeleteRedis;
import cn.miozus.common.constant.OrderConstant;
import cn.miozus.common.enume.OrderStatusEnum;
import cn.miozus.common.exception.NoStockException;
import cn.miozus.common.to.SkuHasStockVo;
import cn.miozus.common.to.mq.OrderTo;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.Query;
import cn.miozus.common.utils.R;
import cn.miozus.common.vo.MemberRespVo;
import cn.miozus.gulimall.order.config.OrderRabbitMqConfig;
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
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.buf.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    PaymentInfoService paymentInfoService;
    @Autowired
    private OrderService orderService;

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
        MemberRespVo loginUser = LoginUserInterceptor.threadLocal.get();
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

        String token = pushTokenRedis(uid);
        confirmVo.setOrderToken(token);

        CompletableFuture.allOf(addressFuture, orderItemFuture).get();

        return confirmVo;
    }

    private void deleteCartItemRedisCache(List<OrderItemEntity> confirmVo) {
        List<Long> skuIds = confirmVo.stream().map(OrderItemEntity::getSkuId).collect(Collectors.toList());
        orderService.deleteOrderCartItemsRedis(skuIds);
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
    @Transactional(rollbackFor = Exception.class)
    @DeleteRedis("删除提交付款的购物车商品")
    public OrderSubmitRespVo submitOrder(OrderSubmitVo orderSubmitVo) throws NoStockException {
        orderSubmitVoThreadLocal.set(orderSubmitVo);

        OrderSubmitRespVo response = new OrderSubmitRespVo();
        response.setCode(0);

        Long execute = checkAndDeleteRedisToken(orderSubmitVo);
        if (execute == 0L) {
            response.setCode(1);
            return response;
        }

        OrderCreateTo orderTo = createOrder();

        double subtract = checkPriceSubtractAccuracy(orderTo, orderSubmitVo);
        if (Math.abs(subtract) >= OrderConstant.FRONT_BACK_PRICE_FLOAT_THRESHOLD) {
            response.setCode(2);
            return response;
        }

        saveOrderAndOrderItems(orderTo);

        R r = lockStockWareFeignService(orderTo);
        if (r.getCode() != 0) {
            String msg = r.getMsg();
            throw new NoStockException(msg);
        }

        response.setOrder(orderTo.getOrder());
        pushMqDelayQueue(orderTo.getOrder());
        return response;
    }


    /**
     * 推送mq延迟队列
     *
     * @param order 订单
     */
    private void pushMqDelayQueue(OrderEntity order) {
        rabbitTemplate.convertAndSend(OrderRabbitMqConfig.EXCHANGE, OrderRabbitMqConfig.DELAY_QUEUE_ROUTING_KEY, order);
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

        OrderEntity order = buildOrderEntity(orderSn);
        List<OrderItemEntity> orderItems = buildOrderItems(orderSn);

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
    public void closeOrder(OrderEntity to) {
        OrderEntity fresh = this.getById(to.getId());
        if (Objects.isNull(fresh)) {
//            throw GulimallBoundException(to.getId() + "号订单不存在，可能事务未提交");
            return;
        }
        Integer orderStatus = fresh.getStatus();
        if (orderStatus != OrderStatusEnum.CREATE_NEW.getCode()) {
            return;
        }
        updateOrderStatusCanceled(fresh);
        pushReleaseOtherQueueMq(fresh);
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
     *
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
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            updateOrderStatusPayed(vo);
        }
        return "success";
    }

    /**
     * 删除订单购物车商品
     * 因为跳转外链后，无法获取登录信息，所以在此删除，支付成败不管了
     */
    @Override
    @DeleteRedis("删除提交付款的购物车商品")
    public Boolean deleteOrderCartItemsRedis(List<Long> skuIds) {
        return false;
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

    private void pushReleaseOtherQueueMq(OrderEntity fresh) {
        OrderTo orderTo = new OrderTo();
        BeanUtils.copyProperties(fresh, orderTo);
        rabbitTemplate.convertAndSend(OrderRabbitMqConfig.EXCHANGE,
                OrderRabbitMqConfig.RELEASE_OTHER_ROUTING_KEY, orderTo);
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
     * 校验并删除缓存令牌
     * ⚛️ 必须使用脚本锁定原子操作
     *
     * @param orderSubmitVo 订单提交签证官
     * @return {@link Long}
     */
    private Long checkAndDeleteRedisToken(OrderSubmitVo orderSubmitVo) {
        MemberRespVo loginUser = LoginUserInterceptor.threadLocal.get();
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
     * @param orderSn 订单sn
     * @return {@link List}<{@link OrderItemEntity}>
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        List<OrderItemVo> orderItems = cartFeignService.fetchOrderCartItems();
        if (CollectionUtils.isEmpty(orderItems)) {
            return Collections.emptyList();
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

        MemberRespVo loginUser = LoginUserInterceptor.threadLocal.get();
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

    private String pushTokenRedis(Long uid) {
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