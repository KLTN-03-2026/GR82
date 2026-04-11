package com.example.exam_support_dtu.service;

import com.example.exam_support_dtu.dto.DownloadFileDto;
import com.example.exam_support_dtu.dto.ExamCrawlDto;
import com.example.exam_support_dtu.dto.FilesCrawlDto;
import com.example.exam_support_dtu.entity.*;
import com.example.exam_support_dtu.entity.ChecksumUtil;
import com.example.exam_support_dtu.enums.FileStatus;
import com.example.exam_support_dtu.enums.FileType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ExamCrawlService {

    private final ExamOriginalService examOriginalService;
    private final FileStorageService fileStorageService;
    private final ExcelParserService excelParserService;

    public ExamCrawlService(ExamOriginalService examOriginalService, FileStorageService fileStorageService, ExcelParserService excelParserService) {
        this.examOriginalService = examOriginalService;
        this.fileStorageService = fileStorageService;
        this.excelParserService = excelParserService;
    }

    private static final String exam_list_url =
            "https://pdaotao.duytan.edu.vn/EXAM_LIST/Default.aspx?lang=VN";


    // =========================================================================
    // 1. CÁC HÀM ĐIỀU KHIỂN CHÍNH (QUÉT TRANG)
    // =========================================================================

    public String CrawlMultiplePages(int maxPages) {
        StringBuilder report = new StringBuilder();

        for (int i = 1; i <= maxPages; i++) {
            String currentUrl = exam_list_url + "&page=" + i;

            System.out.println("==========================================");
            System.out.println(">>> ĐANG QUÉT TRANG " + i + " / " + maxPages);
            System.out.println(">>> URL: " + currentUrl);
            System.out.println("==========================================");

            String result = ProcessOnePage(currentUrl);
            report.append("Trang ").append(i).append(": ").append(result).append("\n");
        }

        return report.toString();
    }

    private String ProcessOnePage(String pageUrl) {
        int success = 0, skip = 0, error = 0;

        // Sử dụng ExamCrawlDto
        List<ExamCrawlDto> exams = ExamCrawl(pageUrl);

        if (exams.isEmpty()) {
            return "Không tìm thấy bài thi nào (hoặc hết trang).";
        }

        for (ExamCrawlDto exam : exams) {
            try {
                ExamOriginal eo = examOriginalService.FindById(exam.getFileUrl());
                if (eo == null) {
                    eo = examOriginalService.SavePending(exam);
                }

                if (eo.getStatus() == FileStatus.downloaded) {
                    System.out.println(">>> SKIPPED (Already Downloaded): " + exam.getFileUrl());
                    skip++;
                    continue;
                }

                String downloadUrl = GetfileDowloadlink(exam.getFileUrl());

                if (downloadUrl != null) {
                    String fileName = extractFileName(downloadUrl);

                    // Sử dụng FilesCrawlDto
                    FilesCrawlDto fileToSave = new FilesCrawlDto(
                            exam.getSourceName(),
                            fileName,
                            downloadUrl
                    );

                    CrawlAndSave(exam, fileToSave);
                    success++;
                } else {
                    System.out.println(">>> SKIP: Không có file đính kèm hợp lệ (.xls, .pdf) tại " + exam.getFileUrl());
                    examOriginalService.SaveError(eo.getId(), "Không tìm thấy file Excel/PDF đính kèm");
                    skip++;
                }
            } catch (Exception e) {
                System.out.println(">>> ERROR: " + exam.getFileUrl() + " - " + e.getMessage());
                error++;
            }
        }
        return String.format("Success=%d | Skipped=%d | Error=%d", success, skip, error);
    }

    // =========================================================================
    // 2. HÀM CORE: LOGIC NGHIỆP VỤ (LƯU DB & TẢI FILE)
    // =========================================================================

    // Đã thay đổi tham số thành DTO
    public void CrawlAndSave(ExamCrawlDto examCrawl, FilesCrawlDto filesCrawl) {
        String source_name = filesCrawl.getSourceName();

        System.out.println(">>> PROCESSING: " + examCrawl.getFileUrl());

        switch (source_name) {
            case "DANH SÁCH THI":
                ExamOriginal eo = examOriginalService.FindById(examCrawl.getFileUrl());

                if (eo == null) {
                    eo = examOriginalService.SavePending(examCrawl);
                } else if (eo.getStatus() == FileStatus.downloaded) {
                    System.out.println(">>> SKIPPED (Already Downloaded): " + eo.getFileOriginalName());
                    return;
                }

                try {
                    examOriginalService.UpdateDownloaded(eo.getId(), filesCrawl.getDownloadUrl());

                    // Sử dụng DownloadFileDto
                    DownloadFileDto dl = DownloadFile(filesCrawl);
                    if (dl == null) throw new RuntimeException("Download failed (Return null)");

                    String checksum = ChecksumUtil.sha256(dl.getFilePath());

                    FileType typeEnum = FileType.valueOf(dl.getExtension());

                    com.example.exam_support_dtu.entity.Files savedFile = fileStorageService.saveFile(
                            filesCrawl.getSourceName(),
                            filesCrawl.getFileOriginalName(),
                            dl.getStoredName(),
                            typeEnum,
                            dl.getFilePath(),
                            dl.getFileSize(),
                            checksum
                    );

                    boolean isDuplicateFile = !savedFile.getFilePath().equals(dl.getFilePath());
                    if (isDuplicateFile) {
                        try {
                            Files.deleteIfExists(Paths.get(dl.getFilePath()));
                            System.out.println(">>> DUPLICATE FILE DETECTED: Deleted temp file " + dl.getFilePath());
                        } catch (Exception ex) {
                            System.out.println(">>> WARNING: Could not delete duplicate file: " + ex.getMessage());
                        }
                    }

                    ExamOriginal doneExam = examOriginalService.SaveDone(
                            eo.getId(),
                            savedFile.getId(),
                            typeEnum
                    );
                    System.out.println(">>> SUCCESS: Saved Exam ID " + eo.getId() + " linked to File ID " + savedFile.getId());

                    // ĐOẠN PARSE EXCEL GIỮ NGUYÊN...
                    if (typeEnum == FileType.xls || typeEnum == FileType.xlsx) {
                        if (isDuplicateFile) {
                            System.out.println(">>> SKIP PARSING: File trùng lặp nội dung. Đã liên kết với file cũ thành công.");
                        } else {
                            System.out.println(">>> START PARSING EXCEL...");
                            try (FileInputStream fis = new FileInputStream(savedFile.getFilePath())) {
                                String parseResult = excelParserService.parseAndSave(fis, doneExam, savedFile);
                                System.out.println(">>> PARSE RESULT: " + parseResult);

                                if (parseResult.startsWith("Thành công")) {
                                    examOriginalService.UpdateStatusToParsed(doneExam.getId());
                                } else {
                                    examOriginalService.SaveError(doneExam.getId(), parseResult);
                                }
                            } catch (Exception ex) {
                                System.out.println(">>> ERROR PARSING: " + ex.getMessage());
                                examOriginalService.SaveError(doneExam.getId(), "Lỗi Exception khi Parse: " + ex.getMessage());
                                ex.printStackTrace();
                            }
                        }
                    } else {
                        System.out.println(">>> SKIP PARSING: File type is " + typeEnum);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    examOriginalService.SaveError(eo.getId(), e.getMessage());
                    System.out.println(">>> ERROR SAVED: " + e.getMessage());
                }
                break;

            case "THÔNG BÁO":
                break;
            default:
                System.out.println(">>> UNKNOWN SOURCE: " + source_name);
                break;
        }
    }

    // =========================================================================
    // 3. CÁC HÀM HỖ TRỢ
    // =========================================================================

    public List<ExamCrawlDto> ExamCrawl(String targetUrl){
        List<ExamCrawlDto> result = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(targetUrl).userAgent("Mozilla/5.0").timeout(15_000).get();
            String sourceName = SourceCrawl(doc);
            Elements links = doc.select("td.border_main a.txt_l4");

            for( Element link : links ){
                String url = link.absUrl("href");
                String originalName = link.text();
                if(!url.isEmpty()){
                    result.add(new ExamCrawlDto(sourceName,originalName,url));
                }
            }
        } catch (Exception e) {
            System.out.println("Lỗi kết nối tới: " + targetUrl);
        }
        return result;
    }

    public String SourceCrawl(Document doc) {
        Elements links = doc.select("a.txt_main_category");
        if (!links.isEmpty()) {
            return links.first().text().trim();
        }
        return "UNKNOWN";
    }

    public String GetfileDowloadlink(String url){
        try {
            Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10_000).get();
            Elements links = doc.select("a");
            for( Element link : links ){
                String detailurl = link.absUrl("href");
                String lower = detailurl.toLowerCase();
                if(lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".pdf")){
                    return detailurl;
                }
            }
        }catch (Exception e){
            System.out.println("Lỗi crawl link file tại: " + url);
        }
        return null;
    }

    // Trả về DownloadFileDto
    public DownloadFileDto DownloadFile(FilesCrawlDto filecrawl) {
        String baseSaveDir = FileStorageService.UPLOAD_DIR;
        try {
            URL rawUrl = new URL(filecrawl.getDownloadUrl());
            String encodedPath = encodePath(rawUrl.getPath());

            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(rawUrl.getProtocol()).append("://");
            urlBuilder.append(rawUrl.getHost());
            if (rawUrl.getPort() != -1) urlBuilder.append(":").append(rawUrl.getPort());
            urlBuilder.append(encodedPath);
            if (rawUrl.getQuery() != null) urlBuilder.append("?").append(rawUrl.getQuery());

            URL encodedUrl = new URL(urlBuilder.toString());

            HttpURLConnection conn = (HttpURLConnection) encodedUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.out.println("Server trả về lỗi: " + responseCode + " URL: " + encodedUrl);
                return null;
            }

            String original_name = filecrawl.getFileOriginalName();
            String storedName = System.currentTimeMillis()
                    + "_" + UUID.randomUUID().toString().substring(0, 8)
                    + "_" + sanitizeFileName(original_name);

            Path savePath = Paths.get(baseSaveDir, storedName);

            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, savePath, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                conn.disconnect();
            }
            return new DownloadFileDto(
                    storedName,
                    GetFileExtension(original_name),
                    savePath.toString(),
                    Files.size(savePath)
            );

        } catch (Exception e) {
            System.out.println("Lỗi tải file: " + filecrawl.getDownloadUrl() + " -> " + e.getMessage());
            return null;
        }
    }

    // Các hàm encodePath, sanitizeFileName, GetFileExtension, extractFileName giữ nguyên 100%
    private String encodePath(String path) { /* ... */ return path; } // Bạn copy code cũ vào đây
    private String sanitizeFileName(String name) { /* ... */ return name; } // Copy code cũ
    public String GetFileExtension(String fileName){ /* ... */ return "xls"; } // Copy code cũ
    private String extractFileName(String fileUrl) { /* ... */ return "file"; } // Copy code cũ
}