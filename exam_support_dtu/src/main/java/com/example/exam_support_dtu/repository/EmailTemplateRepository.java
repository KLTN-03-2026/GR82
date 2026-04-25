package com.example.exam_support_dtu.repository;

import com.example.exam_support_dtu.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate,Long> {
}
