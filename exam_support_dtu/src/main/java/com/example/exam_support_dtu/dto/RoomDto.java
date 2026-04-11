package com.example.exam_support_dtu.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomDto {
    private Long id; // Rất quan trọng để Update phòng thi cụ thể

    private String roomName;  // Tên phòng (VD: 510 Nhà F)
    private String location;  // Vị trí / Cơ sở
    private LocalDate examDate; // Ngày thi (Kiểu Date trong SQL ánh xạ sang LocalDate)
    private String examTime;    // Ca thi / Giờ thi (Dạng chuỗi)
    private Integer capacity; // Tổng số
}