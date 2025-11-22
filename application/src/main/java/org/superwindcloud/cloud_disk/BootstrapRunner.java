package org.superwindcloud.cloud_disk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapRunner implements ApplicationRunner {
  private final StorageSourceRepository storageSourceRepository;
  private final ObjectMapper objectMapper;

  @Value("${storage.local.base-path:./data/storage}")
  private String localBasePath;

  @Value("${storage.s3.bootstrap.enabled:false}")
  private boolean s3BootstrapEnabled;

  @Value("${storage.s3.bootstrap.name:minio-default}")
  private String s3BootstrapName;

  @Value("${storage.s3.bootstrap.endpoint:http://localhost:9000}")
  private String s3Endpoint;

  @Value("${storage.s3.bootstrap.bucket:cloud-disk}")
  private String s3Bucket;

  @Value("${storage.s3.bootstrap.access-key:minioadmin}")
  private String s3AccessKey;

  @Value("${storage.s3.bootstrap.secret-key:minioadmin}")
  private String s3SecretKey;

  @Value("${storage.s3.bootstrap.region:}")
  private String s3Region;

  @Value("${storage.s3.bootstrap.base-path:}")
  private String s3BasePath;

  public BootstrapRunner(
      StorageSourceRepository storageSourceRepository, ObjectMapper objectMapper) {
    this.storageSourceRepository = storageSourceRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void run(@NotNull ApplicationArguments args) throws Exception {
    Files.createDirectories(Path.of(localBasePath));
    storageSourceRepository
        .findByName("local-default")
        .orElseGet(
            () -> {
              StorageSource source = new StorageSource();
              source.setName("local-default");
              source.setType(StorageType.LOCAL);
              source.setConfig(Path.of(localBasePath).toAbsolutePath().toString());
              return storageSourceRepository.save(source);
            });
    if (s3BootstrapEnabled) {
      storageSourceRepository
          .findByName(s3BootstrapName)
          .orElseGet(
              () -> {
                StorageSource source = new StorageSource();
                source.setName(s3BootstrapName);
                source.setType(StorageType.S3);
                source.setConfig(buildS3ConfigJson());
                return storageSourceRepository.save(source);
              });
    }
  }

  private String buildS3ConfigJson() {
    try {
      return objectMapper.writeValueAsString(
          Map.of(
              "endpoint", s3Endpoint,
              "bucket", s3Bucket,
              "accessKey", s3AccessKey,
              "secretKey", s3SecretKey,
              "region", s3Region,
              "basePath", s3BasePath));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize S3 bootstrap config", e);
    }
  }
}
