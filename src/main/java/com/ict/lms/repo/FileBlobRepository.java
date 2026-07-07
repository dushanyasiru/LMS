package com.ict.lms.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ict.lms.model.FileBlob;

public interface FileBlobRepository extends JpaRepository<FileBlob, Long> {
}
