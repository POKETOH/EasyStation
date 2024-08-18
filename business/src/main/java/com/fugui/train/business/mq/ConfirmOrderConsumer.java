package com.fugui.train.business.mq;

import com.alibaba.fastjson.JSON;
import com.fugui.train.business.req.ConfirmOrderDoReq;
import com.fugui.train.business.service.ConfirmOrderService;
import jakarta.annotation.Resource;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(consumerGroup = "default",topic = "CONFIRM_ORDER")
public class ConfirmOrderConsumer implements RocketMQListener<MessageExt> {
@Resource
private ConfirmOrderService confirmOrderService;
    @Override
    public void onMessage(MessageExt messageExt) {
        byte[] body=messageExt.getBody();
        ConfirmOrderDoReq req= JSON.parseObject(new String(body), ConfirmOrderDoReq.class);
        MDC.put("LOG_ID", req.getLogId());
        confirmOrderService.doConfirm(req);
    }
}
