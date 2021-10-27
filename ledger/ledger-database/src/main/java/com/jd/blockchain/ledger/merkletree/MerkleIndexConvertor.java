package com.jd.blockchain.ledger.merkletree;

import com.jd.blockchain.crypto.Crypto;
import com.jd.blockchain.crypto.HashDigest;
import utils.io.BytesOutputBuffer;
import utils.io.BytesUtils;
import utils.io.NumberMask;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * MerkleIndex序列化反序列化，比BinaryProto要快很多
 * TODO 优化BinaryProto而不是使用本工具类
 */
public class MerkleIndexConvertor {

    static byte[] headBytes = new byte[12];

    static {
        BytesUtils.toBytes(118, headBytes);
        BytesUtils.toBytes(6009995945693408607l, headBytes, 4);
    }

    public static byte[] encode(MerkleIndex mi) {
        BytesOutputBuffer out = new BytesOutputBuffer();
        // encode header
        encodeHeader(out);
        // encode offset
        out.write(NumberMask.LONG.generateMask(mi.getOffset()));
        // encode step
        out.write(NumberMask.LONG.generateMask(mi.getStep()));
        // encode childCounts
        encodeChildCounts(mi.getChildCounts(), out);
        // encode childHashs
        encodeChildHashs(mi.getChildHashs(), out);

        return out.toBytes();
    }

    public static MerkleIndex decode(byte[] data) {
        int offset = 12;
        if (null == data || data.length == offset) {
            return null;
        }
        // encode offset
        long offsetData = NumberMask.LONG.resolveMaskedNumber(data, offset);
        // encode step
        offset += NumberMask.LONG.resolveMaskLength(data[offset]);
        long setpData = NumberMask.LONG.resolveMaskedNumber(data, offset);
        // encode childCounts
        offset += NumberMask.LONG.resolveMaskLength(data[offset]);
        long[] childCounts = null;
        int l = NumberMask.NORMAL.resolveMaskLength(data[offset]);
        int length = (int) NumberMask.NORMAL.resolveMaskedNumber(data, offset);
        offset += l;
        if (length > 0) {
            childCounts = new long[length];
            for (int i = 0; i < length; i++) {
                l = NumberMask.LONG.resolveMaskLength(data[offset]);
                childCounts[i] = NumberMask.LONG.resolveMaskedNumber(data, offset);
                offset += l;
            }
        }
        // encode childHashs
        l = NumberMask.NORMAL.resolveMaskLength(data[offset]);
        length = (int) NumberMask.NORMAL.resolveMaskedNumber(data, offset);
        HashDigest[] childHashs = new HashDigest[length];
        if (length > 0) {
            offset += l;
            int vs = 0;
            for (int i = 0; i < length; i++) {
                l = NumberMask.NORMAL.resolveMaskLength(data[offset]);
                vs = (int) NumberMask.NORMAL.resolveMaskedNumber(data, offset);
                offset += l;
                if (vs > 0) {
                    byte[] bs = new byte[vs];
                    System.arraycopy(data, offset, bs, 0, vs);
                    childHashs[i] = Crypto.resolveAsHashDigest(bs);
                    offset += vs;
                } else {
                    childHashs[i] = null;
                }
            }
        } else {
            System.out.println(Arrays.toString(data));
        }

        return new MI(offsetData, setpData, childCounts, childHashs);
    }

    private static void encodeHeader(BytesOutputBuffer out) {
        out.write(headBytes);
    }

    private static void encodeChildCounts(long[] childCounts, BytesOutputBuffer out) {
        int count = childCounts == null ? 0 : Array.getLength(childCounts);

        writeSize(count, out);

        for (int i = 0; i < count; i++) {
            out.write(NumberMask.LONG.generateMask(childCounts[i]));
        }
    }

    private static void encodeChildHashs(HashDigest[] childHashs, BytesOutputBuffer out) {
        int count = childHashs == null ? 0 : Array.getLength(childHashs);

        writeSize(count, out);

        for (int i = 0; i < count; i++) {
            byte[] bytes = childHashs[i] == null ? BytesUtils.EMPTY_BYTES : childHashs[i].toBytes();
            int size = bytes.length;
            writeSize(size, out);
            out.write(bytes);
        }
    }

    private static void writeSize(int size, BytesOutputBuffer out) {
        byte[] countBytes = NumberMask.NORMAL.generateMask(size);
        out.write(countBytes);
    }

    private static class MI implements MerkleIndex {

        private long offset;
        private long step;
        private long[] childCounts;
        private HashDigest[] childHashs;

        public MI(long offset, long step, long[] childCounts, HashDigest[] childHashs) {
            this.offset = offset;
            this.step = step;
            this.childCounts = childCounts;
            this.childHashs = childHashs;
        }

        @Override
        public long getOffset() {
            return offset;
        }

        public void setOffset(long offset) {
            this.offset = offset;
        }

        @Override
        public long getStep() {
            return step;
        }

        public void setStep(long step) {
            this.step = step;
        }

        @Override
        public long[] getChildCounts() {
            return childCounts;
        }

        public void setChildCounts(long[] childCounts) {
            this.childCounts = childCounts;
        }

        @Override
        public HashDigest[] getChildHashs() {
            return childHashs;
        }

        public void setChildHashs(HashDigest[] childHashs) {
            this.childHashs = childHashs;
        }
    }
}
