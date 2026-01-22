package org.example;

public record DiskMetric(long totalSpace, long usedSpace, long freeSpace) {
    public DiskMetric(long total, long free) {
        this(total, total - free, free);
    }
}
