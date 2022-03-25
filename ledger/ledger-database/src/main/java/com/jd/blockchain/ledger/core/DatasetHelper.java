package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.ledger.MerkleProof;

import utils.Bytes;
import utils.DataEntry;
import utils.Dataset;
import utils.Mapper;
import utils.SkippingIterator;

/**
 * Helper for {@link Dataset};
 * 
 * @author huanghaiquan
 *
 */
public class DatasetHelper {

	public static final TypeMapper<Bytes, String> UTF8_STRING_BYTES_MAPPER = new TypeMapper<Bytes, String>() {

		@Override
		public Bytes encode(String t2) {
			return Bytes.fromString(t2);
		}

		@Override
		public String decode(Bytes t1) {
			return t1.toUTF8String();
		}
	};

	public static final TypeMapper<String, Bytes> BYTES_UTF8_STRING_MAPPER = new TypeMapper<String, Bytes>() {

		@Override
		public String encode(Bytes t1) {
			return t1.toUTF8String();
		}

		@Override
		public Bytes decode(String t2) {
			return Bytes.fromString(t2);
		}
	};

	/**
	 * 适配两个不同类型参数的数据集；
	 * 
	 * @param <K1>        适配输入的 键 类型；
	 * @param <K2>        适配输出的 键 类型；
	 * @param <V1>        适配输入的 值 类型；
	 * @param <V2>        适配输出的 值 类型；
	 * @param dataset     数据集；
	 * @param keyMapper   键的映射配置；
	 * @param valueMapper 值的映射配置；
	 * @return
	 */
	public static <V> BaseDataset<String, V> map(BaseDataset<Bytes, V> dataset) {
		return new TypeAdapter<Bytes, String, V, V>(dataset, UTF8_STRING_BYTES_MAPPER, new EmptyMapper<V>());
	}

	/**
	 * 适配两个不同类型参数的数据集；
	 * 
	 * @param <K1>        适配输入的 键 类型；
	 * @param <K2>        适配输出的 键 类型；
	 * @param <V1>        适配输入的 值 类型；
	 * @param <V2>        适配输出的 值 类型；
	 * @param dataset     数据集；
	 * @param keyMapper   键的映射配置；
	 * @param valueMapper 值的映射配置；
	 * @return
	 */
	public static <V1, V2> BaseDataset<String, V2> map(BaseDataset<Bytes, V1> dataset, TypeMapper<V1, V2> valueMapper) {
		return new TypeAdapter<Bytes, String, V1, V2>(dataset, UTF8_STRING_BYTES_MAPPER, valueMapper);
	}

	/**
	 * 适配两个不同类型参数的数据集；
	 * 
	 * @param <K1>        适配输入的 键 类型；
	 * @param <K2>        适配输出的 键 类型；
	 * @param <V1>        适配输入的 值 类型；
	 * @param <V2>        适配输出的 值 类型；
	 * @param dataset     数据集；
	 * @param keyMapper   键的映射配置；
	 * @param valueMapper 值的映射配置；
	 * @return
	 */
	public static <K1, K2, V1, V2> BaseDataset<K2, V2> map(BaseDataset<K1, V1> dataset, TypeMapper<K1, K2> keyMapper,
                                                           TypeMapper<V1, V2> valueMapper) {
		return new TypeAdapter<K1, K2, V1, V2>(dataset, keyMapper, valueMapper);
	}

	/**
	 * 监听对数据集的变更；
	 * 
	 * @param <K>      键 类型；
	 * @param <V>      值 类型；
	 * @param dataset  要监听的数据集；
	 * @param listener 要植入的监听器；
	 * @return 植入监听器的数据集实例；
	 */
	public static <K, V> BaseDataset<K, V> listen(BaseDataset<K, V> dataset, DataChangedListener<K, V> listener) {
		return new DatasetUpdatingMonitor<K, V>(dataset, listener);
	}

	/**
	 * 数据修改监听器；
	 * 
	 * @author huanghaiquan
	 *
	 * @param <K>
	 * @param <V>
	 */
	public static interface DataChangedListener<K, V> {

		void onChanged(K key, V value, long expectedVersion, long newVersion);

	}

	/**
	 * 类型映射接口；
	 * 
	 * @author huanghaiquan
	 *
	 * @param <T1>
	 * @param <T2>
	 */
	public static interface TypeMapper<T1, T2> {

		T1 encode(T2 t2);

		T2 decode(T1 t1);

	}

	private static class EmptyMapper<T> implements TypeMapper<T, T> {

		@Override
		public T encode(T t) {
			return t;
		}

		@Override
		public T decode(T t) {
			return t;
		}

	}

	private static class DatasetUpdatingMonitor<K, V> implements BaseDataset<K, V> {

		private BaseDataset<K, V> dataset;

		private DataChangedListener<K, V> listener;

		public DatasetUpdatingMonitor(BaseDataset<K, V> dataset, DataChangedListener<K, V> listener) {
			this.dataset = dataset;
			this.listener = listener;
		}

		@Override
		public long getDataCount() {
			return dataset.getDataCount();
		}

		@Override
		public long setValue(K key, V value, long version) {
			long newVersion = dataset.setValue(key, value, version);
			if (newVersion > -1) {
				listener.onChanged(key, value, version, newVersion);
			}
			return newVersion;
		}

		@Override
		public long setValue(K key, V value) {
			long newVersion = dataset.setValue(key, value);
			if (newVersion > -1) {
				listener.onChanged(key, value, -1, newVersion);
			}
			return newVersion;
		}

		@Override
		public V getValue(K key, long version) {
			return dataset.getValue(key, version);
		}

		@Override
		public V getValue(K key) {
			return dataset.getValue(key);
		}

		@Override
		public long getVersion(K key) {
			return dataset.getVersion(key);
		}

		@Override
		public DataEntry<K, V> getDataEntry(K key) {
			return dataset.getDataEntry(key);
		}

		@Override
		public DataEntry<K, V> getDataEntry(K key, long version) {
			return dataset.getDataEntry(key, version);
		}
		
		@Override
		public SkippingIterator<DataEntry<K, V>> idIterator() {
			return dataset.idIterator();
		}

		@Override
		public SkippingIterator<DataEntry<K, V>> kvIterator() {
			return dataset.kvIterator();
		}

		@Override
		public SkippingIterator<DataEntry<K, V>> idIteratorDesc() {
			return dataset.idIteratorDesc();
		}

		@Override
		public SkippingIterator<DataEntry<K, V>> kvIteratorDesc() {
			return dataset.kvIteratorDesc();
		}

		@Override
		public boolean isUpdated() {
			return dataset.isUpdated();
		}

		@Override
		public void commit() {
			dataset.commit();
		}

		@Override
		public void cancel() {
			dataset.cancel();
		}

		@Override
		public MerkleProof getProof(K key) {
			return dataset.getProof(key);
		}

		@Override
		public HashDigest getRootHash() {
			return dataset.getRootHash();
		}

		@Override
		public boolean isReadonly() {
			return dataset.isReadonly();
		}

	}

	/**
	 * 类型适配器；
	 * 
	 * @author huanghaiquan
	 *
	 * @param <K1>
	 * @param <K2>
	 * @param <V1>
	 * @param <V2>
	 */
	private static class TypeAdapter<K1, K2, V1, V2> implements BaseDataset<K2, V2> {
		private BaseDataset<K1, V1> dataset;
		private TypeMapper<K1, K2> keyMapper;
		private TypeMapper<V1, V2> valueMapper;

		public TypeAdapter(BaseDataset<K1, V1> dataset, TypeMapper<K1, K2> keyMapper, TypeMapper<V1, V2> valueMapper) {
			this.dataset = dataset;
			this.keyMapper = keyMapper;
			this.valueMapper = valueMapper;
		}

		@Override
		public long getDataCount() {
			return dataset.getDataCount();
		}

		@Override
		public long setValue(K2 key, V2 value, long version) {
			K1 key1 = keyMapper.encode(key);
			V1 value1 = valueMapper.encode(value);
			return dataset.setValue(key1, value1, version);
		}

		@Override
		public long setValue(K2 key, V2 value) {
			K1 key1 = keyMapper.encode(key);
			V1 value1 = valueMapper.encode(value);
			return dataset.setValue(key1, value1);
		}

		@Override
		public V2 getValue(K2 key, long version) {
			K1 k = keyMapper.encode(key);
			V1 v = dataset.getValue(k, version);
			if (v == null) {
				return null;
			}
			return valueMapper.decode(v);
		}

		@Override
		public V2 getValue(K2 key) {
			K1 k = keyMapper.encode(key);
			V1 v = dataset.getValue(k);
			if (v == null) {
				return null;
			}
			return valueMapper.decode(v);
		}

		@Override
		public long getVersion(K2 key) {
			K1 k = keyMapper.encode(key);
			return dataset.getVersion(k);
		}

		@Override
		public DataEntry<K2, V2> getDataEntry(K2 key) {
			K1 k = keyMapper.encode(key);
			DataEntry<K1, V1> entry = dataset.getDataEntry(k);
			if (entry == null) {
				return null;
			}
			V2 v = valueMapper.decode(entry.getValue());
			return new KeyValueEntry<K2, V2>(key, v, entry.getVersion());
		}

		@Override
		public DataEntry<K2, V2> getDataEntry(K2 key, long version) {
			K1 k = keyMapper.encode(key);
			DataEntry<K1, V1> entry = dataset.getDataEntry(k, version);
			if (entry == null) {
				return null;
			}
			V2 v = valueMapper.decode(entry.getValue());
			return new KeyValueEntry<K2, V2>(key, v, entry.getVersion());
		}

		@Override
		public boolean isUpdated() {
			return dataset.isUpdated();
		}

		@Override
		public void commit() {
			dataset.commit();
		}

		@Override
		public void cancel() {
			dataset.cancel();
		}

		@Override
		public MerkleProof getProof(K2 key) {
			K1 key1 = keyMapper.encode(key);
			return dataset.getProof(key1);
		}

		@Override
		public HashDigest getRootHash() {
			return dataset.getRootHash();
		}

		@Override
		public boolean isReadonly() {
			return dataset.isReadonly();
		}

		@Override
		public SkippingIterator<DataEntry<K2, V2>> idIterator() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SkippingIterator<DataEntry<K2, V2>> kvIterator() {
			return dataset.kvIterator().iterateAs(new Mapper<DataEntry<K1,V1>, DataEntry<K2, V2>>() {
				@Override
				public DataEntry<K2, V2> from(DataEntry<K1, V1> source) {
					if (source == null) {
						return null;
					}
					K2 key2 = keyMapper.decode(source.getKey());
					V2 value2 = valueMapper.decode(source.getValue());
					return new KeyValueEntry<K2, V2>(key2, value2, source.getVersion());
				}
			});
		}

		@Override
		public SkippingIterator<DataEntry<K2, V2>> idIteratorDesc() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SkippingIterator<DataEntry<K2, V2>> kvIteratorDesc() {
			// TODO Auto-generated method stub
			return null;
		}
	}

	private static class KeyValueEntry<K, V> implements DataEntry<K, V> {

		private K key;

		private V value;

		private long version;

		public KeyValueEntry(K key, V value, long version) {
			this.key = key;
			this.value = value;
			this.version = version;
		}

		public K getKey() {
			return key;
		}

		public long getVersion() {
			return version;
		}

		public V getValue() {
			return value;
		}

	}
}
