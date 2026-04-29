package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.entity.Users;
import com.example.exam_support_dtu.entity.VerificationToken;
import com.example.exam_support_dtu.repository.UserRepository;
import com.example.exam_support_dtu.repository.VerificationTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.exam_support_dtu.service.EmailService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Controller
public class RegisterController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Dùng để băm mật khẩu
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailService emailService;

    public RegisterController(UserRepository userRepository, PasswordEncoder passwordEncoder, VerificationTokenRepository verificationTokenRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificationTokenRepository = verificationTokenRepository;
        this.emailService = emailService;
    }

    // 1. Hiển thị form đăng ký
    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    // 2. Xử lý khi bấm nút Đăng Ký
    @PostMapping("/register")
    public String processRegister(@RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {

        // Kiểm tra xem 2 mật khẩu có khớp không
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addAttribute("error", "Mật khẩu nhập lại không khớp!");
            return "redirect:/register";
        }

        // Kiểm tra xem Email đã tồn tại trong DB chưa
        if (userRepository.findByEmail(email).isPresent()) {
            redirectAttributes.addAttribute("error", "Email này đã được sử dụng!");
            return "redirect:/register";
        }

        // Tạo User bị khóa
        Users newUser = new Users();
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        newUser.setPasswordHash(passwordEncoder.encode(password));
        newUser.setRole("student");
        newUser.setActive(false); // Khóa tài khoản
        Users savedUser = userRepository.save(newUser);

        // SINH MÃ OTP 6 SỐ NGẪU NHIÊN
        String otpCode = String.format("%06d", new Random().nextInt(999999));

        // Lưu vào DB
        VerificationToken verificationToken = new VerificationToken(otpCode, savedUser.getId());
        verificationTokenRepository.save(verificationToken);

        // GỬI MAIL CHỨA MÃ OTP
        try {
            String subject = "Mã xác thực tài khoản DTU";
            String body = "<h3>Xin chào " + fullName + "</h3>"
                    + "<p>Mã xác thực (OTP) của bạn là:</p>"
                    + "<h1 style='color: #b91c1c; letter-spacing: 5px;'>" + otpCode + "</h1>"
                    + "<p>Mã này có hiệu lực trong 1 giờ. Vui lòng không chia sẻ cho người khác.</p>";

            emailService.sendHtmlEmail(email, subject, body);

            // THAY ĐỔI LỚN NHẤT CHỖ NÀY: Chuyển hướng sang trang nhập OTP và truyền theo email
            redirectAttributes.addAttribute("email", email);
            return "redirect:/verify-otp";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addAttribute("error", "Lỗi gửi mail, vui lòng báo Admin.");
            return "redirect:/register";
        }
    }

    // =======================================================
    // 3. HIỂN THỊ TRANG NHẬP MÃ OTP
    // =======================================================
    @GetMapping("/verify-otp")
    public String showOtpPage(@RequestParam(value = "email", required = false) String email, Model model) {
        if (email == null || email.isEmpty()) {
            return "redirect:/login"; // Tránh ai đó tự gõ URL /verify-otp vào trình duyệt
        }
        model.addAttribute("email", email);
        return "verify-otp";
    }

    // =======================================================
    // 4. XỬ LÝ KHI NGƯỜI DÙNG BẤM NÚT "XÁC NHẬN" MÃ OTP
    // =======================================================
    @PostMapping("/verify-otp")
    public String verifyOtpSubmit(@RequestParam String email,
                                  @RequestParam String otp,
                                  RedirectAttributes redirectAttributes) {

        // 1. Tìm user theo email
        Optional<Users> optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty()) {
            redirectAttributes.addAttribute("error", "Không tìm thấy người dùng!");
            return "redirect:/login";
        }
        Users user = optUser.get();

        // 2. Tìm cái OTP trong DB khớp với User ID và cái mã người ta vừa gõ
        Optional<VerificationToken> optToken = verificationTokenRepository.findByUserIdAndToken(user.getId(), otp);

        if (optToken.isEmpty()) {
            redirectAttributes.addAttribute("error", "Mã xác thực không chính xác!");
            redirectAttributes.addAttribute("email", email); // Giữ lại email để họ nhập lại
            return "redirect:/verify-otp";
        }

        VerificationToken token = optToken.get();

        // 3. Kiểm tra hạn sử dụng
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            redirectAttributes.addAttribute("error", "Mã xác thực đã hết hạn!");
            redirectAttributes.addAttribute("email", email);
            return "redirect:/verify-otp";
        }

        // 4. MỌI THỨ OK -> MỞ KHÓA TÀI KHOẢN
        user.setActive(true);
        userRepository.save(user);

        // Xóa mã đi để không dùng lại được
        verificationTokenRepository.delete(token);

        // Chuyển về trang đăng nhập thành công
        redirectAttributes.addAttribute("success", "Đăng ký thành công! Mời bạn đăng nhập.");
        return "redirect:/login";
    }

    // =======================================================
    // 5. XỬ LÝ NÚT "GỬI LẠI MÃ OTP"
    // =======================================================
    @PostMapping("/resend-otp")
    public String resendOtp(@RequestParam String email, RedirectAttributes redirectAttributes) {

        Optional<Users> optUser = userRepository.findByEmail(email);

        // Nếu ai đó cố tình phá hoại bằng cách gửi email linh tinh
        if (optUser.isEmpty()) {
            return "redirect:/login";
        }

        Users user = optUser.get();

        // Nếu tài khoản đã xác thực rồi thì không cho gửi lại nữa
        if (user.isActive()) {
            redirectAttributes.addAttribute("success", "Tài khoản này đã được đăng ký. Mời bạn học đăng nhập");
            return "redirect:/login";
        }

        // 1. Xóa toàn bộ mã cũ của user này cho sạch DB
        verificationTokenRepository.deleteByUserId(user.getId());

        // 2. Sinh mã OTP mới
        String newOtp = String.format("%06d", new java.util.Random().nextInt(999999));
        VerificationToken newToken = new VerificationToken(newOtp, user.getId());
        verificationTokenRepository.save(newToken);

        // 3. Gửi lại Email
        try {
            String subject = "Mã xác thực tài khoản DTU";
            String body = "<h3>Xin chào " + user.getFullName() + "</h3>"
                    + "<p>Hệ thống vừa tạo một mã xác thực (OTP) <strong>MỚI</strong> cho bạn:</p>"
                    + "<h1 style='color: #b91c1c; letter-spacing: 5px;'>" + newOtp + "</h1>"
                    + "<p>Mã này có hiệu lực trong 1 giờ. Vui lòng nhập mã này vào trang xác thực.</p>";

            emailService.sendHtmlEmail(email, subject, body);

            // Báo thành công màu xanh lá
            redirectAttributes.addAttribute("success", "Đã gửi lại mã OTP mới. Vui lòng kiểm tra email (cả mục Thư Rác)!");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addAttribute("error", "Lỗi mạng khi gửi mail, vui lòng thử lại sau.");
        }

        // Nhớ gắn lại email lên URL để trang verify-otp.html còn biết đường hiển thị
        redirectAttributes.addAttribute("email", email);
        return "redirect:/verify-otp";
    }

}