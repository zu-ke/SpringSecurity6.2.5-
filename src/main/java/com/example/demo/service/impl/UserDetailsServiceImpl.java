package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.entity.LoginUser;
import com.example.demo.entity.User;
import com.example.demo.mapper.MenuMapper;
import com.example.demo.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

//处理登录请求，前往数据库查询账密
//在没有写spring security（SecurityConfig）前，此时数据库存储的是铭文密码，并且密码前面加入{noop}，比如'{noop}123'，这是它的规定

/**
 * @author zukedog@163.com
 * @date 2024/8/5 15:10
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private MenuMapper menuMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //查询用户信息（认证）
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("user_name", username));
        //如果没有查询到用户，就抛出异常
        if (Objects.isNull(user)) {
            //此异常会被spring security的异常过滤器捕获到
            throw new RuntimeException("用户名或者密码错误");
        }

        //查询对应的权限信息（授权）
        //List<String> list = new ArrayList<>(Arrays.asList("test", "admin"));
        List<String> list = menuMapper.selectPermsByUserId(user.getId());

        //把数据封装成userDetails返回
        return new LoginUser(user, list);
    }
}
