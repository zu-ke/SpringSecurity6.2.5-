package com.example.demo.controller;

import com.example.demo.entity.RespBean;
import com.example.demo.entity.User;
import com.example.demo.service.LoginService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

//用户登录控制类

/**
 * @author zukedog@163.com
 * @date 2024/8/4 10:32
 */
@RestController
public class LoginController {

    @Resource
    private LoginService loginService;

    @GetMapping("/user/logout")
    public RespBean logout() {
        return loginService.logout();
    }

    @PostMapping("/user/login")
    public RespBean login(@RequestBody User user) {
        return loginService.login(user);
    }

    @GetMapping("/hi")
    //如果用户用test权限，就让该用户访问
    //@PreAuthorize("hasAuthority('system:test:list11')")
    public RespBean hi() {
        return RespBean.success("hi");
    }

    @GetMapping("/cors")
    public RespBean testCors() {
        return RespBean.success("cors");
    }
}
