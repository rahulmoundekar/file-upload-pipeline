package com.rahul.integration;

import com.rahul.config.ClamAvProperties;
import com.rahul.virus.ClamAvClient;
import com.rahul.virus.ClamAvException;
import com.rahul.virus.ClamAvTcpClient;
import com.rahul.virus.ScanResult;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class ClamAvIntegrationTest {

    @Container
    static GenericContainer<?> clamav =
            new GenericContainer<>("clamav/clamav:stable")
                    .withExposedPorts(3310)
                    .waitingFor(
                            Wait.forListeningPort()
                                    .withStartupTimeout(
                                            Duration.ofMinutes(2)
                                    )
                    );

    @Test
    void clamAvContainerShouldBeRunning() {

        assertThat(clamav.isRunning()).isTrue();

        assertThat(clamav.getMappedPort(3310)).isPositive();
    }

    @Test
    void cleanContentShouldBeAccepted() throws Exception {

        ClamAvProperties properties = new ClamAvProperties(clamav.getHost(), clamav.getMappedPort(3310), 5000, 60000, 8192);

        ClamAvClient client = new ClamAvTcpClient(properties);

        waitUntilClamAvIsReady(client);

        byte[] content = "hello from file upload pipeline".getBytes();

        ScanResult result = client.scan(new ByteArrayInputStream(content));

        assertThat(result.status()).isEqualTo(ScanResult.Status.CLEAN);
    }


    @Test
    void eicarContentShouldBeDetected() throws Exception {

        ClamAvProperties properties = new ClamAvProperties(clamav.getHost(), clamav.getMappedPort(3310), 5000, 60000, 8192);

        ClamAvClient client = new ClamAvTcpClient(properties);

        waitUntilClamAvIsReady(client);

        String eicar = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$" + "EICAR-STANDARD-ANTIVIRUS-TEST-FILE!" + "$H+H*";

        ScanResult result = client.scan(new ByteArrayInputStream(eicar.getBytes()));

        assertThat(result.status()).isEqualTo(ScanResult.Status.INFECTED);

        assertThat(result.signature()).contains("Eicar");
    }



    private static void waitUntilClamAvIsReady(ClamAvClient client) throws Exception {

        long deadline = System.currentTimeMillis() + Duration.ofMinutes(2).toMillis();

        while (System.currentTimeMillis() < deadline) {

            try {

                ScanResult result = client.scan(new ByteArrayInputStream("clamav-readiness-check".getBytes()));

                if (result.status() == ScanResult.Status.CLEAN) {
                    return;
                }

            } catch (ClamAvException ignored) {
                // ClamAV is still initializing.
            }

            Thread.sleep(1000);
        }

        throw new IllegalStateException("ClamAV did not become ready within 2 minutes");
    }
}