package com.fugui.train.member.Service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.fugui.train.common.commonResp.CommonResp;
import com.fugui.train.common.exception.BusinessException;
import com.fugui.train.common.exception.BusinessExceptionEnum;
import com.fugui.train.common.util.JwtUtil;
import com.fugui.train.common.util.SnowUtil;
import com.fugui.train.member.domain.Member;
import com.fugui.train.member.domain.MemberExample;
import com.fugui.train.member.mapper.MemberMapper;
import com.fugui.train.member.req.MemberLoginReq;
import com.fugui.train.member.req.MemberRegisterReq;
import com.fugui.train.member.req.MemberSendCodeReq;
import com.fugui.train.member.resp.MemberLoginResp;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {
    @Resource
    private MemberMapper memberMapper;
    public int count() {
        return Math.toIntExact(memberMapper.countByExample(null));
    }
    private final Logger LOG= LoggerFactory.getLogger(MemberService.class);

    public CommonResp<Long> register(MemberRegisterReq req) {
        //如果号码存在，返回异常
        Member memberDB = selectByMobile(req.getMobile());
        if(ObjectUtil.isNotEmpty(memberDB)){
             throw new BusinessException(BusinessExceptionEnum.MEMBER_MOBILE_EXIST);
         }
        //注册
        Member member = new Member();
        member.setId(SnowUtil.getSnowflaskNextId());
        member.setMobile(req.getMobile());
        memberMapper.insert(member);
        return new CommonResp<>(member.getId());
    }
    public void sendCode(MemberSendCodeReq req) {
        Member member=selectByMobile(req.getMobile());
        if(ObjectUtil.isNull(member)){
            member=new Member();
            member.setMobile(req.getMobile());
            member.setId(SnowUtil.getSnowflaskNextId());
            memberMapper.insert(member);
            LOG.info("手机号{}不存在，执行注册",req.getMobile());
        }
        else{
            LOG.info("手机号{}不存在，执行登录");
        }
        String code= RandomUtil.randomString(4);

    }
    private Member selectByMobile(String mobile) {
        MemberExample memberExample=new MemberExample();
        memberExample.createCriteria().andMobileEqualTo(mobile);
        List<Member> list=memberMapper.selectByExample(memberExample);
        if(CollUtil.isEmpty(list))return null;
        return list.get(0);
    }


    public CommonResp<MemberLoginResp> login(MemberLoginReq req) {
        Member member=selectByMobile(req.getMobile());
        if(ObjectUtil.isNull(member)){
            throw new BusinessException(BusinessExceptionEnum.MEMBER_MOBILE_EXIST);
        }
        if(!req.getCode().equals("8888")){
            throw new BusinessException(BusinessExceptionEnum.CODE_ERROR);
        }
        MemberLoginResp resp=BeanUtil.copyProperties(member,MemberLoginResp.class);
        resp.setToken(JwtUtil.createToken(resp.getId(),resp.getMobile()));
        return new CommonResp<>(resp);
    }
}
