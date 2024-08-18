package com.fugui.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.fugui.train.business.domain.*;
import com.fugui.train.business.enums.ConfirmOrderStatusEnum;
import com.fugui.train.business.enums.SeatColEnum;
import com.fugui.train.business.enums.SeatTypeEnum;
import com.fugui.train.business.mapper.ConfirmOrderMapper;
import com.fugui.train.business.req.ConfirmOrderDoReq;
import com.fugui.train.business.req.ConfirmOrderTicketReq;
import com.fugui.train.business.req.confirmOrderQueryReq;
import com.fugui.train.business.resp.confirmOrderQueryResp;
import com.fugui.train.common.commonResp.CommonResp;
import com.fugui.train.common.exception.BusinessException;
import com.fugui.train.common.exception.BusinessExceptionEnum;
import com.fugui.train.common.resp.PageResp;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ConfirmOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderService.class);

    @Resource
    private DailyTrainTicketService dailyTrainTicketService;
    @Resource
    private SkTokenService SkTokenService;
    @Resource
    private ConfirmOrderMapper confirmOrderMapper;
    @Resource
    private DailyTrainCarriageService dailyTrainCarriageService;
    @Resource
    private AfterConfirmOrderService afterConfirmOrderService;
    @Resource
    private DailyTrainSeatService dailyTrainSeatService;
    //    @Autowired
//    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedisTemplate redisTemplate;
    @Resource
    private Redisson redisson;

    public PageResp<confirmOrderQueryResp> queryList(confirmOrderQueryReq req) {
        ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
        confirmOrderExample.setOrderByClause("id desc");
        ConfirmOrderExample.Criteria criteria = confirmOrderExample.createCriteria();

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<ConfirmOrder> confirmOrderList = confirmOrderMapper.selectByExample(confirmOrderExample);

        PageInfo<ConfirmOrder> pageInfo = new PageInfo<>(confirmOrderList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<confirmOrderQueryResp> list = BeanUtil.copyToList(confirmOrderList, confirmOrderQueryResp.class);

        PageResp<confirmOrderQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        confirmOrderMapper.deleteByPrimaryKey(id);
    }

    //@SentinelResource(value = "doConfirm",blockHandler = "doConfirmBlock")
    public void doConfirm(@Valid ConfirmOrderDoReq req) {
//        boolean getSkToken=SkTokenService.skTokenVail(req.getDate(),req.getTrainCode(),req.getMemberId());
//        if(!getSkToken){
//            throw new BusinessException(BusinessExceptionEnum.NOT_GET_SKTOKEN);
//        }

//        String lockKey = DateUtil.formatDate(req.getDate()) + "-" + req.getTrainCode();
//        boolean notGetLock = redisTemplate.opsForValue().setIfAbsent(lockKey, lockKey, 5, TimeUnit.SECONDS);
//        RLock lock = redisson.getLock(lockKey);
//        boolean tryLock = false;

//            tryLock = lock.tryLock(10, TimeUnit.MICROSECONDS);
//            DateTime now = DateTime.now();
//            String start = req.getStart();
//            String end = req.getEnd();
//            String trainCode = req.getTrainCode();
//            Long memberId = LoginMemberContext.getMember().getId();
//            //保存确认订单表，存入记录
//
//            ConfirmOrder confirmOrder = new ConfirmOrder();
//            confirmOrder.setId(SnowUtil.getSnowflaskNextId());
//            confirmOrder.setMemberId(memberId);
//            confirmOrder.setDate(now);
//            confirmOrder.setTrainCode(trainCode);
//            confirmOrder.setStart(start);
//            confirmOrder.setEnd(end);
//            confirmOrder.setDailyTrainTicketId(req.getDailyTrainTicketId());
//            confirmOrder.setStatus(ConfirmOrderStatusEnum.INIT.getCode());
//            confirmOrder.setCreateTime(now);
//            confirmOrder.setUpdateTime(now);
//            confirmOrder.setTickets(JSON.toJSONString(req.getTickets()));
//            confirmOrderMapper.insert(confirmOrder);
        while (true) {
            ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
            confirmOrderExample.createCriteria().andDateEqualTo(req.getDate()).andTrainCodeEqualTo(req.getTrainCode()).andMemberIdEqualTo(req.getMemberId()).andStatusEqualTo(ConfirmOrderStatusEnum.INIT.getCode());
            PageHelper.startPage(1, 5);
            List<ConfirmOrder> confirmList = confirmOrderMapper.selectByExampleWithBLOBs(confirmOrderExample);
            if (CollUtil.isEmpty(confirmList)) {
                LOG.info("订单处理完毕");
                break;
            } else {
                confirmList.forEach(confirmOrder -> {
                    try {
                        sell(confirmOrder);
                    } catch (BusinessException e) {
                        if (e.getE().equals(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR)) {
                            confirmOrder.setStatus(ConfirmOrderStatusEnum.EMPTY.getCode());
                            updateStatus(confirmOrder);
                        } else {
                            throw e;
                        }
                    }
                });
            }
        }
    }
    /**
     * 查询前面有几个人在排队
     * @param id
     */
    public Integer queryLineCount(Long id) {
        ConfirmOrder confirmOrder = confirmOrderMapper.selectByPrimaryKey(id);
        ConfirmOrderStatusEnum statusEnum = EnumUtil.getBy(ConfirmOrderStatusEnum::getCode, confirmOrder.getStatus());
        int result = switch (statusEnum) {
            case PENDING -> 0; // 排队0
            case SUCCESS -> -1; // 成功
            case FAILURE -> -2; // 失败
            case EMPTY -> -3; // 无票
            case CANCEL -> -4; // 取消
            case INIT -> 999; // 需要查表得到实际排队数量
        };

        if (result == 999) {
            // 前面有几位，下面的写法：where a=1 and (b=1 or c=1) 等价于 where (a=1 and b=1) or (a=1 and c=1)
            ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
            confirmOrderExample.or().andDateEqualTo(confirmOrder.getDate())
                    .andTrainCodeEqualTo(confirmOrder.getTrainCode())
                    .andCreateTimeLessThanOrEqualTo(confirmOrder.getCreateTime())
                    .andStatusEqualTo(ConfirmOrderStatusEnum.INIT.getCode());
            confirmOrderExample.or().andDateEqualTo(confirmOrder.getDate())
                    .andTrainCodeEqualTo(confirmOrder.getTrainCode())
                    .andCreateTimeLessThanOrEqualTo(confirmOrder.getCreateTime())
                    .andStatusEqualTo(ConfirmOrderStatusEnum.PENDING.getCode());
            int count = Math.toIntExact(confirmOrderMapper.countByExample(confirmOrderExample));
            return count > 0 ? count-1 : count;
        } else {
            return result;
        }
    }

    private void sell(ConfirmOrder confirmOrder) {
        String lockKey = DateUtil.formatDate(confirmOrder.getDate()) + "-" + confirmOrder.getTrainCode();
        boolean notGetLock = redisTemplate.opsForValue().setIfAbsent(lockKey, lockKey, 5, TimeUnit.SECONDS);
        RLock lock = redisson.getLock(lockKey);
        boolean tryLock = false;

        try {
            tryLock = lock.tryLock(10, TimeUnit.MICROSECONDS);

            if (!tryLock){
                LOG.info("很遗憾，没抢到锁");
                return;
            }
            ConfirmOrderDoReq req = new ConfirmOrderDoReq();
            req = BeanUtil.copyProperties(confirmOrder, ConfirmOrderDoReq.class);
            req.setImageCodeToken("");
            req.setImageCode("");
            req.setLogId("");
            confirmOrder.setStatus(ConfirmOrderStatusEnum.PENDING.getCode());
            updateStatus(confirmOrder);
            DailyTrainTicket dailyTrainTicket = dailyTrainTicketService.selectTicket(req.getDate(), req.getTrainCode(), req.getStart(), req.getEnd());
            reduceTicket(req, dailyTrainTicket);
            ConfirmOrderTicketReq ticket0 = req.getTickets().get(0);
            //分别对有选座和没选座进行getSeat
            List<DailyTrainSeat> finalSeatList = new LinkedList<>();
            if (StrUtil.isNotBlank(ticket0.getSeat())) {
                //查出偏移值
                String seatTypeCode = ticket0.getSeatTypeCode();
                List<SeatColEnum> colsByType = SeatColEnum.getColsByType(seatTypeCode);
                List<String> seatNameList = new LinkedList<>();
                for (int i = 1; i <= 2; i++) {
                    for (SeatColEnum seatColEnum : colsByType) {
                        seatNameList.add(seatColEnum.getCode() + i);
                    }
                }
                List<Integer> seatIndex4Zero = new LinkedList<>();
                for (ConfirmOrderTicketReq ticket : req.getTickets()) {
                    seatIndex4Zero.add(seatNameList.indexOf(ticket.getSeat()) - seatNameList.indexOf(ticket0.getSeat()));
                }
                getSeat(finalSeatList, req.getDate(), req.getTrainCode(), ticket0.getSeatTypeCode(), seatNameList.get(0).substring(0, 1), seatIndex4Zero, dailyTrainTicket.getStartIndex(), dailyTrainTicket.getEndIndex());
            } else {
                for (ConfirmOrderTicketReq ticket : req.getTickets()) {
                    getSeat(finalSeatList, req.getDate(), req.getTrainCode(), ticket.getSeatTypeCode(), null, null, dailyTrainTicket.getStartIndex(), dailyTrainTicket.getEndIndex());
                }
            }
            afterConfirmOrderService.afterDoConfirm(dailyTrainTicket, finalSeatList, req.getTickets(), confirmOrder);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            LOG.info("购票流程结束，释放锁！");
            if (null != lock && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void updateStatus(ConfirmOrder confirmOrder) {
        confirmOrderMapper.updateByPrimaryKey(confirmOrder);
    }


    private void getSeat(List<DailyTrainSeat> finalSeat, Date date, String trainCode, String seatTypeCode, String colName, List<Integer> seatIndex4Zero, Integer startIndex, Integer endIndex) {
        //查询所有符合条件的车厢
        List<DailyTrainCarriage> dailyTrainCarriageList = dailyTrainCarriageService.selectCarriage(date, trainCode, seatTypeCode);
        //将每个车厢座位筛选出来

        for (DailyTrainCarriage dailyTrainCarriage : dailyTrainCarriageList) {
            List<DailyTrainSeat> dailyTrainSeatList = dailyTrainSeatService.selectSeat(date, trainCode, dailyTrainCarriage.getIndex());
            List<DailyTrainSeat> getSeat = new LinkedList<>();
            for (int i = 0; i < dailyTrainSeatList.size(); i++) {
                //判断是否已选过
                DailyTrainSeat dailyTrainSeat = dailyTrainSeatList.get(i);
                boolean alreadySelect = false;
                boolean isGetAll = true;
                for (DailyTrainSeat trainSeat : getSeat) {
                    if (trainSeat.getId().equals(dailyTrainSeat.getId())) {
                        alreadySelect = true;
                    }
                }
                if (alreadySelect) {
                    continue;
                }
                //判断列号
                if (StrUtil.isNotBlank(colName)) {
                    if (!dailyTrainSeat.getSeatType().equals(colName)) continue;
                }
                //判断是否选座成功
                boolean isChoose = (calSell(dailyTrainSeat, startIndex, endIndex));
                if (!isChoose) {
                    continue;
                }
                getSeat.add(dailyTrainSeat);
                //判断剩余座位
                if (StrUtil.isNotBlank(colName)) {
                    for (int j = 0; j < seatIndex4Zero.size(); j++) {
                        if (i + seatIndex4Zero.get(j) >= dailyTrainSeatList.size()) {
                            isGetAll = false;
                            break;
                        }
                        DailyTrainSeat nextTrainSeat = dailyTrainSeatList.get(i + seatIndex4Zero.get(j));
                        boolean nextChoose = calSell(nextTrainSeat, startIndex, endIndex);
                        if (!nextChoose) {
                            isGetAll = false;
                            break;
                        }
                        getSeat.add(nextTrainSeat);
                    }
                }
                if (!isGetAll) break;
                finalSeat.addAll(getSeat);
                return;
            }
        }

    }

    private boolean calSell(DailyTrainSeat dailyTrainSeat, Integer startIndex, Integer endIndex) {
        String sell = dailyTrainSeat.getSell();
        String realSell = sell.substring(startIndex, endIndex);
        if (Integer.parseInt(realSell) > 0) {
            return false;
        }
        realSell = realSell.replace('0', '1');
        realSell = StrUtil.fillBefore(realSell, '0', endIndex);
        realSell = StrUtil.fillAfter(realSell, '0', sell.length());
        int sellInt = NumberUtil.binaryToInt(realSell) | NumberUtil.binaryToInt(sell);
        realSell = NumberUtil.getBinaryStr(sellInt);
        dailyTrainSeat.setSell(realSell);
        return true;
    }


    private void reduceTicket(ConfirmOrderDoReq req, DailyTrainTicket dailyTrainTicket) {
        for (ConfirmOrderTicketReq ticket : req.getTickets()) {
            SeatTypeEnum seatTypeEnum = SeatTypeEnum.getEnumByCode(ticket.getSeatTypeCode());
            switch (seatTypeEnum) {
                case YDZ -> {
                    int count = dailyTrainTicket.getYdz() - 1;
                    if (count < 0) {
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setYdz(count);
                }
                case RW -> {
                    int count = dailyTrainTicket.getRw() - 1;
                    if (count < 0) {
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setRw(count);
                }
                case YW -> {
                    int count = dailyTrainTicket.getYw() - 1;
                    if (count < 0) {
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setYw(count);
                }
                case EDZ -> {
                    int count = dailyTrainTicket.getEdz() - 1;
                    if (count < 0) {
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setEdz(count);
                }
            }
        }
    }

    public CommonResp<Object> doConfirmBlock(ConfirmOrderDoReq req, BlockException e) {
        LOG.info("购票被限流");
        throw new BusinessException(BusinessExceptionEnum.BUSINESS_NOT_GET_LOCK);
    }
}
