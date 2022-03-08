package com.jd.blockchain.consensus.raft.msgbus;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MessageBusTest {

    private MessageBus messageBus = new MessageBusComponent(1024 * 1024);
    private List<String> topicList = Arrays.asList(new String[]{"a","b","c","d"});

    private Subcriber[] subcribers = new Subcriber[10000];

    @Test
    public void testPublish() throws InterruptedException {

        for(int i = 0;i < subcribers.length; i++){
            int index = i;
            subcribers[i] = new Subcriber() {

                @Override
                public boolean equals(Object obj) {
                    return super.equals(obj);
                }

                @Override
                public void onMessage(byte[] message) {
                    System.out.println(index + ":" + Thread.currentThread().getName());
                }

                @Override
                public void onQuit() {

                }
            };
        }

        Thread[] registers = new Thread[10];
        Thread[] degisters = new Thread[10];
        Thread[] publish = new Thread[30];

        for(int i = 0; i< registers.length; i++){
            registers[i] = new Thread(new RegisterThread());
        }

        for(int i = 0; i< degisters.length; i++){
            degisters[i] = new Thread(new DeRegisterThread());
        }

        for(int i = 0; i< publish.length; i++){
            publish[i] = new Thread(new PublishThread());
        }


        for(int i = 0; i< registers.length; i++){
            registers[i].start();
        }

        for(int i = 0; i< degisters.length; i++){
            degisters[i].start();
        }

        for(int i = 0; i< publish.length; i++){
            publish[i].start();
        }


        Thread.sleep(20*1000);

        System.out.println(messageBus);

    }





    class DeRegisterThread implements Runnable {

        @Override
        public void run() {

            while (true){

                String topic = topicList.get(ThreadLocalRandom.current().nextInt(topicList.size()));

                Subcriber subcriber = subcribers[ThreadLocalRandom.current().nextInt(subcribers.length)];

                messageBus.deregister(topic, subcriber);

//                try {
//                    Thread.sleep(ThreadLocalRandom.current().nextInt(1000,3000));
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }

        }
    }

    class RegisterThread implements Runnable {

        @Override
        public void run() {

            while (true){

                String topic = topicList.get(ThreadLocalRandom.current().nextInt(topicList.size()));

                Subcriber subcriber = subcribers[ThreadLocalRandom.current().nextInt(subcribers.length)];

                messageBus.register(topic, subcriber);

//                try {
//                    Thread.sleep(ThreadLocalRandom.current().nextInt(1000,3000));
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }

        }
    }


    class PublishThread implements Runnable {

        @Override
        public void run() {

            while (true){
                String topic = topicList.get(ThreadLocalRandom.current().nextInt(topicList.size()));

                messageBus.publish(topic, new byte[]{1});

//                try {
//                    Thread.sleep(ThreadLocalRandom.current().nextInt(1000,3000));
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }


        }
    }

}
