package com.fugui.train.batch.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name="business")
public interface businessFeign {
    @GetMapping("/business/hello")
    String hello();
}