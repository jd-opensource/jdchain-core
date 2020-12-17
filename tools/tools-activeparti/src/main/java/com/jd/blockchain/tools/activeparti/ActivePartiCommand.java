package com.jd.blockchain.tools.activeparti;

import com.jd.blockchain.utils.ArgumentSet;
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
public class ActivePartiCommand {

    private static final String ACTIVE_LEDGER_ARG = "-ledger";

    private static final String NEW_PARTI_HTTP_HOST_ARG = "-httphost";

    private static final String NEW_PARTI_HTTP_PORT_ARG = "-httpport";

    private static final String NEW_PARTI_CONSENSUS_HOST_ARG = "-consensushost";

    private static final String NEW_PARTI_CONSENSUS_PORT_ARG = "-consensusport";

    private static final String NEW_PARTI_SYNC_HTTP_HOST_ARG = "-synchost";

    private static final String NEW_PARTI_SYNC_HTTP_PORT_ARG = "-syncport";

    // 是否关闭旧的IP下的共识节点服务
    private static final String SHUTDOWN_OLD_SERVER = "-shutdown";

    // 是否输出调试信息；
    private static final String OPT_DEBUG = "-debug";

    /**
     * 入口；
     *
     * @param args
     */
    public static void main(String[] args) {
        ArgumentSet.Setting setting = ArgumentSet.setting().prefix(ACTIVE_LEDGER_ARG, NEW_PARTI_HTTP_HOST_ARG, NEW_PARTI_HTTP_PORT_ARG, NEW_PARTI_CONSENSUS_HOST_ARG, NEW_PARTI_CONSENSUS_PORT_ARG, NEW_PARTI_SYNC_HTTP_HOST_ARG, NEW_PARTI_SYNC_HTTP_PORT_ARG)
                .option(SHUTDOWN_OLD_SERVER, OPT_DEBUG);
        ArgumentSet argSet = ArgumentSet.resolve(args, setting);
        try {
            ArgumentSet.ArgEntry[] argEntries = argSet.getArgs();
            if (argEntries.length == 0) {
                ConsoleUtils.info("Miss argument!\r\n"
                        + "-ledger : New participant active ledger info.\r\n"
                        + "-httphost : New participant boot http host info.\r\n"
                        + "-httpport : New participant boot http port info.\r\n"
                        + "-consensushost : New participant consensus host info.\r\n"
                        + "-consensusport : New participant consensus port info.\r\n"
                        + "-synchost : New participant sync data source host info.\r\n"
                        + "-syncport : New participant sync data source port info.\r\n"
                        + "-shutdown : Shutdown the node server in the old IP.\r\n"
                        + "-debug : Debug mode, optional.\r\n");
                return;
            }

            if (argSet.getArg(ACTIVE_LEDGER_ARG) == null) {
                ConsoleUtils.info("Miss active ledger info!");
                return;
            }

            ArgumentSet.ArgEntry httpHost = argSet.getArg(NEW_PARTI_HTTP_HOST_ARG);
            if (httpHost == null) {
                ConsoleUtils.info("Miss new participant http host info!");
                return;
            }

            ArgumentSet.ArgEntry httpPort = argSet.getArg(NEW_PARTI_HTTP_PORT_ARG);
            if (httpPort == null) {
                ConsoleUtils.info("Miss new participant http port info!");
                return;
            }

            ArgumentSet.ArgEntry consensusHost = argSet.getArg(NEW_PARTI_CONSENSUS_HOST_ARG);
            if (consensusHost == null) {
                ConsoleUtils.info("Miss new participant consensus host info!");
                return;
            }

            ArgumentSet.ArgEntry consensusPort = argSet.getArg(NEW_PARTI_CONSENSUS_PORT_ARG);
            if (consensusPort == null) {
                ConsoleUtils.info("Miss new participant consensus port info!");
                return;
            }

            ArgumentSet.ArgEntry syncHost = argSet.getArg(NEW_PARTI_SYNC_HTTP_HOST_ARG);
            if (syncHost == null) {
                ConsoleUtils.info("Miss sync data source host info!");
                return;
            }

            ArgumentSet.ArgEntry syncPort = argSet.getArg(NEW_PARTI_SYNC_HTTP_PORT_ARG);
            if (syncPort == null) {
                ConsoleUtils.info("Miss sync data source port info!");
                return;
            }

            if(httpHost.getValue().equals(syncHost.getValue()) && httpPort.getValue().equals(syncPort.getValue())) {
                ConsoleUtils.info("Http host/port and sync host/port must be different!");
                return;
            }

            boolean shutdown = false;
            if (argSet.hasOption(SHUTDOWN_OLD_SERVER)) {
                shutdown = true;
            }

            String url = "http://" + httpHost.getValue() + ":" +  httpPort.getValue() + "/management/delegate/activeparticipant";

            ConsoleUtils.info("Active participant, Url = %s", url);

            HttpPost httpPost = new HttpPost(url);

            List<BasicNameValuePair> para=new ArrayList<BasicNameValuePair>();

            // 账本值根据具体情况进行修改
            BasicNameValuePair base58LedgerHash = new BasicNameValuePair("ledgerHash",  argSet.getArg(ACTIVE_LEDGER_ARG).getValue());

            // 激活的新参与方的共识网络地址
            BasicNameValuePair host = new BasicNameValuePair("consensusHost",  consensusHost.getValue());
            BasicNameValuePair port = new BasicNameValuePair("consensusPort", consensusPort.getValue());

            // 指定已经启动的其他共识节点的HTTP管理端口,该节点拥有共识网络中的最高区块
            BasicNameValuePair manageHost = new BasicNameValuePair("remoteManageHost",  syncHost.getValue());
            BasicNameValuePair managePort = new BasicNameValuePair("remoteManagePort", syncPort.getValue());

            // 是否关闭旧IP下的共识服务节点
            BasicNameValuePair shutdownParam = new BasicNameValuePair("shutdown", shutdown +"");

            para.add(base58LedgerHash);
            para.add(host);
            para.add(port);
            para.add(manageHost);
            para.add(managePort);
            para.add(shutdownParam);

            httpPost.setEntity(new UrlEncodedFormEntity(para,"UTF-8"));
            HttpClient httpClient = HttpClients.createDefault();

            HttpResponse response = httpClient.execute(httpPost);
            JsonResponseConverter jsonConverter = new JsonResponseConverter(WebResponse.class);

            WebResponse webResponse = (WebResponse) jsonConverter.getResponse(null, response.getEntity().getContent(), null);

            ConsoleUtils.info("Active participant ,response result = {%s}", webResponse.isSuccess());

            if (!webResponse.isSuccess()) {
                ConsoleUtils.info("Active participant ,error msg = {%s}", webResponse.getError().getErrorMessage());
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
