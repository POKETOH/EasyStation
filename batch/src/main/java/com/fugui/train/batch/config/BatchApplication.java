package com.fugui.train.batch.config;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

@SpringBootApplication
@ComponentScan("com.fugui")
@MapperScan("com.fugui.train.batch.mapper")
@EnableFeignClients("com.fugui.train.batch.feign")
@EnableCaching
public class BatchApplication {
    private static final Logger LOG=LoggerFactory.getLogger(BatchApplication.class);
    public static void main(String[] args) {
        SpringApplication app=new SpringApplication(BatchApplication.class);
        //从环境获得启动参数
        Environment environment = app.run(args).getEnvironment();
        //打印启动地址和端口号
        LOG.info("启动成功");
        LOG.info("端口号：http://127.0.0.1:{}{}",environment.getProperty("server.port"),environment.getProperty("server.servlet.context-path"));
    }

}
