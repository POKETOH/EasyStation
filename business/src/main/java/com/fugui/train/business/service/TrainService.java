package com.fugui.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.fugui.train.business.domain.*;
import com.fugui.train.business.enums.SeatColEnum;
import com.fugui.train.business.mapper.TrainMapper;
import com.fugui.train.business.mapper.TrainSeatMapper;
import com.fugui.train.business.req.TrainQueryReq;
import com.fugui.train.business.req.TrainSaveReq;
import com.fugui.train.business.resp.TrainQueryResp;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class TrainService {
    @Resource
    private TrainMapper trainMapper;
    @Resource
    private TrainSeatMapper trainSeatMapper;
    @Resource TrainCarriageService trainCarriageService;
    public int count() {
        return Math.toIntExact(trainMapper.countByExample(null));
    }
    private final Logger LOG= LoggerFactory.getLogger(TrainService.class);

    public void save(TrainSaveReq req) {
        Date cur = new Date();
        req.setUpdateTime(cur);
        //如果用户id存在，则执行更新操作
        Train train = BeanUtil.copyProperties(req, Train.class);
        if(req.getId()==null) {
            TrainExample trainExample=new TrainExample();
            trainExample.createCriteria().andCodeEqualTo(req.getCode());
            if(!trainMapper.selectByExample(trainExample).isEmpty()){
                throw new BusinessException(BusinessExceptionEnum.TRAIN_CODE_EXIST_ERROR);
            }
            train.setId(SnowUtil.getSnowflaskNextId());
            train.setCreateTime(cur);
            trainMapper.insert(train);
        }else{
            trainMapper.updateByPrimaryKey(train);
        }
    }
    public PageResp<TrainQueryResp> queryList(TrainQueryReq req) {
        TrainExample trainExample=new TrainExample();
        if(req.getId()!=null){
            trainExample.createCriteria().andIdEqualTo(req.getId());
        }
        PageHelper.startPage(req.getPage(),req.getSize());
        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        List<Train> trains = trainMapper.selectByExample(trainExample);
        PageInfo<Train> pageInfo=new PageInfo<>(trains);
        PageResp<com.fugui.train.business.resp.TrainQueryResp> pageResp=new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());
        pageResp.setList(BeanUtil.copyToList(trains, com.fugui.train.business.resp.TrainQueryResp.class));
        return pageResp;
    }

    public void delete(Long id) {
        trainMapper.deleteByPrimaryKey(id);
    }
    public List<TrainQueryResp> queryAll() {
        TrainExample trainExample=new TrainExample();
        trainExample.setOrderByClause("code asc");
        List<Train> trains = trainMapper.selectByExample(trainExample);
        return BeanUtil.copyToList(trains,TrainQueryResp.class);
    }
    //大量数据操作
    @Transactional
    public void genSeat(String trainCode) {
        //清空座位数据
        TrainSeatExample trainSeatExample=new TrainSeatExample();
        trainSeatExample.createCriteria().andTrainCodeEqualTo(trainCode);
        trainSeatMapper.deleteByExample(trainSeatExample);
        //查询该车次的所有车厢
        List<TrainCarriage> carriageList = trainCarriageService.selectAllByCode(trainCode);
        Date nowTime=new Date();
        for (TrainCarriage carriage : carriageList) {
            //获取当前车厢的所有信息
            Integer rowCount=carriage.getRowCount();
            String type=carriage.getSeatType();
            List<SeatColEnum> colsByType = SeatColEnum.getColsByType(type);
            Integer carriageIndex=0;
            for (int i=1;i<=rowCount;i++){
                for (SeatColEnum seatColEnum : colsByType) {
                    TrainSeat trainSeat=new TrainSeat();
                    trainSeat.setId(SnowUtil.getSnowflaskNextId());
                    trainSeat.setTrainCode(trainCode);
                    trainSeat.setCarriageIndex(carriage.getIndex());
                    trainSeat.setRow(StrUtil.fillBefore(String.valueOf(i),'0',2));
                    trainSeat.setCol(seatColEnum.getCode());
                    trainSeat.setSeatType(carriage.getSeatType());
                    trainSeat.setCarriageSeatIndex(carriageIndex++);
                    trainSeat.setCreateTime(nowTime);
                    trainSeat.setUpdateTime(nowTime);
                    trainSeatMapper.insert(trainSeat);
                }
            }
        }
    }
}