package com.rahul.virus;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VirusScanServiceTest {

    @Test
    void cleanFileShouldReturnClean() {

        ClamAvClient client = mock(ClamAvClient.class);

        VirusScanService service = new VirusScanService(client);

        ByteArrayInputStream input = new ByteArrayInputStream("hello".getBytes());

        when(client.scan(input)).thenReturn(ScanResult.clean());

        ScanResult result = service.scan(input);

        assertThat(result.status()).isEqualTo(ScanResult.Status.CLEAN);

        assertThat(result.signature()).isNull();
    }

    @Test
    void infectedFileShouldReturnSignature() {

        ClamAvClient client = mock(ClamAvClient.class);

        VirusScanService service = new VirusScanService(client);

        ByteArrayInputStream input = new ByteArrayInputStream("malicious".getBytes());

        when(client.scan(input)).thenReturn(ScanResult.infected("Eicar-Test-Signature"));

        ScanResult result = service.scan(input);

        assertThat(result.status()).isEqualTo(ScanResult.Status.INFECTED);

        assertThat(result.signature()).isEqualTo("Eicar-Test-Signature");
    }
}