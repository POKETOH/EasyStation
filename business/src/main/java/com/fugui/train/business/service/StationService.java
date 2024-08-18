package com.fugui.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import com.fugui.train.business.domain.Station;
import com.fugui.train.business.domain.StationExample;
import com.fugui.train.business.mapper.StationMapper;
import com.fugui.train.business.req.StationQueryReq;
import com.fugui.train.business.req.StationSaveReq;
import com.fugui.train.business.resp.StationQueryResp;
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
public class StationService {
    @Resource
    private StationMapper stationMapper;

    public int count() {
        return Math.toIntExact(stationMapper.countByExample(null));
    }

    private final Logger LOG = LoggerFactory.getLogger(StationService.class);

    public void save(StationSaveReq req) {
        Date cur = new Date();
        req.setUpdateTime(cur);
        //如果用户id存在，则执行更新操作
        Station station = BeanUtil.copyProperties(req, Station.class);
        if (req.getId() == null) {
            //判断车站名是否已经存在
            StationExample stationExample=new StationExample();
            stationExample.createCriteria().andNameEqualTo(req.getName());
            if(!stationMapper.selectByExample(stationExample).isEmpty()){
                throw new BusinessException(BusinessExceptionEnum.STATION_NAME_EXIST_ERROR);
            }
            station.setId(SnowUtil.getSnowflaskNextId());
            station.setCreateTime(cur);
            stationMapper.insert(station);
        } else {
            stationMapper.updateByPrimaryKey(station);
        }
    }

    public PageResp<StationQueryResp> queryList(StationQueryReq req) {
        StationExample stationExample = new StationExample();
        PageHelper.startPage(req.getPage(), req.getSize());
        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        List<Station> stations = stationMapper.selectByExample(stationExample);
        PageInfo<Station> pageInfo = new PageInfo<>(stations);
        PageResp<StationQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());
        pageResp.setList(BeanUtil.copyToList(stations, StationQueryResp.class));
        return pageResp;
    }

    public void delete(Long id) {
        stationMapper.deleteByPrimaryKey(id);
    }

    public List<StationQueryResp> queryAll() {
        StationExample stationExample = new StationExample();
        List<Station> stations = stationMapper.selectByExample(stationExample);
        return BeanUtil.copyToList(stations, StationQueryResp.class);
    }
}