package com.jd.blockchain.peer.web;

import java.util.List;

import com.jd.blockchain.ledger.CryptoSetting;
import com.jd.blockchain.ledger.json.ExtendJsonSerializeUtil;
import com.jd.blockchain.ledger.json.serialize.JsonSerializeFactory;
import com.jd.blockchain.web.converters.BinaryMessageConverter;
import com.jd.blockchain.web.converters.HashDigestInputConverter;

import com.jd.blockchain.web.serializes.ByteArrayObjectUtil;
import com.jd.httpservice.utils.web.JsonWebResponseMessageConverter;

import utils.io.ByteArray;
import utils.serialize.json.JSONSerializeUtils;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PeerWebServerConfigurer implements WebMvcConfigurer {

	static {
		JSONSerializeUtils.disableCircularReferenceDetect();
		JSONSerializeUtils.configStringSerializer(ByteArray.class);
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
		converters.add(index, new JsonWebResponseMessageConverter());

		converters.add(0, new BinaryMessageConverter());

		initByteArrayJsonSerialize();
	}
	
	@Override
	public void addFormatters(FormatterRegistry registry) {
		registry.addConverter(new HashDigestInputConverter());
	}

	private void initByteArrayJsonSerialize() {
		ByteArrayObjectUtil.init();
		// 附加CryptoSetting的序列化处理
		ExtendJsonSerializeUtil.init(CryptoSetting.class,
				JsonSerializeFactory.createSerializer(CryptoSetting.class),
				JsonSerializeFactory.createDeserializer(CryptoSetting.class));
	}
}
