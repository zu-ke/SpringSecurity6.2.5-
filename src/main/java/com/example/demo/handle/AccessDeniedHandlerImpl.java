package com.example.demo.handle;

import com.alibaba.fastjson.JSON;
import com.example.demo.entity.RespBean;
import com.example.demo.entity.RespBeanEnum;
import com.example.demo.utils.WebUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

//处理认证失败

/**
 * @author zukedog@163.com
 * @date 2024/8/6 16:05
 */
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        //处理异常
        WebUtils.renderString(response, JSON.toJSONString(RespBean.error(RespBeanEnum.AUTHENTICATION_FAILED)));
    }
}
