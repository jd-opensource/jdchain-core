package com.jd.blockchain.peer.consensus;

import com.jd.blockchain.consensus.BlockStateSnapshot;
import com.jd.blockchain.consensus.service.StateMachineReplicate;
import com.jd.blockchain.consensus.service.StateSnapshot;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.LedgerBlock;
import com.jd.blockchain.ledger.core.LedgerQuery;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LedgerStateManager implements StateMachineReplicate {

    private Map<String, LedgerQuery> ledgersQuery = new ConcurrentHashMap<>();

    public void setLedgerQuery(HashDigest ledger, LedgerQuery ledgerQuery) {
        this.ledgersQuery.put(ledger.toBase58(), ledgerQuery);
    }

    @Override
    public long getLatestStateID(String realmName) {
        return ledgersQuery.get(realmName).getLatestBlockHeight();
    }

    @Override
    public StateSnapshot getSnapshot(String realmName, long stateId) {
        LedgerBlock block = ledgersQuery.get(realmName).getBlock(stateId);

        if (null != block) {
            return new BlockStateSnapshot(block.getHeight(), block.getTimestamp(), block.getHash());
        }

        return null;
    }

    @Override
    public Iterator<StateSnapshot> getSnapshots(String realmName, long fromStateId, long toStateId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream readState(String realmName, long stateId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setupState(String realmName, StateSnapshot snapshot, InputStream state) {
        // TODO Auto-generated method stub

    }

}
