package org.superwindcloud.cloud_disk;

/**
 * Supported storage backends. For now LOCAL is implemented; others can be added behind the
 * StorageService interface.
 */
public enum StorageType {
    LOCAL,
    S3,
    ONEDRIVE,
    SHAREPOINT,
    GOOGLE_DRIVE,
    FTP,
    SFTP
}
