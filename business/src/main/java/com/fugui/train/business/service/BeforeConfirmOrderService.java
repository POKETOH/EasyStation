package com.fugui.train.business.service;

import cn.hutool.core.date.DateTime;
import cn.hutool.log.Log;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.fugui.train.business.domain.ConfirmOrder;
import com.fugui.train.business.enums.ConfirmOrderStatusEnum;
import com.fugui.train.business.enums.RocketMQTopicEnum;
import com.fugui.train.business.mapper.ConfirmOrderMapper;
import com.fugui.train.business.req.ConfirmOrderDoReq;
import com.fugui.train.common.context.LoginMemberContext;
import com.fugui.train.common.exception.BusinessException;
import com.fugui.train.common.exception.BusinessExceptionEnum;
import com.fugui.train.common.util.SnowUtil;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.Redisson;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class BeforeConfirmOrderService {
    @Resource
    private ConfirmOrderMapper confirmOrderMapper;
    @Resource
    private SkTokenService SkTokenService;
    @Resource
    private Redisson redisson;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Resource
    private RedisTemplate redisTemplate;

    @SentinelResource(value = "beforeDoConfirm", blockHandler = "beforDoConfirm")
    public Long beforeDoConfirm(ConfirmOrderDoReq req) throws InterruptedException {
        req.setMemberId(LoginMemberContext.getId());
        boolean getSkToken = SkTokenService.skTokenVail(req.getDate(), req.getTrainCode(), req.getMemberId());
        if (!getSkToken) {
            throw new BusinessException(BusinessExceptionEnum.NOT_GET_SKTOKEN);
        }

        DateTime now = DateTime.now();
        String start = req.getStart();
        String end = req.getEnd();
        String trainCode = req.getTrainCode();
        Long memberId = req.getMemberId();
        //保存确认订单表，存入记录
        ConfirmOrder confirmOrder = new ConfirmOrder();
        confirmOrder.setId(SnowUtil.getSnowflaskNextId());
        confirmOrder.setMemberId(memberId);
        confirmOrder.setDate(now);
        confirmOrder.setTrainCode(trainCode);
        confirmOrder.setStart(start);
        confirmOrder.setEnd(end);
        confirmOrder.setDailyTrainTicketId(req.getDailyTrainTicketId());
        confirmOrder.setStatus(ConfirmOrderStatusEnum.INIT.getCode());
        confirmOrder.setCreateTime(now);
        confirmOrder.setUpdateTime(now);
        confirmOrder.setTickets(JSON.toJSONString(req.getTickets()));
        //发送mq
        confirmOrderMapper.insert(confirmOrder);
        req.setLogId(String.valueOf(Log.get("LOG_ID")));
        String reqJson = JSON.toJSONString(req);
        rocketMQTemplate.convertAndSend(RocketMQTopicEnum.CONFIRM_ORDER.getCode(), reqJson);
        return confirmOrder.getId();
    }

    public void beforeDoConfirmBlock(ConfirmOrderDoReq req, BlockException e) {
        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION);
    }

}
