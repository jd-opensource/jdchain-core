package com.jd.blockchain.gateway.web;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.jd.blockchain.crypto.AddressEncoding;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.gateway.service.DataRetrievalService;

import utils.BaseConstant;
import utils.ConsoleUtils;

@RestController
@RequestMapping(path = "/")
public class DataSearchController {
	
	@Autowired
	private DataRetrievalService dataRetrievalService;

	private String dataRetrievalUrl;
	private String schemaRetrievalUrl;
	
	

	@RequestMapping(method = RequestMethod.GET, path = "utils/pubkey/{pubkey}/addr")
	public String getAddrByPubKey(@PathVariable(name = "pubkey") String strPubKey) {
		PubKey pubKey = KeyGenUtils.decodePubKey(strPubKey);
		return AddressEncoding.generateAddress(pubKey).toBase58();
	}

	@RequestMapping(method = RequestMethod.GET, value = "ledgers/{ledgerHash}/**/search")
	public Object dataRetrieval(@PathVariable(name = "ledgerHash") HashDigest ledgerHash, HttpServletRequest request) {
		String result;
		if (dataRetrievalUrl == null || dataRetrievalUrl.length() <= 0) {
			result = "{'message':'OK','data':'" + "data.retrieval.url is empty" + "'}";
		} else {
			String queryParams = request.getQueryString() == null ? "" : request.getQueryString();
			String fullQueryUrl = new StringBuffer(dataRetrievalUrl).append(request.getRequestURI())
					.append(BaseConstant.DELIMETER_QUESTION).append(queryParams).toString();
			try {
				result = dataRetrievalService.retrieval(fullQueryUrl);
				ConsoleUtils.info("request = {%s} \r\n result = {%s} \r\n", fullQueryUrl, result);
			} catch (Exception e) {
				result = "{'message':'OK','data':'" + e.getMessage() + "'}";
			}
		}
		return result;
	}

	/**
	 * querysql;
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST, value = "schema/querysql")
	public Object queryBySql(HttpServletRequest request, @RequestBody String queryString) {
		String result;
		if (schemaRetrievalUrl == null || schemaRetrievalUrl.length() <= 0) {
			result = "{'message':'OK','data':'" + "schema.retrieval.url is empty" + "'}";
		} else {
			String queryParams = request.getQueryString() == null ? "" : request.getQueryString();
			String fullQueryUrl = new StringBuffer(schemaRetrievalUrl).append(request.getRequestURI())
					.append(BaseConstant.DELIMETER_QUESTION).append(queryParams).toString();
			try {
				result = dataRetrievalService.retrievalPost(fullQueryUrl, queryString);
				ConsoleUtils.info("request = {%s} \r\n result = {%s} \r\n", fullQueryUrl, result);
			} catch (Exception e) {
				result = "{'message':'error','data':'" + e.getMessage() + "'}";
			}
		}
		return result;
	}

	public void setSchemaRetrievalUrl(String schemaRetrievalUrl) {
		this.schemaRetrievalUrl = schemaRetrievalUrl;
	}

	public void setDataRetrievalUrl(String dataRetrievalUrl) {
		this.dataRetrievalUrl = dataRetrievalUrl;
	}
}
