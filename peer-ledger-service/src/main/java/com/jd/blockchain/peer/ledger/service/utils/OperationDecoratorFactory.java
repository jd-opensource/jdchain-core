package com.jd.blockchain.peer.ledger.service.utils;


import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.json.CryptoConfigInfo;
import com.jd.blockchain.transaction.*;

/**
 * Operation接口包装工厂
 *         用于将${@link Operation} 接口对象转换为具体的实现类（可用于JSON序列化）
 *
 * @author shaozhuguang
 *
 */
public class OperationDecoratorFactory {

    public static Operation decorate(Operation op) {
        if (op instanceof ContractCodeDeployOperation) {
            return decorateContractCodeDeployOperation((ContractCodeDeployOperation) op);
        } else if (op instanceof ContractEventSendOperation) {
            return decorateContractEventSendOperation((ContractEventSendOperation) op);
        } else if (op instanceof DataAccountKVSetOperation) {
            return decorateDataAccountKVSetOperation((DataAccountKVSetOperation) op);
        } else if (op instanceof DataAccountRegisterOperation) {
            return decorateDataAccountRegisterOperation((DataAccountRegisterOperation) op);
        } else if (op instanceof LedgerInitOperation) {
            return decorateLedgerInitOperation((LedgerInitOperation) op);
        } else if (op instanceof ParticipantRegisterOperation) {
            return decorateParticipantRegisterOperation((ParticipantRegisterOperation) op);
        } else if (op instanceof ParticipantStateUpdateOperation) {
            return decorateParticipantStateUpdateOperation((ParticipantStateUpdateOperation) op);
        } else if (op instanceof ConsensusSettingsUpdateOperation) {
            return decorateConsensusSettingsUpdateOperation((ConsensusSettingsUpdateOperation) op);
        } else if (op instanceof RolesConfigureOperation) {
            return decorateRolesConfigureOperation((RolesConfigureOperation) op);
        } else if (op instanceof UserAuthorizeOperation) {
            return decorateUserAuthorizeOperation((UserAuthorizeOperation) op);
        } else if (op instanceof UserRegisterOperation) {
            return decorateUserRegisterOperation((UserRegisterOperation) op);
        } else if (op instanceof EventAccountRegisterOperation) {
            return decorateEventAccountRegisterOperation((EventAccountRegisterOperation) op);
        } else if (op instanceof EventPublishOperation) {
            return decorateEventPublishOperation((EventPublishOperation) op);
        } else if (op instanceof UserCAUpdateOperation) {
            return decorateUserCAUpdateOperation((UserCAUpdateOperation) op);
        } else if (op instanceof UserStateUpdateOperation) {
            return decorateUserRevokeOperation((UserStateUpdateOperation) op);
        } else if (op instanceof RootCAUpdateOperation) {
            return decorateRootCAUpdateOperation((RootCAUpdateOperation) op);
        } else if (op instanceof ContractStateUpdateOperation) {
            return decorateContractStateUpdateOperation((ContractStateUpdateOperation) op);
        } else if (op instanceof AccountPermissionSetOperation) {
            return decorateAccountPermissionSetOperation((AccountPermissionSetOperation) op);
        }

        return null;
    }

    /**
     * decorate ContractCodeDeployOperation object
     *
     * @param op
     * @return
     */
    public static Operation decorateContractCodeDeployOperation(ContractCodeDeployOperation op) {
        BlockchainIdentity contractId = decorateBlockchainIdentity(op.getContractID());
        return new ContractCodeDeployOpTemplate(contractId, op.getChainCode(), op.getChainCodeVersion());
    }

    /**
     * decorate ContractEventSendOperation object
     *
     * @param op
     * @return
     */
    public static Operation decorateContractEventSendOperation(ContractEventSendOperation op) {
        BytesDataList dataList;
        if(null != op.getArgs()) {
            dataList = new BytesDataList(decorateBytesValues(op.getArgs().getValues()));
        } else {
            dataList = new BytesDataList();
        }
        return new ContractEventSendOpTemplate(op.getContractAddress(), op.getEvent(), dataList);
    }

    /**
     * decorate DataAccountKVSetOperation object
     *
     * @param op
     * @return
     */
    public static Operation decorateDataAccountKVSetOperation(DataAccountKVSetOperation op) {
        DataAccountKVSetOpTemplate opTemplate = new DataAccountKVSetOpTemplate(op.getAccountAddress());
        DataAccountKVSetOperation.KVWriteEntry[] writeSet = op.getWriteSet();
        if (writeSet != null && writeSet.length > 0) {
            for (DataAccountKVSetOperation.KVWriteEntry entry : writeSet) {
                opTemplate.set(entry.getKey(), decorateBytesValue(entry.getValue()), entry.getExpectedVersion());
            }
        }
        return opTemplate;
    }

    /**
     * decorate DataAccountRegisterOperation object
     *
     * @param op
     * @return
     */
    public static Operation decorateDataAccountRegisterOperation(DataAccountRegisterOperation op) {
        BlockchainIdentity accountId = decorateBlockchainIdentity(op.getAccountID());
        return new DataAccountRegisterOpTemplate(accountId);
    }

    /**
     * decorate LedgerInitOperation object
     *
     * @param op
     * @return
     */
    public static Operation decorateLedgerInitOperation(LedgerInitOperation op) {
        LedgerInitData ledgerInitData = new LedgerInitData();
        ledgerInitData.setConsensusSettings(op.getInitSetting().getConsensusSettings());
        ledgerInitData.setCryptoSetting(new CryptoConfigInfo(op.getInitSetting().getCryptoSetting()));
        ledgerInitData.setLedgerSeed(op.getInitSetting().getLedgerSeed());
        ledgerInitData.setIdentityMode(op.getInitSetting().getIdentityMode());
        if(op.getInitSetting().getIdentityMode() == IdentityMode.CA) {
            ledgerInitData.setLedgerCertificates(op.getInitSetting().getLedgerCertificates());
        }
        ledgerInitData.setConsensusProvider(op.getInitSetting().getConsensusProvider());
        ledgerInitData.setCreatedTime(op.getInitSetting().getCreatedTime());
        ledgerInitData.setLedgerDataStructure(op.getInitSetting().getLedgerDataStructure());
        ParticipantNode[] participantNodes = op.getInitSetting().getConsensusParticipants();
        if (participantNodes != null && participantNodes.length > 0) {
            ParticipantNode[] participants = new ParticipantNode[participantNodes.length];
            for (int i = 0; i < participantNodes.length; i++) {
                ParticipantNode participantNode = participantNodes[i];
                ConsensusParticipantData participant = new ConsensusParticipantData();
                participant.setId(participantNode.getId());
                participant.setName(participantNode.getName());
                participant.setPubKey(participantNode.getPubKey());
                participant.setAddress(participantNode.getAddress());
                participant.setParticipantState(participantNode.getParticipantNodeState());
                participants[i] = participant;
            }

            GenesisUser[] gus = op.getInitSetting().getGenesisUsers();
            if(null == gus || gus.length == 0) {
                gus = new GenesisUserConfig[participantNodes.length];
                for (int i = 0; i < participantNodes.length; i++) {
                    gus[i] = new GenesisUserConfig(participantNodes[i].getPubKey(), null, null, null);
                }
            }
            GenesisUser[] genesisUsers = new GenesisUserConfig[gus.length];
            for(int i=0; i<gus.length; i++) {
                genesisUsers[i] = new GenesisUserConfig(gus[i]);
            }
            ledgerInitData.setGenesisUsers(genesisUsers);

            ledgerInitData.setConsensusParticipants(participants);
        }

        return new LedgerInitOpTemplate(ledgerInitData);
    }

    /**
     * decorate ParticipantRegisterOperation object
     *
     * @param op
     * @return
     */
    public static Operation decorateParticipantRegisterOperation(ParticipantRegisterOperation op) {
        BlockchainIdentity partId = decorateBlockchainIdentity(op.getParticipantID());
        return new ParticipantRegisterOpTemplate(op.getParticipantName(), partId, op.getCertificate());
    }

    /**
     * decorate ParticipantStateUpdateOperation object
     *
     * @param op
     * @return
     */
    public static Operation decorateParticipantStateUpdateOperation(ParticipantStateUpdateOperation op) {
        BlockchainIdentity stateUpdateIdentity = decorateBlockchainIdentity(op.getParticipantID());
        return new ParticipantStateUpdateOpTemplate(stateUpdateIdentity, op.getState());
    }

    /**
     * decorate ConsensusSettingsUpdateOperation object
     *
     * @param op
     * @return
     */
    public static Operation decorateConsensusSettingsUpdateOperation(ConsensusSettingsUpdateOperation op) {
        return new ConsensusSettingsUpdateOpTemplate(op.getProperties());
    }

    /**
     * decorate RolesConfigureOperation object
     *
     * @param op
     * @return
     */
    public static Operation decorateRolesConfigureOperation(RolesConfigureOperation op) {
        RolesConfigureOpDecorator opTemplate = new RolesConfigureOpDecorator();
        RolesConfigureOperation.RolePrivilegeEntry[] roles = op.getRoles();
        if (roles != null && roles.length > 0) {
            for (RolesConfigureOperation.RolePrivilegeEntry role : roles) {
                opTemplate.configure(role);
            }
        }
        return opTemplate;
    }

    /**
     * decorate UserAuthorizeOperation object
     *
     * @param op
     * @return
     */
    public static Operation decorateUserAuthorizeOperation(UserAuthorizeOperation op) {
        UserAuthorizeOpDecorator opTemplate = new UserAuthorizeOpDecorator();
        UserAuthorizeOperation.UserRolesEntry[] userRolesAuthorizations = op.getUserRolesAuthorizations();
        if (userRolesAuthorizations != null && userRolesAuthorizations.length > 0) {
            for (UserAuthorizeOperation.UserRolesEntry entry : userRolesAuthorizations) {
                opTemplate.configure(entry);
            }
        }
        return opTemplate;
    }

    /**
     * decorate UserRegisterOperation object
     *
     * @param op
     * @return
     */
    public static Operation decorateUserRegisterOperation(UserRegisterOperation op) {
        BlockchainIdentity identity = decorateBlockchainIdentity(op.getUserID());
        return new UserRegisterOpTemplate(identity, op.getCertificate());
    }

    /**
     * decorate BlockchainIdentity object
     *
     * @param identity
     * @return
     */
    public static BlockchainIdentity decorateBlockchainIdentity(BlockchainIdentity identity) {
        return new BlockchainIdentityData(identity.getAddress(), identity.getPubKey());
    }

    /**
     * decorate EventAccountRegisterOperation object
     *
     * @param op
     * @return
     */
    public static Operation decorateEventAccountRegisterOperation(EventAccountRegisterOperation op) {
        BlockchainIdentity identity = decorateBlockchainIdentity(op.getEventAccountID());
        return new EventAccountRegisterOpTemplate(identity);
    }

    /**
     * decorate EventPublishOperation object
     *
     * @param op
     * @return
     */
    public static Operation decorateEventPublishOperation(EventPublishOperation op) {
        EventPublishOpTemplate opTemplate = new EventPublishOpTemplate(op.getEventAddress());
        EventPublishOperation.EventEntry[] events = op.getEvents();
        if (events != null && events.length > 0) {
            for (EventPublishOperation.EventEntry entry : events) {
                opTemplate.set(entry.getName(), decorateBytesValue(entry.getContent()), entry.getSequence());
            }
        }
        return opTemplate;
    }

    /**
     * decorate BytesValue to TypedValue
     *
     * @param bytesValue
     * @return
     */
    public static BytesValue decorateBytesValue(BytesValue bytesValue) {
        return TypedValue.wrap(bytesValue);
    }

    /**
     * decorate BytesValue... to TypedValue...
     *
     * @param bytesValues
     * @return
     */
    public static BytesValue[] decorateBytesValues(BytesValue... bytesValues) {
        if (bytesValues != null && bytesValues.length > 0) {
            BytesValue[] typeValues = new BytesValue[bytesValues.length];
            for (int i = 0; i < bytesValues.length; i++) {
                typeValues[i] = decorateBytesValue(bytesValues[i]);
            }

            return typeValues;
        }
        return bytesValues;
    }

    public static Operation decorateUserCAUpdateOperation(UserCAUpdateOperation op) {
        return new UserCAUpdateOpTemplate(op.getUserAddress(), op.getCertificate());
    }

    public static Operation decorateUserRevokeOperation(UserStateUpdateOperation op) {
        return new UserStateUpdateOpTemplate(op.getUserAddress(), op.getState());
    }

    public static Operation decorateRootCAUpdateOperation(RootCAUpdateOperation op) {
        return new RootCAUpdateOpTemplate(op);
    }

    public static Operation decorateContractStateUpdateOperation(ContractStateUpdateOperation op) {
        return new ContractStateUpdateOpTemplate(op.getContractAddress(), op.getState());
    }
    /**
     * decorate AccountPermissionSetOperation object
     *
     * @param op
     * @return
     */
    public static Operation decorateAccountPermissionSetOperation(AccountPermissionSetOperation op) {
        AccountPermissionSetOpTemplate opTemplate = new AccountPermissionSetOpTemplate(op.getAddress(), op.getAccountType());
        opTemplate.setMode(op.getMode());
        opTemplate.setRole(op.getRole());
        return opTemplate;
    }

}
