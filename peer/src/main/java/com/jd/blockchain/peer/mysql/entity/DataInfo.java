package com.jd.blockchain.peer.mysql.entity;

/**
 * @Author: zhangshuang
 * @Date: 2022/5/9 3:09 PM
 * Version 1.0
 */
public class DataInfo {
    private int id;
    private String ledger;
    private String data_account_address;
    private String data_account_pubkey;
    private String data_account_roles;
    private int data_account_privileges;
    private String data_account_creator;
    private String data_account_tx_hash;
//    private long create_time;
    private long data_account_block_height;
    private int state;

    public DataInfo(String ledger, String data_account_address, String data_account_pubkey, String data_account_roles, int data_account_privileges,
                    String data_account_creator, String data_account_tx_hash, long data_account_block_height) {
        this.ledger = ledger;
        this.data_account_address = data_account_address;
        this.data_account_pubkey = data_account_pubkey;
        this.data_account_roles = data_account_roles;
        this.data_account_privileges = data_account_privileges;
        this.data_account_creator = data_account_creator;
        this.data_account_tx_hash = data_account_tx_hash;
        this.data_account_block_height = data_account_block_height;
    }

    public String getLedger() {
        return ledger;
    }

    public String getData_account_address() {
        return data_account_address;
    }

    public String getData_account_pubkey() {
        return data_account_pubkey;
    }

    public String getData_account_roles() {
        return data_account_roles;
    }

//    public long getCreate_time() {
//        return create_time;
//    }

    public String getData_account_creator() {
        return data_account_creator;
    }

    public long getData_account_block_height() {
        return data_account_block_height;
    }

    public int getData_account_privileges() {
        return data_account_privileges;
    }

    public String getData_account_tx_hash() {
        return data_account_tx_hash;
    }

    public int getId() {
        return id;
    }

    public int getState() {
        return state;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setLedger(String ledger) {
        this.ledger = ledger;
    }

    public void setData_account_address(String data_account_address) {
        this.data_account_address = data_account_address;
    }

    public void setData_account_pubkey(String data_account_pubkey) {
        this.data_account_pubkey = data_account_pubkey;
    }

    public void setData_account_roles(String data_account_roles) {
        this.data_account_roles = data_account_roles;
    }


    public void setData_account_creator(String data_account_creator) {
        this.data_account_creator = data_account_creator;
    }

//    public void setCreate_time(long create_time) {
//        this.create_time = create_time;
//    }

    public void setState(int state) {
        this.state = state;
    }

    public void setData_account_block_height(long data_account_block_height) {
        this.data_account_block_height = data_account_block_height;
    }


    public void setData_account_tx_hash(String data_account_tx_hash) {
        this.data_account_tx_hash = data_account_tx_hash;
    }

    public void setData_account_privileges(int data_account_privileges) {
        this.data_account_privileges = data_account_privileges;
    }

    @Override
    public String toString() {
        return "DataInfo{" +
                ", ledger='" + ledger + '\'' +
                ", data_account_address='" + data_account_address + '\'' +
                ", data_account_pubkey='" + data_account_pubkey + '\'' +
                ", data_account_roles='" + data_account_roles + '\'' +
                ", data_account_privileges='" + data_account_privileges + '\'' +
                ", data_account_creator='" + data_account_creator + '\'' +
                ", data_account_tx_hash='" + data_account_tx_hash + '\'' +
//                ", create_time=" + create_time +
                ", data_account_block_height=" + data_account_block_height +
                ", state=" + state +
                '}';
    }

}
