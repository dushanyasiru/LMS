package com.ict.lms.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * The actual bytes of an uploaded file, stored in the database.
 *
 * Vercel containers have an ephemeral filesystem (wiped on every restart and
 * not shared between instances), so files written to disk vanish and downloads
 * 404. Keeping the bytes in Postgres makes them durable and visible to every
 * instance. A plain byte[] (no @Lob) maps to PostgreSQL `bytea`, which holds
 * our files comfortably (uploads are capped at 25MB by the multipart limit).
 *
 * This is a separate table from Resource/Submission on purpose: listing those
 * rows must never drag the file bytes into memory.
 */
@Entity
public class FileBlob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private byte[] data;

    /** MIME type as reported at upload time (e.g. application/pdf). May be null. */
    private String contentType;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
}
