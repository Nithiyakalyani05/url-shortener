package com.example.shortener.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateUrlRequest(
        @NotBlank @Size(max = 2048) String url,
        @Pattern(regexp = "^[A-Za-z0-9_-]{3,32}$", message = "alias must be 3-32 characters: letters, digits, '-' or '_'")
        String alias,
        @Future Instant expiresAt) {
}
