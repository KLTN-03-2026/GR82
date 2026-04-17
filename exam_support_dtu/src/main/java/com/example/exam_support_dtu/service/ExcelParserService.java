package com.example.exam_support_dtu.service;

import com.example.exam_support_dtu.entity.*;
import com.example.exam_support_dtu.repository.ExamOriginalRepository;
import com.example.exam_support_dtu.repository.ExamScheduleRepository;
import com.example.exam_support_dtu.repository.FilesRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class ExcelParserService {

    private final ExamScheduleRepository examScheduleRepository;
    private final FilesRepository filesRepository;
    private final ExamOriginalRepository examOriginalRepository;

    public ExcelParserService(ExamScheduleRepository examScheduleRepository, FilesRepository filesRepository, ExamOriginalRepository examOriginalRepository) {
        this.examScheduleRepository = examScheduleRepository;
        this.filesRepository = filesRepository;
        this.examOriginalRepository = examOriginalRepository;
    }

    @Transactional
    public String parseAndSave(InputStream inputStream, ExamOriginal examOriginal, Files file) {
        // 1. Lưu metadata trước
        if (file != null && file.getId() == null)
            file = filesRepository.save(file);
        if (examOriginal != null && examOriginal.getId() == null)
            examOriginal = examOriginalRepository.save(examOriginal);

        // 2. Xóa dữ liệu cũ
        if (examOriginal != null && examOriginal.getFileOriginalName() != null) {
            // Tìm tất cả ExamOriginal có cùng tên file gốc (ví dụ: DS_THI_MEC_111.xlsx)
            List<ExamOriginal> sameNameOriginals = examOriginalRepository.findByFileOriginalName(examOriginal.getFileOriginalName());

            for (ExamOriginal oldOriginal : sameNameOriginals) {
                Optional<ExamSchedule> oldData = examScheduleRepository.findByExamOriginalId(oldOriginal.getId());
                if (oldData.isPresent()) {
                    examScheduleRepository.delete(oldData.get());
                    System.out.println(">>> ĐÃ XÓA DỮ LIỆU CŨ CỦA BẢN CẬP NHẬT TRƯỚC: " + examOriginal.getFileOriginalName());
                }
            }
            examScheduleRepository.flush();
        }

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            // --- ĐOẠN DEBUG KIỂM TRA CẤU TRÚC FILE ---
            System.out.println("=== KIỂM TRA CẤU TRÚC FILE ===");
            System.out.println("Tổng số sheet: " + workbook.getNumberOfSheets());
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                String name = workbook.getSheetName(i);
                boolean isHidden = workbook.isSheetHidden(i);
                boolean isVeryHidden = workbook.isSheetVeryHidden(i);
                System.out.println("Sheet [" + i + "]: " + name + " | Ẩn: " + (isHidden || isVeryHidden));
            }
            System.out.println("==============================");
            // -------------------------------------------

            ExamSchedule schedule = new ExamSchedule();
            schedule.setExamOriginal(examOriginal);
            schedule.setFile(file);

            // BƯỚC 2: ĐẾM SỐ LƯỢNG SHEET HIỂN THỊ THỰC TẾ
            // ==================================================================
            int visibleSheetCount = 0;
            int totalSheets = workbook.getNumberOfSheets();

            for (int i = 0; i < totalSheets; i++) {
                // Chỉ đếm nếu sheet không bị ẩn
                if (!workbook.isSheetHidden(i) && !workbook.isSheetVeryHidden(i)) {
                    // Có thể thêm logic loại trừ sheet virus tên IDCODE ở đây nếu muốn chặt chẽ hơn
                    String tempName = workbook.getSheetName(i).toUpperCase();
                    if (!tempName.contains("IDCODE") && !tempName.contains("MACRO")) {
                        visibleSheetCount++;
                    }
                }
            }
            System.out.println(">>> Tổng sheet: " + totalSheets + " | Sheet hiển thị hợp lệ: " + visibleSheetCount);

            // 3. QUÉT HEADER TOÀN CỤC
            // --- LOGIC DUYỆT SHEET ĐÃ SỬA ĐỔI ---
            for (int i = 0; i < totalSheets; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName().toUpperCase();

                // 1. ƯU TIÊN SỐ 1: BỎ QUA SHEET ẨN / SHEET VIRUS
                // (Giữ nguyên logic này vì nó đang hoạt động rất tốt để chặn sheet XXXXXXXX)
                if (workbook.isSheetHidden(i) || workbook.isSheetVeryHidden(i)) {
                    System.out.println(">>> SKIP sheet ẩn (virus/rác): " + sheetName);
                    continue;
                }
                if (sheetName.contains("IDCODE") || sheetName.contains("MACRO")) {
                    continue;
                }

                // 2. ƯU TIÊN SỐ 2: CHỈ BỎ QUA "TONGHOP"
                // Vì file bị nhiễm virus sẽ có > 1 sheet, nếu bỏ qua DS_THI là mất sạch dữ liệu.
                // Logic: Nếu chưa tìm thấy số TC hoặc Tên môn -> Quét sheet này thử xem có không
                if (schedule.getCredit() == null || schedule.getCourseName() == null) {
                    extractGlobalInfoManual(sheet, schedule);
                }
                boolean isSummary = sheetName.contains("TONGHOP");

                if (visibleSheetCount > 1 && isSummary) {
                    System.out.println(">>> SKIP sheet tổng hợp: " + sheetName);
                    continue;
                }

                // 3. XỬ LÝ DỮ LIỆU
                System.out.println(">>> ĐANG XỬ LÝ: " + sheetName);
                if (isSummary) {
                    // Nếu là sheet TONGHOP -> Gọi hàm 3
                    processSheetTongHop(sheet, schedule);
                } else {
                    // Nếu là sheet thường -> Gọi hàm cũ
                    processSheetRoomsManual(sheet, schedule);
                }
            }
            // -------------------------------------

            int totalStudents = 0;
            for (ExamRoom r : schedule.getRooms()) totalStudents += r.getStudents().size();
            schedule.setTotalStudents(totalStudents);

            if (!schedule.getRooms().isEmpty()) {
                examScheduleRepository.save(schedule);
                return "Thành công! Đã lưu " + schedule.getRooms().size() + " phòng. Tổng SV: " + totalStudents;
            } else {
                return "File không chứa dữ liệu phòng thi hợp lệ.";
            }

        } catch (IOException e) {
            e.printStackTrace();
            return "Lỗi đọc file: " + e.getMessage();
        }
    }

    // --- HÀM 1: QUÉT HEADER ( BẮT SỐ TÍN CHỈ, HK, MÃ MÔN) ---
    private void extractGlobalInfoManual(Sheet sheet, ExamSchedule schedule) {
        DataFormatter fmt = new DataFormatter();

        // Quét 20 dòng đầu
        for (int i = 0; i < Math.min(20, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            StringBuilder rowTextBuilder = new StringBuilder();
            for (Cell cell : row) rowTextBuilder.append(fmt.formatCellValue(cell)).append(" ");

            // 1. CHUẨN HÓA CHUỖI (QUAN TRỌNG: Thêm xử lý \u00A0)
            String text = rowTextBuilder.toString().replace('\u00A0', ' ').trim();
            while (text.contains("  ")) text = text.replace("  ", " ");

            String textUpper = text.toUpperCase();

            // -----------------------------------------------------------
            // 1. SỐ TÍN CHỈ
            // -----------------------------------------------------------
            if (schedule.getCredit() == null) {
                if (textUpper.contains("SỐ TÍN CHỈ")) schedule.setCredit(parseNumberStrict(text, "SỐ TÍN CHỈ"));
                else if (textUpper.contains("SỐ TC")) schedule.setCredit(parseNumberStrict(text, "SỐ TC"));
                else if (textUpper.contains("TÍN CHỈ")) schedule.setCredit(parseNumberStrict(text, "TÍN CHỈ"));
            }

            // -----------------------------------------------------------
            // 2. HỌC KỲ (LOGIC THÔNG MINH: Có ngoặc thì lấy hết, không ngoặc thì chỉ lấy số)
            // -----------------------------------------------------------
            if (schedule.getSemester() == null) {
                String sem = null;
                if (textUpper.contains("HỌC KỲ")) sem = parseStringAndCut(text, "HỌC KỲ");
                else if (textUpper.contains("HK")) sem = parseStringAndCut(text, "HK");

                if (sem != null && !sem.isEmpty()) {
                    // Bước 1: Cắt bỏ các từ khóa rác của header khác nếu dính vào
                    String[] stopWords = {"LẦN THI", "SỐ TC", "SỐ TÍN CHỈ", "MÃ MÔN", "NGÀY THI"};
                    sem = cutStringAtKeywords(sem, stopWords);

                    // Bước 2: Logic xử lý Ngoặc đơn
                    int openParen = sem.indexOf("(");
                    int closeParen = sem.indexOf(")");

                    if (openParen != -1 && closeParen != -1 && closeParen > openParen) {
                        // TRƯỜNG HỢP 1: Có đầy đủ cặp ngoặc ( )
                        // Ví dụ: "1 (2025-2026)" -> Lấy đến dấu đóng ngoặc
                        sem = sem.substring(0, closeParen + 1).trim();
                    } else {
                        // TRƯỜNG HỢP 2: Không có ngoặc hoặc ngoặc lỗi
                        // Ví dụ: "1" hoặc "1 - Năm học..." hoặc "1;"
                        // -> Chỉ lấy con số đầu tiên tìm thấy
                        StringBuilder sb = new StringBuilder();
                        boolean foundDigit = false;
                        for (char c : sem.toCharArray()) {
                            if (Character.isDigit(c)) {
                                sb.append(c);
                                foundDigit = true;
                            } else if (foundDigit) {
                                // Đã lấy được số (ví dụ '1') mà gặp ký tự khác (cách, chấm, phẩy...)
                                // -> DỪNG NGAY LẬP TỨC
                                break;
                            }
                        }
                        sem = sb.toString();
                    }

                    if (!sem.isEmpty()) {
                        schedule.setSemester(sem);
                    }
                }
            }

            // -----------------------------------------------------------
            // 3. LẦN THI
            // -----------------------------------------------------------
            if (schedule.getAttempt() == null && textUpper.contains("LẦN THI")) {
                schedule.setAttempt(parseNumberStrict(text, "LẦN THI"));
            }

            // -----------------------------------------------------------
            // 4. TÊN MÔN (Logic Cân bằng: Cắt rác XLSX nhưng giữ ngoặc XLS)
            // -----------------------------------------------------------
            if (schedule.getCourseName() == null && (textUpper.contains("MÔN :") || textUpper.contains("MÔN:"))) {
                String val = parseStringAndCut(text, "MÔN");

                // Danh sách "Kẻ thù" cần cắt bỏ.
                // LƯU Ý: Không đưa dấu "(" vào đây để bảo vệ tên môn kiểu "Java (Nâng cao)"
                String[] stopWords = {
                        "*",              // Dấu sao (Đặc trưng của file XLSX lỗi)
                        "SỐ TÍN CHỈ", "SỐ TC", "TÍN CHỈ",
                        "MÃ MÔN", "KHỐI THI",
                        "HK:", "HỌC KỲ", "HK :", "LẦN THI"
                };

                val = cutStringAtKeywords(val, stopWords);
                val = removeTrailingChars(val);

                if (!val.isEmpty()) schedule.setCourseName(val);
            }

            // -----------------------------------------------------------
            // 5. MÃ MÔN
            // -----------------------------------------------------------
            if (schedule.getCourseCode() == null) {
                String code = "";
                if (textUpper.contains("MÃ MÔN")) code = parseStringAndCut(text, "MÃ MÔN");
                else if (textUpper.contains("KHỐI THI")) code = parseStringAndCut(text, "KHỐI THI");

                if (!code.isEmpty()) {
                    // Mã môn thì cắt mạnh tay hơn (không được chứa ngoặc)
                    String[] stopWords = {"HK", "HỌC KỲ", "*", "SỐ TC", "(", ")"};
                    code = cutStringAtKeywords(code, stopWords);
                    schedule.setCourseCode(code);
                }
            }
        }
    }

    // --- HÀM 2: XỬ LÝ FILE THƯỜNG
    private void processSheetRoomsManual(Sheet sheet, ExamSchedule schedule) {
        DataFormatter fmt = new DataFormatter();
        ExamRoom currentRoom = null;
        boolean isReadingStudents = false;
        boolean isVirusSheet = false;

        System.out.println("--------------------------------------------------");
        System.out.println(">>> ĐANG QUÉT SHEET (Logic Thường): " + sheet.getSheetName());

        for (Row row : sheet) {
            if (row == null) continue;
            if (isVirusSheet) break;

            StringBuilder rowBuilder = new StringBuilder();
            for(Cell c : row) rowBuilder.append(fmt.formatCellValue(c)).append(" ");

            String rowText = rowBuilder.toString();
            rowText = rowText.replace('\u00A0', ' ').trim();
            while (rowText.contains("  ")) {
                rowText = rowText.replace("  ", " ");
            }

            String rowTextUpper = rowText.toUpperCase();

            if (rowText.contains("**Infect Workbook**") ||
                    rowText.contains("**Our Values and Paths**") ||
                    rowText.contains("StartUp.xls") ||
                    rowText.contains("XLSTART")) {
                System.out.println(">>> CẢNH BÁO: Phát hiện Sheet chứa Macro Virus hoặc Rác. BỎ QUA SHEET NÀY!");
                isVirusSheet = true;
                break;
            }

//            if (rowText.length() > 5) {
//                // System.out.println("[ROW DEBUG]: " + rowText);
//            }

            // A. PHÁT HIỆN HEADER PHÒNG THI
            boolean hasTime = rowTextUpper.contains("THỜI GIAN") || rowTextUpper.contains("GIỜ THI") || rowTextUpper.contains("TG :");
            boolean hasRoom = rowTextUpper.contains("PHÒNG");

            if (hasTime && hasRoom) {
                System.out.println("   ===> BẮT ĐƯỢC HEADER PHÒNG: " + rowText);

                if (currentRoom != null) {
                    currentRoom.setCapacity(currentRoom.getStudents().size());
                    schedule.getRooms().add(currentRoom);
                }
                currentRoom = new ExamRoom();
                currentRoom.setExamSchedule(schedule);
                isReadingStudents = false;

                parseRoomDetailManual(rowText, currentRoom);
                continue;
            }

            // B. PHÁT HIỆN BẢNG SINH VIÊN
            String cell1 = fmt.formatCellValue(row.getCell(1)).trim();
            String cell2 = fmt.formatCellValue(row.getCell(2)).trim();

            if (cell1.equalsIgnoreCase("STT") || cell2.equalsIgnoreCase("MSV") || cell2.toUpperCase().contains("MÃ SV")) {
                isReadingStudents = true;
                continue;
            }

            // C. ĐỌC SINH VIÊN
            if (isReadingStudents && currentRoom != null) {
                if (isNumeric(cell1) && !cell2.isEmpty()) {
                    ExamStudent student = new ExamStudent();
                    student.setSeatNumber(cell1);
                    student.setStudentCode(cell2);

                    // Code gốc của bạn lấy Họ ở cell 3, Tên ở cell 4
                    student.setLastName(fmt.formatCellValue(row.getCell(3)).trim());
                    student.setFirstName(fmt.formatCellValue(row.getCell(4)).trim());

                    String classCode = fmt.formatCellValue(row.getCell(5)).trim();
                    student.setClassCode(classCode);

                    if (schedule.getCourseCode() == null && !classCode.isEmpty()) {
                        schedule.setCourseCode(classCode);
                    }

                    student.setStudentClass(fmt.formatCellValue(row.getCell(6)).trim());
                    currentRoom.addStudent(student);
                }
                else if (rowTextUpper.contains("TỔNG SỐ BÀI") || rowTextUpper.contains("GIÁM THỊ")) {
                    isReadingStudents = false;
                }
            }
        }

        if (currentRoom != null) {
            currentRoom.setCapacity(currentRoom.getStudents().size());
            schedule.getRooms().add(currentRoom);
            System.out.println(">>> Đã lưu phòng cuối cùng. Tổng số phòng tìm thấy: " + schedule.getRooms().size());
        }
    }

    // --- HÀM 3: XỬ LÝ RIÊNG CHO SHEET TONGHOP ---
    private void processSheetTongHop(Sheet sheet, ExamSchedule schedule) {
        DataFormatter fmt = new DataFormatter();
        ExamRoom currentRoom = null;
        boolean isReadingStudents = false;

        System.out.println("--------------------------------------------------");
        System.out.println(">>> ĐANG QUÉT SHEET (Logic TONGHOP): " + sheet.getSheetName());

        // Mapping cột đặc thù của TONGHOP (STT=0, MSV=1, Họ=2, Tên=3, Lớp=4)
        int colMSV = 1;

        for (Row row : sheet) {
            if (row == null) continue;

            StringBuilder rowBuilder = new StringBuilder();
            for(Cell c : row) rowBuilder.append(fmt.formatCellValue(c)).append(" ");
            String rowText = rowBuilder.toString().replace('\u00A0', ' ').trim();
            String rowTextUpper = rowText.toUpperCase();

            // 1. Nhận diện Header Bảng điểm
            if (rowTextUpper.contains("MSV") || rowTextUpper.contains("MÃ SV")) {
                isReadingStudents = true;
                continue;
            }

            // 2. Đọc Sinh viên
            boolean isStudentRow = false;
            if (isReadingStudents && currentRoom != null) {
                // Điểm dừng bảng
                if (rowTextUpper.contains("TỔNG SỐ BÀI") || rowTextUpper.contains("GIÁM THỊ")) {
                    isReadingStudents = false;
                } else {
                    String msv = fmt.formatCellValue(row.getCell(colMSV)).trim();

                    // [SỬA QUAN TRỌNG TẠI ĐÂY]
                    // Thêm lại 'isNumeric(msv)' để phân biệt mã sinh viên (là số) và các câu Text Header rác
                    if (!msv.isEmpty() && msv.length() >= 5 && isNumeric(msv)) {
                        isStudentRow = true; // Đánh dấu chắc chắn là dòng SV

                        ExamStudent student = new ExamStudent();
                        student.setStudentCode(msv);
                        student.setSeatNumber(fmt.formatCellValue(row.getCell(0)).trim()); // STT cột 0
                        student.setLastName(fmt.formatCellValue(row.getCell(2)).trim());   // HỌ cột 2
                        student.setFirstName(fmt.formatCellValue(row.getCell(3)).trim());  // TÊN cột 3

                        String classCode = fmt.formatCellValue(row.getCell(4)).trim();     // LỚP MH cột 4
                        student.setClassCode(classCode);
                        if (schedule.getCourseCode() == null && !classCode.isEmpty()) schedule.setCourseCode(classCode);

                        student.setStudentClass(fmt.formatCellValue(row.getCell(5)).trim()); // LỚP SH cột 5

                        student.setExamRoom(currentRoom);
                        currentRoom.addStudent(student);
                    }
                }
            }

            // 3. Bắt Header Phòng Thi (Chỉ bắt khi dòng hiện tại KHÔNG PHẢI là sinh viên)
            if (!isStudentRow) {
                boolean hasTime = rowTextUpper.contains("THỜI GIAN") || rowTextUpper.contains("GIỜ THI") || rowTextUpper.startsWith("TG :");
                boolean hasRoom = rowTextUpper.contains("PHÒNG");

                if (hasTime && hasRoom) {
                    if (currentRoom != null) {
                        currentRoom.setCapacity(currentRoom.getStudents().size());
                        schedule.getRooms().add(currentRoom);
                    }
                    currentRoom = new ExamRoom();
                    currentRoom.setExamSchedule(schedule);

                    isReadingStudents = false;
                    parseRoomDetailManual(rowText, currentRoom);
                }
            }
        }

        // Lưu phòng cuối cùng
        if (currentRoom != null) {
            currentRoom.setCapacity(currentRoom.getStudents().size());
            schedule.getRooms().add(currentRoom);
        }
    }


    // --- HÀM 3: PARSE PHÒNG THI (TÁCH ĐỊA ĐIỂM CHUẨN XÁC) ---
    private void parseRoomDetailManual(String text, ExamRoom room) {
        String textUpper = text.toUpperCase();

        // 1. PARSE THỜI GIAN & NGÀY THI (Dùng Regex)
        if (textUpper.contains("THỜI GIAN") || textUpper.contains("GIỜ THI") || textUpper.contains("TG")) {

            // A. Bắt NGÀY THI (dạng dd/MM/yyyy)
            // Regex này tìm chuỗi số dạng: 2 số / 2 số / 4 số -> Tự động bỏ qua chữ "Ngày", "-", "Date"...
            java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("(\\d{1,2}/\\d{1,2}/\\d{4})");
            java.util.regex.Matcher dateMatcher = datePattern.matcher(text);
            if (dateMatcher.find()) {
                String dateStr = dateMatcher.group(1); // Lấy được "19/01/2026"
                try {
                    room.setExamDate(LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("d/M/yyyy")));
                } catch (Exception e) {
                    System.out.println("Lỗi parse ngày: " + dateStr);
                }
            }

            // B. Bắt GIỜ THI (dạng 13h30, 7h30, 13:30...)
            java.util.regex.Pattern timePattern = java.util.regex.Pattern.compile("(\\d{1,2}[hH:]\\d{2})");
            java.util.regex.Matcher timeMatcher = timePattern.matcher(text);
            if (timeMatcher.find()) {
                room.setExamTime(timeMatcher.group(1)); // Lấy được "13h30"
            }
        }

        // 2. Parse Phòng & Địa điểm (Logic cũ của bạn đã tốt, giữ nguyên phần lõi)
        // Ưu tiên tìm "PHÒNG THI" trước để tránh lấy nhầm chữ "THI" trong "PHÒNG THI"
        String roomRaw = "";
        if (textUpper.contains("PHÒNG THI")) {
            roomRaw = parseStringAndCut(text, "PHÒNG THI");
        } else if (textUpper.contains("PHÒNG")) {
            roomRaw = parseStringAndCut(text, "PHÒNG");
        }

        if (!roomRaw.isEmpty()) {
            // --- QUAN TRỌNG: CẮT BỎ CÁC TỪ KHÓA DÍNH PHÍA SAU ---
            // Đây là bước giúp Location sạch sẽ, không bị dính "Lần thi", "Số TC"...
            String[] stopWords = {
                    "LẦN THI", "LẦN:","LAN THI",
                    "SỐ TC", "SỐ TÍN CHỈ",
                    "MÃ MÔN", "HỌC KỲ", "HK:",
                    "SỐ TRANG", "GIÁM THỊ"
            };

            String roomFull = cutStringAtKeywords(roomRaw, stopWords);
            roomFull = removeTrailingChars(roomFull);
            // ----------------------------------------------------

            // 3. Tách Tên phòng và Địa điểm (Logic cũ)
            // Tìm dấu " - " (gạch ngang có khoảng trắng bao quanh)
            int splitDash = roomFull.lastIndexOf(" - ");
            if (splitDash == -1) splitDash = roomFull.indexOf(" -"); // Fallback

            // Nếu vẫn không thấy, thử tìm chữ "CƠ SỞ" để tách
            if (splitDash == -1) {
                int coSoIdx = roomFull.toUpperCase().lastIndexOf("CƠ SỞ");
                if (coSoIdx != -1) splitDash = coSoIdx; // Cắt ngay trước chữ cơ sở
            }

            if (splitDash != -1) {
                String roomName = roomFull.substring(0, splitDash).trim();
                String location = roomFull.substring(splitDash).trim(); // Lấy từ dấu gạch ngang trở đi

                // Dọn dẹp tên phòng (bỏ dấu - ở cuối nếu có)
                if (roomName.endsWith("-")) roomName = roomName.substring(0, roomName.length() - 1).trim();

                // Dọn dẹp địa điểm
                if (location.startsWith("-")) location = location.substring(1).trim();
                if (location.toUpperCase().startsWith("CƠ SỞ")) {
                    location = location.substring(5).trim(); // Bỏ chữ "Cơ sở"
                    if (location.startsWith(":")) location = location.substring(1).trim(); // Bỏ dấu :
                }

                room.setRoomName(roomName);
                room.setLocation(location);
            } else {
                // Không tìm thấy dấu gạch ngang hay cơ sở -> Lưu hết vào tên phòng
                room.setRoomName(roomFull);
            }
        }
    }

    // =========================================================================
    // CÁC HÀM HELPER MỚI (MẠNH MẼ HƠN)
    // =========================================================================

    // 1. Lấy chuỗi sau từ khóa (tự động bỏ qua dấu : và khoảng trắng đầu)
    private String parseStringAndCut(String text, String keyword) {
        int idx = text.toUpperCase().indexOf(keyword.toUpperCase());
        if (idx == -1) return "";

        String sub = text.substring(idx + keyword.length()).trim();
        // Bỏ qua dấu : hoặc khoảng trắng liên tục ở đầu
        while (sub.startsWith(":") || sub.startsWith(" ")) {
            sub = sub.substring(1);
        }
        return sub.trim();
    }

    // 2. Cắt cụt chuỗi ngay khi gặp từ khóa rác đầu tiên
    private String cutStringAtKeywords(String text, String[] stopWords) {
        String resultUpper = text.toUpperCase();
        int minIndex = text.length(); // Mặc định là lấy hết chuỗi

        for (String stop : stopWords) {
            int idx = resultUpper.indexOf(stop.toUpperCase());
            // Nếu tìm thấy từ khóa rác VÀ nó không nằm ngay đầu dòng (tránh cắt nhầm chính nó)
            if (idx != -1 && idx < minIndex) {
                minIndex = idx;
            }
        }
        return text.substring(0, minIndex).trim();
    }

    // 3. Parse số nghiêm ngặt (Gặp chữ là dừng ngay)
    private Integer parseNumberStrict(String text, String keyword) {
        String valStr = parseStringAndCut(text, keyword);
        StringBuilder sb = new StringBuilder();
        for (char c : valStr.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            } else if (sb.length() > 0 && c != '.') {
                // Đang lấy số mà gặp chữ cái/ký tự lạ -> Dừng ngay
                break;
            } else if (sb.length() == 0 && Character.isLetter(c)) {
                // Chưa lấy được số nào mà đã gặp chữ -> Null
                break;
            }
        }
        try {
            return sb.length() > 0 ? (int) Double.parseDouble(sb.toString()) : null;
        } catch (NumberFormatException e) { return null; }
    }

    // 4. Dọn dẹp dấu thừa cuối câu (. , - :)
    private String removeTrailingChars(String text) {
        text = text.trim();
        while (text.endsWith("-") || text.endsWith(":") || text.endsWith(",") || text.endsWith(".")) {
            text = text.substring(0, text.length() - 1).trim();
        }
        return text;
    }

    // 5. Kiểm tra số (Giữ nguyên)
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        for (char c : str.toCharArray()) if (!Character.isDigit(c)) return false;
        return true;
    }
}