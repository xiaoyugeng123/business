package com.geng.controller.backend;

import com.geng.common.ResponseCode;
import com.geng.common.RoleEnum;
import com.geng.common.ServerResponse;
import com.geng.pojo.Product;
import com.geng.pojo.UserInfo;
import com.geng.service.IProductService;
import com.geng.utils.Const;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping(value = "/manage/product/")
public class ProductCotroller {
    @Autowired
    IProductService productService;
    //商品添加&更新
    @RequestMapping(value = "save.do")
    public ServerResponse addOrUpdate(Product product, HttpSession session){
        UserInfo user=(UserInfo) session.getAttribute(Const.CURRENT_USER);
        if (user==null){
            return ServerResponse.createServerResponseByError(ResponseCode.NOT_LOGIN,"未登录");
        }
        int role=user.getRole();
        if (role== RoleEnum.ROLE_USER.getRole()){
            return ServerResponse.createServerResponseByError(ResponseCode.NOT_LOGIN,"权限不足");
        }
        return productService.addOrUpdate(product);
    }
    //搜索商品
    @RequestMapping(value = "search.do")
    public ServerResponse search(@RequestParam(name="productName",required = false) String productName,
                                 @RequestParam(name="productId",required = false) Integer productId,
                                 @RequestParam(name="pageNum",required = false,defaultValue = "1") Integer pageNum,
                                 @RequestParam(name="pageSize",required = false,defaultValue = "10") Integer pageSize,HttpSession session){
        UserInfo user=(UserInfo) session.getAttribute(Const.CURRENT_USER);
        if (user==null){
            return ServerResponse.createServerResponseByError(ResponseCode.NOT_LOGIN,"未登录");
        }
        int role=user.getRole();
        if (role== RoleEnum.ROLE_USER.getRole()){
            return ServerResponse.createServerResponseByError(ResponseCode.NOT_LOGIN,"权限不足");
        }
        return productService.search(productName, productId, pageNum, pageSize);

    }
    @RequestMapping(value = "/{productId}")
    public ServerResponse detail(@PathVariable("productId") Integer productId,HttpSession session){
        UserInfo user=(UserInfo) session.getAttribute(Const.CURRENT_USER);
        if (user==null){
            return ServerResponse.createServerResponseByError(ResponseCode.NOT_LOGIN,"未登录");
        }
        int role=user.getRole();
        if (role== RoleEnum.ROLE_USER.getRole()){
            return ServerResponse.createServerResponseByError(ResponseCode.NOT_LOGIN,"权限不足");
        }
        return productService.detail(productId);
    }
}
