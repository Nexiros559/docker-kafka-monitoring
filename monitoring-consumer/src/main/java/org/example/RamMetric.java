package org.example;

public record RamMetric(long total, long used, long available) {
    public RamMetric(long total, long available) {
        this(total, total - available, available);
    }
}
