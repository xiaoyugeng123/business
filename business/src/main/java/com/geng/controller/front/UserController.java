package com.geng.controller.front;

import com.geng.common.ResponseCode;
import com.geng.common.ServerResponse;
import com.geng.pojo.UserInfo;
import com.geng.service.IUserService;
import com.geng.utils.Const;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/*
  @Controller 是一种特殊化的@Component 类。
  @Controller 习惯于和@RequestMapping绑定来使用，后者是用来指定路由映射的。
  @ResponseBody告诉控制器返回对象会被自动序列化成JSON，并且传回HttpResponse这个对象。

  1.@controller 控制器（注入服务）
    用于标注控制层，相当于struts中的action层
  2、@service 服务（注入dao）
    用于标注服务层，主要用来进行业务的逻辑处理
  3、@repository（实现dao访问）
    用于标注数据访问层，也可以说用于标注数据访问组件，即DAO组件.
  4、@component （把普通pojo实例化到spring容器中，相当于配置文件中的 <bean id="" class=""/>）
    泛指各种组件，就是说当我们的类不属于各种归类的时候（不属于@Controller、@Services等的时候），我们就可以使用@Component来标注这个类。
 */
@RestController
@RequestMapping("/user/")
public class UserController {
    @Autowired    //@Autowired是Spring提供的一种注入Bean的方法。在Service类中定义的注入属性前加@Autowired
    IUserService userService;
    //注册
    @RequestMapping(value="register.do")
    public ServerResponse register(UserInfo user){

        return userService.register(user);

    }
    //登录
    @RequestMapping(value="login/{username}/{password}")
    public ServerResponse login(@PathVariable("username") String username,
                                @PathVariable("password") String password,
                                HttpSession session){
        ServerResponse serverResponse =userService.login(username,password,1);
        //判断是否登录成功
        if (serverResponse.isSuccess()){
                session.setAttribute(Const.CURRENT_USER,serverResponse.getData());
        }

        return serverResponse;
    }
    //根据用户名获取密保问题
    @RequestMapping("forget_get_question/{username}")
    public ServerResponse forget_get_question(@PathVariable("username") String username){
        return userService.forget_get_question(username);
    }
    //提交答案
    @RequestMapping("forget_check_answer.do")
    public ServerResponse forget_check_answer(String username,String question,String answer){
        return userService.forget_check_answer(username,question,answer);
    }
    //修改密码
    @RequestMapping("forget_reset_password.do")
    public ServerResponse forget_reset_password(String username,String newpassword,String forgettoken){
        return userService.forget_reset_password(username,newpassword,forgettoken);
    }
    //修改用户信息
    @RequestMapping("update_information.do")
    public ServerResponse update_information(UserInfo user,HttpSession session){
        UserInfo loginUser=(UserInfo)session.getAttribute(Const.CURRENT_USER);
        if (loginUser==null){
            return ServerResponse.createServerResponseBySuccess(ResponseCode.NOT_LOGIN,"未登录");
        }
        user.setId(loginUser.getId());
        ServerResponse serverResponse=userService.update_information(user);
        return serverResponse;
    }
}
