package com.fugui.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.fugui.train.business.domain.*;
import com.fugui.train.business.enums.SeatColEnum;
import com.fugui.train.business.mapper.TrainCarriageMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.fugui.train.common.resp.PageResp;
import com.fugui.train.common.util.SnowUtil;
import com.fugui.train.business.mapper.DailyTrainCarriageMapper;
import com.fugui.train.business.req.DailyTrainCarriageQueryReq;
import com.fugui.train.business.req.DailyTrainCarriageSaveReq;
import com.fugui.train.business.resp.DailyTrainCarriageQueryResp;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class DailyTrainCarriageService {

    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainCarriageService.class);
    @Resource
    private TrainCarriageMapper trainCarriageMapper;

    @Resource
    private DailyTrainCarriageMapper dailyTrainCarriageMapper;

    public void save(DailyTrainCarriageSaveReq req) {
        DateTime now = DateTime.now();
        DailyTrainCarriage dailyTrainCarriage = BeanUtil.copyProperties(req, DailyTrainCarriage.class);
        if (ObjectUtil.isNull(dailyTrainCarriage.getId())) {
            dailyTrainCarriage.setId(SnowUtil.getSnowflaskNextId());
            dailyTrainCarriage.setCreateTime(now);
            dailyTrainCarriage.setUpdateTime(now);
            dailyTrainCarriage.setColCount(SeatColEnum.getColsByType(dailyTrainCarriage.getSeatType()).size());
            dailyTrainCarriage.setSeatCount(dailyTrainCarriage.getRowCount()* dailyTrainCarriage.getColCount());
            dailyTrainCarriageMapper.insert(dailyTrainCarriage);
        } else {
            dailyTrainCarriage.setUpdateTime(now);
            dailyTrainCarriageMapper.updateByPrimaryKey(dailyTrainCarriage);
        }
    }

    public PageResp<DailyTrainCarriageQueryResp> queryList(DailyTrainCarriageQueryReq req) {
        DailyTrainCarriageExample dailyTrainCarriageExample = new DailyTrainCarriageExample();
        dailyTrainCarriageExample.setOrderByClause("id desc");
        DailyTrainCarriageExample.Criteria criteria = dailyTrainCarriageExample.createCriteria();

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<DailyTrainCarriage> dailyTrainCarriageList = dailyTrainCarriageMapper.selectByExample(dailyTrainCarriageExample);

        PageInfo<DailyTrainCarriage> pageInfo = new PageInfo<>(dailyTrainCarriageList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<DailyTrainCarriageQueryResp> list = BeanUtil.copyToList(dailyTrainCarriageList, DailyTrainCarriageQueryResp.class);

        PageResp<DailyTrainCarriageQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        dailyTrainCarriageMapper.deleteByPrimaryKey(id);
    }

    public void genDaily(Date date, Train train) {
        DailyTrainCarriageExample dailyTrainCarriageExample=new DailyTrainCarriageExample();
        dailyTrainCarriageExample.createCriteria().andTrainCodeEqualTo(train.getCode());
        dailyTrainCarriageMapper.deleteByExample(dailyTrainCarriageExample);

        TrainCarriageExample trainCarriageExample=new TrainCarriageExample();
        trainCarriageExample.createCriteria().andTrainCodeEqualTo(train.getCode());
        List<TrainCarriage> trainCarriages = trainCarriageMapper.selectByExample(trainCarriageExample);
        if(CollUtil.isEmpty(trainCarriages)){
            LOG.info("{}不存在车站，当前任务结束",date);
            return;
        }
        for (TrainCarriage trainCarriage : trainCarriages) {
            genDailyTrainCarriage(date,trainCarriage);
        }
    }

    private void genDailyTrainCarriage(Date date, TrainCarriage trainCarriage) {
        Date now=new Date();
        DailyTrainCarriage dailyTrainCarriage=BeanUtil.copyProperties(trainCarriage,DailyTrainCarriage.class);
        dailyTrainCarriage.setId(SnowUtil.getSnowflaskNextId());
        dailyTrainCarriage.setDate(date);
        dailyTrainCarriage.setCreateTime(now);
        dailyTrainCarriage.setUpdateTime(now);
        dailyTrainCarriageMapper.insert(dailyTrainCarriage);
    }

    public List<DailyTrainCarriage> selectCarriage(Date date, String trainCode, String seatTypeCode) {
        DailyTrainCarriageExample dailyTrainCarriageExample=new DailyTrainCarriageExample();
        dailyTrainCarriageExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(trainCode).andSeatTypeEqualTo(seatTypeCode);
        return dailyTrainCarriageMapper.selectByExample(dailyTrainCarriageExample);
    }
}
