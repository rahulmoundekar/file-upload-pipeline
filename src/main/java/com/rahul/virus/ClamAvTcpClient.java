package com.rahul.virus;

import com.rahul.config.ClamAvProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class ClamAvTcpClient implements ClamAvClient {

    private static final String INSTREAM_COMMAND = "zINSTREAM\0";

    private static final int MAX_CHUNK_SIZE = 1024 * 1024;

    private final ClamAvProperties properties;

    @Override
    public ScanResult scan(InputStream inputStream) {

        try (Socket socket = new Socket()) {

            socket.connect(new InetSocketAddress(properties.host(), properties.port()), properties.connectionTimeoutMs());

            socket.setSoTimeout(properties.readTimeoutMs());

            try (DataOutputStream output = new DataOutputStream(socket.getOutputStream());

                 InputStream input = new BufferedInputStream(inputStream)) {

                output.write(INSTREAM_COMMAND.getBytes(StandardCharsets.UTF_8));

                byte[] buffer = new byte[Math.min(properties.chunkSizeBytes(), MAX_CHUNK_SIZE)];

                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {

                    output.writeInt(bytesRead);

                    output.write(buffer, 0, bytesRead);
                }

                /*
                 * Zero-length chunk terminates INSTREAM.
                 */
                output.writeInt(0);

                output.flush();

                String response = readResponse(socket);

                return parseResponse(response);
            }

        } catch (IOException e) {

            throw new ClamAvException("Unable to scan file with ClamAV", e);
        }
    }

    private String readResponse(Socket socket) throws IOException {

        InputStream input = socket.getInputStream();

        byte[] buffer = new byte[4096];

        int length = input.read(buffer);

        if (length == -1) {

            throw new IOException("ClamAV closed the connection without a response");
        }

        return new String(buffer, 0, length, StandardCharsets.UTF_8).trim();
    }

    private ScanResult parseResponse(String response) {

        /*
         * Clean response:
         *
         * stream: OK
         *
         * Infected response:
         *
         * stream: Eicar-Test-Signature FOUND
         */

        if (response.endsWith("OK")) {

            return ScanResult.clean();
        }

        String marker = " FOUND";

        int foundIndex = response.lastIndexOf(marker);

        if (foundIndex >= 0) {

            String signature = response.substring(response.indexOf(':') + 1, foundIndex).trim();

            return ScanResult.infected(signature);
        }

        throw new ClamAvException("Unexpected ClamAV response: " + response);
    }
}