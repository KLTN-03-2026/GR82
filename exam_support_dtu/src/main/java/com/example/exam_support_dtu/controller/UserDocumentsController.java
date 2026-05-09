package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.entity.Documents;
import com.example.exam_support_dtu.repository.DocumentRepository;
import com.example.exam_support_dtu.service.DocumentService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.util.Optional;

@Controller
public class UserDocumentsController {

    private final DocumentRepository documentRepository;
    private final DocumentService documentService;

    public UserDocumentsController(DocumentRepository documentRepository, DocumentService documentService) {
        this.documentRepository = documentRepository;
        this.documentService = documentService;
    }

    // =========================================================
    // 1. HIỂN THỊ TRANG TÀI LIỆU CỘNG ĐỒNG (đã được APPROVED)
    // =========================================================
    @GetMapping("/user/documents")
    public String showUserDocumentsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Documents> docPage = documentRepository.findByStatus("APPROVED", pageable);

        model.addAttribute("docPage", docPage);

        return "user-documents";
    }

    // =========================================================
    // 2. XEM FILE TÀI LIỆU CỘNG ĐỒNG (inline trình duyệt)
    // =========================================================
    @GetMapping("/api/user/documents/view/{id}")
    public ResponseEntity<Resource> viewDocument(@PathVariable Long id) {

        Optional<Documents> docOpt = documentRepository.findById(id);
        if (docOpt.isEmpty()) return ResponseEntity.notFound().build();

        Documents doc = docOpt.get();
        if (!"APPROVED".equals(doc.getStatus())) return ResponseEntity.status(403).build();

        File physicalFile = new File(doc.getFilePath());
        if (!physicalFile.exists()) return ResponseEntity.notFound().build();

        Resource resource = new FileSystemResource(physicalFile);
        MediaType mediaType = "PDF".equals(doc.getFileType())
                ? MediaType.APPLICATION_PDF
                : MediaType.APPLICATION_OCTET_STREAM;

        // Tăng lượt xem
        documentService.incrementViews(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getTitle() + "\"")
                .contentType(mediaType)
                .contentLength(physicalFile.length())
                .body(resource);
    }

    // =========================================================
    // 3. TẢI FILE TÀI LIỆU CỘNG ĐỒNG XUỐNG
    // =========================================================
    @GetMapping("/api/user/documents/download/{id}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {

        Optional<Documents> docOpt = documentRepository.findById(id);
        if (docOpt.isEmpty()) return ResponseEntity.notFound().build();

        Documents doc = docOpt.get();
        if (!"APPROVED".equals(doc.getStatus())) return ResponseEntity.status(403).build();

        File physicalFile = new File(doc.getFilePath());
        if (!physicalFile.exists()) return ResponseEntity.notFound().build();

        Resource resource = new FileSystemResource(physicalFile);

        // Tăng lượt tải
        documentService.incrementDownloads(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getTitle() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(physicalFile.length())
                .body(resource);
    }

    // =========================================================
    // 4. API JSON: CHI TIẾT TÀI LIỆU
    // =========================================================
    @GetMapping("/api/user/documents/{id}")
    @ResponseBody
    public ResponseEntity<Documents> getDocumentDetail(@PathVariable Long id) {
        return documentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
