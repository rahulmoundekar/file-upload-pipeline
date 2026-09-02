package com.rahul.service;

import com.rahul.exception.InvalidFileException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileValidationServiceTest {

    @Mock
    private FilenameSecurityService filenameSecurityService;

    @Mock
    private FileTypePolicy fileTypePolicy;

    @Mock
    private FileContentDetectionService contentDetectionService;

    @InjectMocks
    private FileValidationService fileValidationService;

    @Test
    void executableExtensionShouldBeRejected() {

        MockMultipartFile file = new MockMultipartFile("file", "malware.exe", "application/octet-stream", "test".getBytes());

        when(filenameSecurityService.validateAndSanitize("malware.exe")).thenReturn("malware.exe");

        when(filenameSecurityService.extension("malware.exe")).thenReturn("exe");

        when(fileTypePolicy.isAllowedExtension("exe")).thenReturn(false);

        assertThatThrownBy(() -> fileValidationService.validate(file)).isInstanceOf(InvalidFileException.class).hasMessage("File type is not allowed");
    }

    @Test
    void executableRenamedToJpgShouldBeRejected() throws Exception {

        MockMultipartFile file = new MockMultipartFile("file", "malware.jpg", "image/jpeg", new byte[]{0x4D, 0x5A, 0x00, 0x00});

        when(filenameSecurityService.validateAndSanitize("malware.jpg")).thenReturn("malware.jpg");

        when(filenameSecurityService.extension("malware.jpg")).thenReturn("jpg");

        when(fileTypePolicy.isAllowedExtension("jpg")).thenReturn(true);

        when(contentDetectionService.detect(any(), eq("malware.jpg"))).thenReturn("application/x-msdownload");

        when(fileTypePolicy.isAllowedMime("jpg", "application/x-msdownload")).thenReturn(false);

        assertThatThrownBy(() -> fileValidationService.validate(file)).isInstanceOf(InvalidFileException.class).hasMessage("File content does not match the allowed file type");
    }

    @Test
    void pathTraversalFilenameShouldBeRejected() {

        MockMultipartFile file = new MockMultipartFile("file", "../../secret.txt", "text/plain", "secret".getBytes());

        when(filenameSecurityService.validateAndSanitize("../../secret.txt")).thenThrow(new InvalidFileException("Path separators are not allowed"));

        assertThatThrownBy(() -> fileValidationService.validate(file)).isInstanceOf(InvalidFileException.class).hasMessage("Path separators are not allowed");
    }
}