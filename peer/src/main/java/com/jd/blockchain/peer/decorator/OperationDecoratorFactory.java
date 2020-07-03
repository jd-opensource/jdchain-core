package com.jd.blockchain.peer.decorator;


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

    public static final int K = 1024;

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
        return new ContractCodeDeployOpTemplate(contractId,
                chainCodeSlash(op.getChainCode()), op.getChainCodeVersion());
    }

    /**
     * decorate ContractEventSendOperation object
     *
     * @param op
     * @return
     */
    public static Operation decorateContractEventSendOperation(ContractEventSendOperation op) {
        BytesDataList dataList = new BytesDataList(decorateBytesValues(op.getArgs().getValues()));
        return new ContractEventSendOpTemplate(op.getContractAddress(),
                op.getEvent(), dataList);
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
        ledgerInitData.setConsensusProvider(op.getInitSetting().getConsensusProvider());
        ledgerInitData.setCreatedTime(op.getInitSetting().getCreatedTime());

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
        BlockchainIdentity partId = decorateBlockchainIdentity(op.getParticipantRegisterIdentity());
        return new ParticipantRegisterOpTemplate(op.getParticipantName(),
                partId, op.getNetworkAddress());
    }

    /**
     * decorate ParticipantStateUpdateOperation object
     *
     * @param op
     * @return
     */
    public static Operation decorateParticipantStateUpdateOperation(ParticipantStateUpdateOperation op) {
        BlockchainIdentity stateUpdateIdentity = decorateBlockchainIdentity(op.getStateUpdateIdentity());
        return new ParticipantStateUpdateOpTemplate(stateUpdateIdentity, op.getState());
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
        return new UserRegisterOpTemplate(identity);
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
            System.out.println(events.length);
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

    /**
     * cut chain code
     *
     * @param chainCode
     * @return
     */
    public static byte[] chainCodeSlash(byte[] chainCode) {
        if (chainCode != null && chainCode.length > K) {
            byte[] slashedCode = new byte[K];
            System.arraycopy(chainCode, 0, slashedCode, 0, K);
            return slashedCode;
        }
        return chainCode;
    }
}
