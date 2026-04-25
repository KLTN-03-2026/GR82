package com.example.exam_support_dtu.controller;


import com.example.exam_support_dtu.entity.Users;
import com.example.exam_support_dtu.repository.UsersRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;


import java.time.OffsetDateTime;
import java.util.Optional;


@Controller
public class AdminUsersController {
    private final UsersRepository usersRepository;

    public AdminUsersController(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }




    @GetMapping("/admin/users")
    public String showUserManagement(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Model model) {

        // 1. Lấy danh sách user phân trang (Sắp xếp mới nhất lên đầu)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Users> userPage = usersRepository.findAll(pageable);

        // 2. Lấy dữ liệu cho 4 ô Thống kê (Bento Stats)
        long totalUsers = usersRepository.count();
        long activeUsers = usersRepository.countByIsActiveTrue();
        long lockedUsers = usersRepository.countByIsActiveFalse();

        // Đếm user mới đăng ký trong 7 ngày qua
        OffsetDateTime sevenDaysAgo = OffsetDateTime.now().minusDays(7);
        long newUsers = usersRepository.countByCreatedAtAfter(sevenDaysAgo);

        // 3. Đẩy dữ liệu sang Thymeleaf
        model.addAttribute("userPage", userPage);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("lockedUsers", lockedUsers);
        model.addAttribute("newUsers", newUsers);

        return "admin-users";
    }

    // =========================================================
    // API 1: LẤY CHI TIẾT 1 USER ĐỂ HIỂN THỊ VÀO MODAL
    // =========================================================
    @GetMapping("/api/admin/users/{id}")
    @ResponseBody
    public ResponseEntity<?> getUserDetails(@PathVariable long id) {
        Optional<Users> userdt = usersRepository.findById(id);
        if (userdt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userdt.get());
    }

    // =========================================================
    // API 2: KHÓA / MỞ KHÓA TÀI KHOẢN (TOGGLE STATUS)
    // =========================================================
    @PostMapping("/api/admin/users/{id}/toggle-status")
    @ResponseBody
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        Optional<Users> userdt = usersRepository.findById(id);
        if (userdt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Users user = userdt.get();
        //Lật ngược trạng lại trạng thái
        user.setIsActive(!user.getIsActive());
        usersRepository.save(user);

        return ResponseEntity.ok().body("{\"message\": \"Cập nhật trạng thái thành công!\"}");
    }


    // =========================================================
    // API 3: UPLOAD AVATAR CHO USER
    // =========================================================
    @PostMapping("/api/admin/users/{id}/avatar")
    @ResponseBody
    public ResponseEntity<?> uploadAvatar(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Vui lòng chọn file ảnh!");
        }

        Optional<Users> userOpt = usersRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            // 1. Lấy đuôi file (vd: .jpg, .png)
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 2. Chặn định dạng lạ (Chỉ cho phép jpg, png, webp)
            String extLower = extension.toLowerCase();
            if(!extLower.equals(".jpg") && !extLower.equals(".jpeg") && !extLower.equals(".png") && !extLower.equals(".webp")) {
                return ResponseEntity.badRequest().body("Chỉ hỗ trợ file ảnh (JPG, PNG, WEBP)");
            }

            // 3. Tạo tên file mới bằng UUID để tránh trùng lặp hoàn toàn
            String newFileName = "user_" + id + "_" + UUID.randomUUID().toString() + extension;

            // 4. Đường dẫn thư mục lưu file thật trên ổ cứng (Giống như bạn setup)
            String uploadDir = "D:/Tracuulich/imgava/";
            Path uploadPath = Paths.get(uploadDir);

            // Tự động tạo thư mục nếu lỡ tay xóa mất
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 5. Lưu file vào ổ cứng
            Path filePath = uploadPath.resolve(newFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 6. Cập nhật Database (Lưu đường dẫn Web)
            Users user = userOpt.get();
            String webPath = "/imgava/" + newFileName;
            user.setAvatarUrl(webPath);
            usersRepository.save(user);

            // Trả về JSON chứa URL mới để Frontend lập tức hiển thị
            return ResponseEntity.ok().body("{\"message\": \"Upload thành công!\", \"avatarUrl\": \"" + webPath + "\"}");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Lỗi khi lưu file trên server.");
        }
    }

    // =========================================================
    // API 4: CẬP NHẬT THÔNG TIN NGƯỜI DÙNG
    // =========================================================
    @PostMapping("/api/admin/users/{id}/update")
    @ResponseBody
    public ResponseEntity<?> updateUserDetails(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Optional<Users> userOpt = usersRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Users user = userOpt.get();

        if (payload.containsKey("fullName")) user.setFullName(payload.get("fullName"));
        if (payload.containsKey("role")) user.setRole(payload.get("role"));
        if (payload.containsKey("studentCode")) user.setStudentCode(payload.get("studentCode"));
        if (payload.containsKey("phoneNumber")) user.setPhoneNumber(payload.get("phoneNumber"));
        if (payload.containsKey("faculty")) user.setFaculty(payload.get("faculty"));
        if (payload.containsKey("className")) user.setClass_name(payload.get("className"));

        usersRepository.save(user);

        return ResponseEntity.ok().body("{\"message\": \"Cập nhật thông tin thành công!\"}");
    }

    // =========================================================
    // API 5: THÊM MỚI NGƯỜI DÙNG (ADD USER)
    // =========================================================
    @PostMapping("/api/admin/users/add")
    @ResponseBody
    public ResponseEntity<?> addUser(@RequestBody Map<String, String> payload) {
        try {
            // 1. Kiểm tra Email rỗng
            String email = payload.get("email");
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("{\"message\": \"Vui lòng nhập Email!\"}");
            }
            String finalEmail = email.trim();

            // 👉 KIỂM TRA TRÙNG LẶP PROACTIVE (Trước khi lưu)
            if (usersRepository.existsByEmail(finalEmail)) {
                return ResponseEntity.badRequest().body("{\"message\": \"Lỗi: Email này đã được sử dụng!\"}");
            }

            // 2. Tạo User mới
            Users newUser = new Users();
            newUser.setEmail(finalEmail);

            // Xử lý MSSV rỗng thành null để tránh lỗi ràng buộc Unique của DB (nếu có)
            String mssv = payload.get("studentCode");
            newUser.setStudentCode((mssv != null && !mssv.trim().isEmpty()) ? mssv.trim() : null);

            newUser.setFullName(payload.get("fullName"));
            newUser.setPhoneNumber(payload.get("phoneNumber"));
            newUser.setFaculty(payload.get("faculty"));
            newUser.setClass_name(payload.get("className"));
            newUser.setRole(payload.getOrDefault("role", "student"));

            newUser.setProvider("local");
            newUser.setPasswordHash("dtu123456"); // Pass mặc định

            usersRepository.save(newUser);
            return ResponseEntity.ok().body("{\"message\": \"Thêm thành công! Mật khẩu mặc định: dtu123456\"}");

        } catch (Exception e) {
            e.printStackTrace(); // In lỗi ra console server để dễ debug
            return ResponseEntity.badRequest().body("{\"message\": \"Lỗi: Email này đã tồn tại trong hệ thống!\"}");
        }
    }







}

