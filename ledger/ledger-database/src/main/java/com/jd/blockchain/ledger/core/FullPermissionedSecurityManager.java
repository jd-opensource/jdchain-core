package com.jd.blockchain.ledger.core;

import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;

import com.jd.blockchain.ledger.LedgerPermission;
import com.jd.blockchain.ledger.LedgerSecurityException;
import com.jd.blockchain.ledger.TransactionPermission;

import utils.Bytes;

class FullPermissionedSecurityManager implements LedgerSecurityManager {

	public static final FullPermissionedSecurityManager INSTANCE = new FullPermissionedSecurityManager();

	@Override
	public SecurityPolicy getSecurityPolicy(Set<Bytes> endpoints, Set<Bytes> nodes) {
		return new FullPermissionedPolicy(endpoints, nodes);
	}

	@Override
	public SecurityPolicy getSecurityPolicy(Set<Bytes> endpoints, Set<Bytes> nodes, X509Certificate rootCa, Map<Bytes, X509Certificate> certs) {
		return new FullPermissionedPolicy(endpoints, nodes, rootCa, certs);
	}

	@Override
	public UserRolesPrivileges getUserRolesPrivilegs(Bytes userAddress) {
		// TODO Auto-generated method stub
		return null;
	}

	private static class FullPermissionedPolicy implements SecurityPolicy {

		private Set<Bytes> endpoints;
		private Set<Bytes> nodes;
		private X509Certificate rootCa;
		private Map<Bytes, X509Certificate> certs;

		public FullPermissionedPolicy(Set<Bytes> endpoints, Set<Bytes> nodes) {
			this.endpoints = endpoints;
			this.nodes = nodes;
		}

		public FullPermissionedPolicy(Set<Bytes> endpoints, Set<Bytes> nodes, X509Certificate rootCa, Map<Bytes, X509Certificate> certs) {
			this.endpoints = endpoints;
			this.nodes = nodes;
			this.rootCa = rootCa;
			this.certs = certs;
		}

		@Override
		public Set<Bytes> getEndpoints() {
			return endpoints;
		}

		@Override
		public Set<Bytes> getNodes() {
			return nodes;
		}

		@Override
		public boolean isEndpointEnable(LedgerPermission permission, MultiIDsPolicy midPolicy) {
			return true;
		}

		@Override
		public boolean isEndpointEnable(TransactionPermission permission, MultiIDsPolicy midPolicy) {
			return true;
		}

		@Override
		public boolean isNodeEnable(LedgerPermission permission, MultiIDsPolicy midPolicy) {
			return true;
		}

		@Override
		public boolean isNodeEnable(TransactionPermission permission, MultiIDsPolicy midPolicy) {
			return true;
		}

		@Override
		public void checkEndpointPermission(LedgerPermission permission, MultiIDsPolicy midPolicy)
				throws LedgerSecurityException {
		}

		@Override
		public void checkEndpointPermission(TransactionPermission permission, MultiIDsPolicy midPolicy)
				throws LedgerSecurityException {
		}

		@Override
		public void checkNodePermission(LedgerPermission permission, MultiIDsPolicy midPolicy) throws LedgerSecurityException {
		}

		@Override
		public void checkNodePermission(TransactionPermission permission, MultiIDsPolicy midPolicy)
				throws LedgerSecurityException {
		}

		@Override
		public void checkRootCa() throws LedgerSecurityException {

		}

		@Override
		public void checkEndpointCa(MultiIDsPolicy midPolicy) throws LedgerSecurityException {

		}

		@Override
		public void checkNodeCa(MultiIDsPolicy midPolicy) throws LedgerSecurityException {

		}

		@Override
		public boolean isEndpointValid(MultiIDsPolicy midPolicy) {
			return true;
		}

		@Override
		public boolean isNodeValid(MultiIDsPolicy midPolicy) {
			return true;
		}

		@Override
		public void checkEndpointValidity(MultiIDsPolicy midPolicy) throws LedgerSecurityException {
		}

		@Override
		public void checkNodeValidity(MultiIDsPolicy midPolicy) throws LedgerSecurityException {
		}

	}

}