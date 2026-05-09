package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.entity.Documents;
import com.example.exam_support_dtu.repository.DocumentRepository;
import com.example.exam_support_dtu.service.DocumentService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/documents")
public class AdminDocumentController {


    private final DocumentService documentService;
    private final DocumentRepository documentRepository;

    public AdminDocumentController(DocumentService documentService, DocumentRepository documentRepository) {
        this.documentService = documentService;
        this.documentRepository = documentRepository;
    }

    // Hiển thị trang Quản lý Tài liệu
    @GetMapping
    public String showDocumentManagement(Model model) {
        List<Documents> pendingDocs = documentService.getPendingDocuments();
        List<Documents> approvedDocs = documentService.getApprovedDocuments();

        model.addAttribute("pendingDocs", pendingDocs);
        model.addAttribute("approvedDocs", approvedDocs);
        model.addAttribute("pendingCount", pendingDocs.size());
        model.addAttribute("approvedCount", approvedDocs.size());
        model.addAttribute("totalDownloads", documentService.getTotalDownloads());

        // Trả về file admin-documents.html
        return "admin-documents";
    }

    // API Xử lý Duyệt
    @PostMapping("/{id}/approve")
    public String approveDoc(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean success = documentService.approveDocument(id);
        if (success) {
            redirectAttributes.addFlashAttribute("successMsg", "Đã duyệt tài liệu thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Không tìm thấy tài liệu!");
        }
        return "redirect:/admin/documents";
    }

    // API Xử lý Từ chối
    @PostMapping("/{id}/reject")
    public String rejectDoc(@PathVariable Long id, @RequestParam String reason, RedirectAttributes redirectAttributes) {
        boolean success = documentService.rejectDocument(id, reason);
        if (success) {
            redirectAttributes.addFlashAttribute("successMsg", "Đã từ chối tài liệu!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Không tìm thấy tài liệu!");
        }
        return "redirect:/admin/documents";
    }

    // API Gỡ tài liệu khỏi kho public (xóa hoàn toàn)
    @PostMapping("/{id}/delete")
    public String deleteApprovedDoc(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean success = documentService.deleteApprovedDocument(id);
        if (success) {
            redirectAttributes.addFlashAttribute("successMsg", "Đã gỡ tài liệu khỏi kho thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Không tìm thấy tài liệu cần gỡ!");
        }
        return "redirect:/admin/documents";
    }

    // API TRẢ VỀ FILE ĐỂ XEM TRƯỚC TRÊN TRÌNH DUYỆT
    @GetMapping("/view/{id}")
    public ResponseEntity<Resource> viewDocument(@PathVariable Long id) {
        // Lấy thông tin Document từ Database (Bạn có thể gọi Service nếu đã bọc hàm)
        Optional<Documents> optDoc = documentRepository.findById(id);
        if (optDoc.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Documents doc = optDoc.get();
        File physicalFile = new File(doc.getFilePath());

        if (!physicalFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(physicalFile);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

        // Định dạng PDF để trình duyệt biết đường mở tab
        if ("PDF".equalsIgnoreCase(doc.getFileType())) {
            mediaType = MediaType.APPLICATION_PDF;
        }

        // Tăng lượt xem
        documentService.incrementViews(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getTitle() + "\"")
                .contentType(mediaType)
                .contentLength(physicalFile.length())
                .body(resource);
    }

    // =========================================================
    // TẢI TÀI LIỆU XUỐNG (DOWNLOAD) VÀ TĂNG LƯỢT TẢI
    // =========================================================
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        // Lấy thông tin Document từ Database
        Optional<Documents> docOptional = documentRepository.findById(id);
        if (docOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Documents dbDoc = docOptional.get();
        File physicalFile = new File(dbDoc.getFilePath());

        if (!physicalFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(physicalFile);

        // Tăng lượt tải
        documentService.incrementDownloads(id);

        // 1. XỬ LÝ TÊN FILE KHI TẢI XUỐNG
        String title = dbDoc.getTitle();
        String ext = (dbDoc.getFileType() != null) ? dbDoc.getFileType().toLowerCase() : "bin";

        // Ghép thêm đuôi file nếu người dùng nhập thiếu (VD: "De_thi" -> "De_thi.pdf")
        String fileName = title.toLowerCase().endsWith("." + ext) ? title : title + "." + ext;

        // [BẢO MẬT] Dọn dẹp tên file: Xóa các ký tự cấm trong Windows (\ / : * ? " < > |) tránh lỗi hệ điều hành
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

        try {
            // 2. MÃ HÓA TÊN FILE (Chống lỗi font tiếng Việt khi tải qua trình duyệt)
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");

            // 3. TRẢ VỀ HEADER ÉP TẢI XUỐNG
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(physicalFile.length())
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }


    @PostMapping("/test-upload")
    public String testUpload(@RequestParam("file") MultipartFile file,
                             @RequestParam("title") String title,
                             @RequestParam("courseCode") String courseCode,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            // Lấy email của Admin đang đăng nhập để gán làm tác giả
            String email = principal.getName();

            documentService.uploadTestDocument(file, title, courseCode, email);
            redirectAttributes.addFlashAttribute("successMsg", "Test Upload thành công! Hãy kiểm tra tab Chờ duyệt.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi upload: " + e.getMessage());
        }
        return "redirect:/admin/documents";
    }

}