package org.superwindcloud.cloud_disk.controller;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.superwindcloud.cloud_disk.FileItem;
import org.superwindcloud.cloud_disk.FileItemRepository;
import org.superwindcloud.cloud_disk.ShortLink;
import org.superwindcloud.cloud_disk.ShortLinkRepository;
import org.superwindcloud.cloud_disk.StorageSource;
import org.superwindcloud.cloud_disk.StorageSourceRepository;
import org.superwindcloud.cloud_disk.service.ShortLinkService;
import org.superwindcloud.cloud_disk.storage.LinkableStorageService;
import org.superwindcloud.cloud_disk.storage.StorageService;

@RestController
@RequestMapping("/api/files")
public class FileController {
  private final List<StorageService> storageServices;
  private final StorageSourceRepository storageSourceRepository;
  private final FileItemRepository fileItemRepository;
  private final ShortLinkService shortLinkService;
  private final ShortLinkRepository shortLinkRepository;

  public FileController(
      List<StorageService> storageServices,
      StorageSourceRepository storageSourceRepository,
      FileItemRepository fileItemRepository,
      ShortLinkService shortLinkService,
      ShortLinkRepository shortLinkRepository) {
    this.storageServices = storageServices;
    this.storageSourceRepository = storageSourceRepository;
    this.fileItemRepository = fileItemRepository;
    this.shortLinkService = shortLinkService;
    this.shortLinkRepository = shortLinkRepository;
  }

  @PostMapping("/upload")
  public FileItem upload(
      @RequestParam Long sourceId,
      @RequestParam(value = "path", required = false, defaultValue = "") String path,
      @RequestParam("file") MultipartFile multipartFile)
      throws Exception {
    String directoryPath = normalizeDirectory(path);
    StorageSource source =
        storageSourceRepository
            .findById(sourceId)
            .orElseThrow(() -> new IllegalArgumentException("Storage source not found"));
    StorageService storageService = resolveStorage(source);
    storageService.ensureDirectory(source, directoryPath);
    FileItem item =
        storageService.store(
            source,
            directoryPath,
            multipartFile.getOriginalFilename(),
            multipartFile.getContentType(),
            multipartFile.getSize(),
            multipartFile.getInputStream());
    return item;
  }

  @GetMapping
  public List<FileItem> list(@RequestParam(required = false) Long sourceId) {
    if (sourceId != null) {
      return fileItemRepository.findByStorageSourceIdOrderByCreatedAtDesc(sourceId);
    }
    return fileItemRepository.findAll();
  }

  @GetMapping("/browse")
  public List<FileItem> browse(
      @RequestParam Long sourceId, @RequestParam(required = false, defaultValue = "") String path) {
    String directoryPath = normalizeDirectory(path);
    return fileItemRepository
        .findByStorageSourceIdAndDirectoryPathOrderByDirectoryDescCreatedAtDesc(
            sourceId, directoryPath);
  }

  @PostMapping("/directories")
  public FileItem createDirectory(
      @RequestParam Long sourceId, @RequestParam(value = "path", required = true) String path) {
    String normalized = normalizeDirectory(path);
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("Directory path cannot be blank");
    }
    String parent = parentPath(normalized);
    String name = leaf(normalized);

    StorageSource source =
        storageSourceRepository
            .findById(sourceId)
            .orElseThrow(() -> new IllegalArgumentException("Storage source not found"));
    StorageService storageService = resolveStorage(source);
    storageService.ensureDirectory(source, normalized);

    return fileItemRepository
        .findFirstByStorageSourceIdAndDirectoryPathAndFilenameAndDirectoryTrue(
            sourceId, parent, name)
        .orElseGet(
            () -> {
              FileItem dir = new FileItem();
              dir.setDirectory(true);
              dir.setDirectoryPath(parent);
              dir.setFilename(name);
              dir.setStorageSource(source);
              dir.setStoragePath(normalized + "/");
              dir.setSize(0L);
              dir.setContentType("inode/directory");
              return fileItemRepository.save(dir);
            });
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<InputStreamResource> download(@PathVariable Long id) {
    FileItem file =
        fileItemRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("File not found"));
    if (file.isDirectory()) {
      throw new IllegalArgumentException("Cannot download a directory");
    }
    StorageSource source = file.getStorageSource();
    StorageService storageService = resolveStorage(source);
    InputStream stream = storageService.load(source, file);
    String contentType =
        StringUtils.hasText(file.getContentType())
            ? file.getContentType()
            : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
        .contentType(MediaType.parseMediaType(contentType))
        .body(new InputStreamResource(stream));
  }

  @GetMapping("/{id}/preview")
  public ResponseEntity<InputStreamResource> preview(@PathVariable Long id) {
    FileItem file =
        fileItemRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("File not found"));
    if (file.isDirectory()) {
      throw new IllegalArgumentException("Cannot preview a directory");
    }
    StorageSource source = file.getStorageSource();
    StorageService storageService = resolveStorage(source);
    InputStream stream = storageService.load(source, file);
    String contentType =
        StringUtils.hasText(file.getContentType())
            ? file.getContentType()
            : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
        .contentType(MediaType.parseMediaType(contentType))
        .body(new InputStreamResource(stream));
  }

  @PostMapping("/{id}/short-link")
  public Map<String, String> createShortLink(
      @PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
    FileItem file =
        fileItemRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("File not found"));
    if (file.isDirectory()) {
      throw new IllegalArgumentException("Cannot create links for a directory");
    }
    Duration ttl = null;
    if (body != null && body.containsKey("ttlSeconds")) {
      Object ttlSeconds = body.get("ttlSeconds");
      if (ttlSeconds instanceof Number number && number.longValue() > 0) {
        ttl = Duration.ofSeconds(number.longValue());
      }
    }
    ShortLink link = shortLinkService.create(file, ttl);
    return Map.of("token", link.getToken(), "url", "/s/" + link.getToken());
  }

  @GetMapping("/short-links")
  public List<ShortLink> shortLinks() {
    return shortLinkRepository.findAll();
  }

  @PostMapping("/{id}/direct-link")
  public Map<String, String> createDirectLink(
      @PathVariable Long id, @RequestBody(required = false) Map<String, Object> body)
      throws Exception {
    FileItem file =
        fileItemRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("File not found"));
    if (file.isDirectory()) {
      throw new IllegalArgumentException("Cannot create links for a directory");
    }
    Duration ttl = null;
    if (body != null && body.containsKey("ttlSeconds")) {
      Object ttlSeconds = body.get("ttlSeconds");
      if (ttlSeconds instanceof Number number && number.longValue() > 0) {
        ttl = Duration.ofSeconds(number.longValue());
      }
    }
    StorageSource source = file.getStorageSource();
    StorageService storageService = resolveStorage(source);
    if (storageService instanceof LinkableStorageService linkable) {
      Optional<String> url = linkable.generateDirectDownloadUrl(source, file, ttl);
      if (url.isPresent()) {
        return Map.of("url", url.get());
      }
    }
    if (ttl != null) {
      ShortLink shortLink = shortLinkService.create(file, ttl);
      return Map.of("url", "/s/" + shortLink.getToken());
    }
    return Map.of("url", "/api/files/" + id + "/download");
  }

  private StorageService resolveStorage(StorageSource source) {
    return storageServices.stream()
        .filter(s -> s.supports(source))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No storage service found"));
  }

  private String normalizeDirectory(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    String normalized = raw.trim().replace("\\", "/").replaceAll("/+", "/");
    if (normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }
    if (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    if (normalized.equals("..") || normalized.contains("../")) {
      throw new IllegalArgumentException("Invalid directory path");
    }
    String[] parts = normalized.split("/");
    for (String part : parts) {
      if (part.isEmpty()
          || part.endsWith(" ")
          || part.endsWith(".")
          || part.matches(".*[<>:\"|?*].*")) {
        throw new IllegalArgumentException("Invalid directory path");
      }
    }
    return normalized;
  }

  private String parentPath(String normalized) {
    int idx = normalized.lastIndexOf('/');
    return idx < 0 ? "" : normalized.substring(0, idx);
  }

  private String leaf(String normalized) {
    int idx = normalized.lastIndexOf('/');
    return idx < 0 ? normalized : normalized.substring(idx + 1);
  }
}
