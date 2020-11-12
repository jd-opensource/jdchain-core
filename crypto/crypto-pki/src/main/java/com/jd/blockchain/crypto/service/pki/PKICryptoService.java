package com.jd.blockchain.crypto.service.pki;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.jd.blockchain.crypto.CryptoBytes;
import com.jd.blockchain.crypto.CryptoEncoding;
import com.jd.blockchain.crypto.CryptoFunction;
import com.jd.blockchain.crypto.CryptoService;
import com.jd.blockchain.crypto.base.DefaultCryptoEncoding;
import com.jd.blockchain.utils.provider.NamedProvider;

/**
 * @author zhanglin33
 * @title: PKICryptoService
 * @description: TODO
 * @date 2019-05-15, 16:35
 */
@NamedProvider("PKI-SOFTWARE")
public class PKICryptoService implements CryptoService {

    public static final SHA1WITHRSA2048SignatureFunction SHA1WITHRSA2048 = new SHA1WITHRSA2048SignatureFunction();

    public static final SHA1WITHRSA4096SignatureFunction SHA1WITHRSA4096 = new SHA1WITHRSA4096SignatureFunction();

    public static final SM3WITHSM2SignatureFunction SM3WITHSM2 = new SM3WITHSM2SignatureFunction();

    private static final Collection<CryptoFunction> FUNCTIONS;
    
	private static final CryptoFunction[] FUNCTION_ARRAY = { SHA1WITHRSA2048, SHA1WITHRSA4096, SM3WITHSM2};

	private static final Encoding ENCODING = new Encoding(FUNCTION_ARRAY);

    static {
        List<CryptoFunction> funcs = Arrays.asList(SHA1WITHRSA2048, SHA1WITHRSA4096, SM3WITHSM2);
        FUNCTIONS = Collections.unmodifiableList(funcs);
    }

    @Override
    public Collection<CryptoFunction> getFunctions() {
        return FUNCTIONS;
    }
    

	@Override
	public CryptoEncoding getEncoding() {
		return ENCODING;
	}

	private static class Encoding extends DefaultCryptoEncoding {

		private final CryptoFunction[] functions;

		public Encoding(CryptoFunction[] functions) {
			this.functions = functions;
		}

		@Override
		protected <T extends CryptoBytes> boolean supportCryptoBytes(short algorithmCode, Class<T> cryptoDataType,
				byte[] encodedCryptoBytes) {
			for (CryptoFunction func : functions) {
				if (func.getAlgorithm().code() == algorithmCode && func.support(cryptoDataType, encodedCryptoBytes)) {
					return true;
				}
			}
			return false;
		}

	}
}
