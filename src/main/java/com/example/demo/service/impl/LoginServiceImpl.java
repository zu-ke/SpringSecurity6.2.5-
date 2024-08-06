package com.example.demo.service.impl;

import com.example.demo.entity.LoginUser;
import com.example.demo.entity.RespBean;
import com.example.demo.entity.RespBeanEnum;
import com.example.demo.entity.User;
import com.example.demo.service.LoginService;
import com.example.demo.utils.JwtUtil;
import com.example.demo.utils.RedisCache;
import jakarta.annotation.Resource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


/**
 * @author zukedog@163.com
 * @date 2024/8/5 16:36
 */
@Service
public class LoginServiceImpl implements LoginService {
    @Resource
    private AuthenticationManager authenticationManager;
    @Resource
    private RedisCache redisCache;

    @Override
    public RespBean logout() {
        //获取SecurityContextHolder中的用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String userid = loginUser.getUser().getId().toString();
        //删除redis中的值
        redisCache.deleteObject("login:" + userid);
        return RespBean.success(RespBeanEnum.LOGOUT_SUCCESS);
    }

    @Override
    public RespBean login(User user) {
        //通过AuthenticationManager的authenticate方法来进行用户认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUserName(), user.getPassword())
        );
        //如果认证没成功，给出相应提示
        if (Objects.isNull(authentication)) {
            throw new RuntimeException("登录失败");
        }
        //如果认证成功的话要生成一个jwt，放入响应中返回，并且把完整的用户信息出入redis，userId作为key
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String userId = loginUser.getUser().getId().toString();
        String jwt = JwtUtil.createJWT(userId);
        HashMap<String, String> map = new HashMap<>();
        map.put("jwt", jwt);
        redisCache.setCacheObject("login:" + userId, loginUser, 30, TimeUnit.MINUTES);
        return RespBean.success(RespBeanEnum.LOGIN_SUCCESS, map);
    }
}
