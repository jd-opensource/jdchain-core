package com.jd.blockchain.ledger.core;

import com.jd.blockchain.crypto.HashDigest;
import com.jd.blockchain.crypto.HashFunction;
import utils.IllegalDataException;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: zhangshuang
 * @Date: 2021/9/6 3:26 PM
 * Version 1.0
 */
public class MerkleTreeSimple {
    /**
     * Zero
     */
    private static final int ZERO = 0;
    /**
     * One
     */
    private static final int ONE = 1;
    /**
     * Two
     */
    private static final int TWO = 2;

    private List<HashDigest> merkleNodes;

    private HashDigest preRootHash;

    private HashFunction hashFunction;


    public MerkleTreeSimple(HashFunction hashFunction, HashDigest preRootHash, List<HashDigest> merkleNodes) {
        this.hashFunction = hashFunction;
        this.preRootHash = preRootHash;
        this.merkleNodes = merkleNodes;
    }

    /**
     * 求默克尔根
     *
     * @return
     */
    public HashDigest root() {
        if (this.merkleNodes == null || this.merkleNodes.isEmpty()) {
            return preRootHash;
        }
        if (preRootHash == null) {
            return hash(this.merkleNodes);
        } else {
            return calcHash(preRootHash, hash(merkleNodes));
        }
    }

    /**
     * 计算默克尔节点Hash
     *
     * @param innerNodes
     * @return
     */
    private HashDigest hash(List<HashDigest> innerNodes) {
        if (innerNodes.size() == ONE) {
            return innerNodes.get(ZERO);
        } else {
            List<HashDigest> nodes = new ArrayList<>();
            final int innerSize = innerNodes.size();
            if (innerSize % TWO == ZERO) {
                // 偶数个
                for (int i = 0; i < innerSize; i++) {
                    HashDigest left = innerNodes.get(i), right = innerNodes.get(i + 1);
                    HashDigest nodeHash = calcHash(left, right);
                    nodes.add(nodeHash);
                    i++;
                }
                return hash(nodes);
            } else {
                // 奇数个，但不是一个，则最后一个无需计算
                for (int i = 0; i < innerSize - 1; i++) {
                    HashDigest left = innerNodes.get(i), right = innerNodes.get(i + 1);
                    HashDigest nodeHash = calcHash(left, right);
                    nodes.add(nodeHash);
                    i++;
                }
                nodes.add(innerNodes.get(innerSize - 1));
                return hash(nodes);
            }
        }
    }

    /**
     * 计算两个节点的Hash
     *
     * @param left
     *         左节点
     * @param right
     *         右节点
     * @return
     */
    private HashDigest calcHash(HashDigest left, HashDigest right) {
        byte[] leftHashContent = left.toBytes(), rightHashContent = right.toBytes();
        byte[] hashContent = new byte[leftHashContent.length + rightHashContent.length];
        // 填充 hashContent
        System.arraycopy(leftHashContent, 0, hashContent, 0, leftHashContent.length);
        System.arraycopy(rightHashContent, 0, hashContent, leftHashContent.length, rightHashContent.length);
        return hashFunction.hash(hashContent);
    }
}
