package org.superwindcloud.cloud_disk.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.superwindcloud.cloud_disk.FileItem;
import org.superwindcloud.cloud_disk.ShortLink;
import org.superwindcloud.cloud_disk.ShortLinkRepository;

@Service
public class ShortLinkService {
  private final ShortLinkRepository shortLinkRepository;

  public ShortLinkService(ShortLinkRepository shortLinkRepository) {
    this.shortLinkRepository = shortLinkRepository;
  }

  public ShortLink create(FileItem fileItem, Duration ttl) {
    ShortLink link = new ShortLink();
    link.setFileItem(fileItem);
    link.setToken(UUID.randomUUID().toString().replace("-", "").substring(0, 10));
    if (ttl != null) {
      link.setExpiresAt(Instant.now().plus(ttl));
    }
    return shortLinkRepository.save(link);
  }

  public Optional<ShortLink> resolve(String token) {
    return shortLinkRepository
        .findByToken(token)
        .filter(link -> link.getExpiresAt() == null || link.getExpiresAt().isAfter(Instant.now()));
  }

  public void cleanupExpired() {
    shortLinkRepository.deleteExpired(Instant.now());
  }
}
