package com.fugui.train.gateway.config;

import com.fugui.train.gateway.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;



@Component
public class LoginMemberFilter implements GlobalFilter, Ordered {
    private Logger LOG= LoggerFactory.getLogger(LoginMemberFilter.class);
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token=exchange.getRequest().getHeaders().getFirst("token");
        String path=exchange.getRequest().getURI().getPath();
        //设置排除链接，如果不存则不进行校验
        if(path.contains("kaptcha")||path.contains("redis")||path.contains("hello")||path.contains("admin")||path.contains("login")||path.contains("sendCode")||path.contains("admin")) {
            LOG.info(path+"不需要登录校验");
            return chain.filter(exchange);
        }
        if(token==null||token.equals("")){
            LOG.info(path+":未登录");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        if(JwtUtil.validate(token)){
            LOG.info(path+"token校验成功");
            return chain.filter(exchange);
        }
        else{
            LOG.info(path+"token校验失败");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
