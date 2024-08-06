package com.example.demo.filter;

import com.alibaba.fastjson.JSONObject;
import com.example.demo.entity.LoginUser;
import com.example.demo.utils.JwtUtil;
import com.example.demo.utils.RedisCache;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

//我们需要自定义一个过滤器，这个过滤器会去获取请求头中的token，对token进行解析取出其中的userid
//使用userid去redis中获取对应的LoginUser对象
//然后封装Authentication对象存入SecurityContextHolder
//原版过滤器有时候有问题，这里选择继承spring提供的过滤器OncePerRequestFilter
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    @Autowired
    private RedisCache redisCache;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        //获取token
        //String token = request.getHeader("token");
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiI4MmY4NTlhZTUyNzE0ZmIyOGJhN2Q4YTIxMjZlNjI1MCIsInN1YiI6IjIiLCJpc3MiOiJ6dWtlIiwiaWF0IjoxNzIyOTM5OTYxLCJleHAiOjE3MjI5NDM1NjF9.gx0H90P-DqvHJnmBv0uNwalHRnjIPAhFxUWxEChoIX8";
        if (!StringUtils.hasText(token)) {
            //未登录，放行，后面的过滤器会检测SecurityContextHolder是否存有该用户
            filterChain.doFilter(request, response);
            return;
        }
        //解析token
        String userid;
        try {
            Claims claims = JwtUtil.parseJWT(token);
            userid = claims.getSubject();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("token非法");
        }
        //从redis中获取用户信息
        String redisKey = "login:" + userid;
        Object userObject = redisCache.getCacheObject(redisKey);
        if (Objects.isNull(userObject)) {
            throw new RuntimeException("用户未登录");
        }
        LoginUser loginUser = ((JSONObject) userObject).toJavaObject(LoginUser.class);
        //存入SecurityContextHolder
        //获取权限信息封装到Authentication中
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        //放行
        filterChain.doFilter(request, response);
    }
}
