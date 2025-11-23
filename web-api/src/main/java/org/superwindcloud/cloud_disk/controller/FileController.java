package org.superwindcloud.cloud_disk.controller;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    ensureDirectoryChain(source, directoryPath);
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

    return ensureDirectoryChain(source, normalized);
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
    ContentDisposition disposition =
        ContentDisposition.attachment()
            .filename(file.getFilename(), StandardCharsets.UTF_8)
            .build();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
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
    ContentDisposition disposition =
        ContentDisposition.inline()
            .filename(file.getFilename(), StandardCharsets.UTF_8)
            .build();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
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
    String accessCode = null;
    if (body != null && body.containsKey("ttlSeconds")) {
      Object ttlSeconds = body.get("ttlSeconds");
      if (ttlSeconds instanceof Number number && number.longValue() > 0) {
        ttl = Duration.ofSeconds(number.longValue());
      }
    }
    if (body != null && body.containsKey("accessCode")) {
      Object code = body.get("accessCode");
      if (code != null && StringUtils.hasText(code.toString())) {
        accessCode = code.toString().trim();
      }
    }
    ShortLink link = shortLinkService.create(file, ttl, accessCode);
    Map<String, String> response = new HashMap<>();
    response.put("token", link.getToken());
    response.put("url", "/s/" + link.getToken());
    if (link.getAccessCode() != null) {
      response.put("accessCode", link.getAccessCode());
    }
    return response;
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
      ShortLink shortLink = shortLinkService.create(file, ttl, null);
      return Map.of("url", "/s/" + shortLink.getToken());
    }
    return Map.of("url", "/api/files/" + id + "/download");
  }

  @PostMapping("/{id}/rename")
  @Transactional
  public FileItem rename(
      @PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
    if (body == null || !body.containsKey("filename")) {
      throw new IllegalArgumentException("New filename is required");
    }
    String newName = normalizeFilename(body.get("filename"));
    FileItem item =
        fileItemRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("File not found"));
    if (newName.equals(item.getFilename())) {
      return item;
    }
    StorageSource source = item.getStorageSource();
    if (fileItemRepository.existsByStorageSourceIdAndDirectoryPathAndFilename(
        source.getId(), item.getDirectoryPath(), newName)) {
      throw new IllegalArgumentException("A file or folder with this name already exists here");
    }
    if (item.isDirectory()) {
      String oldFullPath = fullPath(item);
      String newFullPath =
          item.getDirectoryPath().isBlank()
              ? newName
              : item.getDirectoryPath() + "/" + newName;
      List<FileItem> descendants = fileItemRepository.findDescendants(source.getId(), oldFullPath);
      for (FileItem descendant : descendants) {
        String path = descendant.getDirectoryPath();
        String updatedPath =
            path.equals(oldFullPath)
                ? newFullPath
                : newFullPath + path.substring(oldFullPath.length());
        descendant.setDirectoryPath(updatedPath);
      }
      fileItemRepository.saveAll(descendants);
      item.setStoragePath(newFullPath + "/");
    }
    item.setFilename(newName);
    return fileItemRepository.save(item);
  }

  @DeleteMapping("/{id}")
  @Transactional
  public Map<String, String> delete(@PathVariable Long id) {
    FileItem item =
        fileItemRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("File not found"));
    StorageSource source = item.getStorageSource();
    StorageService storageService = resolveStorage(source);
    if (item.isDirectory()) {
      String fullPath = fullPath(item);
      List<FileItem> descendants = fileItemRepository.findDescendants(source.getId(), fullPath);
      for (FileItem child : descendants) {
        shortLinkRepository.deleteByFileItemId(child.getId());
        if (!child.isDirectory()) {
          storageService.delete(source, child);
        }
      }
      fileItemRepository.deleteAll(descendants);
    }
    shortLinkRepository.deleteByFileItemId(item.getId());
    if (!item.isDirectory()) {
      storageService.delete(source, item);
    }
    fileItemRepository.delete(item);
    return Map.of("status", "deleted");
  }

  private StorageService resolveStorage(StorageSource source) {
    return storageServices.stream()
        .filter(s -> s.supports(source))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No storage service found"));
  }

  private FileItem ensureDirectoryChain(StorageSource source, String normalizedPath) {
    if (normalizedPath == null || normalizedPath.isBlank()) {
      return null;
    }
    String parent = "";
    FileItem last = null;
    for (String part : normalizedPath.split("/")) {
      if (part.isBlank()) {
        continue;
      }
      String currentPath = parent.isBlank() ? part : parent + "/" + part;
      String finalParent = parent;
      last =
          fileItemRepository
              .findFirstByStorageSourceIdAndDirectoryPathAndFilenameAndDirectoryTrue(
                  source.getId(), parent, part)
              .orElseGet(
                  () -> {
                    FileItem dir = new FileItem();
                    dir.setDirectory(true);
                    dir.setDirectoryPath(finalParent);
                    dir.setFilename(part);
                    dir.setStorageSource(source);
                    dir.setStoragePath(currentPath + "/");
                    dir.setSize(0L);
                    dir.setContentType("inode/directory");
                    return fileItemRepository.save(dir);
                  });
      parent = currentPath;
    }
    return last;
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

  private String fullPath(FileItem file) {
    return file.getDirectoryPath().isBlank()
        ? file.getFilename()
        : file.getDirectoryPath() + "/" + file.getFilename();
  }

  private String normalizeFilename(String raw) {
    if (!StringUtils.hasText(raw)) {
      throw new IllegalArgumentException("Filename cannot be blank");
    }
    String cleaned = raw.trim();
    if (cleaned.equals(".") || cleaned.equals("..")) {
      throw new IllegalArgumentException("Invalid filename");
    }
    if (cleaned.contains("/") || cleaned.contains("\\")) {
      throw new IllegalArgumentException("Filename cannot contain path separators");
    }
    if (cleaned.endsWith(" ") || cleaned.endsWith(".")) {
      throw new IllegalArgumentException("Filename cannot end with space or dot");
    }
    if (cleaned.matches(".*[<>:\"|?*].*")) {
      throw new IllegalArgumentException("Filename contains invalid characters");
    }
    return cleaned;
  }
}
