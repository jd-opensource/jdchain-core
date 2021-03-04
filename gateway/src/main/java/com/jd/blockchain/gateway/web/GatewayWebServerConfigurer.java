package com.jd.blockchain.gateway.web;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.jd.binaryproto.DataContractRegistry;
import com.jd.blockchain.consensus.bftsmart.BftsmartNodeSettings;
import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.LedgerMetadata_V2;
import com.jd.blockchain.ledger.PrivilegeSet;
import com.jd.blockchain.ledger.RoleInitSettings;
import com.jd.blockchain.ledger.RoleSet;
import com.jd.blockchain.ledger.RolesConfigureOperation;
import com.jd.blockchain.ledger.SecurityInitSettings;
import com.jd.blockchain.ledger.UserAuthInitSettings;
import com.jd.blockchain.ledger.UserAuthorizeOperation;
import com.jd.blockchain.ledger.json.ExtendJsonSerializeUtil;
import com.jd.blockchain.ledger.json.serialize.JsonSerializeFactory;
import com.jd.blockchain.ledger.proof.MerkleLeaf;
import com.jd.blockchain.ledger.proof.MerklePath;
import com.jd.blockchain.ledger.proof.MerkleTrieData;
import com.jd.blockchain.web.converters.BinaryMessageConverter;
import com.jd.blockchain.web.converters.HashDigestInputConverter;
import com.jd.blockchain.web.serializes.ByteArrayObjectUtil;
import com.jd.httpservice.utils.web.JsonWebResponseMessageConverter;

import utils.io.ByteArray;
import utils.serialize.json.JSONSerializeUtils;

/**
 * @author zhuguang
 * @date 2018-08-08
 */
@Configuration
public class GatewayWebServerConfigurer implements WebMvcConfigurer {

	static {
		JSONSerializeUtils.disableCircularReferenceDetect();
		JSONSerializeUtils.configStringSerializer(ByteArray.class);
		DataContractRegistry.register(BftsmartNodeSettings.class);

		// 注册角色/权限相关接口
		DataContractRegistry.register(RolesConfigureOperation.class);
		DataContractRegistry.register(RolesConfigureOperation.RolePrivilegeEntry.class);
		DataContractRegistry.register(UserAuthorizeOperation.class);
		DataContractRegistry.register(UserAuthorizeOperation.UserRolesEntry.class);
		DataContractRegistry.register(PrivilegeSet.class);
		DataContractRegistry.register(RoleSet.class);
		DataContractRegistry.register(SecurityInitSettings.class);
		DataContractRegistry.register(RoleInitSettings.class);
		DataContractRegistry.register(UserAuthInitSettings.class);
		DataContractRegistry.register(LedgerMetadata_V2.class);

		// 注册默克尔树相关接口
		DataContractRegistry.register(MerkleTrieData.class);
		DataContractRegistry.register(MerkleLeaf.class);
		DataContractRegistry.register(MerklePath.class);
		DataContractRegistry.register(CryptoSetting.class);
	}


	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		int index = converters.size();
		for (int i = 0; i < converters.size(); i++) {
			if (converters.get(i) instanceof MappingJackson2HttpMessageConverter) {
				index = i;
				break;
			}
		}

		JsonWebResponseMessageConverter jsonConverter = new JsonWebResponseMessageConverter(false);

		converters.add(index, jsonConverter);

		converters.add(0, new BinaryMessageConverter());

		initByteArrayJsonSerialize();
	}

	@Override
	public void addFormatters(FormatterRegistry registry) {
		registry.addConverter(new HashDigestInputConverter());
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/webjars/**")
				.addResourceLocations("classpath:/META-INF/resources");
	}

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/").setViewName("web/index.html");
	}

	private void initByteArrayJsonSerialize() {
		ByteArrayObjectUtil.init();
		// 附加CryptoSetting的序列化处理
		ExtendJsonSerializeUtil.init(CryptoSetting.class,
				JsonSerializeFactory.createSerializer(CryptoSetting.class),
				JsonSerializeFactory.createDeserializer(CryptoSetting.class));
	}
}
