package com.geng.service.impl;

import com.geng.common.ResponseCode;
import com.geng.common.ServerResponse;
import com.geng.dao.CategoryMapper;
import com.geng.pojo.Category;
import com.geng.service.ICategoryService;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Service
public class CategoryServiceImpl implements ICategoryService {
    @Autowired
    CategoryMapper categoryMapper;
    @Override
    public ServerResponse addCategory(Category category) {
        if (category==null){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"参数不能为空");
        }
        int result=categoryMapper.insert(category);
        if (result<=0){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"添加品类失败");
        }
        return ServerResponse.createServerResponseBySuccess();
    }

    @Override
    public ServerResponse updateCategory(Category category) {
        if (category==null){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"参数不能为空");
        }
        if (category.getId()==null){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"类别Id必传");
        }
        int result=categoryMapper.updateByPrimaryKey(category);
        if (result<=0){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"跟新品类失败");
        }
        return ServerResponse.createServerResponseBySuccess();
    }

    @Override
    public ServerResponse getCategoryById(Integer categoryId) {
        if (categoryId==null){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"id必传");
        }
        List<Category> categoryList=categoryMapper.selectCategoryById(categoryId);
        return ServerResponse.createServerResponseBySuccess(categoryList,"成功");
    }

    @Override
    public ServerResponse deepCategory(Integer categoryId) {
        if (categoryId==null){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"id必传");
        }
        Set<Category> categorySet= Sets.newHashSet();
        //递归查询
        Set<Category> categorySet1=findAllChildCategory(categorySet,categoryId);
        Set<Integer> categoryIds=Sets.newHashSet();
        Iterator<Category> iterator=categorySet1.iterator();
        while (iterator.hasNext()){
            Category category=iterator.next();
            categoryIds.add(category.getId());
        }
        return ServerResponse.createServerResponseBySuccess(categoryIds);
    }

    @Override
    public ServerResponse<Category> selectCategory(Integer categoryId) {
        if (categoryId==null){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"类别id必传");
        }
        Category category=categoryMapper.selectByPrimaryKey(categoryId);
        return ServerResponse.createServerResponseBySuccess(category);
    }

    public Set<Category> findAllChildCategory(Set<Category> categorySet,Integer categoryId){
        //查看categoryId的类别信息
        Category category=categoryMapper.selectByPrimaryKey(categoryId);
        if (category!=null){
            categorySet.add(category);
        }
        //查看categoryId的平级子类
        List<Category> categoryList=categoryMapper.selectCategoryById(categoryId);
        if (categoryList!=null&&categoryList.size()>0){
            for (Category category1:categoryList){
                findAllChildCategory(categorySet,category1.getId());
            }
        }
        return categorySet;
    }
}
