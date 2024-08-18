package com.fugui.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import com.fugui.train.business.domain.TrainStation;
import com.fugui.train.business.domain.TrainStationExample;
import com.fugui.train.business.mapper.TrainStationMapper;
import com.fugui.train.business.req.TrainStationQueryReq;
import com.fugui.train.business.req.TrainStationSaveReq;
import com.fugui.train.business.resp.TrainStationQueryResp;
import com.fugui.train.common.exception.BusinessException;
import com.fugui.train.common.exception.BusinessExceptionEnum;
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
public class TrainStationService {
    @Resource
    private TrainStationMapper trainStationMapper;
    public int count() {
        return Math.toIntExact(trainStationMapper.countByExample(null));
    }
    private final Logger LOG= LoggerFactory.getLogger(TrainStationService.class);

    public void save(TrainStationSaveReq req) {
        Date cur = new Date();
        req.setUpdateTime(cur);
        //如果用户id存在，则执行更新操作
        TrainStation trainStation = BeanUtil.copyProperties(req, TrainStation.class);
        if(req.getId()==null) {

            TrainStationExample trainStationExample=new TrainStationExample();
            trainStationExample.createCriteria().andTrainCodeEqualTo(req.getTrainCode()).andIndexEqualTo(req.getIndex());
            if(!trainStationMapper.selectByExample(trainStationExample).isEmpty()){
                throw new BusinessException(BusinessExceptionEnum.TRAIN_STATION_CODE_AND_INDEX_EXIST_ERROR);
            }
            trainStation.setId(SnowUtil.getSnowflaskNextId());
            trainStation.setCreateTime(cur);
            trainStationMapper.insert(trainStation);
        }else{
            trainStationMapper.updateByPrimaryKey(trainStation);
        }
    }
    public PageResp<TrainStationQueryResp> queryList(TrainStationQueryReq req) {
        TrainStationExample trainStationExample=new TrainStationExample();
        trainStationExample.setOrderByClause("train_code asc, `index` asc");
        PageHelper.startPage(req.getPage(),req.getSize());
        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        List<TrainStation> trainStations = trainStationMapper.selectByExample(trainStationExample);
        PageInfo<TrainStation> pageInfo=new PageInfo<>(trainStations);
        PageResp<TrainStationQueryResp> pageResp=new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());
        pageResp.setList(BeanUtil.copyToList(trainStations, TrainStationQueryResp.class));
        return pageResp;
    }

    public void delete(Long id) {
        trainStationMapper.deleteByPrimaryKey(id);
    }
    public List<TrainStation> selectByTrainCode(String trainCode) {
        TrainStationExample trainStationExample = new TrainStationExample();
        trainStationExample.createCriteria().andTrainCodeEqualTo(trainCode);
        List<TrainStation> trainStations = trainStationMapper.selectByExample(trainStationExample);
        return trainStations;
    }
}