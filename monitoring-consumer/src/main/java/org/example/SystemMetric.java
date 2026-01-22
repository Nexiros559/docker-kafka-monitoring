package org.example;

public record SystemMetric(long timestamp, CpuMetric cpu, DiskMetric disk, RamMetric ram) {
}
