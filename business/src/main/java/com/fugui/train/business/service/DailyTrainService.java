package com.fugui.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.fugui.train.business.domain.*;
import com.fugui.train.business.mapper.DailyTrainMapper;
import com.fugui.train.business.mapper.TrainMapper;
import com.fugui.train.business.req.DailyTrainQueryReq;
import com.fugui.train.business.req.DailyTrainSaveReq;
import com.fugui.train.business.resp.DailyTrainQueryResp;
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
public class DailyTrainService {

    private static final Logger LOG = LoggerFactory.getLogger(DailyTrainService.class);

    @Resource
    private DailyTrainMapper dailyTrainMapper;
    @Resource
    private TrainMapper trainMapper;
    @Resource
    private DailyTrainCarriageService dailyTrainCarriageService;
    @Resource
    private DailyTrainSeatService dailyTrainSeatService;
    @Resource
    private SkTokenService skTokenService;
    @Resource
    private DailyTrainStationService dailyTrainStationService;
    @Resource
    private DailyTrainTicketService dailyTrainTicketService;

    public void save(DailyTrainSaveReq req) {
        DateTime now = DateTime.now();
        DailyTrain dailyTrain = BeanUtil.copyProperties(req, DailyTrain.class);
        if (ObjectUtil.isNull(dailyTrain.getId())) {
            dailyTrain.setId(SnowUtil.getSnowflaskNextId());
            dailyTrain.setCreateTime(now);
            dailyTrain.setUpdateTime(now);
            dailyTrainMapper.insert(dailyTrain);
        } else {
            dailyTrain.setUpdateTime(now);
            dailyTrainMapper.updateByPrimaryKey(dailyTrain);
        }
    }

    public PageResp<DailyTrainQueryResp> queryList(DailyTrainQueryReq req) {
        DailyTrainExample dailyTrainExample = new DailyTrainExample();
        dailyTrainExample.setOrderByClause("id desc");
        DailyTrainExample.Criteria criteria = dailyTrainExample.createCriteria();

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<DailyTrain> dailyTrainList = dailyTrainMapper.selectByExample(dailyTrainExample);

        PageInfo<DailyTrain> pageInfo = new PageInfo<>(dailyTrainList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<DailyTrainQueryResp> list = BeanUtil.copyToList(dailyTrainList, DailyTrainQueryResp.class);

        PageResp<DailyTrainQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        dailyTrainMapper.deleteByPrimaryKey(id);
    }

    public void genDaily(Date date) {
        DailyTrainExample dailyTrainExample=new DailyTrainExample();
        dailyTrainExample.createCriteria();
        dailyTrainMapper.deleteByExample(dailyTrainExample);

        //查询所有车次
        TrainExample trainExample=new TrainExample();
        trainExample.createCriteria();
        List<Train> trains = trainMapper.selectByExample(trainExample);
        if(CollUtil.isEmpty(trains)){
            LOG.info("{}不存在车次，当前任务结束",date);
            return;
        }
        for (Train train : trains) {
            genDailyTrain(date,train);
        }
        LOG.info("该车次所有信息生成结束，当前任务结束",date);
    }

    private void genDailyTrain(Date date, Train train) {
        //生成当日所有车次
        Date now=new Date();
        DailyTrain dailyTrain=BeanUtil.copyProperties(train,DailyTrain.class);
        dailyTrain.setId(SnowUtil.getSnowflaskNextId());
        dailyTrain.setDate(date);
        dailyTrain.setCreateTime(now);
        dailyTrain.setUpdateTime(now);
        dailyTrainMapper.insert(dailyTrain);

        dailyTrainStationService.genDaily(date,train);
        dailyTrainCarriageService.genDaily(date,train);
        dailyTrainSeatService.genDaily(date,train);
        dailyTrainTicketService.genDaily(date,train);
        skTokenService.genDaily(date,train);
    }
}
