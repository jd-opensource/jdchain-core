package com.jd.blockchain.kvdb.cli;

import com.jd.blockchain.kvdb.protocol.client.ClientConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

@SpringBootApplication
public class CliBooter {

    private static final int DEFAULT_PORT = 7060;
    /**
     * 保存 kvdb-cli 启动参数，参数列表参照{@link ClientConfig}
     */
    private static String[] clientArgs;

    public static void main(String[] args) {
        String[] disabledCommands = {"--spring.shell.command.script.enabled=false"};
        String[] fullArgs = StringUtils.concatenateStringArrays(args, disabledCommands);
        clientArgs = args;
        SpringApplication.run(CliBooter.class, fullArgs);
    }

    @Bean
    public ClientConfig clientConfig() {
        ClientConfig config = new ClientConfig(clientArgs);
        boolean containPort = false;
        for (String arg : clientArgs) {
            if (arg.equals("-p")) {
                containPort = true;
                break;
            }
        }
        if (!containPort) {
            config.setPort(DEFAULT_PORT);
        }
        return config;
    }

}
