package com.jd.blockchain.ledger.merkletree;

import com.jd.binaryproto.BinaryProtocol;
import utils.io.BytesOutputBuffer;
import utils.io.BytesUtils;
import utils.io.NumberMask;

import java.lang.reflect.Array;

/**
 * HashBucketEntry序列化反序列化，比BinaryProto要快很多
 * TODO 优化BinaryProto而不是使用本工具类
 */
public class HashBucketEntryConvertor {

    static byte[] hashBucketEntryHeadBytes = new byte[12];
    static byte[] keyIndexHeadBytes = new byte[12];

    static {
        BytesUtils.toBytes(121, hashBucketEntryHeadBytes);
        BytesUtils.toBytes(9004247678717319455l, hashBucketEntryHeadBytes, 4);

        BytesUtils.toBytes(120, keyIndexHeadBytes);
        BytesUtils.toBytes(-256630039951651650l, keyIndexHeadBytes, 4);
    }

    public static byte[] encode(HashBucketEntry hbe) {
        BytesOutputBuffer out = new BytesOutputBuffer();
        // encode header
        encodeHashBucketEntryHeader(out);
        // encode keyset
        encodeKeySet(hbe.getKeySet(), out);

        return out.toBytes();
    }

    public static HashBucketEntry decode(byte[] data) {
        return BinaryProtocol.decode(data);
    }

    private static void encodeHashBucketEntryHeader(BytesOutputBuffer out) {
        out.write(hashBucketEntryHeadBytes);
    }

    private static void encodeKeySet(KeyIndex[] keySet, BytesOutputBuffer out) {
        int count = keySet == null ? 0 : Array.getLength(keySet);
        writeSize(count, out);

        BytesOutputBuffer in = new BytesOutputBuffer();
        for (int i = 0; i < count; i++) {
            encodeKeyIndex(keySet[i], in);
        }

        writeSize(in.getSize(), out);
        out.write(in);
    }

    private static void writeSize(int size, BytesOutputBuffer out) {
        byte[] countBytes = NumberMask.NORMAL.generateMask(size);
        out.write(countBytes);
    }

    private static void encodeKeyIndexHeader(BytesOutputBuffer out) {
        out.write(keyIndexHeadBytes);
    }

    private static void encodeKeyIndex(KeyIndex keyIndex, BytesOutputBuffer out) {
        // encode header
        encodeKeyIndexHeader(out);
        // encode key
        writeSize(keyIndex.getKey().length, out);
        out.write(keyIndex.getKey());
        // encode rootHash
        byte[] bytes = keyIndex.getRootHash() == null ? BytesUtils.EMPTY_BYTES : keyIndex.getRootHash().toBytes();
        int size = bytes.length;
        writeSize(size, out);
        out.write(bytes);
    }
}
