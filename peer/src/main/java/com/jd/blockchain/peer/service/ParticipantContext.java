package com.jd.blockchain.peer.service;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.LedgerAdminInfo;
import com.jd.blockchain.ledger.core.LedgerRepository;
import utils.net.SSLSecurity;

import java.util.HashMap;
import java.util.Map;

public final class ParticipantContext {

    public static final String HASH_ALG_PROP = "HASH_ALG";
    public static final String ENDPOINT_SIGNER_PROP = "ENDPOINT_SIGNER";

    private static final ThreadLocal<ParticipantContext> THREAD_LOCAL_CONTEXT = new ThreadLocal<>();

    private Map<String, Object> properties = new HashMap<>();

    private HashDigest ledgerHash;

    private LedgerRepository ledgerRepo;

    private LedgerAdminInfo ledgerAdminInfo;

    private String provider;

    private IParticipantManagerService participantService;

    private SSLSecurity sslSecurity;

    private ParticipantContext() {
    }

    public ParticipantContext(HashDigest ledgerHash,
                              LedgerRepository ledgerRepo,
                              LedgerAdminInfo ledgerAdminInfo,
                              String provider,
                              IParticipantManagerService participantService,
                              SSLSecurity sslSecurity) {
        this.ledgerHash = ledgerHash;
        this.ledgerRepo = ledgerRepo;
        this.ledgerAdminInfo = ledgerAdminInfo;
        this.provider = provider;
        this.participantService = participantService;
        this.sslSecurity = sslSecurity;
    }

    public static ParticipantContext buildContext(HashDigest ledgerHash,
                                                  LedgerRepository ledgerRepo,
                                                  LedgerAdminInfo ledgerAdminInfo,
                                                  String provider,
                                                  IParticipantManagerService participantService,
                                                  SSLSecurity sslSecurity) {

        ParticipantContext context = new ParticipantContext(ledgerHash, ledgerRepo, ledgerAdminInfo, provider, participantService, sslSecurity);
        THREAD_LOCAL_CONTEXT.set(context);
        return context;
    }


    public static ParticipantContext context() {
        return THREAD_LOCAL_CONTEXT.get();
    }

    public static void clear() {
        ParticipantContext context = THREAD_LOCAL_CONTEXT.get();
        if (context != null) {
            context.clean();
        }
        THREAD_LOCAL_CONTEXT.remove();
    }


    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }

    public Object getProperty(String name) {
        return properties.get(name);
    }

    public HashDigest ledgerHash() {
        return ledgerHash;
    }

    public LedgerRepository ledgerRepo() {
        return ledgerRepo;
    }

    public LedgerAdminInfo ledgerAdminInfo() {
        return ledgerAdminInfo;
    }

    public IParticipantManagerService participantService() {
        return participantService;
    }

    public SSLSecurity sslSecurity() {
        return sslSecurity;
    }

    public String provider() {
        return provider;
    }

    public void clean() {
        this.ledgerHash = null;
        this.ledgerRepo = null;
        this.ledgerAdminInfo = null;
        this.provider = null;
        this.participantService = null;
        this.sslSecurity = null;
        this.properties.clear();
    }


}
