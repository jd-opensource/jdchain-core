package com.jd.blockchain.tools.initializer.web;

import java.io.InputStream;

import com.jd.binaryproto.BinaryProtocol;
import com.jd.blockchain.ledger.LedgerInitException;
import com.jd.httpservice.HttpServiceContext;
import com.jd.httpservice.ResponseConverter;
import com.jd.httpservice.agent.ServiceRequest;

public class DecisionResponseConverter implements ResponseConverter {

	@Override
	public Object getResponse(ServiceRequest request, InputStream responseStream, HttpServiceContext serviceContext)
			throws Exception {
		LedgerInitResponse resp = LedgerInitResponse.resolve(responseStream);
		if (resp.isError()) {
			throw new LedgerInitException("Error occurred at remote participant! --" + resp.getErrorMessage());
		}
		return BinaryProtocol.decode(resp.getData());
	}

}
