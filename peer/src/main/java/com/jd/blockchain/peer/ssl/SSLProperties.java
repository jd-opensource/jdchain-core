package com.jd.blockchain.peer.ssl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import utils.net.SSLSecurity;

@ConfigurationProperties(prefix = "server.ssl")
public class SSLProperties {

    private String keyStore;
    private String keyStoreType;
    private String keyAlias;
    private String keyStorePassword;
    private String protocol;
    private String enabledProtocols;
    private String ciphers;
    private String trustStore;
    private String trustStorePassword;
    private String trustStoreType;
    private String hostNameVerifier = SSLSecurity.DEFAULT_HOST_NAME_VERIFIER;

    public String getKeyStore() {
        return keyStore;
    }

    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getEnabledProtocols() {
        return enabledProtocols;
    }

    public void setEnabledProtocols(String enabledProtocols) {
        this.enabledProtocols = enabledProtocols;
    }

    public String getCiphers() {
        return ciphers;
    }

    public void setCiphers(String ciphers) {
        this.ciphers = ciphers;
    }

    public String getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(String trustStore) {
        this.trustStore = trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public String getTrustStoreType() {
        return trustStoreType;
    }

    public void setTrustStoreType(String trustStoreType) {
        this.trustStoreType = trustStoreType;
    }

    public String getHostNameVerifier() {
        return hostNameVerifier;
    }

    public void setHostNameVerifier(String hostNameVerifier) {
        this.hostNameVerifier = hostNameVerifier;
    }
}
