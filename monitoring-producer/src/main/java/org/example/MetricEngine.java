package org.example;

public sealed interface MetricEngine permits OshiEngine{
    void start();
    void stop();
    void addListener(MonitorListener listener);
}
