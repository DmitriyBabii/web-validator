package com.validator.services;

import com.validator.models.dto.TidyMessageReport;
import com.validator.models.dto.TidyReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.w3c.tidy.Tidy;
import org.w3c.tidy.TidyMessage;

import java.io.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TidyValidator {

    public TidyReport validate(String html) {
        TidyReport tidyReport = new TidyReport();
        List<TidyMessageReport> messageReports = tidyReport.getMessageReports();

        Tidy tidy = createTidyInstance();
        tidy.setMessageListener(message -> addMessageReport(message, messageReports));

        try (
                InputStream input = new ByteArrayInputStream(html.getBytes());
                OutputStream output = new ByteArrayOutputStream()
        ) {
            tidy.parse(input, output);
        } catch (IOException e) {
            throw new RuntimeException("Error processing HTML with Tidy", e);
        }

        return tidyReport;
    }

    private void addMessageReport(TidyMessage message, List<TidyMessageReport> messageReports) {
        if (message.getMessage().contains("unknown attribute")) {
            return;
        }
        messageReports.add(
                TidyMessageReport.builder()
                        .level(message.getLevel().toString())
                        .message(message.getMessage())
                        .line(message.getLine())
                        .column(message.getColumn())
                        .build()
        );
    }

    private Tidy createTidyInstance() {
        Tidy tidy = new Tidy();
        tidy.setShowWarnings(true);
        tidy.setShowErrors(10);
        tidy.setQuiet(true);
        return tidy;
    }
}
