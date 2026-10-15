package com.example.shortener.repository;

import com.example.shortener.model.ShortUrl;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByCode(String code);

    boolean existsByCode(String code);

    /** Atomic increment in the database, so concurrent clicks are never lost. */
    @Transactional
    @Modifying
    @Query("update ShortUrl s set s.clickCount = s.clickCount + 1 where s.id = :id")
    void incrementClickCount(@Param("id") Long id);

    @Transactional
    long deleteByCode(String code);
}
