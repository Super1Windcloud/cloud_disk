package org.superwindcloud.cloud_disk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "storage_sources")
@Data
public class StorageSource {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StorageType type = StorageType.LOCAL;

  /**
   * For LOCAL this is a folder path. For other backends this can hold serialized JSON config or a
   * connection string; keep flexible for future providers.
   */
  @Column(nullable = false, length = 2000)
  private String config;
}
