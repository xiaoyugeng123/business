package com.geng.service.impl;

import com.geng.common.CheckEnum;
import com.geng.common.ResponseCode;
import com.geng.common.ServerResponse;
import com.geng.dao.CartMapper;
import com.geng.pojo.Cart;
import com.geng.pojo.Product;
import com.geng.service.ICartService;
import com.geng.service.IProductService;
import com.geng.utils.BigDecimalUtils;
import com.geng.vo.CartProductVO;
import com.geng.vo.CartVO;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.util.List;

@Service
public class CartServiceImpl implements ICartService {
    @Autowired
    IProductService productService;
    @Autowired
    CartMapper cartMapper;
    @Override
    public ServerResponse addProductCart(Integer userId, Integer productId, Integer count) {
        //1.参数非空判断
        if (productId==null){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"商品id不能为空");
        }
        if (count==null){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"商品数量不能为0");
        }
        //2.根据商品Id判断商品是否存在（库存）
        ServerResponse<Product> serverResponse=productService.findProductById(productId);
        if (!serverResponse.isSuccess()){//商品不存在
            return ServerResponse.createServerResponseByError(serverResponse.getStatus(),serverResponse.getMsg());
        }else{
            Product product=serverResponse.getData();
            if (product.getStock()<=0){
                return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"商品已售空");
            }
        }
        //3.判断商品是否在购物车
        Cart cart=cartMapper.findCartByuserIdAndProductId(userId,productId);
        if (cart==null){
            //添加
            Cart newCart=new Cart();
            newCart.setUserId(userId);
            newCart.setProductId(productId);
            newCart.setQuantity(count);
            newCart.setChecked(CheckEnum.CART_PRODUCT_CHECK.getCheck());
            int result=cartMapper.insert(newCart);
            if (result<=0){
                return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"添加失败");
            }
        }else{
            //更新商品在购物车中的数量
            cart.setQuantity(cart.getQuantity()+count);
            int result=cartMapper.updateByPrimaryKey(cart);
            if (result<=0){
                return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"更新失败");
            }
        }
        //4.封装购物车对象
        CartVO cartVO=getCartVO(userId);
        //5.返回CartVO
        return ServerResponse.createServerResponseBySuccess(cartVO);
    }

    @Override
    public ServerResponse<List<Cart>> findCartsByUseridAndChecked(Integer userId) {
        List<Cart> cartList=cartMapper.findCartsByUseridAndChecked(userId);
        return ServerResponse.createServerResponseBySuccess(cartList);
    }

    @Override
    public ServerResponse deleteBatch(List<Cart> cartList) {
        if (cartList==null){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"要删除的购物车商品不能为空");
        }
        int result=cartMapper.deleteBatch(cartList);
        if (result!=cartList.size()){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"购物车清空失败");
        }
        return ServerResponse.createServerResponseBySuccess();
    }

    private CartVO getCartVO(Integer userId){
        CartVO cartVO=new CartVO();
        //1.根据userid查询该用户的购物信息  List<Cart>
        List<Cart> cartList=cartMapper.findCartsByUserid(userId);
        if (cartList==null||cartList.size()==0){
            return cartVO;
        }
        //定义购物车商品总价格
        BigDecimal cartTotalPrice=new BigDecimal("0");
        //2.List<Cart>-->List<CartProductVO>
        List<CartProductVO> cartProductVOList= Lists.newArrayList();
        int limi_quantity=0;
        String limitQuantity=null;
        for (Cart cart:cartList){
            //Cart-->CartProduct
            CartProductVO cartProductVO=new CartProductVO();
            cartProductVO.setId(cart.getId());
            cartProductVO.setUserId(cart.getUserId());
            cartProductVO.setProductId(cart.getProductId());

            ServerResponse<Product> serverResponse=productService.findProductById(cart.getProductId());
            if (serverResponse.isSuccess()){
                Product product= (Product)serverResponse.getData();
                if (product.getStock()>=cart.getQuantity()){
                    limi_quantity=cart.getQuantity();
                    limitQuantity="LIMIT_NUM_SUCCESS";
                }else{
                    limi_quantity=product.getStock();
                    limitQuantity="LIMIT_NUM_FAIL";
                }
                cartProductVO.setQuantity(limi_quantity);
                cartProductVO.setLimitQuantity(limitQuantity);
                cartProductVO.setProductName(product.getName());
                cartProductVO.setProductSubtitle(product.getSubtitle());
                cartProductVO.setProductMainImage(product.getMainImage());
                cartProductVO.setProductPrice(product.getPrice());
                cartProductVO.setProductStatus(product.getStatus());
                cartProductVO.setProductTotalPrice(BigDecimalUtils.mul(product.getPrice().doubleValue(),cart.getQuantity()*1.0));
                cartProductVO.setProductStock(product.getStock());
                cartProductVO.setProductChecked(cart.getChecked());
                cartProductVOList.add(cartProductVO);
                if (cart.getChecked()==CheckEnum.CART_PRODUCT_CHECK.getCheck()){
                    //商品被选中
                    cartTotalPrice= BigDecimalUtils.add(cartTotalPrice.doubleValue(),cartProductVO.getProductTotalPrice().doubleValue());
                }
            }
        }
        cartVO.setCartProductVOList(cartProductVOList);
        //3.计算总的价格
         cartVO.setCarttotalprice(cartTotalPrice);

        //4.判断是否全选
        Integer isAllChecked=cartMapper.isAllChecked(userId);
        if (isAllChecked==0){
            //全选
            cartVO.setIsallchecked(true);
        }else{
            cartVO.setIsallchecked(false);
        }

        //5.构建cartvo
        return cartVO;

    }

}
