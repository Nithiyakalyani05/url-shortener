package com.example.shortener.web;

import com.example.shortener.dto.CreateUrlRequest;
import com.example.shortener.dto.UrlResponse;
import com.example.shortener.model.ShortUrl;
import com.example.shortener.service.UrlService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UrlController {

    private final UrlService service;
    private final String baseUrl;

    public UrlController(UrlService service, @Value("${app.base-url}") String baseUrl) {
        this.service = service;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @PostMapping("/api/urls")
    public ResponseEntity<UrlResponse> create(@Valid @RequestBody CreateUrlRequest request) {
        ShortUrl created = service.create(request.url(), request.alias(), request.expiresAt());
        return ResponseEntity
                .created(URI.create("/api/urls/" + created.getCode()))
                .body(UrlResponse.from(created, baseUrl));
    }

    @GetMapping("/api/urls/{code}")
    public UrlResponse stats(@PathVariable String code) {
        return UrlResponse.from(service.getStats(code), baseUrl);
    }

    @DeleteMapping("/api/urls/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        service.delete(code);
        return ResponseEntity.noContent().build();
    }

    /** The public short link: redirects to the original URL and counts the click. */
    @GetMapping("/{code:[A-Za-z0-9_-]{3,32}}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        String target = service.resolveAndTrack(code);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, target)
                .header(HttpHeaders.CACHE_CONTROL, "no-store") // keep clicks countable
                .build();
    }
}
