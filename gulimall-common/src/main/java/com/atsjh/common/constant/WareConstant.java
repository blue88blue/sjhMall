package com.atsjh.common.constant;

/**
 * @author: sjh
 * @date: 2021/6/23 下午7:48
 * @description:
 */
public class WareConstant {
    public enum PurchaseStatusEnum{
        CREATED(0, "新建"),
        ASSIGNED(1, "已分配"),
        RECIVED(2, "已领取"),
        FINISHED(3, "采购结束"),
        ERROR(4, "采购异常");

        PurchaseStatusEnum(int code, String msg){
            this.code = code;
            this.msg = msg;
        }
        private int code;
        private String msg;

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }

    public enum PurchaseDetailStatusEnum{
        CREATED(0, "新建"),
        ASSIGNED(1, "已分配"),
        BUYING(2, "正在采购"),
        FINISHED(3, "已完成"),
        ERROR(4, "采购失败");

        PurchaseDetailStatusEnum(int code, String msg){
            this.code = code;
            this.msg = msg;
        }
        private int code;
        private String msg;

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }

}
