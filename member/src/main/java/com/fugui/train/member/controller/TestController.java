package com.fugui.train.member.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@RefreshScope//该注解会识别该区域 并实时更新nacos中的配置信息
@RestController
public class TestController {

    @Value("${test.nacos}")
    private String testNacos;
    @Autowired
    private Environment environment;

    @GetMapping("/hello")
    public String hello() {
        String port = environment.getProperty("local.server.port");
        return String.format("Hello %s! 端口：%s", testNacos, port);
    }

}
