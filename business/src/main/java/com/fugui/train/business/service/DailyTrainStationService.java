package com.fugui.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.fugui.train.business.domain.*;
import com.fugui.train.business.mapper.DailyTrainStationMapper;
import com.fugui.train.business.mapper.TrainStationMapper;
import com.fugui.train.business.req.DailyTrainStationQueryReq;
import com.fugui.train.business.req.DailyTrainStationSaveReq;
import com.fugui.train.business.resp.DailyTrainStationQueryResp;
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
public class DailyTrainStationService {

    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainStationService.class);
    @Resource
    private TrainStationMapper trainStationMapper;

    @Resource
    private DailyTrainStationMapper dailyTrainStationMapper;

    public void save(DailyTrainStationSaveReq req) {
        DateTime now = DateTime.now();
        DailyTrainStation dailyTrainStation = BeanUtil.copyProperties(req, DailyTrainStation.class);
        if (ObjectUtil.isNull(dailyTrainStation.getId())) {
            dailyTrainStation.setId(SnowUtil.getSnowflaskNextId());
            dailyTrainStation.setCreateTime(now);
            dailyTrainStation.setUpdateTime(now);
            dailyTrainStationMapper.insert(dailyTrainStation);
        } else {
            dailyTrainStation.setUpdateTime(now);
            dailyTrainStationMapper.updateByPrimaryKey(dailyTrainStation);
        }
    }

    public PageResp<DailyTrainStationQueryResp> queryList(DailyTrainStationQueryReq req) {
        DailyTrainStationExample dailyTrainStationExample = new DailyTrainStationExample();
        dailyTrainStationExample.setOrderByClause("id desc");
        DailyTrainStationExample.Criteria criteria = dailyTrainStationExample.createCriteria();

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<DailyTrainStation> dailyTrainStationList = dailyTrainStationMapper.selectByExample(dailyTrainStationExample);

        PageInfo<DailyTrainStation> pageInfo = new PageInfo<>(dailyTrainStationList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<DailyTrainStationQueryResp> list = BeanUtil.copyToList(dailyTrainStationList, DailyTrainStationQueryResp.class);

        PageResp<DailyTrainStationQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        dailyTrainStationMapper.deleteByPrimaryKey(id);
    }

    public void genDaily(Date date, Train train) {
        DailyTrainStationExample dailyTrainStationExample=new DailyTrainStationExample();
        dailyTrainStationExample.createCriteria().andTrainCodeEqualTo(train.getCode());
        dailyTrainStationMapper.deleteByExample(dailyTrainStationExample);

        TrainStationExample trainStationExample=new TrainStationExample();
        trainStationExample.createCriteria().andTrainCodeEqualTo(train.getCode());
        List<TrainStation> trainStations = trainStationMapper.selectByExample(trainStationExample);
        if(CollUtil.isEmpty(trainStations)){
            LOG.info("{}不存在车站，当前任务结束",date);
            return;
        }
        for (TrainStation trainStation : trainStations) {
            genDailyTrainStation(date,trainStation);
        }
    }

    private void genDailyTrainStation(Date date, TrainStation trainStation) {
        Date now=new Date();
        DailyTrainStation dailyTrainStation=BeanUtil.copyProperties(trainStation,DailyTrainStation.class);
        dailyTrainStation.setId(SnowUtil.getSnowflaskNextId());
        dailyTrainStation.setDate(date);
        dailyTrainStation.setCreateTime(now);
        dailyTrainStation.setUpdateTime(now);
        dailyTrainStationMapper.insert(dailyTrainStation);
    }

    public int countStation(Date date, Train train) {
        DailyTrainStationExample dailyTrainStationExample=new DailyTrainStationExample();
        dailyTrainStationExample.createCriteria().andTrainCodeEqualTo(train.getCode()).andDateEqualTo(date);
        Integer count = Math.toIntExact(dailyTrainStationMapper.countByExample(dailyTrainStationExample));
        return count;
    }
}
