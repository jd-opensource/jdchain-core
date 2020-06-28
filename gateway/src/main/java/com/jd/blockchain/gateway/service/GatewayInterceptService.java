package com.jd.blockchain.gateway.service;

import com.jd.blockchain.ledger.TransactionRequest;

import javax.servlet.http.HttpServletRequest;

public interface GatewayInterceptService {

    void intercept(HttpServletRequest request, TransactionRequest txRequest);
}
