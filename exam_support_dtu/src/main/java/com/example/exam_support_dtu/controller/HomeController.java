package com.example.exam_support_dtu.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    // 1. Hàm hiển thị Trang chủ tra cứu
    @GetMapping("/")
    public String showSearchPage() {
        // Trả về file index.html nằm trong thư mục src/main/resources/templates
        return "index";
    }

    // 2. Hàm xử lý khi Sinh viên bấm nút "Tra cứu"
    @GetMapping("/search")
    public String searchExam(@RequestParam("studentCode") String studentCode, Model model) {
        // 'Model' là chiếc túi để Controller bỏ dữ liệu vào và mang ra ngoài giao diện HTML

        // Tạm thời ta cứ gói mã SV mà người dùng nhập vào túi để mang sang trang sau
        model.addAttribute("studentCode", studentCode);

        // (Ở phần sau, chỗ này sẽ gọi hàm DB: List<ExamStudent> list = ... roi put vao model)

        // Trả về file result.html (Trang kết quả - ta sẽ làm ở bước tiếp theo)
        return "result";
    }
}