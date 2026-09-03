package com.rahul.virus;

import java.io.InputStream;

public interface ClamAvClient {

    ScanResult scan(
            InputStream inputStream
    );
}