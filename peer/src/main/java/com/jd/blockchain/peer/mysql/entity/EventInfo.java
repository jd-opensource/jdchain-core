package com.jd.blockchain.peer.mysql.entity;

/**
 * @Author: zhangshuang
 * @Date: 2022/5/9 3:13 PM
 * Version 1.0
 */
public class EventInfo {
    private int id;
    private String ledger;
    private String event_account_address;
    private String event_account_pubkey;
    private String event_account_roles;
    private String event_account_privileges;
    private String event_account_creator;
    private String event_account_tx_hash;
//    private long create_time;
    private long event_account_block_height;
    private int state;

    public EventInfo(String ledger, String event_account_address, String event_account_pubkey, String event_account_roles,
                     String event_account_privileges, String event_account_creator, String event_account_tx_hash, long event_account_block_height) {
        this.ledger = ledger;
        this.event_account_address = event_account_address;
        this.event_account_pubkey = event_account_pubkey;
        this.event_account_roles = event_account_roles;
        this.event_account_privileges = event_account_privileges;
        this.event_account_creator = event_account_creator;
        this.event_account_tx_hash = event_account_tx_hash;
        this.event_account_block_height = event_account_block_height;
    }

    public String getEvent_account_address() {
        return event_account_address;
    }

    public String getEvent_account_pubkey() {
        return event_account_pubkey;
    }

    public String getEvent_account_roles() {
        return event_account_roles;
    }

    public String getEvent_account_privileges() {
        return event_account_privileges;
    }

    public String getEvent_account_creator() {
        return event_account_creator;
    }

    public String getLedger() {
        return ledger;
    }

    public int getId() {
        return id;
    }

    public int getState() {
        return state;
    }

//    public long getCreate_time() {
//        return create_time;
//    }

    public long getEvent_account_block_height() {
        return event_account_block_height;
    }

    public void setEvent_account_privileges(String event_account_privileges) {
        this.event_account_privileges = event_account_privileges;
    }

    public String getEvent_account_tx_hash() {
        return event_account_tx_hash;
    }

    public void setLedger(String ledger) {
        this.ledger = ledger;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setEvent_account_address(String event_account_address) {
        this.event_account_address = event_account_address;
    }

    public void setEvent_account_pubkey(String event_account_pubkey) {
        this.event_account_pubkey = event_account_pubkey;
    }

    public void setEvent_account_roles(String event_account_roles) {
        this.event_account_roles = event_account_roles;
    }

    public void setEvent_account_creator(String event_account_creator) {
        this.event_account_creator = event_account_creator;
    }

//    public void setCreate_time(long create_time) {
//        this.create_time = create_time;
//    }

    public void setState(int state) {
        this.state = state;
    }

    public void setEvent_account_block_height(long event_account_block_height) {
        this.event_account_block_height = event_account_block_height;
    }

    public void setEvent_account_tx_hash(String event_account_tx_hash) {
        this.event_account_tx_hash = event_account_tx_hash;
    }

    @Override
    public String toString() {
        return "EventInfo{" +
                ", ledger='" + ledger + '\'' +
                ", event_account_address='" + event_account_address + '\'' +
                ", event_account_pubkey='" + event_account_pubkey + '\'' +
                ", event_account_roles='" + event_account_roles + '\'' +
                ", event_account_privileges='" + event_account_privileges + '\'' +
                ", event_account_creator='" + event_account_creator + '\'' +
                ", event_account_tx_hash='" + event_account_tx_hash + '\'' +
//                ", create_time=" + create_time +
                ", event_account_block_height=" + event_account_block_height +
                ", state=" + state +
                '}';
    }

}
