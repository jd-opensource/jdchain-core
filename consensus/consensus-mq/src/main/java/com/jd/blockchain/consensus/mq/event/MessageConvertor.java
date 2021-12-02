/**
 * Copyright: Copyright 2016-2020 JD.COM All Right Reserved
 * FileName: com.jd.blockchain.sdk.mq.event.MessageConvertUtil
 * Author: shaozhuguang
 * Department: 区块链研发部
 * Date: 2018/11/21 下午7:28
 * Description:
 */
package com.jd.blockchain.consensus.mq.event;

import com.alibaba.fastjson.JSON;
import org.springframework.util.DigestUtils;


/**
 * @author shaozhuguang
 * @create 2018/11/21
 * @since 1.0.0
 */

public class MessageConvertor {

    public static final String defaultCharsetName = "UTF-8";

    public static String messageKey(byte[] src) {
        return DigestUtils.md5DigestAsHex(src);
    }

    public static TxResultMessage convertBytesToTxResultEvent(byte[] txResult) {
        try {
            return convertStringToTxResultEvent(new String(txResult, defaultCharsetName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static TxResultMessage convertStringToTxResultEvent(String txResult) {
        return JSON.parseObject(txResult, TxResultMessage.class);
    }

    public static byte[] serializeTxResultEvent(TxResultMessage txResultMessage) {
        try {
            return JSON.toJSONString(txResultMessage).getBytes(defaultCharsetName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] serializeBlockTxs(BlockMessage blockMessage) {
        byte[] serializeBytes;
        try {
            serializeBytes = JSON.toJSONString(blockMessage).getBytes(defaultCharsetName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return serializeBytes;
    }

    public static BlockMessage convertBytesToBlockTxs(byte[] bytes) {
        try {
            return JSON.parseObject(new String(bytes, defaultCharsetName), BlockMessage.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}