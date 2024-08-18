package com.fugui.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.fugui.train.business.domain.*;
import com.fugui.train.business.enums.SeatTypeEnum;
import com.fugui.train.business.enums.TrainTypeEnum;
import com.fugui.train.business.mapper.DailyTrainTicketMapper;
import com.fugui.train.business.mapper.TrainStationMapper;
import com.fugui.train.business.req.DailyTrainTicketQueryReq;
import com.fugui.train.business.req.DailyTrainTicketSaveReq;
import com.fugui.train.business.resp.DailyTrainTicketQueryResp;
import com.fugui.train.common.resp.PageResp;
import com.fugui.train.common.util.SnowUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

@Service
public class DailyTrainTicketService {

    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainTicketService.class);
    @Resource
    private DailyTrainSeatService dailyTrainSeatService;
    @Resource
    private DailyTrainTicketMapper dailyTrainTicketMapper;
    @Resource
    private TrainStationMapper trainStationMapper;

    public void save(DailyTrainTicketSaveReq req) {
        DateTime now = DateTime.now();
        DailyTrainTicket dailyTrainTicket = BeanUtil.copyProperties(req, DailyTrainTicket.class);
        if (ObjectUtil.isNull(dailyTrainTicket.getId())) {
            dailyTrainTicket.setId(SnowUtil.getSnowflaskNextId());
            dailyTrainTicket.setCreateTime(now);
            dailyTrainTicket.setUpdateTime(now);
            dailyTrainTicketMapper.insert(dailyTrainTicket);
        } else {
            dailyTrainTicket.setUpdateTime(now);
            dailyTrainTicketMapper.updateByPrimaryKey(dailyTrainTicket);
        }
    }
    @CachePut(value = "DailyTrainTicketService.queryList")
    public PageResp<DailyTrainTicketQueryResp> queryList2(DailyTrainTicketQueryReq req) {
        return queryList(req);
    }

    @Cacheable(value = "DailyTrainTicketService.queryList")
    public PageResp<DailyTrainTicketQueryResp> queryList(DailyTrainTicketQueryReq req) {

        DailyTrainTicketExample dailyTrainTicketExample = new DailyTrainTicketExample();
        dailyTrainTicketExample.setOrderByClause("id desc");
        DailyTrainTicketExample.Criteria criteria = dailyTrainTicketExample.createCriteria();
        if (ObjUtil.isNotNull(req.getDate())) {
            criteria.andDateEqualTo(req.getDate());
        }
        if (ObjUtil.isNotEmpty(req.getTrainCode())) {
            criteria.andTrainCodeEqualTo(req.getTrainCode());
        }
        if (ObjUtil.isNotEmpty(req.getStart())) {
            criteria.andStartEqualTo(req.getStart());
        }
        if (ObjUtil.isNotEmpty(req.getEnd())) {
            criteria.andEndEqualTo(req.getEnd());
        }
        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<DailyTrainTicket> dailyTrainTicketList = dailyTrainTicketMapper.selectByExample(dailyTrainTicketExample);

        PageInfo<DailyTrainTicket> pageInfo = new PageInfo<>(dailyTrainTicketList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<DailyTrainTicketQueryResp> list = BeanUtil.copyToList(dailyTrainTicketList, DailyTrainTicketQueryResp.class);

        PageResp<DailyTrainTicketQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        dailyTrainTicketMapper.deleteByPrimaryKey(id);
    }

    public void genDaily(Date date, Train train) {
        DailyTrainTicketExample dailyTrainTicketExample = new DailyTrainTicketExample();
        dailyTrainTicketExample.createCriteria().andTrainCodeEqualTo(train.getCode()).andDateEqualTo(date);
        dailyTrainTicketMapper.deleteByExample(dailyTrainTicketExample);
        TrainStationExample trainStationExample = new TrainStationExample();
        trainStationExample.createCriteria().andTrainCodeEqualTo(train.getCode());
        List<TrainStation> trainStations = trainStationMapper.selectByExample(trainStationExample);
        if (CollUtil.isEmpty(trainStations)) {
            LOG.info("{}不存在车站，当前任务结束", date);
            return;
        }
        Date now = new Date();
        for (int i = 0; i < trainStations.size(); i++) {
            BigDecimal allKM = BigDecimal.ZERO;
            for (int j = i + 1; j < trainStations.size(); j++) {
                allKM = allKM.add(trainStations.get(j).getKm());
                DailyTrainTicket dailyTrainTicket = new DailyTrainTicket();
                dailyTrainTicket.setId(SnowUtil.getSnowflaskNextId());
                dailyTrainTicket.setDate(date);
                dailyTrainTicket.setTrainCode(train.getCode());
                dailyTrainTicket.setStart(trainStations.get(i).getName());
                dailyTrainTicket.setStartPinyin(trainStations.get(i).getNamePinyin());
                dailyTrainTicket.setStartTime(trainStations.get(i).getInTime());
                dailyTrainTicket.setStartIndex(trainStations.get(i).getIndex());
                dailyTrainTicket.setEnd(trainStations.get(j).getName());
                dailyTrainTicket.setEndPinyin(trainStations.get(j).getNamePinyin());
                dailyTrainTicket.setEndTime(trainStations.get(j).getStopTime());
                dailyTrainTicket.setEndIndex(trainStations.get(j).getIndex());

                BigDecimal priceRate = EnumUtil.getFieldBy(TrainTypeEnum::getPriceRate, TrainTypeEnum::getCode, train.getType());

                dailyTrainTicket.setYdz(dailyTrainSeatService.count(date, train, SeatTypeEnum.YDZ.getCode()));
                dailyTrainTicket.setYdzPrice(allKM.multiply(priceRate).multiply(SeatTypeEnum.YDZ.getPrice().setScale(2, RoundingMode.HALF_UP)));
                dailyTrainTicket.setEdz(dailyTrainSeatService.count(date, train, SeatTypeEnum.EDZ.getCode()));
                dailyTrainTicket.setEdzPrice(allKM.multiply(priceRate).multiply(SeatTypeEnum.EDZ.getPrice().setScale(2, RoundingMode.HALF_UP)));
                dailyTrainTicket.setRw(dailyTrainSeatService.count(date, train, SeatTypeEnum.RW.getCode()));
                dailyTrainTicket.setRwPrice(allKM.multiply(priceRate).multiply(SeatTypeEnum.RW.getPrice().setScale(2, RoundingMode.HALF_UP)));
                dailyTrainTicket.setYw(dailyTrainSeatService.count(date, train, SeatTypeEnum.YW.getCode()));
                dailyTrainTicket.setYwPrice(allKM.multiply(priceRate).multiply(SeatTypeEnum.YW.getPrice().setScale(2, RoundingMode.HALF_UP)));
                dailyTrainTicket.setCreateTime(now);
                dailyTrainTicket.setUpdateTime(now);
                dailyTrainTicketMapper.insert(dailyTrainTicket);
            }
        }
    }

    public DailyTrainTicket selectTicket(Date date, String trainCode, String start, String end) {
        DailyTrainTicketExample dailyTrainTicketExample = new DailyTrainTicketExample();
        dailyTrainTicketExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(trainCode).andStartEqualTo(start).andEndEqualTo(end);

        List<DailyTrainTicket> dailyTrainTickets = dailyTrainTicketMapper.selectByExample(dailyTrainTicketExample);
        if (CollUtil.isEmpty(dailyTrainTickets)) {
            return null;
        }
        return dailyTrainTickets.get(0);
    }
}
