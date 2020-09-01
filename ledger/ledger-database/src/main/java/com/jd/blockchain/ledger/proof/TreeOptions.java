package com.jd.blockchain.ledger.proof;

import com.jd.blockchain.ledger.core.MerkleProofException;

/**
 * 默克尔树的配置选项；
 * 
 * @author huanghaiquan
 *
 */
public class TreeOptions {

	/**
	 * 默认的哈希算法；
	 * 
	 */
	private short defaultHashAlgorithm;

	/**
	 * 从存储介质加载树节点的时候是否进行哈希验证；
	 */
	private boolean verifyHashOnLoad = true;

	/**
	 * 是否报告重复写入相同的数据节点；
	 */
	private boolean reportDuplicatedData = false;

	private TreeOptions() {
	}

	public static TreeOptions build() {
		return new TreeOptions();
	}

	/**
	 * \ 默认的哈希算法；
	 * 
	 * @return
	 */
	public short getDefaultHashAlgorithm() {
		return defaultHashAlgorithm;
	}

	public TreeOptions setDefaultHashAlgorithm(short defaultHashAlgorithm) {
		this.defaultHashAlgorithm = defaultHashAlgorithm;
		return this;
	}

	/**
	 * 从存储介质加载树节点的时候是否进行哈希验证；
	 * 
	 * @return
	 */
	public boolean isVerifyHashOnLoad() {
		return verifyHashOnLoad;
	}

	public TreeOptions setVerifyHashOnLoad(boolean verifyHashOnLoad) {
		this.verifyHashOnLoad = verifyHashOnLoad;
		return this;
	}

	/**
	 * 是否报告重复写入相同的数据节点； 默认为 false；<br>
	 * 
	 * 默克尔树在保存数据节点时，以数据的哈希为”键“进行保存，因此相同的数据将以相同的”键“进行保存；<br>
	 * 
	 * 此选项指示在保存数据节点时如果检测到相同的数据已经存在的情况下，是否引发异常 {@link MerkleProofException} 进行报告；
	 * 
	 * <br>
	 * 如果设置为 false ，则忽略重复的数据节点；
	 * 
	 * 
	 * @return
	 */
	public boolean isReportDuplicatedData() {
		return reportDuplicatedData;
	}

	/**
	 * 是否报告重复写入相同的数据节点； 默认为 false；<br>
	 * 
	 * 默克尔树在保存数据节点时，以数据的哈希为”键“进行保存，因此相同的数据将以相同的”键“进行保存；<br>
	 * 
	 * 此选项指示在保存数据节点时如果检测到相同的数据已经存在的情况下，是否引发异常 {@link MerkleProofException} 进行报告；
	 * 
	 * <br>
	 * 如果设置为 false ，则忽略重复的数据节点；
	 * 
	 * @param reportDuplicatedData
	 */
	public TreeOptions setReportDuplicatedData(boolean reportDuplicatedData) {
		this.reportDuplicatedData = reportDuplicatedData;
		return this;
	}

}
