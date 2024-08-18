package com.fugui.train.member.controller.admin;


import com.fugui.train.common.commonResp.CommonResp;
import com.fugui.train.common.resp.PageResp;
import com.fugui.train.member.Service.TicketService;
import com.fugui.train.member.req.TicketQueryReq;
import com.fugui.train.member.resp.TicketQueryResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/admin/ticket")
public class TicketAdminController {

    @Resource
    private TicketService ticketService;

    @GetMapping("/query-list")
    public CommonResp<PageResp<TicketQueryResp>> queryList(@Valid TicketQueryReq req) {
        PageResp<TicketQueryResp> list = ticketService.queryList(req);
        return new CommonResp<>(list);
    }

}
