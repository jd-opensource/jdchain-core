package com.jd.blockchain.gateway.service;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jd.blockchain.gateway.GatewayServerBooter;

import utils.io.FileSystemStorage;
import utils.io.RuntimeIOException;
import utils.io.Storage;

@Configuration
public class ComponentConfiguration {

	@Bean
	public Storage runtimeStorage() {
		try {
			return new FileSystemStorage(GatewayServerBooter.RUNTIME_STORAGE_DIR);
		} catch (IOException e) {
			throw new RuntimeIOException(e.getMessage(), e);
		}
	}

}
