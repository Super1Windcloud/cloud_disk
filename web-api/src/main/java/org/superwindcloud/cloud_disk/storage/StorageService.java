package org.superwindcloud.cloud_disk.storage;

import java.io.InputStream;
import org.superwindcloud.cloud_disk.FileItem;
import org.superwindcloud.cloud_disk.StorageSource;

public interface StorageService {
  boolean supports(StorageSource source);

  void ensureDirectory(StorageSource source, String directoryPath);

  FileItem store(
      StorageSource source,
      String directoryPath,
      String filename,
      String contentType,
      long size,
      InputStream data);

  InputStream load(StorageSource source, FileItem file);

  void delete(StorageSource source, FileItem file);
}
