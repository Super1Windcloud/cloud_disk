package org.superwindcloud.cloud_disk;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {
    Optional<ShortLink> findByToken(String token);

    @Transactional
    @Modifying
    @Query("delete from ShortLink s where s.expiresAt is not null and s.expiresAt < :now")
    void deleteExpired(@Param("now") Instant now);
}
