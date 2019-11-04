package com.geng.common;

public enum PaymentTypeEnum {
    PAYMENT_TYPE_ONLINE(1,"在线支付"),
    PAYMENT_TYPE_OFFLINE(2,"货到付款")
    ;
    int code;
    String desc;
    public int getCode() {
        return code;
    }
    public void setCode(int code) {
        this.code = code;
    }
    public String getDesc() {
        return desc;
    }
    public void setDesc(String desc) {
        this.desc = desc;
    }
    PaymentTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    public static  PaymentTypeEnum codeOf(Integer code){
        for (PaymentTypeEnum paymentTypeEnum:values()){
            if(paymentTypeEnum.getCode() == code){
                return paymentTypeEnum;
            }
        }
        return  null;
    }
}