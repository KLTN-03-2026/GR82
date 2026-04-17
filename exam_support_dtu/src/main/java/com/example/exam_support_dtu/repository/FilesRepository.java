package com.example.exam_support_dtu.repository;

import com.example.exam_support_dtu.entity.Files;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FilesRepository extends JpaRepository<Files, Long> {
    Files findByChecksum(String checksum);
    Optional<Files> findByStoredName(String storedName);
}
