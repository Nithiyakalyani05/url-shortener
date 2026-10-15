package com.example.shortener.dto;

import com.example.shortener.model.ShortUrl;
import java.time.Instant;

public record UrlResponse(
        String code,
        String shortUrl,
        String originalUrl,
        Instant createdAt,
        Instant expiresAt,
        long clickCount) {

    public static UrlResponse from(ShortUrl entity, String baseUrl) {
        return new UrlResponse(
                entity.getCode(),
                baseUrl + "/" + entity.getCode(),
                entity.getOriginalUrl(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getClickCount());
    }
}
