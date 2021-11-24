package test.com.jd.blockchain.gateway;

import com.jd.blockchain.gateway.GatewayConfigProperties;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import utils.net.NetworkAddress;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

@Ignore("耗时太长，不同机器环境不一定都能通过，测试用例需要重新设计")
public class GatewayConfigPropertiesTest {

    @Test
    public void test() {
        ClassPathResource gatewayConfigResource = new ClassPathResource("gateway.conf");
        try (InputStream in = gatewayConfigResource.getInputStream()) {
            GatewayConfigProperties configProps = GatewayConfigProperties.resolve(in);
            assertEquals("0.0.0.0", configProps.http().getHost());
            assertEquals(8081, configProps.http().getPort());
            assertNull(configProps.http().getContextPath());

            NetworkAddress networkAddress = configProps.masterPeerAddress();
            assertEquals("127.0.0.1", networkAddress.getHost());
            assertEquals(12000, networkAddress.getPort());

            assertTrue(configProps.isStoreTopology());

            assertEquals("http://127.0.0.1:10001", configProps.dataRetrievalUrl());

            assertEquals("7VeRLdGtSz1Y91gjLTqEdnkotzUfaAqdap3xw6fQ1yKHkvVq", configProps.keys().getDefault().getPubKeyValue());
            assertNull(configProps.keys().getDefault().getPrivKeyPath());
            assertEquals("177gjzHTznYdPgWqZrH43W3yp37onm74wYXT4v9FukpCHBrhRysBBZh7Pzdo5AMRyQGJD7x", configProps.keys().getDefault().getPrivKeyValue());
            assertEquals("DYu3G8aGTMBW1WrTw76zxQJQU4DHLw9MLyy7peG4LKkY", configProps.keys().getDefault().getPrivKeyPassword());

        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

}
