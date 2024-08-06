package com.example.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

//响应状态码和信息定义

@Getter
@AllArgsConstructor
public enum RespBeanEnum {

    //通用信息
    success(200, "操作成功"),
    ERROR(500, "服务端异常"),

    //用户登录
    LOGIN_SUCCESS(200, "登录成功"),
    LOGOUT_SUCCESS(200, "退出登录成功"),
    LOGIN_ERROR(400, "账号或密码错误"),

    //认证或授权
    AUTHENTICATION_FAILED(401,"账号权限不足");

    private final Integer code;
    private final String msg;
}
