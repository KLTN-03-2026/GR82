package com.example.exam_support_dtu.service;


import com.example.exam_support_dtu.dto.ExamCrawlDto;
import com.example.exam_support_dtu.entity.ExamOriginal;
import com.example.exam_support_dtu.enums.FileStatus;
import com.example.exam_support_dtu.enums.FileType;
import com.example.exam_support_dtu.repository.ExamOriginalRepository;
import com.example.exam_support_dtu.repository.FilesRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ExamOriginalService {
    private final ExamOriginalRepository eorepo;
    private final FilesRepository filesRepo; // Inject thêm để findById

    public ExamOriginalService(ExamOriginalRepository eo_repo, FilesRepository filesRepo) {
        this.eorepo = eo_repo;
        this.filesRepo = filesRepo;
    }
    // ====================================================================
    public ExamOriginal SavePending(ExamCrawlDto exam) {
        String cleanUrl = exam.getFileUrl().trim();
        //  Dùng chính hàm FindById ở dưới để kiểm tra xem link này có trong DB chưa
        ExamOriginal exist = FindById(cleanUrl);

        // Nếu đã tồn tại (do bị trôi trang và quét lại lần 2) -> Trả về luôn cái cũ
        if (exist != null) {
            return exist;
        }

        // Nếu chưa tồn tại -> Mới tạo bản ghi mới để lưu
        ExamOriginal e = new ExamOriginal();
        e.setFileUrl(cleanUrl);
        e.setFileOriginalName(exam.getOriginalName());
        e.setStatus(FileStatus.pending);

        return eorepo.save(e);
    }
    // ====================================================================
    public ExamOriginal SaveDone(Long examOriginalId,
                                 Long savedFile,
                                 FileType fileType) {
        ExamOriginal e = eorepo.findById(examOriginalId)
                .orElseThrow(() -> new RuntimeException("Exam Original not found: " + examOriginalId));

        // Liên kết Entity (JPA sẽ tự lấy ID từ savedFile để lưu vào cột saved_file_id)
        e.setSavedFileId(savedFile);

        e.setFileType(fileType);
        e.setStatus(FileStatus.downloaded);
        e.setLastCheckedAt(LocalDateTime.now());
        e.setNote("Success");
        return eorepo.save(e);
    }
    // ====================================================================
    public ExamOriginal SaveError(
            Long examOriginalId,
            String note
    ){
        ExamOriginal e = eorepo
                .findById(examOriginalId)
                .orElseThrow(() -> new RuntimeException("Exam Original with id: " + examOriginalId + " not found"));

        e.setStatus(FileStatus.error);
        e.setLastCheckedAt(LocalDateTime.now());
        if (note != null && note.length() > 1000) {
            note = note.substring(0, 1000);
        }
        e.setNote(note);
        return eorepo.save(e);
    }
    // ====================================================================
    public ExamOriginal FindById(String fileUrl){
        return eorepo.findByFileUrl(fileUrl).orElse(null);
    }
    // ====================================================================
    public ExamOriginal UpdateDownloaded(
            Long examOriginalId,
            String download_url
    ){
        ExamOriginal eo = eorepo.findById(examOriginalId)
                .orElseThrow(() -> new RuntimeException("Exam Original with id: " + examOriginalId + " not found"));

        //chi update khi chưa có hoac đã thay đổi
        if(eo.getDownloadUrl() == null || !eo.getDownloadUrl().equals(download_url))
        {
            eo.setDownloadUrl(download_url);
            eo.setLastCheckedAt(LocalDateTime.now());
        }
        return eorepo.save(eo);
    }

    // ====================================================================
    // Cập nhật trạng thái thành PARSED sau khi đọc Excel thành công
    public void UpdateStatusToParsed(Long examOriginalId) {
        ExamOriginal eo = eorepo.findById(examOriginalId)
                .orElseThrow(() -> new RuntimeException("Exam Original with id: " + examOriginalId + " not found"));

        eo.setStatus(FileStatus.parsed);
        eo.setLastCheckedAt(LocalDateTime.now());
        eorepo.save(eo);
    }

}
