package com.fugui.train.business.req;

import com.fugui.train.common.req.PageReq;
import lombok.Data;

@Data
public class TrainQueryReq extends PageReq {
    private Long id;

    @Override
    public Integer getSize() {
        return super.getSize();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
