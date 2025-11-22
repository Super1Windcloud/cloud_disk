package org.superwindcloud.cloud_disk.storage;

import java.time.Duration;
import java.util.Optional;
import org.superwindcloud.cloud_disk.FileItem;
import org.superwindcloud.cloud_disk.StorageSource;

public interface LinkableStorageService extends StorageService {
  Optional<String> generateDirectDownloadUrl(StorageSource source, FileItem file, Duration ttl)
      throws Exception;
}
