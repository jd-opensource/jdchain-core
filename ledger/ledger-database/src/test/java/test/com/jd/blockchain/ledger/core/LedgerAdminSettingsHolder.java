package test.com.jd.blockchain.ledger.core;

import com.jd.blockchain.ledger.*;
import com.jd.blockchain.ledger.core.RolePrivilegeDataset;
import com.jd.blockchain.ledger.core.UserRoleDatasetEditor;

public class LedgerAdminSettingsHolder implements LedgerAdminSettings {

    private RolePrivilegeDataset rolePrivilegeDataset;
    private UserRoleDatasetEditor userRolesDataset;
    private LedgerMetadata_V2 metadata;

    public LedgerAdminSettingsHolder(RolePrivilegeDataset rolePrivilegeDataset, UserRoleDatasetEditor userRolesDataset, LedgerMetadata_V2 metadata) {
        this.rolePrivilegeDataset = rolePrivilegeDataset;
        this.userRolesDataset = userRolesDataset;
        this.metadata = metadata;
    }

    @Override
    public UserAuthorizationSettings getAuthorizations() {
        return userRolesDataset;
    }

    @Override
    public RolePrivilegeSettings getRolePrivileges() {
        return rolePrivilegeDataset;
    }

    @Override
    public LedgerMetadata_V2 getMetadata() {
        return metadata;
    }

    @Override
    public LedgerSettings getSettings() {
        return null;
    }

    @Override
    public ParticipantNode[] getParticipants() {
        return new ParticipantNode[0];
    }

    @Override
    public long getParticipantCount() {
        return 0;
    }
}
