package org.example.crop;

public record Range(double min, double max) {
    public Range {
        if (min > max) {
            throw new IllegalArgumentException("Invalid range: min=" + min + ", max=" + max);
        }
    }
}
