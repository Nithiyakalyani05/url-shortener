package com.example.shortener.service;

import com.example.shortener.exception.ApiExceptions.AliasTakenException;
import com.example.shortener.exception.ApiExceptions.ExpiredException;
import com.example.shortener.exception.ApiExceptions.InvalidUrlException;
import com.example.shortener.exception.ApiExceptions.NotFoundException;
import com.example.shortener.model.ShortUrl;
import com.example.shortener.repository.ShortUrlRepository;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class UrlService {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CODE_LENGTH = 7;
    private static final int MAX_ATTEMPTS = 5;
    private static final Set<String> RESERVED = Set.of("api", "actuator", "error");

    private final ShortUrlRepository repository;
    private final SecureRandom random = new SecureRandom();

    public UrlService(ShortUrlRepository repository) {
        this.repository = repository;
    }

    /**
     * Not @Transactional on purpose: each save runs in its own transaction, so a
     * unique-constraint collision can be caught and retried cleanly.
     */
    public ShortUrl create(String originalUrl, String alias, Instant expiresAt) {
        validateUrl(originalUrl);

        if (alias != null && !alias.isBlank()) {
            if (RESERVED.contains(alias.toLowerCase()) || repository.existsByCode(alias)) {
                throw new AliasTakenException(alias);
            }
            try {
                return repository.saveAndFlush(new ShortUrl(alias, originalUrl, expiresAt));
            } catch (DataIntegrityViolationException race) {
                throw new AliasTakenException(alias); // lost a race with another request
            }
        }

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = randomCode();
            if (RESERVED.contains(code.toLowerCase()) || repository.existsByCode(code)) {
                continue;
            }
            try {
                return repository.saveAndFlush(new ShortUrl(code, originalUrl, expiresAt));
            } catch (DataIntegrityViolationException collision) {
                // extremely unlikely; try a new code
            }
        }
        throw new IllegalStateException("Could not generate a unique short code");
    }

    /** Resolves a code to its target URL and records the click. */
    public String resolveAndTrack(String code) {
        ShortUrl url = repository.findByCode(code).orElseThrow(() -> new NotFoundException(code));
        if (url.isExpired()) {
            throw new ExpiredException(code);
        }
        repository.incrementClickCount(url.getId());
        return url.getOriginalUrl();
    }

    public ShortUrl getStats(String code) {
        return repository.findByCode(code).orElseThrow(() -> new NotFoundException(code));
    }

    public void delete(String code) {
        if (repository.deleteByCode(code) == 0) {
            throw new NotFoundException(code);
        }
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    static void validateUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new InvalidUrlException("URL must start with http:// or https://");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new InvalidUrlException("URL must contain a host");
            }
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("URL is not well formed");
        }
    }
}
