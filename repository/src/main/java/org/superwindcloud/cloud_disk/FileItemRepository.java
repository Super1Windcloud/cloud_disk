package org.superwindcloud.cloud_disk;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileItemRepository extends JpaRepository<FileItem, Long> {
  List<FileItem> findByStorageSourceIdOrderByCreatedAtDesc(Long storageSourceId);

  List<FileItem> findByStorageSourceIdAndDirectoryPathOrderByDirectoryDescCreatedAtDesc(
      Long storageSourceId, String directoryPath);

  Optional<FileItem> findFirstByStorageSourceIdAndDirectoryPathAndDirectoryTrue(
      Long storageSourceId, String directoryPath);

  Optional<FileItem> findFirstByStorageSourceIdAndDirectoryPathAndFilenameAndDirectoryTrue(
      Long storageSourceId, String directoryPath, String filename);

  boolean existsByStorageSourceIdAndDirectoryPathAndFilename(
      Long storageSourceId, String directoryPath, String filename);

  @Query(
      "select f from FileItem f where f.storageSource.id = :sourceId and "
          + "(f.directoryPath = :path or f.directoryPath like concat(:path, '/%'))")
  List<FileItem> findDescendants(
      @Param("sourceId") Long storageSourceId, @Param("path") String directoryPath);
}
