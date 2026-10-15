package com.example.shortener;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.shortener.model.ShortUrl;
import com.example.shortener.repository.ShortUrlRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UrlApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ShortUrlRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private static String body(String url, String alias) {
        String aliasPart = alias == null ? "" : ", \"alias\": \"" + alias + "\"";
        return "{\"url\": \"" + url + "\"" + aliasPart + "}";
    }

    @Test
    void createsShortLinkAndRedirects() throws Exception {
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content(body("https://example.com/some/long/path", "demo")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("demo"))
                .andExpect(jsonPath("$.shortUrl", endsWith("/demo")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        mvc.perform(get("/demo"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/some/long/path"));
    }

    @Test
    void generatesRandomCodeWhenNoAliasGiven() throws Exception {
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content(body("https://example.com", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").isString());
    }

    @Test
    void duplicateAliasReturnsConflict() throws Exception {
        String json = body("https://example.com", "taken");
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsNonHttpUrls() throws Exception {
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content(body("ftp://example.com/file", null)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content(body("javascript:alert(1)", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidAlias() throws Exception {
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content(body("https://example.com", "a b")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.alias", notNullValue()));
    }

    @Test
    void unknownCodeReturnsNotFound() throws Exception {
        mvc.perform(get("/nope123")).andExpect(status().isNotFound());
    }

    @Test
    void expiredLinkReturnsGone() throws Exception {
        repository.save(new ShortUrl("old", "https://example.com", Instant.now().minus(1, ChronoUnit.HOURS)));
        mvc.perform(get("/old")).andExpect(status().isGone());
    }

    @Test
    void clicksAreCounted() throws Exception {
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                .content(body("https://example.com", "stats")));

        mvc.perform(get("/stats"));
        mvc.perform(get("/stats"));
        mvc.perform(get("/stats"));

        mvc.perform(get("/api/urls/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount").value(3));
    }

    @Test
    void deleteRemovesLink() throws Exception {
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                .content(body("https://example.com", "gone")));

        mvc.perform(delete("/api/urls/gone")).andExpect(status().isNoContent());
        mvc.perform(get("/gone")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/urls/gone")).andExpect(status().isNotFound());
    }
}
