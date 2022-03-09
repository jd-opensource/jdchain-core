package com.jd.blockchain.runtime.modular;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import com.jd.blockchain.runtime.RuntimeContext;

import com.jd.blockchain.runtime.RuntimeSecurityManager;
import utils.StringUtils;
import utils.io.FileSystemStorage;
import utils.io.RuntimeIOException;
import utils.io.Storage;

public class ModularRuntimeContext extends RuntimeContext {

	private String runtimeDir;

	private JarsModule libModule;
	
	private EnvSettings environment;

	protected RuntimeSecurityManager securityManager;

	public ModularRuntimeContext(String runtimeDir, JarsModule libModule, 
			boolean productMode) {
		this.environment = new EnvSettings();
		this.environment.setProductMode(productMode);
		this.environment.setRuntimeDir(runtimeDir);
		this.runtimeDir = runtimeDir;
		this.libModule = libModule;
		String securityPolicy = System.getProperty("java.security.policy");
		if(!StringUtils.isEmpty(securityPolicy)) {
			securityManager = new RuntimeSecurityManager(false);
			System.setSecurityManager(securityManager);
		}
	}
	
	void register() {
		RuntimeContext.set(this);
	}

	@Override
	public Environment getEnvironment() {
		return environment;
	}

	@Override
	public RuntimeSecurityManager getSecurityManager() {
		return securityManager;
	}

	@Override
	protected String getRuntimeDir() {
		return runtimeDir;
	}

	@Override
	protected URLClassLoader createDynamicModuleClassLoader(URL jarURL) {
		return new URLClassLoader(new URL[] {jarURL}, libModule.getModuleClassLoader());
	}

	// --------------------------- inner types -----------------------------

	private static class EnvSettings implements Environment {

		private boolean productMode;
		private Storage runtimeStorage;

		@Override
		public boolean isProductMode() {
			return productMode;
		}

		public void setProductMode(boolean productMode) {
			this.productMode = productMode;
		}

		@Override
		public Storage getRuntimeStorage() {
			return runtimeStorage;
		}

		public void setRuntimeDir(String runtimeDir) {
			try {
				this.runtimeStorage = new FileSystemStorage(runtimeDir);
			} catch (IOException e) {
				throw new RuntimeIOException(e.getMessage(), e);
			}
		}
	}
}
