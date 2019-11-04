package com.geng.service;

import com.geng.common.ServerResponse;
import com.geng.pojo.Product;
import com.geng.vo.ProductDetailVO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

public interface IProductService {
    public ServerResponse addOrUpdate(Product product);
    //后台商品搜索
    public ServerResponse search(String productName, Integer productId, Integer pageNum, Integer pageSize);
    //商品详情
    public ServerResponse<ProductDetailVO> detail(Integer productId);
    //根据商品id查询商品信息
    public ServerResponse<Product> findProductById(Integer productId);
    //扣库存
    public ServerResponse reduceStock(Integer productId,Integer stock);
}
