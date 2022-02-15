package com.jd.blockchain.gateway.web;

import com.jd.blockchain.crypto.AddressEncoding;
import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.gateway.service.DataRetrievalService;
import com.jd.blockchain.gateway.service.DataSearchService;
import com.jd.blockchain.ledger.BlockchainIdentity;
import com.jd.blockchain.ledger.ContractInfo;
import com.jd.blockchain.ledger.DataAccountInfo;
import com.jd.blockchain.ledger.UserInfo;
import com.jd.httpservice.utils.web.WebResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import utils.BaseConstant;
import utils.ConsoleUtils;
import utils.StringUtils;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(path = "/")
public class DataSearchController {

	@Autowired
	private DataRetrievalService dataRetrievalService;
	@Autowired
	private DataSearchService dataSearchService;

	private String dataRetrievalUrl;
	private String schemaRetrievalUrl;

	@RequestMapping(method = RequestMethod.GET, path = "utils/pubkey/{pubkey}/addr")
	public String getAddrByPubKey(@PathVariable(name = "pubkey") String strPubKey) {
		PubKey pubKey = KeyGenUtils.decodePubKey(strPubKey);
		return AddressEncoding.generateAddress(pubKey).toBase58();
	}

	@RequestMapping(method = RequestMethod.GET, value = "ledgers/{ledgerHash}/*/*/search")
	public Object dataRetrieval(@PathVariable(name = "ledgerHash") HashDigest ledgerHash,
								HttpServletRequest request,
								@RequestParam(name = "keyword") String keyword) {
		if (dataRetrievalUrl == null || dataRetrievalUrl.length() <= 0) {
			// 未配置高级检索，使用完整查询
			return dataQuery(request.getRequestURI(), ledgerHash, keyword);
		} else {
			String queryParams = request.getQueryString() == null ? "" : request.getQueryString();
			String fullQueryUrl = new StringBuffer(dataRetrievalUrl).append(request.getRequestURI())
					.append(BaseConstant.DELIMETER_QUESTION).append(queryParams).toString();
			try {
				String result = dataRetrievalService.retrieval(fullQueryUrl);
				ConsoleUtils.info("request = {%s} \r\n result = {%s} \r\n", fullQueryUrl, result);
				return result;
			} catch (Exception e) {
				return "{'message':'OK','data':'" + e.getMessage() + "'}";
			}
		}
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
				e.printStackTrace();
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

	private WebResponse dataQuery(String uri, HashDigest ledgerHash, String keyword) {
		if (StringUtils.isEmpty(keyword)) {
			return WebResponse.createFailureResult(new WebResponse.ErrorMessage(-1, "empty keywords"));
		}
		String ledger = ledgerHash.toString();
		String type = uri.substring(uri.indexOf(ledger) + ledger.length(), uri.lastIndexOf("search"));
		Object data = null;
		switch (type) {
			case "/all/":// 综合查询
				data = dataSearchService.searchAll(ledgerHash, keyword);
				break;
			case "/accounts/count/":// 数据账户数
				data = dataSearchService.searchDataAccountCount(ledgerHash, keyword);
				break;
			case "/accounts/":// 数据账户
				DataAccountInfo dataAccount = dataSearchService.searchDataAccount(ledgerHash, keyword);
				data = null != dataAccount ? new DataAccountInfo[]{dataAccount} : new DataAccountInfo[]{};
				break;
			case "/events/accounts/count/":// 事件账户账户数
				data = dataSearchService.searchEventAccountCount(ledgerHash, keyword);
				break;
			case "/events/accounts/":// 事件账户
				BlockchainIdentity eventAccount = dataSearchService.searchEventAccount(ledgerHash, keyword);
				data = null != eventAccount ? new BlockchainIdentity[]{eventAccount} : new BlockchainIdentity[]{};
				break;
			case "/contracts/count/":// 合约账户数
				data = dataSearchService.searchContractAccountCount(ledgerHash, keyword);
				break;
			case "/contracts/":// 合约
				ContractInfo contract = dataSearchService.searchContractAccount(ledgerHash, keyword);
				data = null != contract ? new ContractInfo[]{contract} : new ContractInfo[]{};
				break;
			case "/users/count/":// 用户账户数
				data = dataSearchService.searchUserCount(ledgerHash, keyword);
				break;
			case "/users/":// 用户
				UserInfo user = dataSearchService.searchUser(ledgerHash, keyword);
				data = null != user ? new UserInfo[]{user} : new UserInfo[]{};
				break;
			default:
				return WebResponse.createFailureResult(new WebResponse.ErrorMessage(-1, "404"));
		}

		return WebResponse.createSuccessResult(data);
	}
}
