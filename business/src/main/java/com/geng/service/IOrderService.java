package com.geng.service;

import com.geng.common.ServerResponse;

import java.util.Map;

public interface IOrderService {
    //创建订单
    public ServerResponse createOrder(Integer userId,Integer shippingId);
    //支付
    public ServerResponse pay(Integer userId,Long orderNo);
    //支付回调接口
    public String callback(Map<String,String> requestParams);
}
