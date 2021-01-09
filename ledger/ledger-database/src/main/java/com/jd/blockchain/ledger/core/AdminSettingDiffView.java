package com.jd.blockchain.ledger.core;

import utils.SkippingIterator;

public interface AdminSettingDiffView extends DiffView {

	// TODO: LedgerSetting 的存储实现机制无法支持跨多个区块批量检索差异；
//	SkippingIterator<LedgerAdminSettings> getSettingsDiff();


}
