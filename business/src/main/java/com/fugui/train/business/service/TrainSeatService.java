package com.fugui.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import com.fugui.train.business.domain.TrainSeat;
import com.fugui.train.business.domain.TrainSeatExample;
import com.fugui.train.business.mapper.TrainSeatMapper;
import com.fugui.train.business.req.TrainSeatQueryReq;
import com.fugui.train.business.req.TrainSeatSaveReq;
import com.fugui.train.business.resp.TrainSeatQueryResp;
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
public class TrainSeatService {
    @Resource
    private TrainSeatMapper trainSeatMapper;
    public int count() {
        return Math.toIntExact(trainSeatMapper.countByExample(null));
    }
    private final Logger LOG= LoggerFactory.getLogger(TrainSeatService.class);

    public void save(TrainSeatSaveReq req) {
        Date cur = new Date();
        req.setUpdateTime(cur);
        //如果用户id存在，则执行更新操作
        TrainSeat trainSeat = BeanUtil.copyProperties(req, TrainSeat.class);
        if(req.getId()==null) {
            trainSeat.setId(SnowUtil.getSnowflaskNextId());
            trainSeat.setCreateTime(cur);
            trainSeatMapper.insert(trainSeat);
        }else{
            trainSeatMapper.updateByPrimaryKey(trainSeat);
        }
    }
    public PageResp<TrainSeatQueryResp> queryList(TrainSeatQueryReq req) {
        TrainSeatExample trainSeatExample=new TrainSeatExample();
        PageHelper.startPage(req.getPage(),req.getSize());
        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        List<TrainSeat> trainSeats = trainSeatMapper.selectByExample(trainSeatExample);
        PageInfo<TrainSeat> pageInfo=new PageInfo<>(trainSeats);
        PageResp<TrainSeatQueryResp> pageResp=new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());
        pageResp.setList(BeanUtil.copyToList(trainSeats, TrainSeatQueryResp.class));
        return pageResp;
    }

    public void delete(Long id) {
        trainSeatMapper.deleteByPrimaryKey(id);
    }

}