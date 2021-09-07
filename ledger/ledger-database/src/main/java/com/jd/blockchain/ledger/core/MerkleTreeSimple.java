//package com.jd.blockchain.ledger.core;
//
//import com.jd.blockchain.crypto.HashDigest;
//import utils.IllegalDataException;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * @Author: zhangshuang
// * @Date: 2021/9/6 3:26 PM
// * Version 1.0
// */
//public class MerkleTreeSimple {
//    /**
//     * Zero
//     */
//    private static final int ZERO = 0;
//    /**
//     * One
//     */
//    private static final int ONE = 1;
//    /**
//     * Two
//     */
//    private static final int TWO = 2;
//
//    private List<MerkleNodeSimple> merkleNodes;
//
//    public MerkleTreeSimple() {
//        merkleNodes = new ArrayList<>();
//    }
//
//    public MerkleTreeSimple(List<MerkleNodeSimple> merkleNodes) {
//        this.merkleNodes = merkleNodes;
//    }
//
//    /**
//     * 求默克尔根
//     *
//     * @return
//     */
//    public HashDigest root() {
//        if (this.merkleNodes == null || this.merkleNodes.isEmpty()) {
//            return null;
//        }
//        return hash(this.merkleNodes);
//    }
//
//    /**
//     * 计算默克尔节点Hash
//     *
//     * @param innerNodes
//     * @return
//     */
//    private HashDigest hash(List<MerkleNodeSimple> innerNodes) {
//        if (innerNodes.size() == ONE) {
//            return innerNodes.get(ZERO).hash();
//        } else {
//            List<MerkleNodeSimple> nodes = new ArrayList<>();
//            final int innerSize = innerNodes.size();
//            if (innerSize % TWO == ZERO) {
//                // 偶数个
//                for (int i = 0; i < innerSize; i++) {
//                    MerkleNodeSimple left = innerNodes.get(i), right = innerNodes.get(i + 1);
//                    HashDigest nodeHash = calcHash(left, right);
//                    nodes.add(new MerkleNodeData(nodeHash));
//                    i++;
//                }
//                return hash(nodes);
//            } else {
//                // 奇数个，但不是一个，则最后一个无需计算
//                for (int i = 0; i < innerSize - 1; i++) {
//                    MerkleNodeSimple left = innerNodes.get(i), right = innerNodes.get(i + 1);
//                    HashDigest nodeHash = calcHash(left, right);
//                    nodes.add(new MerkleNodeData(nodeHash));
//                    i++;
//                }
//                nodes.add(innerNodes.get(innerSize - 1));
//                return hash(nodes);
//            }
//        }
//    }
//
//    /**
//     * 计算两个节点的Hash
//     *
//     * @param left
//     *         左节点
//     * @param right
//     *         右节点
//     * @return
//     */
//    private HashDigest calcHash(MerkleNodeSimple left, MerkleNodeSimple right) {
//        HashDigest leftHash = left.hash(), rightHash = right.hash();
//        // 两者的Hash算法需要一致
//        if (leftHash.getAlgorithm() != rightHash.getAlgorithm()) {
//            throw new IllegalDataException(String.format("%s and %s have not same hash algorithm !!!",
//                    leftHash.toBase58(), rightHash.toBase58()));
//        }
//        // 使用任一hash算法进行处理
//        byte algorithm = leftHash.getAlgorithm();
//        byte[] leftHashContent = leftHash.getContent(), rightHashContent = rightHash.getContent();
//        byte[] hashContent = new byte[leftHashContent.length + rightHashContent.length];
//        // 填充 hashContent
//        System.arraycopy(leftHashContent, 0, hashContent, 0, leftHashContent.length);
//        System.arraycopy(rightHashContent, 0, hashContent, leftHashContent.length, rightHashContent.length);
//        return HashFactory.hash(hashContent, algorithm);
//    }
//
//    /**
//     * 默克尔节点
//     *
//     */
//    public static class MerkleNodeData implements MerkleNodeSimple {
//
//        private HashDigest hash;
//
//        public MerkleNodeData(HashDigest hash) {
//            this.hash = hash;
//        }
//
//        @Override
//        public HashDigest hash() {
//            return hash;
//        }
//    }
//}
