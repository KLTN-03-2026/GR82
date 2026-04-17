package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.dto.RoomDto;
import com.example.exam_support_dtu.dto.ScheduleDto;
import com.example.exam_support_dtu.entity.ExamRoom;
import com.example.exam_support_dtu.entity.ExamSchedule;
import com.example.exam_support_dtu.repository.ExamRoomRepository;
import com.example.exam_support_dtu.repository.ExamScheduleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class AdminScheduleController {
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamRoomRepository examRoomRepository;
    public AdminScheduleController(ExamScheduleRepository examScheduleRepository, ExamRoomRepository examRoomRepository) {
        this.examScheduleRepository = examScheduleRepository;
        this.examRoomRepository = examRoomRepository;
    }

    // =========================================================
    // PHẦN 1: TRẢ VỀ GIAO DIỆN THYMELEAF (Không có @ResponseBody)
    // =========================================================

    @GetMapping("/admin/schedules")
    public String showSchedulePage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        // Tạo Pageable để phân trang (sắp xếp ID giảm dần cho mới nhất lên đầu)
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        // Lấy dữ liệu phân trang từ DB
        Page<ExamSchedule> schedulePage = examScheduleRepository.findAll(pageable);

        // Đẩy biến "schedulePage" sang cho file HTML Thymeleaf đọc
        model.addAttribute("schedulePage", schedulePage);

        // Trả về tên file HTML
        return "admin-schedules";
    }



    // ==========================================
    // 2. READ (XEM CHI TIẾT)
    // ==========================================

    @GetMapping("/api/admin/schedules/{id}")
    @ResponseBody //Báo cho Spring biết hàm này trả JSON
    public ResponseEntity<ScheduleDto> getScheduleDetail(@PathVariable Long id) {
        //Tìm lịch thi trong db
        Optional<ExamSchedule> optionalSchedule  = examScheduleRepository.findById(id);

        //kiểm tra căn bản: nếu k tìm thấy thì trả về lỗi 404
        if(optionalSchedule.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        //Lấy đối tượng ra optional
        ExamSchedule schedule = optionalSchedule.get();

        //tạo 1 danh sách rỗng để chứa DTO của phòng thi
        List<RoomDto> roomDtoList = new ArrayList<>();

        //Duyệt qua từng phòng thi
        if(schedule.getRooms() != null) {
            for(ExamRoom examRoom : schedule.getRooms()) {
                //Đóng gói từng phòng vào DTO
                RoomDto roomDto = new RoomDto(
                        examRoom.getId(),
                        examRoom.getRoomName(),
                        examRoom.getLocation(),
                        examRoom.getExamDate(),
                        examRoom.getExamTime(),
                        examRoom.getCapacity()
                );
                roomDtoList.add(roomDto);
            }
        }

        //Đóng gói tất cả vào ScheduleDto tổng
        ScheduleDto scheduleDto = new ScheduleDto(
                schedule.getId(),
                schedule.getCourseCode(),
                schedule.getCourseName(),
                schedule.getCredit(),
                schedule.getSemester(),
                schedule.getAttempt(),
                schedule.getTotalStudents(),
                schedule.getNotes(),
                roomDtoList
        );
        return ResponseEntity.ok(scheduleDto);
    }


    // ==========================================
    // 3. UPDATE (CẬP NHẬT)
    // ==========================================
    @PutMapping("/api/admin/schedules/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> updateSchedule(@PathVariable Long id,
                                            @RequestBody ScheduleDto scheduleDto) {
        try {
            // 1. Kiểm tra Lịch thi có tồn tại không
            Optional<ExamSchedule> optionalSchedule = examScheduleRepository.findById(id);
            if (optionalSchedule.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ExamSchedule schedule = optionalSchedule.get();

            // 2. Cập nhật thông tin gốc của Lịch thi
            schedule.setCourseCode(scheduleDto.getCourseCode());
            schedule.setCourseName(scheduleDto.getCourseName());
            schedule.setCredit(scheduleDto.getCredit());
            schedule.setSemester(scheduleDto.getSemester());
            schedule.setAttempt(scheduleDto.getAttempt());
            schedule.setTotalStudents(scheduleDto.getTotalStudents());
            schedule.setNotes(scheduleDto.getNotes());

            examScheduleRepository.save(schedule);

            // ========================================================
            // 3. CẬP NHẬT THÔNG TIN PHÒNG THI
            // ========================================================
            if (scheduleDto.getRooms() != null) {
                for (RoomDto roomDto : scheduleDto.getRooms()) {

                    // Chỉ xử lý những phòng thi có ID (nghĩa là đã tồn tại trong DB)
                    if (roomDto.getId() != null) {
                        Optional<ExamRoom> optRoom = examRoomRepository.findById(roomDto.getId());

                        if (optRoom.isPresent()) {
                            ExamRoom existingRoom = optRoom.get();

                            // Cập nhật thông tin phòng thi
                            existingRoom.setRoomName(roomDto.getRoomName());
                            existingRoom.setLocation(roomDto.getLocation());
                            existingRoom.setExamDate(roomDto.getExamDate());
                            existingRoom.setExamTime(roomDto.getExamTime());
                            existingRoom.setCapacity(roomDto.getCapacity());

                            // Lưu lại -> Hibernate sẽ chạy lệnh UPDATE vì existingRoom đã có ID
                            examRoomRepository.save(existingRoom);
                        }
                    }
                }
            }

            return ResponseEntity.ok().body("{\"message\": \"Cập nhật thành công!\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // ==========================================
    // 4. DELETE (XÓA)
    // ==========================================
    @DeleteMapping("/api/admin/schedules/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> deleteSchedule(@PathVariable Long id) {
        if(!examScheduleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        // xóa con trước cha sau
        examRoomRepository.deleteByExamScheduleId(id);
        examScheduleRepository.deleteById(id);

        return ResponseEntity.ok().body("{\"message\": \"Xóa thành công!\"}");
    }

    // ==========================================
    // 5. LẤY DANH SÁCH SINH VIÊN THEO PHÒNG THI
    // ==========================================
    @GetMapping("/api/admin/rooms/{roomId}/students")
    @ResponseBody
    public ResponseEntity<?> getStudentsByRoom(@PathVariable Long roomId) {
        Optional<ExamRoom>  optionalExamRoom = examRoomRepository.findById(roomId);
        if(optionalExamRoom.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        //lấy ra
        ExamRoom examRoom = optionalExamRoom.get();

        List<Object> studentList = new ArrayList<>();
        if(examRoom.getStudents() != null) {
            examRoom.getStudents().forEach(st -> {
                // Tạo DTO ẩn danh để tránh lỗi đệ quy JSON
                studentList.add(new Object() {
                    public final String studentCode = st.getStudentCode();
                    public final String fullName = st.getLastName() + " " + st.getFirstName();
                    public final String studentClass = st.getStudentClass();
                    public final String seatNumber = st.getSeatNumber();
                });
            });
        }
        return ResponseEntity.ok(studentList);
    }

}