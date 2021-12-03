package com.nimvb.app.springkafkaexample;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.LongStream;

@SpringBootApplication
public class SpringKafkaExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringKafkaExampleApplication.class, args);
    }


    @Bean
    NewTopic topic() {
        return new NewTopic("livestreams", 3, (short) 3);
    }


    @Component
    @RequiredArgsConstructor
    static class Producer {
        private final KafkaTemplate<Long, String> kafkaTemplate;

        @EventListener(ConsumersRegistrationSuccessedEvent.class)
        public void produce() {
            LongStream.range(0, 10).forEach(value -> kafkaTemplate.send("livestreams", value, "livestreams"+value+" !")
                    .addCallback(result -> {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Record created at topic ").append(result.getRecordMetadata().topic()).append("\n");
                        sb.append("\toffset: ").append(result.getRecordMetadata().offset()).append("\n");
                        sb.append("\tpartition: ").append(result.getRecordMetadata().partition()).append("\n");
                        System.out.println(sb.toString());
                    }, ex -> {
                        System.err.println(ex.getMessage());
                    }));
        }

    }


    @Data
    static class ConsumersRegistrationSuccessedEvent {
        //private String message;
    }


    @Component
    @RequiredArgsConstructor
    static class Consumer {


        @KafkaListener(groupId = "#{T(java.util.UUID).randomUUID().toString()}",topics = "livestreams",id = "consumer",autoStartup = "false")
        public void consume(ConsumerRecord<Long,String> record){
            StringBuilder sb = new StringBuilder();
            sb.append("Record consumed at ").append(record.timestamp()).append(" for ").append("consumer").append("\n");
            sb.append("\tkey: ").append(record.key()).append("\n");
            sb.append("\tvalue: ").append(record.value()).append("\n");
            sb.append("\tpartition: ").append(record.partition()).append("\n");
            sb.append("\toffset: ").append(record.offset()).append("\n");
            sb.append("\theaders: ").append(record.headers()).append("\n");
            System.out.println(sb.toString());
        }

    }


    @Component
    @RequiredArgsConstructor
    static class AnotherConsumer {


        @KafkaListener(groupId = "#{T(java.util.UUID).randomUUID().toString()}",topics = "livestreams",id = "anotherConsumer",autoStartup = "false")
        public void consume(ConsumerRecord<Long,String> record){
            StringBuilder sb = new StringBuilder();
            sb.append("Record consumed at ").append(record.timestamp()).append(" for ").append("anotherConsumer").append("\n");
            sb.append("\tkey: ").append(record.key()).append("\n");
            sb.append("\tvalue: ").append(record.value()).append("\n");
            sb.append("\tpartition: ").append(record.partition()).append("\n");
            sb.append("\toffset: ").append(record.offset()).append("\n");
            sb.append("\theaders: ").append(record.headers()).append("\n");
            System.out.println(sb.toString());
        }

    }


    @Component
    @RequiredArgsConstructor
    static class ConsumerStarter {

        private final KafkaListenerEndpointRegistry registry;
        private final ApplicationEventPublisher publisher;
        private final CountDownLatch counter = new CountDownLatch(10);

        @EventListener(ApplicationStartedEvent.class)
        public void start() throws InterruptedException, ExecutionException {
            for (MessageListenerContainer listenerContainer : registry.getListenerContainers()) {
                listenerContainer.start();
                while (!listenerContainer.isChildRunning()){

                }
            }

            Void unused = CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).get();



            publisher.publishEvent(new ConsumersRegistrationSuccessedEvent());
        }
    }

}
