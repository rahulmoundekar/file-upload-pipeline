package com.rahul.virus;

public record ScanResult(Status status, String signature) {

    public enum Status {
        CLEAN, INFECTED
    }

    public static ScanResult clean() {
        return new ScanResult(Status.CLEAN, null);
    }

    public static ScanResult infected(String signature) {
        return new ScanResult(Status.INFECTED, signature);
    }
}