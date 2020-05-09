package com.jd.blockchain.kvdb.cli;

import com.jd.blockchain.kvdb.protocol.client.ClientConfig;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

/**
 * 自定义spring-shell提示信息
 */
@Component
public class KVDBPromptProvider implements PromptProvider {

    @Autowired
    private ClientConfig config;

    @Override
    public AttributedString getPrompt() {
        // host:port>
        if (null != config) {
            return new AttributedString(config.getHost() + ":" + config.getPort() + ">",
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
        } else {
            return new AttributedString("server-unknown:>",
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
        }
    }

}