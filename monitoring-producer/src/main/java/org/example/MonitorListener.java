package org.example;
import  org.example.SystemMetric;

public interface MonitorListener {
    void onMeasureReceived (SystemMetric metric);
}
