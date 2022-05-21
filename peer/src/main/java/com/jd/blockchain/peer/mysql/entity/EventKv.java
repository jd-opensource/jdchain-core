package com.jd.blockchain.peer.mysql.entity;

/**
 * @Author: zhangshuang
 * @Date: 2022/5/9 3:14 PM
 * Version 1.0
 */
public class EventKv {
    private int id;
    private String ledger;
    private String event_account_address;
    private String event_name;
    private String event_tx_hash;
    private String event_type;
    private String event_value;
    private String event_contract_address;
    private long create_time;
    private long event_block_height;
    private int state;
    private long event_sequence;

    public EventKv(String ledger, String event_account_address, String event_name, long event_sequence, String event_type, String event_value,
                   String event_tx_hash, long event_block_height) {
        this.ledger = ledger;
        this.event_account_address = event_account_address;
        this.event_name = event_name;
        this.event_sequence = event_sequence;
        this.event_type = event_type;
        this.event_value = event_value;
        this.event_tx_hash = event_tx_hash;
        this.event_block_height = event_block_height;
    }

    public int getId() {
        return id;
    }

    public String getLedger() {
        return ledger;
    }

    public String getEvent_account_address() {
        return event_account_address;
    }

    public String getEvent_contract_address() {
        return event_contract_address;
    }

    public long getEvent_block_height() {
        return event_block_height;
    }

    public long getEvent_sequence() {
        return event_sequence;
    }

    public String getEvent_name() {
        return event_name;
    }

    public String getEvent_tx_hash() {
        return event_tx_hash;
    }

    public String getEvent_type() {
        return event_type;
    }

    public String getEvent_value() {
        return event_value;
    }

    public long getCreate_time() {
        return create_time;
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

    public void setEvent_account_address(String event_account_address) {
        this.event_account_address = event_account_address;
    }

    public void setEvent_block_height(long event_block_height) {
        this.event_block_height = event_block_height;
    }

    public void setEvent_contract_address(String event_contract_address) {
        this.event_contract_address = event_contract_address;
    }

    public void setEvent_sequence(long event_sequence) {
        this.event_sequence = event_sequence;
    }

    public void setEvent_name(String event_name) {
        this.event_name = event_name;
    }

    public void setEvent_tx_hash(String event_tx_hash) {
        this.event_tx_hash = event_tx_hash;
    }

    public void setEvent_type(String event_type) {
        this.event_type = event_type;
    }

    public void setEvent_value(String event_value) {
        this.event_value = event_value;
    }

    public void setCreate_time(long create_time) {
        this.create_time = create_time;
    }

    public void setState(int state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "EventKv{" +
                ", ledger='" + ledger + '\'' +
                ", event_block_height=" + event_block_height +
                ", event_sequence=" + event_sequence +
                ", event_account_address='" + event_account_address + '\'' +
                ", event_name='" + event_name + '\'' +
                ", event_tx_hash='" + event_tx_hash + '\'' +
                ", event_type='" + event_type + '\'' +
                ", event_value='" + event_value + '\'' +
                ", event_contract_address='" + event_contract_address + '\'' +
                ", create_time=" + create_time +
                ", state=" + state +
                '}';
    }

}
