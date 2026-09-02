package com.rahul.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;

@Service
public class FileChecksumService {

    public String sha256(MultipartFile file) {

        try (InputStream inputStream = file.getInputStream()) {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] buffer = new byte[8192];

            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {

                digest.update(buffer, 0, bytesRead);
            }

            return toHex(digest.digest());

        } catch (Exception e) {

            throw new IllegalStateException("Unable to calculate file checksum", e);
        }
    }

    private String toHex(byte[] bytes) {

        StringBuilder result = new StringBuilder(bytes.length * 2);

        for (byte value : bytes) {

            result.append(String.format("%02x", value));
        }

        return result.toString();
    }
}