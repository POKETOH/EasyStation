package com.fugui.train.member.controller;

import com.fugui.train.common.context.LoginMemberContext;
import com.fugui.train.common.commonResp.CommonResp;
import com.fugui.train.common.resp.PageResp;
import com.fugui.train.member.domain.Passenger;
import com.fugui.train.member.req.PassengerQueryReq;
import com.fugui.train.member.req.PassengerSaveReq;
import com.fugui.train.member.resp.PassengerQueryResp;
import com.fugui.train.member.Service.PassengerService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/passenger")
public class PassengerController {

    @Resource
    private PassengerService passengerService;

    @PostMapping("/save")
    public CommonResp<Object> save(@Valid @RequestBody PassengerSaveReq req) {
        passengerService.save(req);
        return new CommonResp<>();
    }

    @GetMapping("/query-list")
    public CommonResp<PageResp<PassengerQueryResp>> queryList(@Valid PassengerQueryReq req) {
        req.setMemberId(LoginMemberContext.getId());
        PageResp<PassengerQueryResp> list = passengerService.queryList(req);
        return new CommonResp<>(list);
    }
    @GetMapping("/query-mine")
    public CommonResp<List<Passenger>> queryMine() {
        List<Passenger> list = passengerService.queryMine();
        return new CommonResp<>(list);
    }

    @DeleteMapping("/delete/{id}")
    public CommonResp<Object> delete(@PathVariable Long id) {
        passengerService.delete(id);
        return new CommonResp<>();
    }

}
