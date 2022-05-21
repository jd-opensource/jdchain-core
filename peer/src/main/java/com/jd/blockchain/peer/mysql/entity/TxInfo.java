package com.jd.blockchain.peer.mysql.entity;

/**
 * @Author: zhangshuang
 * @Date: 2022/5/9 3:10 PM
 * Version 1.0
 */
public class TxInfo {
    private int id;
    private String ledger;
    private String tx_hash;
    private String tx_node_pubkeys;
    private String tx_endpoint_pubkeys;
    private String tx_response_msg;
    private long create_time;
    private long tx_block_height;
    private int tx_index;
    private int tx_response_state;
    private byte[] tx_contents;
    private int state;

    public TxInfo(String ledger, long tx_block_height, String tx_hash, int tx_index, String tx_node_pubkeys, String tx_endpoint_pubkeys, int tx_response_state, byte[] tx_contents) {
        this.ledger = ledger;
        this.tx_block_height = tx_block_height;
        this.tx_hash = tx_hash;
        this.tx_index = tx_index;
        this.tx_node_pubkeys = tx_node_pubkeys;
        this.tx_endpoint_pubkeys = tx_endpoint_pubkeys;
        this.tx_response_state = tx_response_state;
        this.tx_contents = tx_contents;
    }

    public int getId() {
        return id;
    }

    public String getLedger() {
        return ledger;
    }

    public int getTx_index() {
        return tx_index;
    }

    public long getTx_block_height() {
        return tx_block_height;
    }

    public String getTx_hash() {
        return tx_hash;
    }

    public String getTx_node_pubkeys() {
        return tx_node_pubkeys;
    }

    public String getTx_endpoint_pubkeys() {
        return tx_endpoint_pubkeys;
    }

    public int getTx_response_state() {
        return tx_response_state;
    }

    public String getTx_response_msg() {
        return tx_response_msg;
    }

    public byte[] getTx_contents() {
        return tx_contents;
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

    public void setTx_block_height(long tx_block_height) {
        this.tx_block_height = tx_block_height;
    }

    public void setTx_contents(byte[] tx_contents) {
        this.tx_contents = tx_contents;
    }

    public void setTx_endpoint_pubkeys(String tx_endpoint_pubkeys) {
        this.tx_endpoint_pubkeys = tx_endpoint_pubkeys;
    }

    public void setTx_node_pubkeys(String tx_node_pubkeys) {
        this.tx_node_pubkeys = tx_node_pubkeys;
    }

    public void setTx_hash(String tx_hash) {
        this.tx_hash = tx_hash;
    }

    public void setTx_index(int tx_index) {
        this.tx_index = tx_index;
    }

    public void setCreate_time(long create_time) {
        this.create_time = create_time;
    }

    public void setTx_response_msg(String tx_response_msg) {
        this.tx_response_msg = tx_response_msg;
    }

    public void setTx_response_state(int tx_response_state) {
        this.tx_response_state = tx_response_state;
    }

    public void setState(int state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "TxInfo{" +
                ", ledger='" + ledger + '\'' +
                ", tx_block_height=" + tx_block_height +
                ", tx_index=" + tx_index +
                ", tx_response_state=" + tx_response_state +
                ", tx_hash='" + tx_hash + '\'' +
                ", tx_node_pubkeys='" + tx_node_pubkeys + '\'' +
                ", tx_endpoint_pubkeys='" + tx_endpoint_pubkeys + '\'' +
//                ", tx_response_msg='" + tx_response_msg + '\'' +
                ", tx_contents='" + tx_contents + '\'' +
                ", create_time=" + create_time +
                ", state=" + state +
                '}';
    }
}
