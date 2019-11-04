package com.geng.controller;

import com.geng.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {
//   @Value("${limit.minMoney}")
////    private BigDecimal minMoney;
////    @Value("${limit.maxMoney}")
////    private BigDecimal maxMoney;
    @Autowired
    AppConfig appConfig;

    //@RequestMapping({"/login","/login1"})//多路径访问
    @ResponseBody  //以json格式返回
    @PostMapping({"/login","/login1"})
    public String login(@RequestParam(value = "username", required = false,defaultValue = "admin") String username){
        //required 可传可不传，如果不传值，则默认值为defaultValue

        //System.out.println();
       // System.out.println(username);
        //System.out.println(password);
        return username;

    }
    @ResponseBody       //@Controller注解需要有@ResponseBody注解， 如果使用@RestController注解，可用没有@ResponseBody注解
    @RequestMapping("/login/{username}/{password}")
    public String testrestful(@PathVariable("username") String username,@PathVariable("password") String password){
        return username+" "+password;
    }
}
