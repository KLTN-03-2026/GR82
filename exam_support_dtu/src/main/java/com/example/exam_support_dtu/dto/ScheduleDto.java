package com.example.exam_support_dtu.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDto {
    private Long id;
    private String courseCode; // Mã môn
    private String courseName; // Tên môn
    private Integer credit;    // Tín chỉ (SQL SMALLINT dùng Integer trong Java là đẹp)
    private String semester;   // Học kỳ
    private Integer attempt;   // Lần thi (SQL SMALLINT -> Integer)
    private Integer totalStudents; // Tổng sinh viên
    private String notes;      // Ghi chú (Hình thức thi...)
    private List<RoomDto> rooms;
}