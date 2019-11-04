package com.geng.service;

import com.geng.common.ServerResponse;
import com.geng.pojo.Category;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

public interface ICategoryService {
    //添加类别
    public ServerResponse addCategory(Category category);
    //修改类别  categoryId,categoryName,categoryUrl
    public ServerResponse updateCategory(Category category);
    //查看类别  categoryId,categoryName,categoryUrl
    public ServerResponse getCategoryById(Integer categoryId);
    //查看类别  categoryId,categoryName,categoryUrl
    public ServerResponse deepCategory(Integer categoryId);
    //根据Id查询类别
    public ServerResponse<Category> selectCategory(Integer categoryId);
}
