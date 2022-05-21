package com.jd.blockchain.peer.mysql.entity;

/**
 * @Author: zhangshuang
 * @Date: 2022/5/9 3:13 PM
 * Version 1.0
 */
public class BlockInfo {
    private int id;
    private String ledger;
    private long block_height;
    private String block_hash;
    private String pre_block_hash;
    private String txs_set_hash;
    private String users_set_hash;
    private String contracts_set_hash;
    private String configurations_set_hash;
    private String dataaccounts_set_hash;
    private String eventaccounts_set_hash;
    private long block_timestamp;
    private long create_time;
    private int state;

    public BlockInfo(String ledger, long block_height, String block_hash, String pre_block_hash, String txs_set_hash, String users_set_hash,
                      String dataaccounts_set_hash, String contracts_set_hash, String eventaccounts_set_hash, String configurations_set_hash,
                      long block_timestamp) {
        this.ledger = ledger;
        this.block_height = block_height;
        this.block_hash = block_hash;
        this.pre_block_hash = pre_block_hash;
        this.txs_set_hash = txs_set_hash;
        this.users_set_hash = users_set_hash;
        this.dataaccounts_set_hash = dataaccounts_set_hash;
        this.contracts_set_hash = contracts_set_hash;
        this.eventaccounts_set_hash = eventaccounts_set_hash;
        this.configurations_set_hash = configurations_set_hash;
        this.block_timestamp = block_timestamp;
    }

    public int getId() {
        return id;
    }

    public String getLedger() {
        return ledger;
    }

    public long getBlock_height() {
        return block_height;
    }

    public long getBlock_timestamp() {
        return block_timestamp;
    }

    public String getBlock_hash() {
        return block_hash;
    }

    public String getConfigurations_set_hash() {
        return configurations_set_hash;
    }

    public String getContracts_set_hash() {
        return contracts_set_hash;
    }

    public String getDataaccounts_set_hash() {
        return dataaccounts_set_hash;
    }

    public String getPre_block_hash() {
        return pre_block_hash;
    }

    public String getEventaccounts_set_hash() {
        return eventaccounts_set_hash;
    }

    public String getTxs_set_hash() {
        return txs_set_hash;
    }

    public String getUsers_set_hash() {
        return users_set_hash;
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

    public void setBlock_hash(String block_hash) {
        this.block_hash = block_hash;
    }

    public void setBlock_height(long block_height) {
        this.block_height = block_height;
    }

    public void setConfigurations_set_hash(String configurations_set_hash) {
        this.configurations_set_hash = configurations_set_hash;
    }

    public void setContracts_set_hash(String contracts_set_hash) {
        this.contracts_set_hash = contracts_set_hash;
    }

    public void setDataaccounts_set_hash(String dataaccounts_set_hash) {
        this.dataaccounts_set_hash = dataaccounts_set_hash;
    }

    public void setEventaccounts_set_hash(String eventaccounts_set_hash) {
        this.eventaccounts_set_hash = eventaccounts_set_hash;
    }

    public void setPre_block_hash(String pre_block_hash) {
        this.pre_block_hash = pre_block_hash;
    }

    public void setTxs_set_hash(String txs_set_hash) {
        this.txs_set_hash = txs_set_hash;
    }

    public void setUsers_set_hash(String users_set_hash) {
        this.users_set_hash = users_set_hash;
    }

    public void setBlock_timestamp(long block_timestamp) {
        this.block_timestamp = block_timestamp;
    }

    public void setCreate_time(long create_time) {
        this.create_time = create_time;
    }

    public void setState(int state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "BlockInfo{" +
                ", ledger='" + ledger + '\'' +
                ", block_height=" + block_height +
                ", block_hash='" + block_hash + '\'' +
                ", pre_block_hash='" + pre_block_hash + '\'' +
                ", txs_set_hash='" + txs_set_hash + '\'' +
                ", users_set_hash='" + users_set_hash + '\'' +
                ", contracts_set_hash='" + contracts_set_hash + '\'' +
                ", configurations_set_hash='" + configurations_set_hash + '\'' +
                ", dataaccounts_set_hash='" + dataaccounts_set_hash + '\'' +
                ", eventaccounts_set_hash='" + dataaccounts_set_hash + '\'' +
                ", block_timestamp=" + block_timestamp +
                ", create_time=" + create_time +
                ", state=" + state +
            '}';
    }

}
