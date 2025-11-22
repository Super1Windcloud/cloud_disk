package org.superwindcloud.cloud_disk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "file_items",
    indexes = {
      @Index(name = "idx_file_storage_path", columnList = "storage_source_id,storage_path"),
      @Index(name = "idx_file_created_at", columnList = "created_at")
    })
@Data
public class FileItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "storage_source_id", nullable = false)
  private StorageSource storageSource;

  @Column(nullable = false)
  private String filename;

  @Column(name = "storage_path", nullable = false)
  private String storagePath;

  @Column(nullable = false)
  private Long size;

  @Column(name = "content_type")
  private String contentType;

  @Column(name = "directory_path", nullable = false)
  private String directoryPath = "";

  @Column(name = "is_directory", nullable = false, columnDefinition = "boolean default false")
  private boolean directory = false;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
