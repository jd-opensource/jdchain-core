package com.jd.blockchain.runtime;

import com.jd.blockchain.ledger.ContractExecuteException;
import sun.security.util.SecurityConstants;

import java.io.File;
import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.security.Permission;

public class RuntimeSecurityManager extends SecurityManager {

    private String libsDir;
    private ThreadLocal<Boolean> enabledFlag;

    public RuntimeSecurityManager(String libsDir, final boolean enabledByDefault) {
        this.libsDir = libsDir;
        enabledFlag = new ThreadLocal<Boolean>() {

            @Override
            protected Boolean initialValue() {
                return enabledByDefault;
            }

            @Override
            public void set(Boolean value) {
                super.set(value);
            }
        };
    }

    @Override
    public void checkPermission(Permission permission) {
        checkPermission(permission, null);
    }

    @Override
    public void checkPermission(Permission permission, Object context) {
        if (isEnabled()) {
            if (permission.getName().equals("createClassLoader") && permission.getClass().getName().equals(RuntimePermission.class.getName())) {
                return;
            } else if (permission.getName().equals("suppressAccessChecks") && permission.getClass().getName().equals(ReflectPermission.class.getName())) {
                return;
            } else if (permission.getName().startsWith(libsDir + File.separator) &&
                    permission.getClass().getName().equals(FilePermission.class.getName()) &&
                    permission.getActions().equals(SecurityConstants.FILE_READ_ACTION)) {
                return;
            } else {
                throw new ContractExecuteException("access denied " + permission);
            }
        }
    }

    public void enable() {
        enabledFlag.set(true);
    }

    public void disable() {
        enabledFlag.set(false);
    }

    public boolean isEnabled() {
        return enabledFlag.get();
    }

}