package com.jd.blockchain.peer.ssl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.stereotype.Component;
import utils.crypto.sm.GmSSLProvider;
import utils.net.SSLSecurity;

@ConditionalOnExpression("#{T(utils.crypto.sm.GmSSLProvider).supportGMSSL(environment['server.ssl.enabled'], environment['server.ssl.protocol'])}")
@Component
public class CustomizationBean implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    @Autowired
    private SSLSecurity sslSecurity;

    @Override
    public void customize(ConfigurableServletWebServerFactory server) {
        GmSSLProvider.enableGMSupport(GmSSLProvider.GMTLS);

        Ssl ssl = new Ssl();
        ssl.setKeyStore(sslSecurity.getKeyStore());
        ssl.setKeyStoreType(sslSecurity.getKeyStoreType());
        ssl.setKeyStorePassword(sslSecurity.getKeyStorePassword());
        ssl.setKeyStoreProvider(GmSSLProvider.GM_PROVIDER);
        ssl.setEnabledProtocols(sslSecurity.getEnabledProtocols());
        ssl.setProtocol(sslSecurity.getProtocol());
        ssl.setCiphers(sslSecurity.getCiphers());
        server.setSsl(ssl);
    }
}
