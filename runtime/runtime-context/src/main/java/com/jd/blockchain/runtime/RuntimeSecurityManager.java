package com.jd.blockchain.runtime;

import java.security.Permission;

public class RuntimeSecurityManager extends SecurityManager {

    ThreadLocal<Boolean> enabledFlag;

    public RuntimeSecurityManager(final boolean enabledByDefault) {

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
        if (isEnabled()) {
            super.checkPermission(permission);
        }
    }

    @Override
    public void checkPermission(Permission permission, Object context) {
        if (isEnabled()) {
            super.checkPermission(permission, context);
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