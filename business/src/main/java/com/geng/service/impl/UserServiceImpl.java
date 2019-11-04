package com.geng.service.impl;

import com.geng.common.ResponseCode;
import com.geng.common.RoleEnum;
import com.geng.common.ServerResponse;
import com.geng.dao.UserInfoMapper;
import com.geng.pojo.UserInfo;
import com.geng.service.IUserService;
import com.geng.utils.MD5Utils;
import com.geng.utils.TokenCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserServiceImpl implements IUserService {
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Override
    public ServerResponse register(UserInfo user) {
        //1.参数校验
        if(user==null){
            return ServerResponse.createServerResponseByError(ResponseCode.PARAM_NOT_NULL,"参数不能为空");
        }
        //2.判断用户名是否存在
        int result=userInfoMapper.isexistsusername(user.getUsername());
        if(result>0){
            return ServerResponse.createServerResponseByError(ResponseCode.USERNAME_EXISTS,"用户名已存在");

        }
        //3.判断邮箱是否存在
        int resultemail=userInfoMapper.isexistsemail(user.getEmail());
        if(resultemail>0){//邮箱已存在
            return ServerResponse.createServerResponseByError(ResponseCode.EMAIL_EXISTS,"邮箱已存在");

        }
        //4.密码加密，设置用户角色

        user.setPassword(MD5Utils.getMD5Code(user.getPassword()));
        user.setRole(RoleEnum.ROLE_USER.getRole());
        //5.注册
        int insertResult=userInfoMapper.insert(user);
        if(insertResult<=0){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"注册失败");
        }
        //6.返回

        return ServerResponse.createServerResponseBySuccess();
    }

    @Override
    public ServerResponse login(String username, String password,int type) {
        //1.参数校验
        if(username==null||username==""){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"用户名不能为空");
        }
        if(password==null||password==""){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"密码不能为空");
        }
        //2.判断用户名是否存在
        int result=userInfoMapper.isexistsusername(username);
        if(result<=0){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"用户名不存在");
        }
        //密码加密
        password= MD5Utils.getMD5Code(password);
        //登录
        UserInfo user=userInfoMapper.findUserByUsernameAndPassword(username,password);
        if (user==null){
            //密码错误
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"密码错误");
        }
        if (type==0){//管理员
            if (user.getRole()==RoleEnum.ROLE_USER.getRole()){  //没有管理员权限
                return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"登录权限不足");

            }
        }
        return ServerResponse.createServerResponseBySuccess(user);
    }

    @Override
    public ServerResponse forget_get_question(String username) {
        //1.参数非空校验
        if(username==null||username==""){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"用户名不能为空");
        }
        //2.根据用户名查询问题
        String question=userInfoMapper.forget_get_question(username);
        if (question==null){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"没有查询到密保问题");
        }
        //3.返回结果
        return ServerResponse.createServerResponseBySuccess(question);
    }

    @Override
    public ServerResponse forget_check_answer(String username, String question, String answer) {
        //1.非空校验
        if(username==null||username==""){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"用户名不能为空");
        }
        if(question==null||question==""){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"密保问题不能为空");
        }
        if(answer==null||answer==""){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"密保答案不能为空");
        }
        //2.校验答案是否正确
        int result=userInfoMapper.forget_check_answer(username,question,answer);
        if (result<=0){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"答案错误");
        }
        //3.返回结果
        //生成token
        String token=UUID.randomUUID().toString();
        TokenCache.set("username"+username, token);
        return ServerResponse.createServerResponseBySuccess(ResponseCode.SUCCESS,token);
    }

    @Override
    public ServerResponse forget_reset_password(String username, String newpassword,String forgettoken) {
        //1.非空校验
        if(username==null||username==""){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"用户名不能为空");
        }
        if(newpassword==null||newpassword==""){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"新密码不能为空");
        }
        if(forgettoken==null||forgettoken==""){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"token不能为空");
        }
        //是否修改的是自己的账号
        String token=TokenCache.get("username"+username);
        if (token==null){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"不能修改别人的密码或者token已过期");
        }
        if (!token.equals(forgettoken)){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"无效的token");
        }
        int result=userInfoMapper.forget_reset_password(username,MD5Utils.getMD5Code(newpassword));
        if (result<=0){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"密码修改失败");
        }
        return ServerResponse.createServerResponseBySuccess();
    }
    //修改信息
    public ServerResponse update_information(UserInfo user){
        //1.非空校验
        if(user==null){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"参数不能为空");
        }
        int result=userInfoMapper.updateUserByActive(user);
        if (result<=0){
            return ServerResponse.createServerResponseByError(ResponseCode.ERROR,"修改失败");
        }
        return ServerResponse.createServerResponseBySuccess();
    }
}
