package com.geng.dao;

import com.geng.pojo.UserInfo;
import org.apache.ibatis.annotations.Param;

public interface ILoginDao {
    public Integer isusernameexists(@Param("username") String username);
    public UserInfo findAdminByUsernameAndPassword(@Param("username") String username, @Param("password") String password);
}
