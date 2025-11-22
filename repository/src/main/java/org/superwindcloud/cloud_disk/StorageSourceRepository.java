package org.superwindcloud.cloud_disk;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageSourceRepository extends JpaRepository<StorageSource, Long> {
  Optional<StorageSource> findByName(String name);
}
