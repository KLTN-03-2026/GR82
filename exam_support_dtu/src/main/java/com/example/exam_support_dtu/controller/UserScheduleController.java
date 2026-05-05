package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.dto.RoomDto;
import com.example.exam_support_dtu.dto.ScheduleDto;
import com.example.exam_support_dtu.dto.UserExamResultDto;
import com.example.exam_support_dtu.entity.ExamRoom;
import com.example.exam_support_dtu.entity.ExamSchedule;
import com.example.exam_support_dtu.entity.ExamStudent;
import com.example.exam_support_dtu.repository.ExamRoomRepository;
import com.example.exam_support_dtu.repository.ExamScheduleRepository;
import com.example.exam_support_dtu.repository.ExamStudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class UserScheduleController {

    private final ExamScheduleRepository examScheduleRepository;
    private final ExamRoomRepository examRoomRepository;
    private final ExamStudentRepository examStudentRepository;

    public UserScheduleController(ExamScheduleRepository examScheduleRepository,
                                  ExamRoomRepository examRoomRepository,
                                  ExamStudentRepository examStudentRepository) {
        this.examScheduleRepository = examScheduleRepository;
        this.examRoomRepository = examRoomRepository;
        this.examStudentRepository = examStudentRepository;
    }

    // =========================================================
    // 1. HIỂN THỊ TRANG LỊCH THI USER
    // =========================================================
    @GetMapping("/user/schedules")
    public String showUserSchedulePage(
            @RequestParam(required = false) String studentCode,
            @RequestParam(required = false) String courseFilter,
            @RequestParam(required = false) String semester,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        List<UserExamResultDto> examResults = new ArrayList<>();

        // Nếu sinh viên nhập mã SV thì ưu tiên tra cứu theo mã SV
        if (studentCode != null && !studentCode.trim().isEmpty()) {
            List<ExamStudent> students = examStudentRepository.findByStudentCodeContainingIgnoreCase(studentCode.trim());

            for (ExamStudent student : students) {
                ExamRoom room = student.getExamRoom();

                if (room == null) {
                    continue;
                }

                ExamSchedule schedule = room.getExamSchedule();

                if (schedule == null) {
                    continue;
                }

                // Lọc theo môn học nếu người dùng nhập courseFilter
                if (courseFilter != null && !courseFilter.trim().isEmpty()) {
                    String keyword = courseFilter.trim().toLowerCase();

                    boolean matchCourseCode = schedule.getCourseCode() != null
                            && schedule.getCourseCode().toLowerCase().contains(keyword);

                    boolean matchCourseName = schedule.getCourseName() != null
                            && schedule.getCourseName().toLowerCase().contains(keyword);

                    if (!matchCourseCode && !matchCourseName) {
                        continue;
                    }
                }

                // Lọc theo học kỳ nếu có chọn semester
                if (semester != null && !semester.trim().isEmpty()) {
                    if (schedule.getSemester() == null
                            || !schedule.getSemester().equalsIgnoreCase(semester.trim())) {
                        continue;
                    }
                }

                UserExamResultDto dto = new UserExamResultDto(
                        schedule.getId(),
                        room.getId(),
                        student.getStudentCode(),
                        student.getLastName() + " " + student.getFirstName(),
                        student.getStudentClass(),
                        student.getClassCode(),
                        student.getSeatNumber(),
                        schedule.getCourseCode(),
                        schedule.getCourseName(),
                        schedule.getCredit(),
                        schedule.getSemester(),
                        schedule.getAttempt(),
                        room.getRoomName(),
                        room.getLocation(),
                        room.getExamDate(),
                        room.getExamTime(),
                        student.getNote()
                );

                examResults.add(dto);
            }

            model.addAttribute("examResults", examResults);
        } else {
            // Nếu chưa nhập mã SV, hiển thị danh sách lịch thi tổng quát có phân trang
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
            Page<ExamSchedule> schedulePage = examScheduleRepository.findAll(pageable);

            model.addAttribute("schedulePage", schedulePage);
            model.addAttribute("examResults", examResults);
        }

        model.addAttribute("studentCode", studentCode);
        model.addAttribute("courseFilter", courseFilter);
        model.addAttribute("semester", semester);
        model.addAttribute("totalResults", examResults.size());

        return "user-schedules";
    }

    // ==========================================
    // 2. READ - XEM CHI TIẾT LỊCH THI
    // ==========================================
    @GetMapping("/api/user/schedules/{id}")
    @ResponseBody
    public ResponseEntity<ScheduleDto> getUserScheduleDetail(@PathVariable Long id) {

        Optional<ExamSchedule> optionalSchedule = examScheduleRepository.findById(id);

        if (optionalSchedule.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ExamSchedule schedule = optionalSchedule.get();

        List<RoomDto> roomDtoList = new ArrayList<>();

        if (schedule.getRooms() != null) {
            for (ExamRoom examRoom : schedule.getRooms()) {
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
    // 3. LẤY DANH SÁCH SINH VIÊN THEO PHÒNG THI
    // ==========================================
    @GetMapping("/api/user/rooms/{roomId}/students")
    @ResponseBody
    public ResponseEntity<?> getStudentsByRoom(@PathVariable Long roomId) {

        Optional<ExamRoom> optionalExamRoom = examRoomRepository.findById(roomId);

        if (optionalExamRoom.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ExamRoom examRoom = optionalExamRoom.get();

        List<Object> studentList = new ArrayList<>();

        if (examRoom.getStudents() != null) {
            examRoom.getStudents().forEach(st -> {
                studentList.add(new Object() {
                    public final String studentCode = st.getStudentCode();
                    public final String fullName = st.getLastName() + " " + st.getFirstName();
                    public final String studentClass = st.getStudentClass();
                    public final String classCode = st.getClassCode();
                    public final String seatNumber = st.getSeatNumber();
                });
            });
        }

        return ResponseEntity.ok(studentList);
    }
}