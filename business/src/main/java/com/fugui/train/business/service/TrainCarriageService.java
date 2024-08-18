package com.fugui.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import com.fugui.train.business.domain.TrainCarriage;
import com.fugui.train.business.domain.TrainCarriageExample;
import com.fugui.train.business.enums.SeatColEnum;
import com.fugui.train.business.mapper.TrainCarriageMapper;
import com.fugui.train.business.req.TrainCarriageQueryReq;
import com.fugui.train.business.req.TrainCarriageSaveReq;
import com.fugui.train.business.resp.TrainCarriageQueryResp;
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
public class TrainCarriageService {
    @Resource
    private TrainCarriageMapper trainCarriageMapper;
    public int count() {
        return Math.toIntExact(trainCarriageMapper.countByExample(null));
    }
    private final Logger LOG= LoggerFactory.getLogger(TrainCarriageService.class);

    public void save(TrainCarriageSaveReq req) {
        Date cur = new Date();
        req.setUpdateTime(cur);
        //如果用户id存在，则执行更新操作
        TrainCarriage trainCarriage = BeanUtil.copyProperties(req, TrainCarriage.class);
        Integer row=trainCarriage.getRowCount();
        Integer col= SeatColEnum.getColsByType(trainCarriage.getSeatType()).size();
        trainCarriage.setColCount(col);
        trainCarriage.setSeatCount(row*col);
        if(req.getId()==null) {
            TrainCarriageExample trainCarriageExample=new TrainCarriageExample();
            trainCarriageExample.createCriteria().andIndexEqualTo(req.getIndex()).andTrainCodeEqualTo(req.getTrainCode());
            if(!trainCarriageMapper.selectByExample(trainCarriageExample).isEmpty()){
                throw new BusinessException(BusinessExceptionEnum.CARRIAGE_CODE_AND_INDEX_EXIST_ERROR);
            }
            trainCarriage.setId(SnowUtil.getSnowflaskNextId());
            trainCarriage.setCreateTime(cur);
            trainCarriageMapper.insert(trainCarriage);
        }else{
            trainCarriageMapper.updateByPrimaryKey(trainCarriage);
        }
    }
    public PageResp<TrainCarriageQueryResp> queryList(TrainCarriageQueryReq req) {
        TrainCarriageExample trainCarriageExample=new TrainCarriageExample();
        PageHelper.startPage(req.getPage(),req.getSize());
        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        List<TrainCarriage> trainCarriages = trainCarriageMapper.selectByExample(trainCarriageExample);
        PageInfo<TrainCarriage> pageInfo=new PageInfo<>(trainCarriages);
        PageResp<TrainCarriageQueryResp> pageResp=new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());
        pageResp.setList(BeanUtil.copyToList(trainCarriages, TrainCarriageQueryResp.class));
        return pageResp;
    }

    public void delete(Long id) {
        trainCarriageMapper.deleteByPrimaryKey(id);
    }
    public List<TrainCarriage> selectAllByCode(String trainCode) {
        TrainCarriageExample trainCarriageExample = new TrainCarriageExample();
        trainCarriageExample.createCriteria().andTrainCodeEqualTo(trainCode);
        trainCarriageExample.setOrderByClause("'index' asc");
        return trainCarriageMapper.selectByExample(trainCarriageExample);
    }
}