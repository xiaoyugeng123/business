package com.geng.controller.front;

import com.geng.common.ResponseCode;
import com.geng.common.ServerResponse;
import com.geng.pojo.Shipping;
import com.geng.pojo.UserInfo;
import com.geng.service.IShippingService;
import com.geng.utils.Const;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/shipping/")
public class ShippingController {
    @Autowired
    IShippingService shippingService;
    @RequestMapping(value = "add.do")
    public ServerResponse add(Shipping shipping, HttpSession session){
        UserInfo user=(UserInfo) session.getAttribute(Const.CURRENT_USER);
        if (user==null){
            return ServerResponse.createServerResponseByError(ResponseCode.NOT_LOGIN,"未登录");
        }
        shipping.setUserId(user.getId());
        return shippingService.add(shipping);
    }
}
