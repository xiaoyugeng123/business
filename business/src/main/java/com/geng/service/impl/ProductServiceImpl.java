package com.geng.service.impl;

import com.geng.common.ResponseCode;
import com.geng.common.ServerResponse;
import com.geng.dao.ProductMapper;
import com.geng.pojo.Category;
import com.geng.pojo.Product;
import com.geng.service.ICategoryService;
import com.geng.service.IProductService;
import com.geng.utils.DateUtils;
import com.geng.vo.ProductDetailVO;
import com.geng.vo.ProductListVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;


import javax.servlet.http.HttpSession;
import java.util.List;

@Service
public class ProductServiceImpl implements IProductService {
    @Autowired
    ICategoryService categoryService;
    @Autowired
    ProductMapper productMapper;
    @Value("${business.imageHost}")
    private String imageHost;
    @Override
    public ServerResponse addOrUpdate(Product product) {
        if (product==null){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"参数不能为空");
        }
        //subimages 1.jpg 2.jpg 3.png
        //2.设置商品的主图 sub_images-->1.jpg 2.jpg 3.png
        String subImages=product.getSubImages();
        if (subImages!=null&&!subImages.equals("")){
            String[] subImageArr=subImages.split(",");
            if (subImageArr.length>0){
                //设置商品的主图
                product.setMainImage(subImageArr[0]);
            }
        }
        Integer productId=product.getId();
        if (productId==null)
        {
            //添加
            int result=productMapper.insert(product);
            if (result<=0){
                return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"添加失败");
            }else{
                return ServerResponse.createServerResponseBySuccess();
            }
        }else{
            //更新
            int result=productMapper.updateByPrimaryKey(product);
            if (result<=0){
                return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"更新失败");
            }else{
                return ServerResponse.createServerResponseBySuccess();
            }
        }
    }

    @Override
    public ServerResponse search(String productName, Integer productId, Integer pageNum, Integer pageSize) {
        if (productName!=null){
            productName="%"+productName+"%";
        }
        Page page=PageHelper.startPage(pageNum,pageSize);
        List<Product> productList=productMapper.findProductsByNameAndId(productId,productName);
        List<ProductListVO> prodectListVOList;
        prodectListVOList = Lists.newArrayList();
        //List<Product>  -->  List<ProductListVO>
        if(productList!=null&&productList.size()>0){
            for (Product product:productList){
                //product->productListVO
                ProductListVO productListVO=assembleProductListVO(product);
                prodectListVOList.add(productListVO);
            }
        }
        PageInfo pageInfo=new PageInfo(page);
        return ServerResponse.createServerResponseBySuccess(pageInfo);
    }
    private ProductListVO assembleProductListVO(Product product){
        ProductListVO productListVO=new ProductListVO();
        productListVO.setId(product.getId());
        productListVO.setCategoryId(product.getCategoryId());
        productListVO.setMainImage(product.getMainImage());
        productListVO.setName(product.getName());
        productListVO.setPrice(product.getPrice());
        productListVO.setStatus(product.getStatus());
        productListVO.setSubtitle(product.getSubtitle());
        return  productListVO;
    }
    public ServerResponse<ProductDetailVO> detail(Integer productId){
        if (productId==null){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"参数不能为空");
        }
        Product product=productMapper.selectByPrimaryKey(productId);
        if (product==null){
            return ServerResponse.createServerResponseBySuccess();
        }
        //product  -->  productDetailVo
        ProductDetailVO productDetailVO=assembleProductDetailVO(product);
        return ServerResponse.createServerResponseBySuccess(productDetailVO);
    }

    @Override
    public ServerResponse<Product> findProductById(Integer productId) {
        if (productId==null){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"商品id必须传值");
        }
        Product product=productMapper.selectByPrimaryKey(productId);
        if (product==null){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"商品不存在");
        }
        return ServerResponse.createServerResponseBySuccess(product);
    }

    @Override
    public ServerResponse reduceStock(Integer productId, Integer stock) {
        if (productId==null){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"商品id必须传值");
        }
        if (stock==null){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"库存参数必须传值");
        }
        int result=productMapper.reduceProductStock(productId,stock);
        if (result<=0){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"扣库存失败");
        }
        return ServerResponse.createServerResponseBySuccess();
    }

    private ProductDetailVO assembleProductDetailVO(Product product){
        ProductDetailVO productDetailVO=new ProductDetailVO();
        productDetailVO.setCategoryId(product.getCategoryId());
        productDetailVO.setCreateTime(DateUtils.dateToStr(product.getCreateTime()));
        productDetailVO.setDetail(product.getDetail());
        productDetailVO.setImageHost(imageHost);
        productDetailVO.setName(product.getName());
        productDetailVO.setMainImage(product.getMainImage());
        productDetailVO.setId(product.getId());
        productDetailVO.setPrice(product.getPrice());
        productDetailVO.setStatus(product.getStatus());
        productDetailVO.setStock(product.getStock());
        productDetailVO.setSubImages(product.getSubImages());
        productDetailVO.setSubtitle(product.getSubtitle());
        productDetailVO.setUpdateTime(DateUtils.dateToStr(product.getUpdateTime()));
    //    Category category= categoryMapper.selectByPrimaryKey(product.getCategoryId());
        ServerResponse<Category> serverResponse= categoryService.selectCategory(product.getCategoryId());
        Category category=serverResponse.getData();
        if (category!=null){
            productDetailVO.setParentCategoryId(category.getParentId());
        }
        return productDetailVO;
    }
}
