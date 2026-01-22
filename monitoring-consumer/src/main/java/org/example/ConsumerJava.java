package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class ConsumerJava {


    private final KafkaConsumer<String, String> consumer;
    private final ObjectMapper mapper;
    private volatile boolean running;


    public ConsumerJava() {

        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:29092");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "monitoring-consumer-group");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");


        this.consumer = new KafkaConsumer<>(properties);


        this.consumer.subscribe(Collections.singletonList("system-metrics"));


        this.mapper = new ObjectMapper();


        this.running = true;
    }

    public void start(){
        while(running){
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            for (ConsumerRecord<String, String> record : records) {
                String jsonValue = record.value();
                try{
                    SystemMetric metric = mapper.readValue(jsonValue, SystemMetric.class);
                    double cpuLoadPercent = metric.cpu().cpuLoad() * 100;
                    long ramUsedGo = metric.ram().used() / (1024 * 1024 * 1024);
                    long diskFreeGo = metric.disk().freeSpace() / (1024 * 1024 * 1024);

                    System.out.printf("CPU: %.1f°C (%.1f%%) | RAM: %d Go | Disk: %d Go%n", metric.cpu().temperature(), cpuLoadPercent, ramUsedGo, diskFreeGo);
                } catch (JsonProcessingException e) {
                    System.err.println("Erreur désérialisation JSON: " + e.getMessage());
                    System.err.println("Message reçu: " + jsonValue);
                    e.printStackTrace();
                }
            }
        }
    }

    public void stop() {
        this.running = false;
        if (consumer != null) {
            consumer.close();
        }
    }
}


