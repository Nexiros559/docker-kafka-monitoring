package org.example.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfluxConfig {

    @Bean
    public InfluxDBClient influxDBClient() {

        String url = System.getenv().getOrDefault("INFLUX_URL", "http://localhost:8086");
        String token = System.getenv().getOrDefault("INFLUX_TOKEN", "my-super-secret-auth-token");
        String org = System.getenv().getOrDefault("INFLUX_ORG", "monitoring-org");
        String bucket = System.getenv().getOrDefault("INFLUX_BUCKET", "monitoring");


        return InfluxDBClientFactory.create(url, token.toCharArray(), org, bucket);
    }
}