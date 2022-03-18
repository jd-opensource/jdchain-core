package com.jd.blockchain.consensus.raft.rpc;

import com.alipay.remoting.exception.CodecException;
import com.alipay.remoting.serialization.Serializer;
import com.alipay.remoting.serialization.SerializerManager;
import com.alipay.sofa.jraft.CliService;
import com.alipay.sofa.jraft.RaftServiceFactory;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.core.CliServiceImpl;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.error.RemotingException;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.rpc.CliRequests;
import com.alipay.sofa.jraft.rpc.InvokeCallback;
import com.alipay.sofa.jraft.rpc.RpcClient;
import com.alipay.sofa.jraft.rpc.RpcResponseClosureAdapter;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import com.alipay.sofa.jraft.util.Endpoint;
import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import com.jd.binaryproto.DataContractAutoRegistrar;
import net.bytebuddy.implementation.bytecode.Throw;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Ignore
public class SubmitTxRequestProcessorTest {

    String groupId = "j5j4L7EcFkVJUyLN4wWbT66pTwsgNSsnHYwC3xSducjer6";

    CliClientServiceImpl cliService = (CliClientServiceImpl)(((CliServiceImpl)RaftServiceFactory.createAndInitCliService(new CliOptions())).getCliClientService());
    RpcClient rpcClient = cliService.getRpcClient();

    PeerId peerId0 = PeerId.parsePeer("127.0.0.1:16000");
    PeerId peerId1 = PeerId.parsePeer("127.0.0.1:16001");
    PeerId peerId2 = PeerId.parsePeer("127.0.0.1:16002");
    PeerId peerId3 = PeerId.parsePeer("127.0.0.1:16003");

    List<PeerId> peerIdList = Lists.newArrayList(peerId0, peerId1, peerId2,peerId3);

    public PeerId getLeader() throws ExecutionException, InterruptedException {
        final String[] leader = {null};
        Future<Message> messageFuture = cliService.getLeader(peerId1.getEndpoint(),
                CliRequests.GetLeaderRequest.newBuilder().setGroupId(groupId).build(), new RpcResponseClosureAdapter<CliRequests.GetLeaderResponse>(){

                    @Override
                    public void run(Status status) {
                        CliRequests.GetLeaderResponse response = getResponse();
                        leader[0] = response.getLeaderId();
                    }
                });

        messageFuture.get();
        return PeerId.parsePeer(leader[0]);
    }


    @Test
    public void testGetLeader() throws ExecutionException, InterruptedException {
        PeerId leader = getLeader();
        System.out.println(leader);

    }

    @Test
    public void testSubmitTxRequestToLeader() throws ExecutionException, InterruptedException, RemotingException {

        PeerId leader = getLeader();

        System.out.println("request to leader: " + leader);

        SubmitTxRequest submitTxRequest = new SubmitTxRequest();
        submitTxRequest.setTx(new byte[]{10,11,12});

        RpcResponse response = (RpcResponse) rpcClient.invokeSync(leader.getEndpoint(), submitTxRequest, 1000*20L);

        System.out.println(response.getResult());
        System.out.println(response.isSuccess());
        System.out.println(response.getErrorMessage());
    }

    @Test
    public void testSubmitTxRequestToFollower() throws ExecutionException, InterruptedException, RemotingException {

        PeerId leader = getLeader();
        PeerId follower = selectFollower(leader);

        System.out.println("request to: " + follower);

        SubmitTxRequest submitTxRequest = new SubmitTxRequest();
        submitTxRequest.setTx(new byte[]{10,11,12});

        RpcResponse response = (RpcResponse) rpcClient.invokeSync(follower.getEndpoint(), submitTxRequest, 1000*10L);

        System.out.println(response.getResult());
        System.out.println(response.isSuccess());
        System.out.println(response.getErrorMessage());
    }



    @Test
    public void benchTest() throws InterruptedException, ExecutionException {

        PeerId leader = getLeader();

        if(leader == null){
            System.err.println("leader is null");
            System.exit(1);
        }

        System.out.println("leader is: " + leader);


        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger error = new AtomicInteger(0);

        int threadCount = 200;

        Thread[] threads = new Thread[threadCount];
        for(int i= 0; i< threadCount; i++){
            BenchRunable benchRunable = new BenchRunable(200, true, true, leader, success, error);
            threads[i] = new Thread(benchRunable, "Thread-"+i);
        }

        long time = System.currentTimeMillis();

        for(Thread thread : threads){
            thread.start();
        }

        for(Thread thread : threads){
            thread.join();
        }

        long end = System.currentTimeMillis();

        System.out.println("cost ms:" + (end - time));
        System.out.println("success: " + success.get());
        System.out.println("error:" + error.get());

    }

    class BenchRunable implements Runnable {

        private int loops;
        private boolean sync;
        private boolean onlyToLeader;
        private PeerId leader;
        private AtomicInteger success;
        private AtomicInteger error;

        private SubmitTxRequest submitTxRequest = new SubmitTxRequest();

        private List<Long> costmsList = new ArrayList<>(loops);

        public BenchRunable(int loops, boolean sync, boolean onlyToLeader, PeerId leader, AtomicInteger success, AtomicInteger error){
            this.loops = loops;
            this.sync = sync;
            this.onlyToLeader = onlyToLeader;
            this.leader = leader;
            this.success = success;
            this.error = error;
            submitTxRequest.setTx(new byte[]{10,11,12});
        }

        @Override
        public void run() {

            final List<CompletableFuture> futureList = new ArrayList<>(loops);

            for(int i = 1; i<= loops; i++){
                PeerId peerId = onlyToLeader ? leader : peerIdList.get(ThreadLocalRandom.current().nextInt(peerIdList.size()));
                try {
                    if(sync){

                        long start = System.currentTimeMillis();

                        RpcResponse rpcResponse = (RpcResponse)rpcClient.invokeSync(peerId.getEndpoint(), submitTxRequest, 1000*20L);

                        if(rpcResponse.isSuccess()){
                            success.incrementAndGet();
                        }else{
                            error.incrementAndGet();
                        }

                        long end = System.currentTimeMillis();

                        costmsList.add((end - start));

                    }else{

                        CompletableFuture future = new CompletableFuture();
                        futureList.add(future);

                        System.out.println(Thread.currentThread().getName() + "----" + peerId.getEndpoint());

                        rpcClient.invokeAsync(peerId.getEndpoint(), submitTxRequest, new InvokeCallback() {
                            @Override
                            public void complete(Object o, Throwable throwable) {

                                if(throwable != null){
                                    error.incrementAndGet();
                                }else{
                                    RpcResponse rpcResponse = (RpcResponse) o;
                                    if(rpcResponse.isSuccess()){
                                        success.incrementAndGet();
                                    }else{
                                        error.incrementAndGet();
                                    }
                                }

                                future.complete(new Object());
                            }
                        },  1000*20L);
                    }



                } catch (InterruptedException e) {
                    e.printStackTrace();
                    error.incrementAndGet();
                } catch (RemotingException e) {
                    e.printStackTrace();
                    error.incrementAndGet();
                }
            }

            if(!futureList.isEmpty()){
                for(Future future : futureList){
                    try {
                        future.get();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }

            String collect = costmsList.stream().map(x -> x.toString()).sorted().collect(Collectors.joining(","));
            System.out.println(Thread.currentThread().getName() +": ["+ collect +"]");

        }
    }


    @Test
    public void testSerialize() throws CodecException {

        Serializer serializer = SerializerManager.getSerializer(SerializerManager.Hessian2);

        List<byte[]> txs = new ArrayList<>();

        byte[] b = serializer.serialize(txs);

        List<byte[]> o = serializer.deserialize(b, b.getClass().getName());

        System.out.println(b);
        System.out.println(o);


        txs.add(new byte[]{10,11,12});
        b = serializer.serialize(txs);
        o = serializer.deserialize(b, b.getClass().getName());
        System.out.println(b);
        System.out.println(o);

        txs.add(null);
        b = serializer.serialize(txs);
        o = serializer.deserialize(b, b.getClass().getName());
        System.out.println(b);
        System.out.println(o);


        txs.add(new byte[]{22,23,24});
        b = serializer.serialize(txs);
        o = serializer.deserialize(b, b.getClass().getName());
        System.out.println(b);
        System.out.println(o);
    }


    private PeerId selectFollower(PeerId leader) {
        if(leader.equals(peerId0)){
            return peerId1;
        }
        if(leader.equals(peerId1)){
            return peerId2;
        }

        if(leader.equals(peerId2)){
            return peerId0;
        }
        return null;
    }


    @Test
    public void testLoadBinary(){

        ServiceLoader<DataContractAutoRegistrar> load = ServiceLoader.load(DataContractAutoRegistrar.class);

        for (Iterator<DataContractAutoRegistrar> it = load.iterator(); it.hasNext(); ) {
            DataContractAutoRegistrar dataContractAutoRegistrar = it.next();
            System.out.println(dataContractAutoRegistrar);
        }


    }

}
