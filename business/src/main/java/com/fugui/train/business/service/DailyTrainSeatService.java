package com.fugui.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.fugui.train.business.domain.*;
import com.fugui.train.business.mapper.DailyTrainSeatMapper;
import com.fugui.train.business.mapper.TrainSeatMapper;
import com.fugui.train.business.req.DailyTrainSeatQueryReq;
import com.fugui.train.business.req.DailyTrainSeatSaveReq;
import com.fugui.train.business.resp.DailyTrainSeatQueryResp;
import com.fugui.train.common.resp.PageResp;
import com.fugui.train.common.util.SnowUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DailyTrainSeatService {

    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainSeatService.class);

    @Resource
    private DailyTrainSeatMapper dailyTrainSeatMapper;
    @Resource
    private TrainStationService trainStationService;
    @Resource
    private TrainSeatMapper trainSeatMapper;

    public void save(DailyTrainSeatSaveReq req) {
        DateTime now = DateTime.now();
        DailyTrainSeat dailyTrainSeat = BeanUtil.copyProperties(req, DailyTrainSeat.class);
        if (ObjectUtil.isNull(dailyTrainSeat.getId())) {
            dailyTrainSeat.setId(SnowUtil.getSnowflaskNextId());
            dailyTrainSeat.setCreateTime(now);
            dailyTrainSeat.setUpdateTime(now);
            dailyTrainSeatMapper.insert(dailyTrainSeat);
        } else {
            dailyTrainSeat.setUpdateTime(now);
            dailyTrainSeatMapper.updateByPrimaryKey(dailyTrainSeat);
        }
    }

    public PageResp<DailyTrainSeatQueryResp> queryList(DailyTrainSeatQueryReq req) {
        DailyTrainSeatExample dailyTrainSeatExample = new DailyTrainSeatExample();
        dailyTrainSeatExample.setOrderByClause("id desc");
        DailyTrainSeatExample.Criteria criteria = dailyTrainSeatExample.createCriteria();

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<DailyTrainSeat> dailyTrainSeatList = dailyTrainSeatMapper.selectByExample(dailyTrainSeatExample);

        PageInfo<DailyTrainSeat> pageInfo = new PageInfo<>(dailyTrainSeatList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<DailyTrainSeatQueryResp> list = BeanUtil.copyToList(dailyTrainSeatList, DailyTrainSeatQueryResp.class);

        PageResp<DailyTrainSeatQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        dailyTrainSeatMapper.deleteByPrimaryKey(id);
    }

    public void genDaily(Date date, Train train) {
        DailyTrainSeatExample dailyTrainSeatExample = new DailyTrainSeatExample();
        dailyTrainSeatExample.createCriteria().andTrainCodeEqualTo(train.getCode());
        dailyTrainSeatMapper.deleteByExample(dailyTrainSeatExample);

        TrainSeatExample trainSeatExample = new TrainSeatExample();
        trainSeatExample.createCriteria().andTrainCodeEqualTo(train.getCode());
        List<TrainSeat> trainSeats = trainSeatMapper.selectByExample(trainSeatExample);
        if (CollUtil.isEmpty(trainSeats)) {
            LOG.info("{}不存在车站，当前任务结束", date);
            return;
        }
        for (TrainSeat trainSeat : trainSeats) {
            genDailyTrainSeat(date, trainSeat);
        }
    }

    private void genDailyTrainSeat(Date date, TrainSeat trainSeat) {
        Date now = new Date();
        DailyTrainSeat dailyTrainSeat = BeanUtil.copyProperties(trainSeat, DailyTrainSeat.class);

        List<TrainStation> stationList = trainStationService.selectByTrainCode(trainSeat.getTrainCode());
        String sell = StrUtil.fillBefore("", '0', stationList.size() - 1);
        dailyTrainSeat.setId(SnowUtil.getSnowflaskNextId());
        dailyTrainSeat.setDate(date);
        dailyTrainSeat.setCreateTime(now);
        dailyTrainSeat.setUpdateTime(now);
        dailyTrainSeat.setSell(sell);
        dailyTrainSeatMapper.insert(dailyTrainSeat);
    }

    public Integer count(Date date, Train train, String type) {
        DailyTrainSeatExample dailyTrainSeatExample = new DailyTrainSeatExample();
        dailyTrainSeatExample.createCriteria().andTrainCodeEqualTo(train.getCode()).andDateEqualTo(date).andSeatTypeEqualTo(type);
        List<DailyTrainSeat> dailyTrainSeats = dailyTrainSeatMapper.selectByExample(dailyTrainSeatExample);
        if (CollUtil.isEmpty(dailyTrainSeats)) {
            return -1;
        }
        return dailyTrainSeats.size();
    }
    public Integer countSeat(Date date, Train train) {
        DailyTrainSeatExample dailyTrainSeatExample = new DailyTrainSeatExample();
        dailyTrainSeatExample.createCriteria().andTrainCodeEqualTo(train.getCode()).andDateEqualTo(date);
        List<DailyTrainSeat> dailyTrainSeats = dailyTrainSeatMapper.selectByExample(dailyTrainSeatExample);
        if (CollUtil.isEmpty(dailyTrainSeats)) {
            return -1;
        }
        return dailyTrainSeats.size();
    }

    public List<DailyTrainSeat> selectSeat(Date date, String trainCode, Integer index) {
        DailyTrainSeatExample dailyTrainSeatExample = new DailyTrainSeatExample();
        dailyTrainSeatExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(trainCode).andCarriageIndexEqualTo(index);
        return dailyTrainSeatMapper.selectByExample(dailyTrainSeatExample);
    }
}
