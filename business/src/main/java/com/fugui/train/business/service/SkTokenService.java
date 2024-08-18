package com.fugui.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.fugui.train.business.domain.SkToken;
import com.fugui.train.business.domain.SkTokenExample;
import com.fugui.train.business.domain.Train;
import com.fugui.train.business.enums.RedisKeyPreEnum;
import com.fugui.train.business.mapper.SkTokenMapper;
import com.fugui.train.business.mapper.cust.SkTokenMapperCust;
import com.fugui.train.business.req.SkTokenQueryReq;
import com.fugui.train.business.req.SkTokenSaveReq;
import com.fugui.train.business.resp.SkTokenQueryResp;
import com.fugui.train.common.resp.PageResp;
import com.fugui.train.common.util.SnowUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SkTokenService {

    private static final Logger LOG = LoggerFactory.getLogger(SkTokenService.class);

    @Resource
    private SkTokenMapper skTokenMapper;
    @Resource
    private SkTokenMapperCust skTokenMapperCust;
    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private DailyTrainSeatService dailyTrainSeatService;
    @Resource
    private DailyTrainStationService dailyTrainStationService;

    public void save(SkTokenSaveReq req) {
        DateTime now = DateTime.now();
        SkToken skToken = BeanUtil.copyProperties(req, SkToken.class);
        if (ObjectUtil.isNull(skToken.getId())) {
            skToken.setId(SnowUtil.getSnowflaskNextId());
            skToken.setCreateTime(now);
            skToken.setUpdateTime(now);
            skTokenMapper.insert(skToken);
        } else {
            skToken.setUpdateTime(now);
            skTokenMapper.updateByPrimaryKey(skToken);
        }
    }

    public PageResp<SkTokenQueryResp> queryList(SkTokenQueryReq req) {
        SkTokenExample skTokenExample = new SkTokenExample();
        skTokenExample.setOrderByClause("id desc");
        SkTokenExample.Criteria criteria = skTokenExample.createCriteria();

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<SkToken> skTokenList = skTokenMapper.selectByExample(skTokenExample);

        PageInfo<SkToken> pageInfo = new PageInfo<>(skTokenList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<SkTokenQueryResp> list = BeanUtil.copyToList(skTokenList, SkTokenQueryResp.class);

        PageResp<SkTokenQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        skTokenMapper.deleteByPrimaryKey(id);
    }

    public void genDaily(Date date, Train train) {
        SkTokenExample skTokenExample = new SkTokenExample();
        skTokenExample.createCriteria().andTrainCodeEqualTo(train.getCode()).andDateEqualTo(date);
        skTokenMapper.deleteByExample(skTokenExample);
        SkToken skToken = new SkToken();
        int seatCount = dailyTrainSeatService.countSeat(date, train);
        int stationCount = dailyTrainStationService.countStation(date, train);
        Date now = new Date();
        skToken.setId(SnowUtil.getSnowflaskNextId());
        skToken.setDate(date);
        skToken.setTrainCode(train.getCode());
        skToken.setCount(seatCount * stationCount * 3 / 4);
        skToken.setCreateTime(now);
        skToken.setUpdateTime(now);

        skToken.setUpdateTime(now);
        skTokenMapper.insert(skToken);
    }

    public boolean skTokenVail(Date date, String trainCode, Long memberId) {
        String lockKey = RedisKeyPreEnum.CONFIRM_ORDER + DateUtil.formatDate(date) + trainCode + memberId;
        Boolean setIfAbsent = redisTemplate.opsForValue().setIfAbsent(lockKey, lockKey, 5, TimeUnit.SECONDS);
        if (setIfAbsent) {
            LOG.info("抢票成功");
        } else {
            LOG.info("抢票失败");
            return false;
        }
        String countSk = RedisKeyPreEnum.SK_TOKEN_COUNT + DateUtil.formatDate(date) + trainCode + memberId;
        Object skCount = redisTemplate.opsForValue().get(countSk);
        if (skCount != null) {
            Long count = redisTemplate.opsForValue().decrement(countSk, 1);
            redisTemplate.expire(skCount, 60, TimeUnit.SECONDS);
            if (count < 0L) {
                return false;
            }
            if (count % 5 == 0) {
                skTokenMapperCust.updateSkToken(date, trainCode, 5);
            }
            return true;
        } else {
            SkTokenExample skTokenExample = new SkTokenExample();
            skTokenExample.createCriteria().andDateEqualTo(date).andTrainCodeEqualTo(trainCode);
            List<SkToken> skTokens = skTokenMapper.selectByExample(skTokenExample);
            if (CollUtil.isEmpty(skTokens)) {
                return false;
            }
            SkToken skToken = skTokens.get(0);
            if (skToken.getCount() <= 0) {
                return false;
            }
            redisTemplate.opsForValue().set(countSk, skToken.getCount() - 1, 60, TimeUnit.SECONDS);
            return true;
        }
//        Integer res=skTokenMapperCust.updateSkToken(date,trainCode);
//        if(res>0)return  true;
//        else return false;
    }
}
