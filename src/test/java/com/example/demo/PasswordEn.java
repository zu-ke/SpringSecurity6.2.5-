package com.example.demo;

import com.example.demo.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * @author zukedog@163.com
 * @date 2024/8/5 16:09
 */
public class PasswordEn {

    @Test
    public void enpwd(){
        //加密
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String enPwd = encoder.encode("123");
        System.out.println(enPwd);
        //校验
        System.out.println(encoder.matches("123", enPwd));
        System.out.println(encoder.matches("12345", enPwd));
    }
}
