package org.example.controller;

import org.example.repository.InfluxRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.example.SystemMetric;

import java.util.List;

@RestController
@RequestMapping("/api/metrics")
@CrossOrigin("*")
public class MetricsController {
    private final InfluxRepository influxRepository;

    public MetricsController(InfluxRepository influxRepository){
        this.influxRepository = influxRepository;
    }

    @GetMapping
    public List<SystemMetric> getAllMetrics(){
        List<SystemMetric> metrics = influxRepository.getMetrics();
        return metrics ;
    }
}
