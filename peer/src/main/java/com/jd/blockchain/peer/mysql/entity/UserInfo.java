package com.jd.blockchain.peer.mysql.entity;

import java.util.Date;

/**
 * @Author: zhangshuang
 * @Date: 2022/5/9 3:09 PM
 * Version 1.0
 */

public class UserInfo {
    private int id;
    private String ledger;
    private String user_address;
    private String user_pubkey;
    private String user_key_algorithm;
    private String user_certificate;
    private String user_state;
    private String user_roles;
    private String user_roles_policy;
    private String user_tx_hash;
//    private Date create_time;
    private long user_block_height;
    private int state;

    public UserInfo(String ledger, String user_address, String user_pubkey, String user_key_algorithm, String user_certificate, String user_state, String user_roles, String user_roles_policy, String user_tx_hash, long user_block_height) {
        this.ledger = ledger;
        this.user_address = user_address;
        this.user_pubkey = user_pubkey;
        this.user_key_algorithm = user_key_algorithm;
        this.user_certificate = user_certificate;
        this.user_state = user_state;
        this.user_roles = user_roles;
        this.user_roles_policy = user_roles_policy;
        this.user_tx_hash = user_tx_hash;
        this.user_block_height = user_block_height;
    }

    public int getId() {
        return id;
    }

    public String getLedger() {
        return ledger;
    }

    public String getUser_address() {
        return user_address;
    }

    public String getUser_pubkey() {
        return user_pubkey;
    }

    public String getUser_key_algorithm() {
        return user_key_algorithm;
    }

    public String getUser_certificate() {
        return user_certificate;
    }

    public String getUser_roles() {
        return user_roles;
    }

    public String getUser_roles_policy() {
        return user_roles_policy;
    }

    public String getUser_state() {
        return user_state;
    }

//    public long getCreate_time() {
//        return create_time;
//    }

    public int getState() {
        return state;
    }

    public long getUser_block_height() {
        return user_block_height;
    }

    public String getUser_tx_hash() {
        return user_tx_hash;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setLedger(String ledger) {
        this.ledger = ledger;
    }

    public void setUser_address(String user_address) {
        this.user_address = user_address;
    }

    public void setUser_pubkey(String user_pubkey) {
        this.user_pubkey = user_pubkey;
    }

    public void setUser_key_algorithm(String user_key_algorithm) {
        this.user_key_algorithm = user_key_algorithm;
    }

    public void setUser_state(String user_state) {
        this.user_state = user_state;
    }

    public void setUser_roles(String user_roles) {
        this.user_roles = user_roles;
    }

    public void setUser_roles_policy(String user_roles_policy) {
        this.user_roles_policy = user_roles_policy;
    }

//    public void setCreate_time(long create_time) {
//        this.create_time = create_time;
//    }

    public void setState(int state) {
        this.state = state;
    }

    public void setUser_block_height(long user_block_height) {
        this.user_block_height = user_block_height;
    }

    public void setUser_tx_hash(String user_tx_hash) {
        this.user_tx_hash = user_tx_hash;
    }

    public void setUser_certificate(String user_certificate) {
        this.user_certificate = user_certificate;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                ", ledger='" + ledger + '\'' +
                ", user_address='" + user_address + '\'' +
                ", user_pubkey='" + user_pubkey + '\'' +
                ", user_key_algorithm='" + user_key_algorithm + '\'' +
                ", user_certificate='" + user_certificate + '\'' +
                ", user_state='" + user_state + '\'' +
                ", user_roles='" + user_roles + '\'' +
                ", user_roles_policy='" + user_roles_policy + '\'' +
                ", user_tx_hash='" + user_tx_hash + '\'' +
//                ", create_time=" + create_time +
                ", user_block_height=" + user_block_height +
                ", state=" + state +
                '}';
    }

}
