package com.example.exam_support_dtu.service;

import com.example.exam_support_dtu.entity.*;
import com.example.exam_support_dtu.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ExamInterestService {

    private final ExamInterestRepository examInterestRepository;
    private final ExamStudentRepository examStudentRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamRoomRepository examRoomRepository;
    private final UserRepository userRepository;
    private final ExamOriginalRepository examOriginalRepository;

    public ExamInterestService(ExamInterestRepository examInterestRepository,
            ExamStudentRepository examStudentRepository,
            ExamScheduleRepository examScheduleRepository,
            ExamRoomRepository examRoomRepository,
            UserRepository userRepository,
            ExamOriginalRepository examOriginalRepository) {
        this.examInterestRepository = examInterestRepository;
        this.examStudentRepository = examStudentRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examRoomRepository = examRoomRepository;
        this.userRepository = userRepository;
        this.examOriginalRepository = examOriginalRepository;
    }

    /**
     * Đồng bộ lịch thi cá nhân dựa trên MSSV của người dùng
     */
    @Transactional
    public void syncPersonalExams(Users user) {
        try {
            String studentCode = user.getStudentCode();
            if (studentCode == null || studentCode.trim().isEmpty())
                return;

            String trimmedCode = studentCode.trim();

            // 1. Xóa các bản ghi "Lịch thi của tôi" cũ không còn khớp với MSSV mới
            // (Phòng ngừa trường hợp đổi MSSV trong Profile)
            List<ExamInterest> currentMyExams = examInterestRepository.findMyExamsByUserId(user.getId());
            for (ExamInterest ei : currentMyExams) {
                if (ei.getExamStudent() != null
                        && !trimmedCode.equalsIgnoreCase(ei.getExamStudent().getStudentCode())) {
                    examInterestRepository.delete(ei);
                }
            }

            // 2. Tìm các bản ghi trong danh sách thi khớp với MSSV mới
            List<ExamStudent> students = examStudentRepository.findByStudentCode(trimmedCode);

            java.util.Set<Long> processedRoomIds = new java.util.HashSet<>();

            for (ExamStudent student : students) {
                ExamRoom room = student.getExamRoom();
                if (room == null || processedRoomIds.contains(room.getId()))
                    continue;

                processedRoomIds.add(room.getId());

                ExamSchedule schedule = room.getExamSchedule();
                if (schedule == null)
                    continue;

                // Kiểm tra xem đã có record cho phòng này chưa
                List<ExamInterest> existingList = examInterestRepository
                        .findAllByUserIdAndRoomId(user.getId(), room.getId());

                if (!existingList.isEmpty()) {
                    ExamInterest interest = existingList.get(0);
                    if (interest.getExamStudent() == null) {
                        interest.setExamStudent(student);
                        examInterestRepository.save(interest);
                    }
                } else {
                    ExamInterest interest = new ExamInterest();
                    interest.setUserId(user.getId());
                    interest.setExamRoom(room);
                    interest.setExamSchedule(schedule);
                    interest.setExamStudent(student);
                    examInterestRepository.save(interest);
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi đồng bộ lịch thi cá nhân: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách lịch thi cá nhân (tự động)
     */
    @Transactional(readOnly = true)
    public List<ExamInterest> getMyExams(Long userId) {
        return examInterestRepository.findMyExamsByUserId(userId);
    }

    /**
     * Lấy danh sách lịch thi quan tâm (thủ công)
     */
    @Transactional(readOnly = true)
    public List<ExamInterest> getFollowedExams(Long userId) {
        return examInterestRepository.findFollowedByUserId(userId);
    }

    /**
     * Theo dõi môn học thông minh:
     * 1. Kiểm tra xem đã theo dõi chưa (bất kể là tự động hay thủ công)
     * 2. Nếu chưa, kiểm tra xem MSSV của user có trong danh sách thi môn này không
     * 3. Nếu có -> Tự động gán phòng/SBD (Nâng cấp thành Lịch thi của tôi)
     * 4. Nếu không -> Lưu là theo dõi thủ công
     */
    @Transactional
    public ExamInterest followScheduleSmart(Long scheduleId, Users user) {
        // 1. Kiểm tra xem đã có bất kỳ record nào cho môn này chưa
        List<ExamInterest> existing = examInterestRepository
                .findAllByUserIdAndExamScheduleId(user.getId(), scheduleId);

        if (!existing.isEmpty()) {
            return existing.get(0); // Đã theo dõi rồi, trả về bản ghi đầu tiên
        }

        // 2. Tìm môn học
        ExamSchedule schedule = examScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy môn học!"));

        // 3. Kiểm tra xem user có lịch thi môn này không (Dựa vào MSSV)
        String studentCode = user.getStudentCode();
        if (studentCode != null && !studentCode.trim().isEmpty()) {
            Optional<ExamStudent> studentOpt = examStudentRepository
                    .findByStudentCodeAndScheduleId(studentCode.trim(), scheduleId);

            if (studentOpt.isPresent()) {
                ExamStudent student = studentOpt.get();
                // TỰ ĐỘNG NÂNG CẤP: Lưu đầy đủ thông tin cá nhân
                ExamInterest interest = new ExamInterest();
                interest.setUserId(user.getId());
                interest.setExamSchedule(schedule);
                interest.setExamRoom(student.getExamRoom());
                interest.setExamStudent(student);
                return examInterestRepository.save(interest);
            }
        }

        // 4. Nếu không có lịch thi cá nhân -> Lưu theo dõi thủ công
        ExamInterest interest = new ExamInterest();
        interest.setUserId(user.getId());
        interest.setExamSchedule(schedule);
        // examRoom và examStudent để null
        return examInterestRepository.save(interest);
    }

    /**
     * Theo dõi phòng thi cụ thể (Theo dõi cá nhân)
     */
    @Transactional
    public ExamInterest followRoom(Long scheduleId, Long roomId, Long userId) {
        // Kiểm tra xem đã theo dõi phòng này chưa
        Optional<ExamInterest> existing = examInterestRepository.findByUserIdAndRoomId(userId, roomId);
        if (existing.isPresent()) {
            return existing.get();
        }

        ExamInterest interest = new ExamInterest();
        interest.setUserId(userId);

        ExamSchedule schedule = examScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy môn học!"));
        interest.setExamSchedule(schedule);

        ExamRoom room = examRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng thi!"));
        interest.setExamRoom(room);

        // Tìm sinh viên tương ứng để lấy SBD nếu có
        com.example.exam_support_dtu.entity.Users user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getStudentCode() != null) {
            Optional<ExamStudent> studentOpt = examStudentRepository
                    .findByStudentCodeAndScheduleId(user.getStudentCode(), scheduleId);
            studentOpt.ifPresent(interest::setExamStudent);
        }

        return examInterestRepository.save(interest);
    }

    @Transactional
    public ExamInterest followOriginal(Long originalId, Long userId) {
        // Kiểm tra xem đã theo dõi file này chưa
        List<ExamInterest> existing = examInterestRepository.findAllByUserIdAndExamOriginalId(userId, originalId);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        ExamOriginal original = examOriginalRepository.findById(originalId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tệp gốc!"));

        ExamInterest interest = new ExamInterest();
        interest.setUserId(userId);
        interest.setExamOriginal(original);
        // examSchedule, examRoom, examStudent để null
        return examInterestRepository.save(interest);
    }

    /**
     * Cập nhật số ngày nhắc hẹn
     */
    @Transactional
    public boolean updateNotificationDays(Long interestId, Long userId, Integer days) {
        Optional<ExamInterest> opt = examInterestRepository.findById(interestId);
        if (opt.isPresent() && opt.get().getUserId().equals(userId)) {
            ExamInterest interest = opt.get();
            interest.setStudentNotifyDays(days);
            examInterestRepository.save(interest);
            return true;
        }
        return false;
    }
}