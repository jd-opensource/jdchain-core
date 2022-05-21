package com.jd.blockchain.peer.mysql.entity;

/**
 * @Author: zhangshuang
 * @Date: 2022/5/9 3:14 PM
 * Version 1.0
 */
public class DataKv {
    private int id;
    private String ledger;
    private String data_account_address;
    private String data_key;
    private String data_type;
    private String data_tx_hash;
    private byte[] data_value;
    private long create_time;
    private long  data_block_height;
    private long data_version;
    private int state;

    public DataKv(String ledger, String data_account_address, String data_key, byte[] data_value, long data_version,
                  String data_type, String data_tx_hash, long  data_block_height) {
        this.ledger = ledger;
        this.data_account_address = data_account_address;
        this.data_key = data_key;
        this.data_value = data_value;
        this.data_type = data_type;
        this.data_version = data_version;
        this.data_tx_hash = data_tx_hash;
        this.data_block_height = data_block_height;
    }

    public int getId() {
        return id;
    }

    public String getLedger() {
        return ledger;
    }

    public String getData_account_address() {
        return data_account_address;
    }

    public byte[] getData_value() {
        return data_value;
    }

    public long getData_version() {
        return data_version;
    }

    public String getData_key() {
        return data_key;
    }

    public String getData_type() {
        return data_type;
    }

    public long getCreate_time() {
        return create_time;
    }

    public int getState() {
        return state;
    }

    public long getData_block_height() {
        return data_block_height;
    }

    public String getData_tx_hash() {
        return data_tx_hash;
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

    public void setData_key(String data_key) {
        this.data_key = data_key;
    }

    public void setData_type(String data_type) {
        this.data_type = data_type;
    }

    public void setData_value(byte[] data_value) {
        this.data_value = data_value;
    }

    public void setData_version(long data_version) {
        this.data_version = data_version;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setCreate_time(long create_time) {
        this.create_time = create_time;
    }

    public void setData_block_height(long data_block_height) {
        this.data_block_height = data_block_height;
    }

    public void setData_tx_hash(String data_tx_hash) {
        this.data_tx_hash = data_tx_hash;
    }

    @Override
    public String toString() {
        return "DataKv{" +
                ", ledger='" + ledger + '\'' +
                ", data_account_address='" + data_account_address + '\'' +
                ", data_key='" + data_key + '\'' +
                ", data_type='" + data_type + '\'' +
                ", data_value='" + data_value + '\'' +
                ", data_tx_hash='" + data_tx_hash + '\'' +
                ", data_version=" + data_version +
                ", data_block_height=" + data_block_height +
                ", create_time=" + create_time +
                ", state=" + state +
                '}';
    }

}
