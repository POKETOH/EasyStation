package com.fugui.train.member.feign;


import com.fugui.train.common.commonResp.CommonResp;
import com.fugui.train.common.req.MemberTicketReq;
import com.fugui.train.member.Service.TicketService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

;

@RestController
@RequestMapping("/feign/ticket")
public class FeignTicketController {

    @Autowired
    private TicketService ticketService;

    @PostMapping("/save")
    public CommonResp<Object> save(@Valid @RequestBody MemberTicketReq req) throws Exception {
        ticketService.save(req);
        return new CommonResp<>();
    }

}
