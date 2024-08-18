
package com.fugui.train.business.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.fugui.train.business.req.ConfirmOrderDoReq;
import com.fugui.train.business.service.BeforeConfirmOrderService;
import com.fugui.train.business.service.ConfirmOrderService;
import com.fugui.train.common.commonResp.CommonResp;
import com.fugui.train.common.exception.BusinessExceptionEnum;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/confirm-order")
public class ConfirmOrderController {

    @Resource
    private BeforeConfirmOrderService beforeConfirmOrderService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ConfirmOrderService confirmOrderService;

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderController.class);

    @SentinelResource(value = "confirmOrderDo", blockHandler = "doConfirmBlock")
    @PostMapping("/do")
    public CommonResp<Object> doConfirm(@Valid @RequestBody ConfirmOrderDoReq req) throws InterruptedException {
        String imageCode = req.getImageCode();
        String imageCodeToken = req.getImageCodeToken();
        if(!stringRedisTemplate.opsForValue().get(imageCodeToken).equals(imageCode)){
            CommonResp<Object> commonResp=new CommonResp<>();
            commonResp.setSuccess(false);
            commonResp.setMessage("图片验证码错误");
        }else{
            stringRedisTemplate.delete(imageCodeToken);
        }
        Long id = beforeConfirmOrderService.beforeDoConfirm(req);
        return new CommonResp<>(String.valueOf(id));
    }
    @GetMapping("/query-line-count/{id}")
    public CommonResp<Integer> queryLineCount(@PathVariable Long id) {
        Integer count = confirmOrderService.queryLineCount(id);
        return new CommonResp<>(count);
    }

    public CommonResp<Object> doConfirmBlock(ConfirmOrderDoReq req, BlockException e) {
        LOG.info("购票被限流");
        CommonResp<Object> commonResp = new CommonResp<>();
        commonResp.setSuccess(false);
        commonResp.setMessage(BusinessExceptionEnum.BUSINESS_NOT_GET_LOCK.getDesc());
        return commonResp;
    }
}
