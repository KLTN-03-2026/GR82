package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.entity.Documents;
import com.example.exam_support_dtu.entity.Users;
import com.example.exam_support_dtu.repository.DocumentRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
public class UserDocumentUploadController {

    private static final String UPLOAD_DIR = "D:/Tracuulich/user-docs/";

    private final DocumentRepository documentRepository;

    public UserDocumentUploadController(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    // =========================================================
    // 1. HIỂN THỊ TRANG TÀI LIỆU CỦA TÔI
    // =========================================================
    @GetMapping("/user/my-documents")
    public String showMyDocumentsPage(@AuthenticationPrincipal Users currentUser, Model model) {
        List<Documents> myDocs = documentRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
        model.addAttribute("myDocs", myDocs);
        return "user-my-documents";
    }

    // =========================================================
    // 2. API: UPLOAD TÀI LIỆU MỚI
    // =========================================================
    @PostMapping("/api/user/documents/upload")
    @ResponseBody
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "courseCode", required = false) String courseCode,
            @AuthenticationPrincipal Users currentUser) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng chọn file để tải lên!"));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tên file không hợp lệ!"));
        }

        // Kiểm tra định dạng
        String ext = "";
        int dotIdx = originalFilename.lastIndexOf(".");
        if (dotIdx >= 0) ext = originalFilename.substring(dotIdx).toLowerCase();
        if (!List.of(".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".zip").contains(ext)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Định dạng file không được hỗ trợ! Chỉ chấp nhận: PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX, ZIP."));
        }

        // Kiểm tra kích thước tối đa 50MB
        if (file.getSize() > 50L * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("message", "File quá lớn! Giới hạn tối đa là 50MB."));
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String newFileName = "doc_" + currentUser.getId() + "_" + UUID.randomUUID() + ext;
            Path filePath = uploadPath.resolve(newFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            Documents doc = new Documents();
            doc.setUser(currentUser);
            doc.setTitle(title != null && !title.trim().isEmpty() ? title.trim() : originalFilename);
            doc.setCourseCode(courseCode != null ? courseCode.trim() : "");
            doc.setFilePath(filePath.toString());
            doc.setFileType(ext.replace(".", "").toUpperCase());
            doc.setStatus("PENDING"); // Chờ duyệt
            documentRepository.save(doc);

            return ResponseEntity.ok(Map.of("message", "Tải lên thành công! Tài liệu đang chờ phê duyệt."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi khi tải file: " + e.getMessage()));
        }
    }

    // =========================================================
    // 3. API: XEM / TẢI FILE TÀI LIỆU
    // =========================================================
    @GetMapping("/api/user/my-documents/view/{id}")
    public ResponseEntity<Resource> viewMyDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal Users currentUser) {

        Optional<Documents> docOpt = documentRepository.findById(id);
        if (docOpt.isEmpty()) return ResponseEntity.notFound().build();

        Documents doc = docOpt.get();
        // Chỉ cho phép xem file của chính mình hoặc file đã approved
        if (!doc.getUser().getId().equals(currentUser.getId()) && !"APPROVED".equals(doc.getStatus())) {
            return ResponseEntity.status(403).build();
        }

        File physicalFile = new File(doc.getFilePath());
        if (!physicalFile.exists()) return ResponseEntity.notFound().build();

        Resource resource = new FileSystemResource(physicalFile);
        MediaType mediaType = "PDF".equals(doc.getFileType()) ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getTitle() + "\"")
                .contentType(mediaType)
                .contentLength(physicalFile.length())
                .body(resource);
    }

    // =========================================================
    // 4. API: XÓA TÀI LIỆU (chỉ xóa khi đang PENDING)
    // =========================================================
    @DeleteMapping("/api/user/my-documents/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteMyDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal Users currentUser) {

        Optional<Documents> docOpt = documentRepository.findById(id);
        if (docOpt.isEmpty()) return ResponseEntity.notFound().build();

        Documents doc = docOpt.get();
        if (!doc.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Bạn không có quyền xóa tài liệu này!"));
        }
        if ("APPROVED".equals(doc.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không thể xóa tài liệu đã được phê duyệt!"));
        }

        // Xóa file vật lý
        try { new File(doc.getFilePath()).delete(); } catch (Exception ignored) {}
        documentRepository.delete(doc);

        return ResponseEntity.ok(Map.of("message", "Đã xóa tài liệu thành công!"));
    }
}
