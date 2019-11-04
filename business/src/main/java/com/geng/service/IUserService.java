package com.geng.service;

import com.geng.common.ServerResponse;
import com.geng.pojo.UserInfo;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

public interface IUserService {
    //注册接口
    public ServerResponse register(UserInfo user);
    //登录接口   type:1:普通用户   0：管理员
    public ServerResponse login(String username,String password,int type);
    public ServerResponse forget_get_question(@PathVariable("username") String username);
    //提交答案
    public ServerResponse forget_check_answer(String username,String question,String answer);
    //修改密码
    public ServerResponse forget_reset_password(String username,String newpassword,String forgettoken);
    //修改信息
    public ServerResponse update_information(UserInfo user);
}
