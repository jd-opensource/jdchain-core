package com.jd.blockchain.peer.mysql.entity;

/**
 * @Author: zhangshuang
 * @Date: 2022/5/13 11:32 AM
 * Version 1.0
 */
public class RolePrivilegeInfo {
    private int id;
    private String ledger;
    private String role;
    private String ledger_privileges;
    private String tx_privileges;
    private long block_height;
    private String tx_hash;
    private long create_time;
    private int state;

    public RolePrivilegeInfo(String ledger, String role, String ledger_privileges, String tx_privileges, long block_height, String tx_hash) {
        this.ledger = ledger;
        this.role = role;
        this.ledger_privileges = ledger_privileges;
        this.tx_privileges = tx_privileges;
        this.block_height =block_height;
        this.tx_hash = tx_hash;
    }

    public int getState() {
        return state;
    }

    public long getCreate_time() {
        return create_time;
    }

    public String getLedger() {
        return ledger;
    }

    public int getId() {
        return id;
    }

    public String getLedger_privileges() {
        return ledger_privileges;
    }

    public String getTx_privileges() {
        return tx_privileges;
    }

    public String getTx_hash() {
        return tx_hash;
    }

    public long getBlock_height() {
        return block_height;
    }

    public String getRole() {
        return role;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setCreate_time(long create_time) {
        this.create_time = create_time;
    }

    public void setLedger(String ledger) {
        this.ledger = ledger;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setLedger_privileges(String ledger_privileges) {
        this.ledger_privileges = ledger_privileges;
    }

    public void setTx_privileges(String tx_privileges) {
        this.tx_privileges = tx_privileges;
    }

    public void setTx_hash(String tx_hash) {
        this.tx_hash = tx_hash;
    }

    public void setBlock_height(long block_height) {
        this.block_height = block_height;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "RolePrivilegeInfo{" +
                ", ledger='" + ledger + '\'' +
                ", block_height=" + block_height +
                ", role='" + role + '\'' +
                ", ledger_privileges='" + ledger_privileges + '\'' +
                ", tx_privileges='" + tx_privileges + '\'' +
                ", tx_hash='" + tx_hash + '\'' +
                ", create_time=" + create_time +
                ", state=" + state +
                '}';
    }
}
