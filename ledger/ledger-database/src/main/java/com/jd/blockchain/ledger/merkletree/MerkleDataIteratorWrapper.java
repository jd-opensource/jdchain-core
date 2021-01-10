package com.jd.blockchain.ledger.merkletree;

import utils.AbstractSkippingIterator;

class MerkleDataIteratorWrapper<T> extends AbstractSkippingIterator<MerkleValue<T>> {

		private long id;

		private byte[] valueBytes;

		private BytesConverter<T> converter;

		public MerkleDataIteratorWrapper(long id, byte[] valueBytes, BytesConverter<T> converter) {
			this.id = id;
			this.valueBytes = valueBytes;
			this.converter = converter;
		}

		@Override
		public long getTotalCount() {
			return 1;
		}

		@Override
		public MerkleValue<T> next() {
			if (hasNext()) {
				cursor++;
				T value = converter.fromBytes(valueBytes);
				return new IDValue<T>(id, value);
			}
			return null;
		}

	}