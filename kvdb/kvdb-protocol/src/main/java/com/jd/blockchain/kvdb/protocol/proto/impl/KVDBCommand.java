package com.jd.blockchain.kvdb.protocol.proto.impl;

import com.jd.blockchain.kvdb.protocol.proto.Command;
import com.jd.blockchain.utils.Bytes;

public class KVDBCommand implements Command {

    private String name;
    private Bytes[] parameters;

    public KVDBCommand(String name, Bytes... parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParameters(Bytes... parameters) {
        this.parameters = parameters;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Bytes[] getParameters() {
        return parameters;
    }
}
