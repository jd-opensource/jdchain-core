package com.jd.blockchain.tools.deactiveparti;

import com.jd.blockchain.crypto.AddressEncoding;
import com.jd.blockchain.crypto.KeyGenUtils;
import com.jd.blockchain.crypto.PubKey;
import com.jd.blockchain.utils.ArgumentSet;
import com.jd.blockchain.utils.Bytes;
import com.jd.blockchain.utils.ConsoleUtils;
import com.jd.blockchain.utils.http.converters.JsonResponseConverter;
import com.jd.blockchain.utils.web.model.WebResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: zhangshuang
 * @Date: 2020/10/22 5:59 PM
 * Version 1.0
 */
public class DeactivePartiCommand {

    private static final String DEACTIVE_LEDGER_ARG = "-ledger";

    private static final String DEACTIVE_PARTI_ADDRESS_ARG = "-participantAddress";

    private static final String DEACTIVE_PARTI_HTTP_HOST_ARG = "-httphost";

    private static final String DEACTIVE_PARTI_HTTP_PORT_ARG = "-httpport";

    private static final String DEACTIVE_PARTI_SYNC_HTTP_HOST_ARG = "-synchost";

    private static final String DEACTIVE_PARTI_SYNC_HTTP_PORT_ARG = "-syncport";

    // 是否输出调试信息；
    private static final String OPT_DEBUG = "-debug";

    /**
     * 入口；
     *
     * @param args
     */
    public static void main(String[] args) {
        ArgumentSet.Setting setting = ArgumentSet.setting().prefix(DEACTIVE_LEDGER_ARG, DEACTIVE_PARTI_ADDRESS_ARG, DEACTIVE_PARTI_HTTP_HOST_ARG, DEACTIVE_PARTI_HTTP_PORT_ARG, DEACTIVE_PARTI_SYNC_HTTP_HOST_ARG, DEACTIVE_PARTI_SYNC_HTTP_PORT_ARG)
                .option(OPT_DEBUG);
        ArgumentSet argSet = ArgumentSet.resolve(args, setting);
        try {
            ArgumentSet.ArgEntry[] argEntries = argSet.getArgs();
            if (argEntries.length == 0) {
                ConsoleUtils.info("Miss argument!\r\n"
                        + "-ledger : Deactive participant ledger info.\r\n"
                        + "-participantAddress : Deactive participant address info.\r\n"
                        + "-httphost : Deactive participant boot http host info.\r\n"
                        + "-httpport : Deactive participant boot http port info.\r\n"
                        + "-synchost : Deactive participant sync data source host info.\r\n"
                        + "-syncport : Deactive participant sync data source port info.\r\n"
                        + "-debug : Debug mode, optional.\r\n");
                return;
            }

            if (argSet.getArg(DEACTIVE_LEDGER_ARG) == null) {
                ConsoleUtils.info("Miss deactive ledger info!");
                return;
            }

            if (argSet.getArg(DEACTIVE_PARTI_HTTP_HOST_ARG) == null) {
                ConsoleUtils.info("Miss deactive participant http host info!");
                return;
            }

            if (argSet.getArg(DEACTIVE_PARTI_HTTP_PORT_ARG) == null) {
                ConsoleUtils.info("Miss deactive participant http port info!");
                return;
            }

            if (argSet.getArg(DEACTIVE_PARTI_SYNC_HTTP_HOST_ARG) == null) {
                ConsoleUtils.info("Miss sync data source host info!");
                return;
            }

            if (argSet.getArg(DEACTIVE_PARTI_HTTP_PORT_ARG) == null) {
                ConsoleUtils.info("Miss sync data source port info!");
                return;
            }

            String url = "http://" + argSet.getArg(DEACTIVE_PARTI_HTTP_HOST_ARG).getValue() + ":" +  argSet.getArg(DEACTIVE_PARTI_HTTP_PORT_ARG).getValue() + "/management/delegate/deactiveparticipant";

            System.out.println("url = " + url);

            PubKey deactivePubKey = KeyGenUtils.decodePubKey(argSet.getArg(DEACTIVE_PARTI_ADDRESS_ARG).getValue());

            Bytes address = AddressEncoding.generateAddress(deactivePubKey);

            HttpPost httpPost = new HttpPost(url);

            List<BasicNameValuePair> para=new ArrayList<BasicNameValuePair>();

            // 账本值根据具体情况进行修改
            BasicNameValuePair base58LedgerHash = new BasicNameValuePair("ledgerHash",  argSet.getArg(DEACTIVE_LEDGER_ARG).getValue());

            BasicNameValuePair deactiveAddress = new BasicNameValuePair("participantAddress", address.toBase58());
            // 指定已经启动的其他共识节点的HTTP管理端口
            BasicNameValuePair manageHost = new BasicNameValuePair("remoteManageHost",  argSet.getArg(DEACTIVE_PARTI_SYNC_HTTP_HOST_ARG).getValue());
            BasicNameValuePair managePort = new BasicNameValuePair("remoteManagePort", argSet.getArg(DEACTIVE_PARTI_SYNC_HTTP_PORT_ARG).getValue());

            para.add(base58LedgerHash);
            para.add(deactiveAddress);
            para.add(manageHost);
            para.add(managePort);

            httpPost.setEntity(new UrlEncodedFormEntity(para,"UTF-8"));
            HttpClient httpClient = HttpClients.createDefault();

            HttpResponse response = httpClient.execute(httpPost);
            JsonResponseConverter jsonConverter = new JsonResponseConverter(WebResponse.class);

            WebResponse webResponse = (WebResponse) jsonConverter.getResponse(null, response.getEntity().getContent(), null);

            ConsoleUtils.info("Deactive participant ,response result = %s", webResponse.isSuccess());

            if (!webResponse.isSuccess()) {
                ConsoleUtils.info("Deactive participant ,error msg = %s", webResponse.getError().getErrorMessage());
            }
        }
        catch (Exception e) {
            ConsoleUtils.info("Error!!! %s", e.getMessage());
            if (argSet.hasOption(OPT_DEBUG)) {
                e.printStackTrace();
            }
        }

    }
}
