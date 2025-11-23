package org.superwindcloud.cloud_disk.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.superwindcloud.cloud_disk.FileItem;
import org.superwindcloud.cloud_disk.FileItemRepository;
import org.superwindcloud.cloud_disk.StorageSource;
import org.superwindcloud.cloud_disk.StorageType;

@Service
public class LocalStorageService implements StorageService {
  private final FileItemRepository fileItemRepository;

  public LocalStorageService(FileItemRepository fileItemRepository) {
    this.fileItemRepository = fileItemRepository;
  }

  @Override
  public boolean supports(StorageSource source) {
    return source.getType() == StorageType.LOCAL;
  }

  @Override
  public void ensureDirectory(StorageSource source, String directoryPath) {
    Path root = Path.of(source.getConfig()).toAbsolutePath().normalize();
    Path targetDir = directoryPath.isBlank() ? root : root.resolve(directoryPath).normalize();
    if (!targetDir.startsWith(root)) {
      throw new IllegalArgumentException("Invalid directory path");
    }
    try {
      Files.createDirectories(targetDir);
    } catch (IOException e) {
      throw new RuntimeException("Failed to create directory", e);
    }
  }

  @Override
  public FileItem store(
      StorageSource source,
      String directoryPath,
      String filename,
      String contentType,
      long size,
      InputStream data) {
    Path root = Path.of(source.getConfig()).toAbsolutePath().normalize();
    Path targetDir =
        (directoryPath == null || directoryPath.isBlank())
            ? root
            : root.resolve(directoryPath).normalize();
    if (!targetDir.startsWith(root)) {
      throw new IllegalArgumentException("Invalid directory path");
    }
    Path targetFile =
        targetDir.resolve(UUID.randomUUID() + "-" + filename); // avoid collisions on same filename
    try {
      Files.createDirectories(targetDir);
      Files.copy(data, targetFile, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new RuntimeException("Failed to store file", e);
    }

    FileItem item = new FileItem();
    item.setFilename(filename);
    item.setStorageSource(source);
    item.setStoragePath(root.relativize(targetFile).toString());
    item.setDirectoryPath(directoryPath == null ? "" : directoryPath);
    item.setSize(size);
    item.setContentType(contentType);
    return fileItemRepository.save(item);
  }

  @Override
  public InputStream load(StorageSource source, FileItem file) {
    Path root = Path.of(source.getConfig()).toAbsolutePath().normalize();
    Path path = root.resolve(file.getStoragePath()).normalize();
    if (!path.startsWith(root)) {
      throw new IllegalArgumentException("Invalid file path");
    }
    try {
      return Files.newInputStream(path);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read file", e);
    }
  }

  @Override
  public void delete(StorageSource source, FileItem file) {
    Path root = Path.of(source.getConfig()).toAbsolutePath().normalize();
    Path path = root.resolve(file.getStoragePath()).normalize();
    if (!path.startsWith(root)) {
      throw new IllegalArgumentException("Invalid file path");
    }
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      throw new RuntimeException("Failed to delete file", e);
    }
  }
}
