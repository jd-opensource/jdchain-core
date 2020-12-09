package com.jd.blockchain.consensus.bftsmart;

import com.jd.blockchain.binaryproto.DataContractRegistry;
import com.jd.blockchain.utils.Property;

public class BftsmartConsensusConfig implements BftsmartConsensusViewSettings {

	private Property[] bftsmartSystemConfig;

	private BftsmartNodeSettings[] nodes;

	private int viewId;

	static {
		DataContractRegistry.register(BftsmartConsensusViewSettings.class);
	}
	/**
	 * 创建 bftsmart 共识配置；
	 *
	 * @param nodes
	 *            节点列表
	 * @param commitBlockSettings
	 *            结块设置；
	 * @param bftsmartSystemConfigs
	 *            bftsmart系统配置；
	 */
	public BftsmartConsensusConfig(BftsmartNodeSettings[] nodes,
//								   BftsmartCommitBlockSettings commitBlockSettings,
								   Property[] bftsmartSystemConfigs, int viewId) {
		this.nodes = nodes;
//		this.commitBlockSettings = commitBlockSettings;
		this.bftsmartSystemConfig = bftsmartSystemConfigs;
		this.viewId = viewId;
	}

	@Override
	public BftsmartNodeSettings[] getNodes() {
		return nodes;
	}

	@Override
	public Property[] getSystemConfigs() {
		return bftsmartSystemConfig;
	}

	@Override
	public int getViewId() {
		return viewId;
	}

//	@Override
//	public BftsmartCommitBlockSettings getCommitBlockSettings() {
//		return commitBlockSettings;
//	}
//
//
//	public void setCommitBlockSettings(BftsmartCommitBlockSettings commitBlockSettings) {
//		this.commitBlockSettings = commitBlockSettings;
//	}
}
