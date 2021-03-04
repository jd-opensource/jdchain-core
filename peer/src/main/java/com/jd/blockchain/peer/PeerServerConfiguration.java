package com.jd.blockchain.peer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jd.blockchain.runtime.RuntimeContext;

import utils.io.Storage;

@Configuration
public class PeerServerConfiguration {

	@Bean
	public Storage runtimeStorage() {
		return RuntimeContext.get().getEnvironment().getRuntimeStorage();
	}
}
