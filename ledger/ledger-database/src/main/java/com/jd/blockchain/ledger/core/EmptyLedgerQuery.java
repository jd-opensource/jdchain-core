package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.LedgerAdminInfo;
import com.jd.blockchain.ledger.LedgerAdminSettings;
import com.jd.blockchain.ledger.LedgerBlock;

class EmptyLedgerQuery implements LedgerQuery {

		private LedgerDataSet dataset = EmptyLedgerDataSet.INSTANCE;

		@Override
		public HashDigest getHash() {
			return null;
		}

		@Override
		public long getVersion() {
			return LedgerStructureConfig.VERSION;
		}

		@Override
		public long getLatestBlockHeight() {
			return 0;
		}

		@Override
		public HashDigest getLatestBlockHash() {
			return null;
		}

		@Override
		public LedgerBlock getLatestBlock() {
			return null;
		}

		@Override
		public HashDigest getBlockHash(long height) {
			return null;
		}

		@Override
		public LedgerBlock getBlock(long height) {
			return null;
		}

		@Override
		public LedgerAdminInfo getAdminInfo() {
			return null;
		}

		@Override
		public LedgerAdminInfo getAdminInfo(LedgerBlock block) {
			return null;
		}

		@Override
		public LedgerAdminSettings getAdminSettings() {
			return null;
		}

		@Override
		public LedgerAdminSettings getAdminSettings(LedgerBlock block) {
			return null;
		}

		@Override
		public LedgerBlock getBlock(HashDigest hash) {
			return null;
		}

		@Override
		public LedgerDataSet getLedgerDataSet(LedgerBlock block) {
			return dataset;
		}

		@Override
		public LedgerEventSet getLedgerEvents(LedgerBlock block) {
			return null;
		}

		@Override
		public TransactionSet getTransactionSet(LedgerBlock block) {
			return null;
		}

		@Override
		public UserAccountSet getUserAccountSet(LedgerBlock block) {
			return dataset.getUserAccountSet();
		}

		@Override
		public DataAccountSet getDataAccountSet(LedgerBlock block) {
			return dataset.getDataAccountSet();
		}

		@Override
		public ContractAccountSet getContractAccountSet(LedgerBlock block) {
			return dataset.getContractAccountSet();
		}

		@Override
		public EventGroup getSystemEvents(LedgerBlock block) {
			return null;
		}

		@Override
		public EventAccountSet getUserEvents(LedgerBlock block) {
			return null;
		}

		@Override
		public LedgerBlock retrieveLatestBlock() {
			// TODO
			return null;
		}

		@Override
		public long retrieveLatestBlockHeight() {
			return 0;
		}

		@Override
		public HashDigest retrieveLatestBlockHash() {
			return null;
		}

	}