package com.ict.lms.service;

import java.io.IOException;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ict.lms.model.FileBlob;
import com.ict.lms.repo.FileBlobRepository;

/**
 * Stores uploaded files in the database (see {@link FileBlob}) and reads them
 * back for download.
 *
 * It used to write to a local "uploads/" folder, but Vercel container instances
 * have an ephemeral, non-shared filesystem, so those files disappeared and
 * downloads 404'd. The public interface is unchanged — the opaque "path" that
 * callers persist (Resource.filePath, Submission.filePath, Assignment
 * questionFilePath) is now simply the FileBlob id as a string.
 */
@Service
public class FileStorageService {

    private final FileBlobRepository blobs;

    public FileStorageService(FileBlobRepository blobs) {
        this.blobs = blobs;
    }

    /** Store a file's bytes; returns a reference (the blob id) + original name. */
    public StoredFile store(MultipartFile file, String subdir) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is missing or empty");
        }
        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        try {
            FileBlob blob = new FileBlob();
            blob.setData(file.getBytes());
            blob.setContentType(file.getContentType());
            FileBlob saved = blobs.save(blob);
            return new StoredFile(String.valueOf(saved.getId()), original);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file");
        }
    }

    /** Load a stored file for download by its reference (blob id). */
    public Resource loadAsResource(String ref) {
        FileBlob blob = blobs.findById(parseId(ref))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        return new ByteArrayResource(blob.getData());
    }

    public void delete(String ref) {
        Long id = tryParseId(ref);
        if (id != null && blobs.existsById(id)) blobs.deleteById(id);   // best-effort
    }

    private Long parseId(String ref) {
        Long id = tryParseId(ref);
        if (id == null) {
            // Legacy reference from the old filesystem storage — that file is gone.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        return id;
    }

    private Long tryParseId(String ref) {
        if (ref == null) return null;
        try {
            return Long.parseLong(ref.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
