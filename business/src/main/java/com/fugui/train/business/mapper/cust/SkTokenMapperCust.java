package com.fugui.train.business.mapper.cust;

import org.apache.ibatis.annotations.Param;

import java.util.Date;

public interface SkTokenMapperCust {

    Integer updateSkToken(
            @Param("date") Date date
            , @Param("trainCode") String trainCode, int i);
}
