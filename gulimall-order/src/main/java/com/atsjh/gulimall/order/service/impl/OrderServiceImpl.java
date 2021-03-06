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
        //????????????
        MemberResponseTo memberResponseTo = LoginUserInteceptor.toThreadLocal.get();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        //??????????????????????????????
        CompletableFuture<Void> getAddressTask = CompletableFuture.runAsync(() -> {
            //?????????????????????????????????????????????threadlocal????????????????????????????????????
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> addressList = memberFeignService.getAddressList(memberResponseTo.getId());
            orderConfirmVo.setMemberAddressVos(addressList);
        },executor);

        //????????????????????????????????????
        CompletableFuture<Void> getItemsTask = CompletableFuture.runAsync(() -> {
            //?????????????????????????????????????????????threadlocal????????????????????????????????????
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> itemsForOrder = cartFeignService.getItemsForOrder();
            orderConfirmVo.setItems(itemsForOrder);
        }, executor);

        //????????????
        orderConfirmVo.setIntegration(memberResponseTo.getIntegration());

        //????????????????????? ???redis????????????????????????????????? ??????????????????????????????
        String token = UUID.randomUUID().toString().substring(16);
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseTo.getUsername(), token);
        orderConfirmVo.setOrderToken(token);

        CompletableFuture.allOf(getAddressTask, getItemsTask).get();
        return orderConfirmVo;
    }

    /**
     * ???????????????
     * ????????????????????????
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
            //1.1 ????????????????????????
            responseVo.setCode(1);
            return responseVo;
        }
        else{
            //2. ????????????????????????
            OrderCreateTo order = createOrderTo(member, orderSubmitVo);
            //3. ??????
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = payAmount;//orderSubmitVo.getPayPrice();
            if(Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.001){
                //????????????
                saveOrder(order);
                //????????????
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
                    //??????????????????????????????
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder());
                    //??????????????????
                    System.out.println("??????????????????");
                    responseVo.setCode(0);
                    responseVo.setOrder(order.getOrder());
                    return responseVo;
                }
                else{
                    //??????????????????
                    responseVo.setCode(3);
                    return responseVo;
                }
            }
            else{
                //??????????????????
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
        //????????????????????? ???????????????????????? ?????????
        OrderEntity byId = this.getById(order.getId());
        if(byId.getStatus().equals(OrderStatusEnum.CREATE_NEW.getCode())){
            OrderEntity orderEntity = new OrderEntity();
            orderEntity.setId(order.getId());
            orderEntity.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(orderEntity);
            log.info("????????????:{}", order.getOrderSn());

            //????????????????????????????????????????????????
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
        //???IdWorker???????????????
        String orderSn = IdWorker.getTimeId().substring(10);
        System.out.println(orderSn);
        //????????????
        OrderEntity entity = buildOrder(memberResponseVo, submitVo,orderSn);
        //???????????????
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderSn);
        //????????????
        compute(entity, orderItemEntities);
        OrderCreateTo createTo = new OrderCreateTo();
        createTo.setOrder(entity);
        createTo.setOrderItems(orderItemEntities);
        return createTo;
    }

    private void compute(OrderEntity entity, List<OrderItemEntity> orderItemEntities) {
        //??????
        BigDecimal total = BigDecimal.ZERO;
        //????????????
        BigDecimal promotion=new BigDecimal("0.0");
        BigDecimal integration=new BigDecimal("0.0");
        BigDecimal coupon=new BigDecimal("0.0");
        //??????
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

        //????????????=????????????+??????
        entity.setPayAmount(entity.getFreightAmount().add(total));

        //??????????????????(0-????????????1-?????????)
        entity.setDeleteStatus(0);
    }


    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        List<OrderItemVo>  checkedItems = cartFeignService.getCheckedItems();
        List<OrderItemEntity> orderItemEntities = checkedItems.stream().map((item) -> {
            OrderItemEntity orderItemEntity = buildOrderItem(item);
            //1) ???????????????
            orderItemEntity.setOrderSn(orderSn);
            return orderItemEntity;
        }).collect(Collectors.toList());
        return orderItemEntities;
    }



    private OrderItemEntity buildOrderItem(OrderItemVo item) {
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        Long skuId = item.getSkuId();
        //2) ??????sku????????????
        orderItemEntity.setSkuId(skuId);
        orderItemEntity.setSkuName(item.getTitle());
        orderItemEntity.setSkuAttrsVals(StringUtils.collectionToDelimitedString(item.getSkuAttrValues(), ";"));
        orderItemEntity.setSkuPic(item.getImage());
        orderItemEntity.setSkuPrice(item.getPrice());
        orderItemEntity.setSkuQuantity(item.getCount());
        //3) ??????skuId??????spu?????????????????????
        R r = productFeignService.getSpuBySkuId(skuId);
        if (r.getCode() == 0) {
            SpuInfoTo spuInfo = r.getData(new TypeReference<SpuInfoTo>() {});
            orderItemEntity.setSpuId(spuInfo.getId());
            orderItemEntity.setSpuName(spuInfo.getSpuName());
            orderItemEntity.setSpuBrand(spuInfo.getBrandName());
            orderItemEntity.setCategoryId(spuInfo.getCatalogId());
        }
        //4) ?????????????????????(??????)

        //5) ?????????????????????????????????x??????
        orderItemEntity.setGiftGrowth(item.getPrice().multiply(new BigDecimal(item.getCount())).intValue());
        orderItemEntity.setGiftIntegration(item.getPrice().multiply(new BigDecimal(item.getCount())).intValue());

        //6) ???????????????????????????
        orderItemEntity.setPromotionAmount(BigDecimal.ZERO);
        orderItemEntity.setCouponAmount(BigDecimal.ZERO);
        orderItemEntity.setIntegrationAmount(BigDecimal.ZERO);

        //7) ????????????
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

        //2) ??????????????????
        orderEntity.setMemberId(memberResponse.getId());
        orderEntity.setMemberUsername(memberResponse.getUsername());

        //3) ???????????????????????????????????????
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

        //4) ?????????????????????????????????
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setConfirmStatus(0);
        orderEntity.setAutoConfirmDay(7);

        return orderEntity;
    }

}