package com.fugui.train.business.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kaptcha")
public class KaptchaController {


    @Resource
    public StringRedisTemplate stringRedisTemplate;

    @GetMapping("/image-code/{imageCodeToken}")
    public void imageCode(@PathVariable(value = "imageCodeToken") String imageCodeToken, HttpServletResponse response) throws Exception{
        LineCaptcha vCode = CaptchaUtil.createLineCaptcha(130, 38, 5, 10);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpge");
        String code = vCode.getCode();
        stringRedisTemplate.opsForValue().set(imageCodeToken,code);
        // 图形验证码写出，可以写出到文件，也可以写出到流
        vCode.write(response.getOutputStream());
        response.getOutputStream().close();
    }
}
