package com.atsjh.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atsjh.common.to.MemberResponseTo;
import com.atsjh.common.to.mq.OrderTo;
import com.atsjh.common.utils.R;
import com.atsjh.gulimall.order.constant.OrderConstant;
import com.atsjh.gulimall.order.entity.OrderItemEntity;
import com.atsjh.gulimall.order.enume.OrderStatusEnum;
import com.atsjh.gulimall.order.feign.CartFeignService;
import com.atsjh.gulimall.order.feign.MemberFeignService;
import com.atsjh.gulimall.order.feign.ProductFeignService;
import com.atsjh.gulimall.order.feign.WareFeignService;
import com.atsjh.gulimall.order.inteceptor.LoginUserInteceptor;
import com.atsjh.gulimall.order.service.OrderItemService;
import com.atsjh.gulimall.order.to.OrderCreateTo;
import com.atsjh.gulimall.order.to.SpuInfoTo;
import com.atsjh.gulimall.order.vo.*;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atsjh.common.utils.PageUtils;
import com.atsjh.common.utils.Query;

import com.atsjh.gulimall.order.dao.OrderDao;
import com.atsjh.gulimall.order.entity.OrderEntity;
import com.atsjh.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Slf4j
@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {
    @Autowired
    MemberFeignService memberFeignService;
    @Autowired
    CartFeignService cartFeignService;
    @Autowired
    ThreadPoolExecutor executor;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    OrderItemService orderItemService;
    @Autowired
    WareFeignService wareFeignService;
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();
        //用户信息
        MemberResponseTo memberResponseTo = LoginUserInteceptor.toThreadLocal.get();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        //远程查询收货地址列表
        CompletableFuture<Void> getAddressTask = CompletableFuture.runAsync(() -> {
            //异步编排中由于请求上下文保存在threadlocal中，别的线程中要手动加入
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> addressList = memberFeignService.getAddressList(memberResponseTo.getId());
            orderConfirmVo.setMemberAddressVos(addressList);
        },executor);

        //远程查询购物车中的购物项
        CompletableFuture<Void> getItemsTask = CompletableFuture.runAsync(() -> {
            //异步编排中由于请求上下文保存在threadlocal中，别的线程中要手动加入
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> itemsForOrder = cartFeignService.getItemsForOrder();
            orderConfirmVo.setItems(itemsForOrder);
        }, executor);

        //用户积分
        orderConfirmVo.setIntegration(memberResponseTo.getIntegration());

        //设置防重令牌， 在redis中和浏览器中各放一个， 订单提交时校验并删除
        String token = UUID.randomUUID().toString().substring(16);
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseTo.getUsername(), token);
        orderConfirmVo.setOrderToken(token);

        CompletableFuture.allOf(getAddressTask, getItemsTask).get();
        return orderConfirmVo;
    }

    /**
     * 订单提交：
     * 令牌防止重复验证
     * @return
     * @param orderSubmitVo
     */
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo orderSubmitVo) {
        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        responseVo.setCode(0);

        MemberResponseTo member = LoginUserInteceptor.toThreadLocal.get();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long execute = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + member.getUsername()), orderSubmitVo.getOrderToken());
        if(execute == 0L){
            //1.1 防重令牌验证失败
            responseVo.setCode(1);
            return responseVo;
        }
        else{
            //2. 创建订单、订单项
            OrderCreateTo order = createOrderTo(member, orderSubmitVo);
            //3. 验价
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = payAmount;//orderSubmitVo.getPayPrice();
            if(Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.001){
                //保存订单
                saveOrder(order);
                //锁定库存
                WareSkuLockVo wareSkuLockVo = new WareSkuLockVo();
                wareSkuLockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> collect = order.getOrderItems().stream().map(item -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    orderItemVo.setTitle(item.getSkuName());
                    return orderItemVo;
                }).collect(Collectors.toList());
                wareSkuLockVo.setLocks(collect);
                R r = wareFeignService.lockOder(wareSkuLockVo);
                if(r.getCode() == 0){
                    //发消息，订单过期取消
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder());
                    //锁定库存成功
                    System.out.println("库存保存成功");
                    responseVo.setCode(0);
                    responseVo.setOrder(order.getOrder());
                    return responseVo;
                }
                else{
                    //锁定库存失败
                    responseVo.setCode(3);
                    return responseVo;
                }
            }
            else{
                //验证价格失败
                responseVo.setCode(2);
                return responseVo;
            }
        }

    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        OrderEntity orderEntity = this.baseMapper.selectOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));

        return orderEntity;
    }

    @Override
    public void closeOrder(OrderEntity order) {
        //重新查找订单， 如果状态为新建， 则关闭
        OrderEntity byId = this.getById(order.getId());
        if(byId.getStatus().equals(OrderStatusEnum.CREATE_NEW.getCode())){
            OrderEntity orderEntity = new OrderEntity();
            orderEntity.setId(order.getId());
            orderEntity.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(orderEntity);
            log.info("关闭订单:{}", order.getOrderSn());

            //向库存解锁队列发送消息，解锁库存
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(order, orderTo);
            rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);
        }
    }

    private void saveOrder(OrderCreateTo orderCreateTo) {
        OrderEntity order = orderCreateTo.getOrder();
        order.setCreateTime(new Date());
        order.setModifyTime(new Date());
        this.save(order);
        orderItemService.saveBatch(orderCreateTo.getOrderItems());
    }

    private OrderCreateTo createOrderTo(MemberResponseTo memberResponseVo, OrderSubmitVo submitVo) {
        //用IdWorker生成订单号
        String orderSn = IdWorker.getTimeId().substring(10);
        System.out.println(orderSn);
        //构建订单
        OrderEntity entity = buildOrder(memberResponseVo, submitVo,orderSn);
        //构建订单项
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderSn);
        //计算价格
        compute(entity, orderItemEntities);
        OrderCreateTo createTo = new OrderCreateTo();
        createTo.setOrder(entity);
        createTo.setOrderItems(orderItemEntities);
        return createTo;
    }

    private void compute(OrderEntity entity, List<OrderItemEntity> orderItemEntities) {
        //总价
        BigDecimal total = BigDecimal.ZERO;
        //优惠价格
        BigDecimal promotion=new BigDecimal("0.0");
        BigDecimal integration=new BigDecimal("0.0");
        BigDecimal coupon=new BigDecimal("0.0");
        //积分
        Integer integrationTotal = 0;
        Integer growthTotal = 0;

        for (OrderItemEntity orderItemEntity : orderItemEntities) {
            total=total.add(orderItemEntity.getRealAmount());
            promotion=promotion.add(orderItemEntity.getPromotionAmount());
            integration=integration.add(orderItemEntity.getIntegrationAmount());
            coupon=coupon.add(orderItemEntity.getCouponAmount());
            integrationTotal += orderItemEntity.getGiftIntegration();
            growthTotal += orderItemEntity.getGiftGrowth();
        }

        entity.setTotalAmount(total);
        entity.setPromotionAmount(promotion);
        entity.setIntegrationAmount(integration);
        entity.setCouponAmount(coupon);
        entity.setIntegration(integrationTotal);
        entity.setGrowth(growthTotal);

        //付款价格=商品价格+运费
        entity.setPayAmount(entity.getFreightAmount().add(total));

        //设置删除状态(0-未删除，1-已删除)
        entity.setDeleteStatus(0);
    }


    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        List<OrderItemVo>  checkedItems = cartFeignService.getCheckedItems();
        List<OrderItemEntity> orderItemEntities = checkedItems.stream().map((item) -> {
            OrderItemEntity orderItemEntity = buildOrderItem(item);
            //1) 设置订单号
            orderItemEntity.setOrderSn(orderSn);
            return orderItemEntity;
        }).collect(Collectors.toList());
        return orderItemEntities;
    }



    private OrderItemEntity buildOrderItem(OrderItemVo item) {
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        Long skuId = item.getSkuId();
        //2) 设置sku相关属性
        orderItemEntity.setSkuId(skuId);
        orderItemEntity.setSkuName(item.getTitle());
        orderItemEntity.setSkuAttrsVals(StringUtils.collectionToDelimitedString(item.getSkuAttrValues(), ";"));
        orderItemEntity.setSkuPic(item.getImage());
        orderItemEntity.setSkuPrice(item.getPrice());
        orderItemEntity.setSkuQuantity(item.getCount());
        //3) 通过skuId查询spu相关属性并设置
        R r = productFeignService.getSpuBySkuId(skuId);
        if (r.getCode() == 0) {
            SpuInfoTo spuInfo = r.getData(new TypeReference<SpuInfoTo>() {});
            orderItemEntity.setSpuId(spuInfo.getId());
            orderItemEntity.setSpuName(spuInfo.getSpuName());
            orderItemEntity.setSpuBrand(spuInfo.getBrandName());
            orderItemEntity.setCategoryId(spuInfo.getCatalogId());
        }
        //4) 商品的优惠信息(不做)

        //5) 商品的积分成长，为价格x数量
        orderItemEntity.setGiftGrowth(item.getPrice().multiply(new BigDecimal(item.getCount())).intValue());
        orderItemEntity.setGiftIntegration(item.getPrice().multiply(new BigDecimal(item.getCount())).intValue());

        //6) 订单项订单价格信息
        orderItemEntity.setPromotionAmount(BigDecimal.ZERO);
        orderItemEntity.setCouponAmount(BigDecimal.ZERO);
        orderItemEntity.setIntegrationAmount(BigDecimal.ZERO);

        //7) 实际价格
        BigDecimal origin = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity()));
        BigDecimal realPrice = origin.subtract(orderItemEntity.getPromotionAmount())
                .subtract(orderItemEntity.getCouponAmount())
                .subtract(orderItemEntity.getIntegrationAmount());
        orderItemEntity.setRealAmount(realPrice);

        return orderItemEntity;
    }

    private OrderEntity buildOrder(MemberResponseTo memberResponse, OrderSubmitVo submitVo, String orderSn) {

        OrderEntity orderEntity =new OrderEntity();

        orderEntity.setOrderSn(orderSn);

        //2) 设置用户信息
        orderEntity.setMemberId(memberResponse.getId());
        orderEntity.setMemberUsername(memberResponse.getUsername());

        //3) 获取邮费和收件人信息并设置
//        FareVo fareVo = wareFeignService.getFare(submitVo.getAddrId());
        FareVo fareVo = memberFeignService.getAddressById(submitVo.getAddrId());
        BigDecimal fare = fareVo.getFare();
        orderEntity.setFreightAmount(fare);
        MemberAddressVo address = fareVo.getAddress();

        orderEntity.setReceiverName(address.getName());
        orderEntity.setReceiverPhone(address.getPhone());
        orderEntity.setReceiverPostCode(address.getPostCode());
        orderEntity.setReceiverProvince(address.getProvince());
        orderEntity.setReceiverCity(address.getCity());
        orderEntity.setReceiverRegion(address.getRegion());
        orderEntity.setReceiverDetailAddress(address.getDetailAddress());

        //4) 设置订单相关的状态信息
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setConfirmStatus(0);
        orderEntity.setAutoConfirmDay(7);

        return orderEntity;
    }

}