package com.fugui.train.business.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

import java.util.LinkedList;
import java.util.List;

@SpringBootApplication
@ComponentScan("com.fugui")
@MapperScan("com.fugui.train.business.mapper")
@EnableFeignClients("com.fugui.train.business.feign")
@EnableCaching
public class BusinessApplication {
    private static final Logger LOG=LoggerFactory.getLogger(BusinessApplication.class);
    public static void main(String[] args) {
        SpringApplication app=new SpringApplication(BusinessApplication.class);
        //从环境获得启动参数
        Environment environment = app.run(args).getEnvironment();
        //打印启动地址和端口号
        LOG.info("启动成功");
        LOG.info("端口号：http://127.0.0.1:{}{}",environment.getProperty("server.port"),environment.getProperty("server.servlet.context-path"));
        initFlowRules();
        LOG.info("已定义限流");
    }

    private static void initFlowRules() {
        List<FlowRule> rules=new LinkedList<>();
        FlowRule rule=new FlowRule();
        rule.setResource("confirmOrderDo");
        rule.setCount(1);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rules.add(rule);
        FlowRuleManager.loadRules(rules);
    }
}
