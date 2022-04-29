package com.jd.blockchain.peer.ssl;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import utils.net.SSLSecurity;

@ConditionalOnExpression("#{T(utils.crypto.sm.GmSSLProvider).supportGMSSL(environment['server.ssl.enabled'], environment['server.ssl.protocol'])}")
@Component
@EnableConfigurationProperties(SSLProperties.class)
public class SMSSLConfiguration {
    @Bean
    public ConfigurableServletWebServerFactory webServerFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addConnectorCustomizers(new SmSslCustomizer());
        return factory;
    }

    @Bean
    public SSLSecurity sslSecurity(SSLProperties sslProperties) {
        SSLSecurity sslSecurity = new SSLSecurity();

        sslSecurity.setKeyStore(sslProperties.getKeyStore());
        sslSecurity.setKeyStoreType(sslProperties.getKeyStoreType());
        sslSecurity.setKeyStorePassword(sslProperties.getKeyStorePassword());
        sslSecurity.setProtocol(sslProperties.getProtocol());
        sslSecurity.setEnabledProtocols(sslProperties.getEnabledProtocols().split(","));
        sslSecurity.setCiphers(sslProperties.getCiphers().split(","));

        sslSecurity.setKeyAlias(sslProperties.getKeyAlias());
        sslSecurity.setTrustStore(sslProperties.getTrustStore());
        sslSecurity.setTrustStorePassword(sslProperties.getTrustStorePassword());
        sslSecurity.setTrustStoreType(sslProperties.getTrustStoreType());
        sslSecurity.setHostNameVerifier(sslProperties.getHostNameVerifier());

        return sslSecurity;
    }

}

class SmSslCustomizer implements TomcatConnectorCustomizer {
    @Override
    public void customize(Connector connector) {
        Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
        protocol.setSslImplementationName("utils.crypto.sm.tomcat.ssl.GMJSSEImplementation");
        protocol.setDisableUploadTimeout(false);
    }
}