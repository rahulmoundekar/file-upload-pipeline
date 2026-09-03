package com.rahul.virus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class VirusScanService {

    private final ClamAvClient clamAvClient;

    public ScanResult scan(InputStream inputStream) {

        return clamAvClient.scan(inputStream);
    }
}