package com.example.demo.service;

import com.example.demo.entity.RespBean;
import com.example.demo.entity.User;

/**
 * @author zukedog@163.com
 * @date 2024/8/5 16:32
 */
public interface LoginService {
    RespBean logout();
    RespBean login(User user);
}
