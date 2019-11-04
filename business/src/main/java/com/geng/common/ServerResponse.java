package com.geng.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * 服务端向前端返回的高复用的对象[json格式]
 *  - 1. @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
 *      SpringBoot 2.0 弃用，故用 @JsonInclude(JsonInclude.Include.NON_NULL)
 *      让字段为null的属性不被序列化成json
 *  - 2. @JsonIgnore
 *      系统会将isXXX作为某个getter方法，从而将其转为json，而本需求不需要，故忽略
 * @param <T>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServerResponse<T> {
    private int status; //返回给前端的状态码
    private T data; //返回给前端的数据
    private String msg; //当 status != 0时的，错误信息

    private ServerResponse() {
    }

    private ServerResponse(int status) {
        this.status = status;
    }

    private ServerResponse(int status, T data) {
        this.status = status;
        this.data = data;
    }

    private ServerResponse(int status, String msg) {
        this.status = status;
        this.msg = msg;
    }
    private ServerResponse(int status, T data, String msg) {
        this.status = status;
        this.data = data;
        this.msg = msg;
    }

    /**
     * 调用接口成功时回调
     */
    public static ServerResponse createServerResponseBySuccess(){
        return new ServerResponse(ResponseCode.SUCCESS);
    }

    public  static  <T> ServerResponse createServerResponseBySuccess(T data){
        return new ServerResponse(ResponseCode.SUCCESS,data);
    }

    public  static  <T> ServerResponse createServerResponseBySuccess(T data,String msg){
        return new ServerResponse(ResponseCode.SUCCESS,data,msg);

    }

    /**
     * 调用接口失败时回调
     */

    public static ServerResponse createServerResponseByError(){
        return new ServerResponse(ResponseCode.ERROR);
    }

    public static  ServerResponse createServerResponseByError(String msg){
        return new ServerResponse(ResponseCode.ERROR,msg);
    }

    public static  ServerResponse createServerResponseByError(int status){
        return new ServerResponse(status);
    }

    public static  ServerResponse createServerResponseByError(int status,String msg){
        return new ServerResponse(status,msg);

    }

    /**
     * 判断接口是否返回正确
     * status == 0
     */
    @JsonIgnore
    public boolean isSuccess(){
        return this.status == ResponseCode.SUCCESS;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}














