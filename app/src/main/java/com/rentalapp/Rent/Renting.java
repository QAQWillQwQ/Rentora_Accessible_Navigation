package com.rentalapp.Rent;

//租房实体类
public class Renting {
    private int id;
    private int uid;
    private int hid;
    private String signature;//签名照片
    private String contract;//合同
    private String rentaltime;//租房期限
    private String addtime;//租房时间
    private String status;//合同状态（有效、续签、结束）

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getHid() {
        return hid;
    }

    public void setHid(int hid) {
        this.hid = hid;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getContract() {
        return contract;
    }

    public void setContract(String contract) {
        this.contract = contract;
    }

    public String getRentaltime() {
        return rentaltime;
    }

    public void setRentaltime(String rentaltime) {
        this.rentaltime = rentaltime;
    }

    public String getAddtime() {
        return addtime;
    }

    public void setAddtime(String addtime) {
        this.addtime = addtime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
