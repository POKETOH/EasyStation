package com.fugui.train.member.controller;

import com.fugui.train.common.commonResp.CommonResp;
import com.fugui.train.member.Service.MemberService;
import com.fugui.train.member.req.MemberLoginReq;
import com.fugui.train.member.req.MemberRegisterReq;
import com.fugui.train.member.req.MemberSendCodeReq;
import com.fugui.train.member.resp.MemberLoginResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/member")
public class MemberController {
    @Resource
    private MemberService memberService;
    @GetMapping("/count")
    public int count(){
        return memberService.count();
    }
    @PostMapping("/register")
    public CommonResp<Long> register(@Valid MemberRegisterReq req){
        return memberService.register(req);
    }
    @PostMapping("/sendCode")
    public CommonResp sendCode(@Valid@RequestBody MemberSendCodeReq req){
        memberService.sendCode(req);
        return new CommonResp();
    }
    @PostMapping("/login")
    public CommonResp<MemberLoginResp> login(@Valid@RequestBody MemberLoginReq req){
        return memberService.login(req);
    }
}
