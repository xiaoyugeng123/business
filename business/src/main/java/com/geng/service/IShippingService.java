package com.geng.service;


import com.geng.common.ServerResponse;
import com.geng.pojo.Shipping;

public interface IShippingService {
    public ServerResponse add(Shipping shipping);
    public ServerResponse findShippingById(Integer shippingid);
}
