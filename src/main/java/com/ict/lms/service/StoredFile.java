package com.ict.lms.service;

/** Result of saving an upload: where it lives on disk + its original name. */
public record StoredFile(String path, String originalName) {
}
