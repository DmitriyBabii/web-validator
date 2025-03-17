package com.validator.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlProcessor {

    private final RestTemplate restTemplate;

    public Optional<String> getHtml(String url) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() != HttpStatus.OK || !checkContentType(response)) {
                return Optional.empty();
            }

            String html = response.getBody();
            if (html == null) {
                return Optional.empty();
            }

            return Optional.of(html);

        } catch (HttpStatusCodeException e) {
            log.info("Cant fetch html from {} with status code {}", url, e.getStatusCode());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    private boolean checkContentType(ResponseEntity<?> response) {
        try {
            HttpHeaders headers = response.getHeaders();
            MediaType contentType = headers.getContentType();
            return contentType != null && contentType.toString().contains(MediaType.TEXT_HTML_VALUE);
        } catch (Exception ex) {
            return false;
        }
    }
}
