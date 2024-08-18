package com.fugui.train.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

@SpringBootApplication
@ComponentScan("com.fugui")
public class GateApplication {
    private static final Logger LOG=LoggerFactory.getLogger(GateApplication.class);
    public static void main(String[] args) {
        SpringApplication app=new SpringApplication(GateApplication.class);
        //从环境获得启动参数
        Environment environment = app.run(args).getEnvironment();
        //打印启动地址和端口号
        LOG.info("启动成功");
        LOG.info("地址：http://127.0.0.1:{}",environment.getProperty("server.port"));
    }

}
