package org.superwindcloud.cloud_disk.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.superwindcloud.cloud_disk.StorageSource;
import org.superwindcloud.cloud_disk.StorageSourceRepository;

@RestController
@RequestMapping("/api/storage-sources")
public class StorageSourceController {
    private final StorageSourceRepository storageSourceRepository;

    public StorageSourceController(StorageSourceRepository storageSourceRepository) {
        this.storageSourceRepository = storageSourceRepository;
    }

    @GetMapping
    public List<StorageSource> list() {
        return storageSourceRepository.findAll();
    }
}
