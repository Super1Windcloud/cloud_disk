package org.superwindcloud.cloud_disk.controller;

import java.io.InputStream;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.superwindcloud.cloud_disk.FileItem;
import org.superwindcloud.cloud_disk.ShortLink;
import org.superwindcloud.cloud_disk.StorageSource;
import org.superwindcloud.cloud_disk.service.ShortLinkService;
import org.superwindcloud.cloud_disk.storage.StorageService;

@RestController
public class ShortLinkRedirectController {
  private final ShortLinkService shortLinkService;
  private final java.util.List<StorageService> storageServices;

  public ShortLinkRedirectController(
      ShortLinkService shortLinkService, java.util.List<StorageService> storageServices) {
    this.shortLinkService = shortLinkService;
    this.storageServices = storageServices;
  }

  @GetMapping("/s/{token}")
  public ResponseEntity<InputStreamResource> resolve(
      @PathVariable String token, @RequestParam(value = "code", required = false) String accessCode) {
    ShortLink link =
        shortLinkService
            .resolve(token, accessCode)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        accessCode == null ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN,
                        "Link not found, expired, or access code invalid"));
    FileItem file = link.getFileItem();
    if (file.isDirectory()) {
      throw new IllegalArgumentException("Cannot serve directories via short link");
    }
    StorageSource source = file.getStorageSource();
    StorageService storageService =
        storageServices.stream()
            .filter(s -> s.supports(source))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No storage service found"));
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
}
