package com.geng.service.impl;

import com.alipay.api.AlipayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayMonitorService;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayMonitorServiceImpl;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.alipay.demo.trade.service.impl.AlipayTradeWithHBServiceImpl;
import com.alipay.demo.trade.utils.ZxingUtils;
import com.geng.alipay.Main;
import com.geng.common.*;
import com.geng.dao.OrderItemMapper;
import com.geng.dao.OrderMapper;
import com.geng.dao.PayInfoMapper;
import com.geng.pojo.*;
import com.geng.service.ICartService;
import com.geng.service.IOrderService;
import com.geng.service.IProductService;
import com.geng.service.IShippingService;
import com.geng.utils.BigDecimalUtils;
import com.geng.utils.DateUtils;
import com.geng.vo.*;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.geng.pojo.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
@Service
public class OrderServiceImpl implements IOrderService {
    @Autowired
    ICartService cartService;
    @Autowired
    IProductService productService;
    @Autowired
    IShippingService shippingService;
    @Autowired
    OrderMapper orderMapper;
    @Autowired
    OrderItemMapper orderItemMapper;
    @Autowired
    PayInfoMapper payInfoMapper;
    @Value("${business.imageHost}")
    private String imageHost;
    @Override
    public ServerResponse createOrder(Integer userId, Integer shippingId) {
        //1.参数非空校验
        if (shippingId==null){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"收获地址必传");
        }
        //判断shippingid是否存在


        //2.查看用户购物车中已选中的商品List<Cart>
        ServerResponse<List<Cart>> serverResponse=cartService.findCartsByUseridAndChecked(userId);
        List<Cart> cartList=serverResponse.getData();
        if (cartList==null||cartList.size()==0){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"购物车为空或者未选中");
        }
        //3.List<Cart>-->List<OrderItem>
        ServerResponse orderItems_serverResponse=getCartOrderItem(userId,cartList);
        if (!orderItems_serverResponse.isSuccess()){
            return orderItems_serverResponse;
        }
        List<OrderItem> orderItemList=(List<OrderItem>)orderItems_serverResponse.getData();
        //4.创建Order实体类并保存到DB--order
        ServerResponse<Order> order_serverResponse=creatOrder(userId,shippingId,orderItemList);
        if (!order_serverResponse.isSuccess()){
            return order_serverResponse;
        }
        Order order=order_serverResponse.getData();
        //5.保存订单明细List<OrderItemVO>--order_item
            ServerResponse serverResponse1=saveOrderItems(orderItemList,order);
            if (!serverResponse1.isSuccess()){
                return serverResponse1;
        }
        //6.扣库存
        reduceProductStock(orderItemList);
        //7.清空购物车中下单的商品
        ServerResponse cart_serverResponse=cartService.deleteBatch(cartList);
        if (!cart_serverResponse.isSuccess()){
            return cart_serverResponse;
        }
        //8.返回OrderVO
        return assembleOrderVO(order,orderItemList,shippingId);
    }

    @Override
    public ServerResponse pay(Integer userId, Long orderNo) {
        if (orderNo==null){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"订单号必传");
        }
        Order order=orderMapper.findOrderByOrderNo(orderNo);
        if (order==null){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"订单不存在");
        }
        return pay(order);
    }

    @Override
    public String callback(Map<String, String> requestParams) {
        // 订单号，
        String orderNo=requestParams.get("out_trade_no");
        // 流水号，
        String trade_no=requestParams.get("trade_no");
        // 支付状态，
        String trade_status=requestParams.get("trade_status");
        // 付款时间
        String payment_time=requestParams.get("gmt_payment");

        //根据订单号查询订单
        Order order=orderMapper.findOrderByOrderNo(Long.parseLong(orderNo));
        if (order==null){
            return "fail";
        }
        if (trade_status.equals("TRADE_SUCCESS")){
            //支付成功
            //修改订单状态
            Order order1=new Order();
            order1.setOrderNo(Long.parseLong(orderNo));
            order1.setStatus(OrderStatusEnum.ORDER_PAYED.getStatus());
            order1.setPaymentTime(DateUtils.strToDate(payment_time));
            int result=orderMapper.updateOrderStatusAndPaymentByOrderNo(order1);
            if (result<=0){
                return "fail";
            }
        }
        //添加支付记录
        PayInfo payInfo=new PayInfo();
        payInfo.setOrderNo(Long.parseLong(orderNo));
        payInfo.setUserId(order.getUserId());
        payInfo.setPayPlatform(PaymentTypeEnum.PAYMENT_TYPE_ONLINE.getCode());
        payInfo.setPlatformNumber(trade_no);
        payInfo.setPlatformStatus(trade_status);
        int pay_result=payInfoMapper.insert(payInfo);
        if (pay_result<=0){
            return "fail";
        }
        return "pay_success";
    }

    private static Log log = LogFactory.getLog(Main.class);

    // 支付宝当面付2.0服务
    private static AlipayTradeService tradeService;

    // 支付宝当面付2.0服务（集成了交易保障接口逻辑）
    private static AlipayTradeService   tradeWithHBService;

    // 支付宝交易保障接口服务，供测试接口api使用，请先阅读readme.txt
    private static AlipayMonitorService monitorService;

    static {
        /** 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
         *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
         */
        Configs.init("zfbinfo.properties");

        /** 使用Configs提供的默认参数
         *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
         */
        tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();

        // 支付宝当面付2.0服务（集成了交易保障接口逻辑）
        tradeWithHBService = new AlipayTradeWithHBServiceImpl.ClientBuilder().build();

        /** 如果需要在程序中覆盖Configs提供的默认参数, 可以使用ClientBuilder类的setXXX方法修改默认参数 否则使用代码中的默认设置 */
        monitorService = new AlipayMonitorServiceImpl.ClientBuilder()
                .setGatewayUrl("http://mcloudmonitor.com/gateway.do").setCharset("GBK")
                .setFormat("json").build();
    }
    // 测试当面付2.0生成支付二维码
    public ServerResponse pay(Order order) {
        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        String outTradeNo =String.valueOf(order.getOrderNo());

        // (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
        String subject = "【乐淘】平台商品支付";

        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        String totalAmount = order.getPayment().toString();

        // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
        // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
        String undiscountableAmount = "0";

        // 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
        // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
        String sellerId = "";

        // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
        String body = "购买商品共"+order.getPayment()+"元";

        // 商户操作员编号，添加此参数可以为商户操作员做销售统计
        String operatorId = "test_operator_id";

        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String storeId = "test_store_id";

        // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088100200300400500");

        // 支付超时，定义为120分钟
        String timeoutExpress = "120m";

        //根据orderNo查询商品明细
        List<OrderItem> orderItemList=orderItemMapper.findOrderItemByOrderNo(order.getOrderNo());
        if (orderItemList==null||orderItemList.size()==0){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"没有可购买的商品");
        }

        // 商品明细列表，需填写购买商品详细信息，
        List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();

        for (OrderItem orderItem:orderItemList){
            // 创建一个商品信息，参数含义分别为商品id（使用国标）、名称、单价（单位为分）、数量，如果需要添加商品类别，详见GoodsDetail
            GoodsDetail goods1 = GoodsDetail.newInstance(String.valueOf(orderItem.getProductId()), orderItem.getProductName(), orderItem.getCurrentUnitPrice().intValue(), orderItem.getQuantity());
            // 创建好一个商品后添加至商品明细列表
            goodsDetailList.add(goods1);
        }



        // 创建扫码支付请求builder，设置请求参数
        AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder()
                .setSubject(subject).setTotalAmount(totalAmount).setOutTradeNo(outTradeNo)
                .setUndiscountableAmount(undiscountableAmount).setSellerId(sellerId).setBody(body)
                .setOperatorId(operatorId).setStoreId(storeId).setExtendParams(extendParams)
                .setTimeoutExpress(timeoutExpress)
                //支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置
                .setNotifyUrl("http://y5w9na.natappfree.cc/order/callback.do")
                .setGoodsDetailList(goodsDetailList);

        AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);
        switch (result.getTradeStatus()) {
            case SUCCESS:
                log.info("支付宝预下单成功: )");

                AlipayTradePrecreateResponse response = result.getResponse();
                dumpResponse(response);

                // 需要修改为运行机器上的路径
                String filePath = String.format("F:/upload/qr-%s.png",
                        response.getOutTradeNo());
                log.info("filePath:" + filePath);
                ZxingUtils.getQRCodeImge(response.getQrCode(), 256, filePath);
                PayVO payVO=new PayVO(order.getOrderNo(),imageHost+"qr-"+response.getOutTradeNo()+".png");
                return ServerResponse.createServerResponseBySuccess(payVO);

            case FAILED:
                log.error("支付宝预下单失败!!!");
                break;

            case UNKNOWN:
                log.error("系统异常，预下单状态未知!!!");
                break;

            default:
                log.error("不支持的交易状态，交易返回异常!!!");
                break;
        }
        return ServerResponse.createServerResponseByError();
    }
    // 简单打印应答
    private void dumpResponse(AlipayResponse response) {
        if (response != null) {
            log.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
            if (StringUtils.isNotEmpty(response.getSubCode())) {
                log.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(),
                        response.getSubMsg()));
            }
            log.info("body:" + response.getBody());
        }
    }
    //OrderVO
    private ServerResponse assembleOrderVO(Order order, List<OrderItem> orderItemList, Integer shippingId){
        OrderVO orderVO=new OrderVO();
        List<OrderItemVO> orderItemVOList=Lists.newArrayList();
        for(OrderItem orderItem:orderItemList){
            OrderItemVO orderItemVO= assembleOrderItemVO(orderItem);
            orderItemVOList.add(orderItemVO);
        }
        orderVO.setOrderItemVoList(orderItemVOList);
        orderVO.setImageHost(imageHost);
        ServerResponse<Shipping> serverResponse=shippingService.findShippingById(shippingId);
        if (!serverResponse.isSuccess()){
            return serverResponse;
        }
        Shipping shipping=serverResponse.getData();
        if(shipping!=null){
            orderVO.setShippingId(shippingId);
            ShippingVO shippingVO= assmbleShippingVO(shipping);
            orderVO.setShippingVo(shippingVO);
            orderVO.setReceiverName(shipping.getReceiverName());
        }
        orderVO.setStatus(order.getStatus());
        OrderStatusEnum orderStatusEnum= OrderStatusEnum.statusOf(order.getStatus());
        if(orderStatusEnum!=null){
            orderVO.setStatusDesc(orderStatusEnum.getDesc());
        }
        orderVO.setPostage(0);
        orderVO.setPayment(order.getPayment());
        orderVO.setPaymentType(order.getPaymentType());
        PaymentTypeEnum paymentTypeEnum=PaymentTypeEnum.codeOf(order.getPaymentType());
        if(paymentTypeEnum!=null){
            orderVO.setPaymentTypeDesc(paymentTypeEnum.getDesc());
        }
        orderVO.setOrderNo(order.getOrderNo());
        return ServerResponse.createServerResponseBySuccess(orderVO);
    }
    private ShippingVO assmbleShippingVO(Shipping shipping){
        ShippingVO shippingVO=new ShippingVO();
        if(shipping!=null){
            shippingVO.setReceiverAddress(shipping.getReceiverAddress());
            shippingVO.setReceiverCity(shipping.getReceiverCity());
            shippingVO.setReceiverDistrict(shipping.getReceiverDistrict());
            shippingVO.setReceiverMobile(shipping.getReceiverMobile());
            shippingVO.setReceiverName(shipping.getReceiverName());
            shippingVO.setReceiverPhone(shipping.getReceiverPhone());
            shippingVO.setReceiverProvince(shipping.getReceiverProvince());
            shippingVO.setReceiverZip(shipping.getReceiverZip());
        }
        return shippingVO;
    }
    //扣库存方法
    private ServerResponse reduceProductStock(List<OrderItem> orderItemList){
        for (OrderItem orderItem:orderItemList){
            Integer productId=orderItem.getProductId();
            ServerResponse<Product> serverResponse=productService.findProductById(productId);
            Product product=serverResponse.getData();
            int stock=product.getStock()-orderItem.getQuantity();
            ServerResponse serverResponse1=productService.reduceStock(productId,stock);
            if (!serverResponse1.isSuccess()){
                return serverResponse1;
            }
        }
        return ServerResponse.createServerResponseBySuccess();
    }
    //5.保存订单明细List<OrderItemVO>--order_item
    private ServerResponse saveOrderItems(List<OrderItem> orderItemList,Order order){
        for (OrderItem orderItem:orderItemList){
            orderItem.setOrderNo(order.getOrderNo());
        }
        int result=orderItemMapper.insertBatch(orderItemList);
        if (result!=orderItemList.size()){
            //有些订单明细没有插入成功
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"订单明细保存失败");
        }
        return ServerResponse.createServerResponseBySuccess();
    }
    //创建Order实体类并保存到DB--order
    private ServerResponse creatOrder(Integer userId,Integer shippingId,List<OrderItem> orderItemList){
        Order order=new Order();
        order.setUserId(userId);
        order.setShippingId(shippingId);
        order.setOrderNo(generatorOrderNo());
        order.setPayment(getOrderTotalPrice(orderItemList));
        order.setPaymentType(PaymentTypeEnum.PAYMENT_TYPE_ONLINE.getCode());
        order.setPostage(0);
        order.setStatus(OrderStatusEnum.ORDER_ON_PAY.getStatus());
        int result=orderMapper.insert(order);
        if (result<=0){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"订单保存失败");
        }
        return ServerResponse.createServerResponseBySuccess(order);
    }
    //生成订单号
    public Long generatorOrderNo(){
        return System.currentTimeMillis()+new Random().nextInt(100);
    }
    //计算订单的总价格
    public BigDecimal getOrderTotalPrice(List<OrderItem> orderItems){
        BigDecimal orderTotalPrice=new BigDecimal("0");
        for(OrderItem orderItem:orderItems){
            orderTotalPrice=BigDecimalUtils.add(orderItem.getTotalPrice().doubleValue(),orderTotalPrice.doubleValue());
        }
        return orderTotalPrice;
    }
    private  ServerResponse getCartOrderItem(Integer userId,List<Cart> cartList){
        List<OrderItem> orderItemList= Lists.newArrayList();
        for(Cart cart:cartList){
            OrderItem orderItem=new OrderItem();
            orderItem.setUserId(userId);
            ServerResponse<Product> serverResponse=productService.findProductById(cart.getProductId());
            if (!serverResponse.isSuccess()){
                return serverResponse;
            }
            Product product=serverResponse.getData();
            if(product==null){
                return  ServerResponse.createServerResponseByError("id为"+cart.getProductId()+"的商品不存在");
            }
            if(product.getStatus()!= ProductStatusEnum.PRODUCT_SALE.getStatus()){//商品下架
                return ServerResponse.createServerResponseByError("id为"+product.getId()+"的商品已经下架");
            }
            if(product.getStock()<cart.getQuantity()){//库存不足
                return ServerResponse.createServerResponseByError("id为"+product.getId()+"的商品库存不足");
            }
            orderItem.setQuantity(cart.getQuantity());
            orderItem.setCurrentUnitPrice(product.getPrice());
            orderItem.setProductId(product.getId());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setProductName(product.getName());
            orderItem.setTotalPrice(BigDecimalUtils.mul(product.getPrice().doubleValue(),cart.getQuantity().doubleValue()));
            orderItemList.add(orderItem);
        }
        return  ServerResponse.createServerResponseBySuccess(orderItemList);

    }
    private OrderItemVO assembleOrderItemVO(OrderItem orderItem){
        OrderItemVO orderItemVO=new OrderItemVO();
        if(orderItem!=null){
            orderItemVO.setQuantity(orderItem.getQuantity());
            orderItemVO.setCreateTime(DateUtils.dateToStr(orderItem.getCreateTime()));
            orderItemVO.setCurrentUnitPrice(orderItem.getCurrentUnitPrice());
            orderItemVO.setOrderNo(orderItem.getOrderNo());
            orderItemVO.setProductId(orderItem.getProductId());
            orderItemVO.setProductImage(orderItem.getProductImage());
            orderItemVO.setProductName(orderItem.getProductName());
            orderItemVO.setTotalPrice(orderItem.getTotalPrice());
        }
        return orderItemVO;
    }
}
