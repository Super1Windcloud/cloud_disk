package org.superwindcloud.cloud_disk.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.superwindcloud.cloud_disk.FileItem;
import org.superwindcloud.cloud_disk.FileItemRepository;
import org.superwindcloud.cloud_disk.StorageSource;
import org.superwindcloud.cloud_disk.StorageType;

@Service
public class S3StorageService implements LinkableStorageService {
    private final FileItemRepository fileItemRepository;
    private final ObjectMapper objectMapper;
    private final Map<Long, MinioClient> clients = new ConcurrentHashMap<>();

    public S3StorageService(FileItemRepository fileItemRepository, ObjectMapper objectMapper) {
        this.fileItemRepository = fileItemRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(StorageSource source) {
        return source.getType() == StorageType.S3;
    }

    @Override
    public void ensureDirectory(StorageSource source, String directoryPath) {
        S3Config config = parseConfig(source);
        if (directoryPath == null || directoryPath.isBlank()) {
            ensureBucket(config, source.getId());
            return;
        }
        String key = buildKey(config, normalizePath(directoryPath) + "/");
        try {
            ensureBucket(config, source.getId());
            getClient(config, source.getId())
                    .putObject(
                            PutObjectArgs.builder()
                                    .bucket(config.bucket())
                                    .object(key)
                                    .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                                    .contentType("application/x-directory")
                                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create directory in S3", e);
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
        S3Config config = parseConfig(source);
        String normalizedDir = normalizePath(directoryPath);
        String key = buildKey(config, normalizedDir, filename);
        try {
            ensureBucket(config, source.getId());
            getClient(config, source.getId())
                    .putObject(
                            PutObjectArgs.builder()
                                    .bucket(config.bucket())
                                    .object(key)
                                    .contentType(contentType)
                                    .stream(data, size, -1)
                                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload object to S3", e);
        }

        FileItem item = new FileItem();
        item.setFilename(filename);
        item.setStorageSource(source);
        item.setStoragePath(key);
        item.setDirectoryPath(normalizedDir);
        item.setSize(size);
        item.setContentType(contentType);
        return fileItemRepository.save(item);
    }

    @Override
    public InputStream load(StorageSource source, FileItem file) {
        S3Config config = parseConfig(source);
        try {
            return getClient(config, source.getId())
                    .getObject(
                            GetObjectArgs.builder()
                                    .bucket(config.bucket())
                                    .object(file.getStoragePath())
                                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch object from S3", e);
        }
    }

    @Override
    public Optional<String> generateDirectDownloadUrl(StorageSource source, FileItem file, Duration ttl)
            throws Exception {
        S3Config config = parseConfig(source);
        int expirySeconds = (int) (ttl != null ? ttl.getSeconds() : Duration.ofHours(1).getSeconds());
        expirySeconds = Math.min(Math.max(expirySeconds, 60), 7 * 24 * 3600); // S3 presign bounds
        String url =
                getClient(config, source.getId())
                        .getPresignedObjectUrl(
                                GetPresignedObjectUrlArgs.builder()
                                        .method(Method.GET)
                                        .bucket(config.bucket())
                                        .object(file.getStoragePath())
                                        .expiry(expirySeconds)
                                        .build());
        return Optional.ofNullable(url);
    }

    private MinioClient getClient(S3Config config, Long sourceId) {
        return clients.computeIfAbsent(
                sourceId,
                id -> {
                    MinioClient.Builder builder =
                            MinioClient.builder()
                                    .endpoint(config.endpoint())
                                    .credentials(config.accessKey(), config.secretKey());
                    if (config.region() != null && !config.region().isBlank()) {
                        builder.region(config.region());
                    }
                    return builder.build();
                });
    }

    private void ensureBucket(S3Config config, Long sourceId) {
        try {
            MinioClient client = getClient(config, sourceId);
            boolean exists =
                    client.bucketExists(
                            io.minio.BucketExistsArgs.builder().bucket(config.bucket()).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(config.bucket()).build());
            }
        } catch (ErrorResponseException e) {
            // If access is denied or bucket is owned by you, skip creation attempt
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure bucket exists", e);
        }
    }

    private S3Config parseConfig(StorageSource source) {
        try {
            S3Config config = objectMapper.readValue(source.getConfig(), S3Config.class);
            if (config.bucket() == null
                    || config.endpoint() == null
                    || config.accessKey() == null
                    || config.secretKey() == null) {
                throw new IllegalArgumentException("Missing required S3 config fields");
            }
            return config;
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid S3 config JSON", e);
        }
    }

    private String buildKey(S3Config config, String normalizedDir, String filename) {
        String base = config.basePath() == null ? "" : normalizePath(config.basePath());
        String combined = joinPaths(base, normalizedDir);
        String key = joinPaths(combined, filename);
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        return key;
    }

    private String buildKey(S3Config config, String key) {
        String base = config.basePath() == null ? "" : normalizePath(config.basePath());
        String combined = joinPaths(base, key);
        if (combined.startsWith("/")) {
            combined = combined.substring(1);
        }
        return combined;
    }

    private String joinPaths(String left, String right) {
        if (left == null || left.isBlank()) {
            return normalizePath(right);
        }
        if (right == null || right.isBlank()) {
            return normalizePath(left);
        }
        return normalizePath(left) + "/" + normalizePath(right);
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        String cleaned = path.replace("\\", "/").replaceAll("/+", "/");
        if (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        if (cleaned.equals("..") || cleaned.contains("../")) {
            throw new IllegalArgumentException("Invalid path");
        }
        return cleaned;
    }

    private record S3Config(
            String endpoint,
            String bucket,
            String accessKey,
            String secretKey,
            String region,
            String basePath) {}
}
