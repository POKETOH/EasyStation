package com.fugui.train.common.exception;


public enum BusinessExceptionEnum {

    MEMBER_MOBILE_EXIST("手机号已注册"),
    MEMBER_MOBILE_NOT_EXIST("请先获取验证码"),
    CONFIRM_ORDER_TICKET_COUNT_ERROR("余票不足"),
    CODE_ERROR("短信验证码错误"),
    STATION_NAME_EXIST_ERROR("当前车站已经存在"),
    TRAIN_CODE_EXIST_ERROR("当前车次已经存在"),
    CARRIAGE_CODE_AND_INDEX_EXIST_ERROR("当前厢次已经存在"),
    TRAIN_STATION_CODE_AND_INDEX_EXIST_ERROR("当前途径车站已经存在"),
    BUSINESS_NOT_GET_LOCK("访问人数过多，请重试"),
    CONFIRM_ORDER_FLOW_EXCEPTION("访问人数过多，请重试"),
    NOT_GET_SKTOKEN("车票抢完");

    private String desc;

    BusinessExceptionEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return "BusinessExceptionEnum{" +
                "desc='" + desc + '\'' +
                "} " + super.toString();
    }
}
