package com.fugui.train.common.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fugui.train.common.commonResp.MemberLoginResp;
import com.fugui.train.common.context.LoginMemberContext;
import com.fugui.train.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
@Component
public class MemberInterceptor implements HandlerInterceptor {
    Logger LOG= LoggerFactory.getLogger(MemberInterceptor.class);
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //从request取token
        String token = request.getHeader("token");

        if(StrUtil.isNotBlank(token)){
            JSONObject member = JwtUtil.getJSONObject(token);
            //如果不为空，记录日志
            LOG.info("当前登录用户信息为{}",member);
            LOG.info("当前登录token为{}",token);
            //把用户信息设置到线程变量中
            LoginMemberContext.setMember(JSONUtil.toBean(member, MemberLoginResp.class));
        }
        return true;
    }
}
