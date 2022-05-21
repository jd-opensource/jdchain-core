package com.jd.blockchain.peer.mysql.entity;

/**
 * @Author: zhangshuang
 * @Date: 2022/5/9 3:12 PM
 * Version 1.0
 */
public class ContractInfo {
    private int id;
    private String ledger;
    private String contract_address;
    private String contract_pubkey;
    private String contract_roles;
    private String contract_priviledges;
    private String contract_status;
    private String contract_creator;
    private String contract_lang;
    private String contract_tx_hash;
    private long create_time;
    private long contract_block_height;
    private int state;
    private long contract_version;
    private byte[] contract_content;

    public ContractInfo(String ledger, String contract_address, String contract_pubkey, String contract_status, String contract_roles, String contract_priviledges,
                        String contract_creator, String contract_lang, long contract_version, byte[] contract_content, String contract_tx_hash, long contract_block_height) {
        this.ledger = ledger;
        this.contract_address = contract_address;
        this.contract_pubkey = contract_pubkey;
        this.contract_status = contract_status;
        this.contract_roles = contract_roles;
        this.contract_priviledges = contract_priviledges;
        this.contract_creator = contract_creator;
        this.contract_lang = contract_lang;
        this.contract_version = contract_version;
        this.contract_content = contract_content;
        this.contract_tx_hash = contract_tx_hash;
        this.contract_block_height = contract_block_height;
    }

    public String getLedger() {
        return ledger;
    }

    public String getContract_address() {
        return contract_address;
    }

    public long getContract_version() {
        return contract_version;
    }

    public String getContract_pubkey() {
        return contract_pubkey;
    }

    public String getContract_roles() {
        return contract_roles;
    }

    public String getContract_priviledges() {
        return contract_priviledges;
    }

    public String getContract_status() {
        return contract_status;
    }

    public String getContract_creator() {
        return contract_creator;
    }

    public byte[] getContract_content() {
        return contract_content;
    }

    public String getContract_lang() {
        return contract_lang;
    }

    public int getId() {
        return id;
    }

    public long getCreate_time() {
        return create_time;
    }

    public int getState() {
        return state;
    }

    public long getContract_block_height() {
        return contract_block_height;
    }

    public String getContract_tx_hash() {
        return contract_tx_hash;
    }

    public void setLedger(String ledger) {
        this.ledger = ledger;
    }

    public void setContract_address(String contract_address) {
        this.contract_address = contract_address;
    }

    public void setContract_pubkey(String contract_pubkey) {
        this.contract_pubkey = contract_pubkey;
    }

    public void setContract_version(long contract_version) {
        this.contract_version = contract_version;
    }

    public void setContract_roles(String contract_roles) {
        this.contract_roles = contract_roles;
    }


    public void setContract_creator(String contract_creator) {
        this.contract_creator = contract_creator;
    }

    public void setContract_status(String contract_status) {
        this.contract_status = contract_status;
    }

    public void setContract_content(byte[] contract_content) {
        this.contract_content = contract_content;
    }

    public void setCreate_time(long create_time) {
        this.create_time = create_time;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setContract_lang(String contract_lang) {
        this.contract_lang = contract_lang;
    }

    public void setContract_block_height(long contract_block_height) {
        this.contract_block_height = contract_block_height;
    }

    public void setContract_priviledges(String contract_priviledges) {
        this.contract_priviledges = contract_priviledges;
    }

    public void setContract_tx_hash(String contract_tx_hash) {
        this.contract_tx_hash = contract_tx_hash;
    }

    @Override
    public String toString() {
        return "ContractInfo{" +
                ", ledger='" + ledger + '\'' +
                ", contract_version=" + contract_version +
                ", contract_address='" + contract_address + '\'' +
                ", contract_pubkey='" + contract_pubkey + '\'' +
                ", contract_roles='" + contract_roles + '\'' +
                ", contract_priviledges='" + contract_priviledges + '\'' +
                ", contract_status='" + contract_status + '\'' +
                ", contract_lang='" + contract_lang + '\'' +
                ", contract_creator='" + contract_creator + '\'' +
                ", contract_tx_hash='" + contract_tx_hash + '\'' +
                ", contract_content='" + contract_content + '\'' +
                ", create_time=" + create_time +
                ", contract_block_height=" + contract_block_height +
                ", state=" + state +
                '}';
    }
}
