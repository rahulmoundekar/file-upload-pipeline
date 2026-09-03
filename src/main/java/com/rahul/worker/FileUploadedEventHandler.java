package com.rahul.worker;

import com.rahul.event.FileUploadedEvent;
import com.rahul.event.FileUploadedEventValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileUploadedEventHandler {

    private final FileUploadedEventValidator validator;

    public void handle(FileUploadedEvent event) {

        validator.validate(event);

        // Step 9:
        // event has been accepted.
        //
        // Actual virus scan starts later.
    }
}