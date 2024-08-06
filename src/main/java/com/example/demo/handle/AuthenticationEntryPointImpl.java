package com.example.demo.handle;

import com.alibaba.fastjson.JSON;
import com.example.demo.entity.RespBean;
import com.example.demo.entity.RespBeanEnum;
import com.example.demo.utils.WebUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

//处理授权失败

@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        //处理异常
        WebUtils.renderString(response, JSON.toJSONString(RespBean.error(RespBeanEnum.LOGIN_ERROR)));
    }
}