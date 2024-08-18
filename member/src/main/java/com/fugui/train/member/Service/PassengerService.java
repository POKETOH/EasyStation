package com.fugui.train.member.Service;

import cn.hutool.core.bean.BeanUtil;
import com.fugui.train.common.resp.PageResp;
import com.fugui.train.common.context.LoginMemberContext;
import com.fugui.train.common.util.SnowUtil;
import com.fugui.train.member.domain.Passenger;
import com.fugui.train.member.domain.PassengerExample;
import com.fugui.train.member.mapper.PassengerMapper;
import com.fugui.train.member.req.PassengerQueryReq;
import com.fugui.train.member.req.PassengerSaveReq;
import com.fugui.train.member.resp.PassengerQueryResp;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class PassengerService {
    @Resource
    private PassengerMapper passengerMapper;
    public int count() {
        return Math.toIntExact(passengerMapper.countByExample(null));
    }
    private final Logger LOG= LoggerFactory.getLogger(PassengerService.class);

    public void save(PassengerSaveReq req) {
        Date cur = new Date();
        req.setUpdateTime(cur);
        //如果用户id存在，则执行更新操作
        Passenger passenger = BeanUtil.copyProperties(req, Passenger.class);
        if(req.getId()==null) {
            passenger.setId(SnowUtil.getSnowflaskNextId());
            passenger.setMemberId(LoginMemberContext.getId());
            passenger.setCreateTime(cur);
            passengerMapper.insert(passenger);
        }else{
            passengerMapper.updateByPrimaryKey(passenger);
        }
    }
    public PageResp<PassengerQueryResp> queryList(PassengerQueryReq req) {
        PassengerExample passengerExample=new PassengerExample();
        if(req.getMemberId()!=null){
            passengerExample.createCriteria().andMemberIdEqualTo(req.getMemberId());
        }
        PageHelper.startPage(req.getPage(),req.getSize());
        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        List<Passenger> passengers = passengerMapper.selectByExample(passengerExample);
        PageInfo<Passenger> pageInfo=new PageInfo<>(passengers);
        PageResp<PassengerQueryResp> pageResp=new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());
        pageResp.setList(BeanUtil.copyToList(passengers, PassengerQueryResp.class));
        return pageResp;
    }

    public void delete(Long id) {
        passengerMapper.deleteByPrimaryKey(id);
    }

    public List<Passenger> queryMine() {
        PassengerExample passengerExample=new PassengerExample();
        passengerExample.createCriteria().andMemberIdEqualTo(LoginMemberContext.getMember().getId());
        List<Passenger> passengerList = passengerMapper.selectByExample(passengerExample);
        return passengerList;
    }
}