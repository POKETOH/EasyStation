package com.fugui.train.business.req;

import com.fugui.train.common.req.PageReq;
import lombok.Data;
@Data
public class TrainStationQueryReq extends PageReq {
    String trainCode;
}
