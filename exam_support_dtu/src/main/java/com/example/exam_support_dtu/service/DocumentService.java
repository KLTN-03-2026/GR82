package com.example.exam_support_dtu.service;

import com.example.exam_support_dtu.annotation.LoggableAction;
import com.example.exam_support_dtu.entity.Documents;
import com.example.exam_support_dtu.entity.EmailTemplate;
import com.example.exam_support_dtu.entity.Users;
import com.example.exam_support_dtu.repository.DocumentRepository;
import com.example.exam_support_dtu.repository.EmailTemplateRepository;
import com.example.exam_support_dtu.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final AuditLogService auditLogService;
    private UserRepository userRepository;

    private EmailTemplateRepository emailTemplateRepository;
    private final EmailService emailService;

    // Biến lưu trữ đường dẫn thư mục
    private final String documentUploadDir;

    public DocumentService(DocumentRepository documentRepository,
                           AuditLogService auditLogService ,
                           UserRepository userRepository, EmailService emailService,
                           @Value("${document.upload-dir}") String documentUploadDir)
    {
        this.documentRepository = documentRepository;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.documentUploadDir = documentUploadDir;

        // Tạo thư mục nếu nó chưa tồn tại lúc khởi động ứng dụng
        java.io.File dir = new java.io.File(this.documentUploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }



    // Lấy danh sách chờ duyệt
    public List<Documents> getPendingDocuments() {
        return documentRepository.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    // Lấy danh sách kho tài liệu đã duyệt
    public List<Documents> getApprovedDocuments() {
        return documentRepository.findByStatusOrderByCreatedAtDesc("APPROVED");
    }

    // Hàm DUYỆT tài liệu
    @LoggableAction(action = "APPROVE_DOCUMENT", targetType = "DOCUMENT", targetId = "#id")
    public boolean approveDocument(Long id) {
        Optional<Documents> optDoc = documentRepository.findById(id);
        if (optDoc.isPresent()) {
            Documents doc = optDoc.get();
            doc.setStatus("APPROVED");
            documentRepository.save(doc);

            // Ghi Log hệ thống
            String details = String.format("Duyệt tài liệu: '%s' (Môn: %s) do SV %s tải lên.",
                    doc.getTitle(), doc.getCourseCode(), doc.getUser().getFullName());
            // Bạn có thể thêm 1 hàm logDocumentAction vào AuditLogService nếu muốn, hoặc dùng logInfo tạm
            auditLogService.logConfigChange(true, 1); // Thay bằng hàm log thích hợp sau
            return true;
        }
        return false;
    }

    // Hàm TỪ CHỐI tài liệu
    @LoggableAction(action = "REJECT_DOCUMENT", targetType = "DOCUMENT", targetId = "#id")
    public boolean rejectDocument(Long id, String reason) {
        Optional<Documents> optDoc = documentRepository.findById(id);
        if (optDoc.isPresent()) {
            //Cập nhật trạng thái
            Documents doc = optDoc.get();
            doc.setStatus("REJECTED");
            documentRepository.save(doc);

            // Xóa file vật lý trên ổ cứng (Theo đúng thông báo "Hệ thống đã gỡ bỏ tệp tin")
            try {
                File physicalFile = new File(doc.getFilePath());
                if (physicalFile.exists()) {
                    physicalFile.delete();
                }
            } catch (Exception e) {
                System.out.println("Cảnh báo: Không thể xóa file vật lý: " + e.getMessage());
            }

           try {
                Users student = doc.getUser();
                if(student != null && student.getEmail() != null) {

                    // Tạo Map chứa các biến để thay thế vào mẫu Mail
                    Map<String, String> emailParams = new HashMap<>();
                    emailParams.put("FULL_NAME", student.getFullName() != null ? student.getFullName() : "Sinh viên");
                    emailParams.put("DOCUMENT_TITLE", doc.getTitle());
                    emailParams.put("REASON", reason); // Lý do admin nhập từ Modal

                    // GỌI HÀM GỬI MAIL
                    // GỌI HÀM sendEmailUsingTemplate ĐÃ ĐƯỢC ĐỊNH NGHĨA TRONG EmailService
                    boolean mailSent = emailService.sendEmailUsingTemplate(student.getEmail(), "DOCUMENT_REJECT", emailParams);

                    if (!mailSent) {
                        System.out.println("Cảnh báo: Không thể gửi mail thông báo từ chối tài liệu.");
                    }
                }

           }catch (Exception e){
               System.out.println("Lỗi khi gửi mail từ chối: " + e.getMessage());
           }


            // Ghi Log hệ thống
            String details = String.format("Từ chối tài liệu: '%s'. Lý do: %s", doc.getTitle(), reason);
            // Ghi log ERROR hoặc WARNING
            return true;
        }
        return false;
    }

    // ==========================================
    //  HÀM GỠ TÀI LIỆU KHỎI KHO PUBLIC
    // ==========================================
    @LoggableAction(action = "DELETE_DOCUMENT", targetType = "DOCUMENT", targetId = "#id")
    public boolean deleteApprovedDocument(Long id) {
        Optional<Documents> optDoc = documentRepository.findById(id);
        if (optDoc.isEmpty()) return false;

        Documents doc = optDoc.get();

        // Xóa file vật lý nếu tồn tại
        try {
            File physicalFile = new File(doc.getFilePath());
            if (physicalFile.exists()) physicalFile.delete();
        } catch (Exception e) {
            System.out.println("Cảnh báo: Không thể xóa file vật lý: " + e.getMessage());
        }

        documentRepository.delete(doc);
        return true;
    }

    // ==========================================
    //  HÀM TĂNG LƯỢT XEM VÀ LƯỢT TẢI
    // ==========================================
    public void incrementViews(Long id) {
        documentRepository.findById(id).ifPresent(doc -> {
            doc.setViews((doc.getViews() != null ? doc.getViews() : 0) + 1);
            documentRepository.save(doc);
        });
    }

    public void incrementDownloads(Long id) {
        documentRepository.findById(id).ifPresent(doc -> {
            doc.setDownloads((doc.getDownloads() != null ? doc.getDownloads() : 0) + 1);
            documentRepository.save(doc);
        });
    }

    public long getTotalViews() {
        Long sum = documentRepository.sumViewsByStatus("APPROVED");
        return sum != null ? sum : 0;
    }

    public long getTotalDownloads() {
        Long sum = documentRepository.sumDownloadsByStatus("APPROVED");
        return sum != null ? sum : 0;
    }

    // ==========================================
    //  HÀM UPLOAD TEST (Sử dụng documentUploadDir)
    // ==========================================
    public void uploadTestDocument(MultipartFile file, String title, String courseCode, String email) throws IOException {

        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User"));

        // 1. Dùng biến đường dẫn đã được tiêm từ properties (thay vì "uploads/documents/")
        Path uploadPath = Paths.get(this.documentUploadDir);

        // 2. Tạo tên file duy nhất
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);

        // 3. Copy file vào ổ đĩa D:
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 4. Trích xuất loại file
        String fileType = "UNKNOWN";
        if (fileName != null) {
            String lowerCaseName = fileName.toLowerCase();
            if (lowerCaseName.endsWith(".pdf")) fileType = "PDF";
            else if (lowerCaseName.endsWith(".doc") || lowerCaseName.endsWith(".docx")) fileType = "DOCX";
            else if (lowerCaseName.endsWith(".zip") || lowerCaseName.endsWith(".rar")) fileType = "ZIP";
            else if (lowerCaseName.endsWith(".xls") || lowerCaseName.endsWith(".xlsx")) fileType = "EXCEL";
        }

        // 5. Lưu vào Database
        Documents doc = new Documents();
        doc.setUser(user);
        doc.setTitle(title);
        doc.setCourseCode(courseCode);

        // Lưu đường dẫn tuyệt đối (hoặc tương đối) tùy bạn. Ở đây lưu đường dẫn D:\...
        doc.setFilePath(filePath.toString());

        doc.setFileType(fileType);
        doc.setStatus("PENDING");

        documentRepository.save(doc);
    }
}