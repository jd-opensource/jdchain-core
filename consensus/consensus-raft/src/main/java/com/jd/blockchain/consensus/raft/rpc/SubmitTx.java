package com.jd.blockchain.consensus.raft.rpc;

import com.alipay.sofa.jraft.Closure;

import java.io.Serializable;
import java.util.Arrays;

public class SubmitTx implements Serializable {

    private static final long serialVersionUID = -3787277896422263587L;

    private byte[] values;

    private Closure done;

    private boolean blockEvent = false;

    public SubmitTx() {
    }

    public SubmitTx(byte[] values, Closure done) {
        this.values = values;
        this.done = done;
    }

    public void reset(){
        this.values = null;
        this.done = null;
        this.blockEvent = false;
    }

    public byte[] getValues() {
        return values;
    }

    public void setValues(byte[] values) {
        this.values = values;
    }

    public Closure getDone() {
        return done;
    }

    public void setDone(Closure done) {
        this.done = done;
    }

    public boolean isBlockEvent() {
        return blockEvent;
    }

    public void setBlockEvent(boolean blockEvent) {
        this.blockEvent = blockEvent;
    }

    @Override
    public String toString() {
        return "SubmitTx{" +
                "values=" + Arrays.toString(values) +
                ", done=" + done +
                '}';
    }
}
