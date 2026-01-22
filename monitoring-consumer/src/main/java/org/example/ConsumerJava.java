package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;


import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.example.SystemMetric;
import org.example.CpuMetric;
import org.example.RamMetric;
import org.example.DiskMetric;



public class ConsumerJava {

    private final KafkaConsumer<String, String> consumer;
    private final ObjectMapper mapper;
    private volatile boolean running;
    private final InfluxDBClient influxDBClient;
    private final WriteApiBlocking writeApi;
    private static final String HOST_TAG = "localhost";


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
        String url = System.getenv().getOrDefault("INFLUX_URL", "http://localhost:8086");
        String token = System.getenv().getOrDefault("INFLUX_TOKEN", "my-super-secret-auth-token");
        String org = System.getenv().getOrDefault("INFLUX_ORG", "monitoring-org");
        String bucket = System.getenv().getOrDefault("INFLUX_BUCKET", "monitoring");


        try {
            this.influxDBClient = InfluxDBClientFactory.create(url, token.toCharArray(), org, bucket);
            this.writeApi = influxDBClient.getWriteApiBlocking();
        } catch (Exception e) {
            System.err.println("Erreur connexion InfluxDB: " + e.getMessage());
            throw new RuntimeException("Impossible d'initialiser InfluxDB", e);
        }


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
                    writeToInfluxDB(metric);

                } catch (JsonProcessingException e) {
                    System.err.println("Erreur désérialisation JSON: " + e.getMessage());
                    System.err.println("Message reçu: " + jsonValue);
                    e.printStackTrace();
                }catch (Exception e) {
                    System.err.println("Erreur écriture InfluxDB: " + e.getMessage());
                    e.printStackTrace();
            }
        }
    }
    }
    public void writeToInfluxDB(SystemMetric metric){
        Instant timestamp = Instant.ofEpochMilli(metric.timestamp());
        Point cpuPoint = Point.measurement("cpu_metrics")
                .addTag("host", HOST_TAG)
                .addField("temperature", metric.cpu().temperature())
                .addField("load_percent", metric.cpu().cpuLoad() * 100)
                .addField("is_overheating", metric.cpu().isOverheating())
                .addField("is_overloaded", metric.cpu().isOverloaded())
                .time(timestamp, WritePrecision.MS);

        // Point RAM
        double ramUsagePercent = (double) metric.ram().used() / metric.ram().total() * 100;
        Point ramPoint = Point.measurement("ram_metrics")
                .addTag("host", HOST_TAG)
                .addField("total_bytes", metric.ram().total())
                .addField("used_bytes", metric.ram().used())
                .addField("available_bytes", metric.ram().available())
                .addField("usage_percent", ramUsagePercent)
                .time(timestamp, WritePrecision.MS);

        // Point Disk
        double diskUsagePercent = (double) metric.disk().usedSpace() / metric.disk().totalSpace() * 100;
        Point diskPoint = Point.measurement("disk_metrics")
                .addTag("host", HOST_TAG)
                .addField("total_bytes", metric.disk().totalSpace())
                .addField("used_bytes", metric.disk().usedSpace())
                .addField("free_bytes", metric.disk().freeSpace())
                .addField("usage_percent", diskUsagePercent)
                .time(timestamp, WritePrecision.MS);

        writeApi.writePoints(Arrays.asList(cpuPoint, ramPoint, diskPoint));
    }

    public void stop() {
        this.running = false;
        if (consumer != null) {
            consumer.close();
        }
        if (influxDBClient != null) {
            influxDBClient.close();
        }

    }
}


