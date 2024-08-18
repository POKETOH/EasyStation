package com.fugui.train.batch.controller;

import com.fugui.train.batch.feign.businessFeign;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Test {
    @Resource
    private businessFeign feign;
    @GetMapping("/hello")
    public String hello(){
        return feign.hello();
    }
}