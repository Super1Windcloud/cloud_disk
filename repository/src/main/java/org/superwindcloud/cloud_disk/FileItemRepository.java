package org.superwindcloud.cloud_disk;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileItemRepository extends JpaRepository<FileItem, Long> {
    List<FileItem> findByStorageSourceIdOrderByCreatedAtDesc(Long storageSourceId);

    List<FileItem> findByStorageSourceIdAndDirectoryPathOrderByDirectoryDescCreatedAtDesc(
            Long storageSourceId, String directoryPath);

    Optional<FileItem> findFirstByStorageSourceIdAndDirectoryPathAndDirectoryTrue(
            Long storageSourceId, String directoryPath);

    Optional<FileItem> findFirstByStorageSourceIdAndDirectoryPathAndFilenameAndDirectoryTrue(
            Long storageSourceId, String directoryPath, String filename);
}
