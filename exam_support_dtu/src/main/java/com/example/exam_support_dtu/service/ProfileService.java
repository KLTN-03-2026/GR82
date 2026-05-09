package com.example.exam_support_dtu.service;

import com.example.exam_support_dtu.annotation.LoggableAction;
import com.example.exam_support_dtu.entity.Users;
import com.example.exam_support_dtu.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Users getProfile(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    @LoggableAction(action = "UPDATE_PROFILE", targetType = "USER", targetId = "#userId")
    public void updateProfileInfo(Long userId, Map<String, String> payload) throws Exception {
        Users user = userRepository.findById(userId).orElseThrow(() -> new Exception("Không tìm thấy người dùng"));

        if (payload.containsKey("fullName")) user.setFullName(payload.get("fullName"));
        if (payload.containsKey("studentCode")) user.setStudentCode(payload.get("studentCode"));
        if (payload.containsKey("phoneNumber")) user.setPhoneNumber(payload.get("phoneNumber"));
        if (payload.containsKey("faculty")) user.setFaculty(payload.get("faculty"));
        if (payload.containsKey("className")) user.setClass_name(payload.get("className"));

        userRepository.save(user);
    }

    @LoggableAction(action = "UPDATE_AVATAR", targetType = "USER", targetId = "#userId")
    public String updateAvatar(Long userId, MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new Exception("Vui lòng chọn file ảnh!");
        }

        Users user = userRepository.findById(userId).orElseThrow(() -> new Exception("Không tìm thấy người dùng"));

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String extLower = extension.toLowerCase();
        if (!extLower.equals(".jpg") && !extLower.equals(".jpeg") && !extLower.equals(".png") && !extLower.equals(".webp")) {
            throw new Exception("Chỉ hỗ trợ file ảnh (JPG, PNG, WEBP)");
        }

        String newFileName = "user_" + user.getId() + "_" + UUID.randomUUID().toString() + extension;
        String uploadDir = "D:/Tracuulich/imgava/";
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(newFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String webPath = "/imgava/" + newFileName;
        user.setAvatarUrl(webPath);
        userRepository.save(user);

        return webPath;
    }

    @LoggableAction(action = "CHANGE_PASSWORD", targetType = "USER", targetId = "#userId")
    public void changePassword(Long userId, String oldPassword, String newPassword) throws Exception {
        Users user = userRepository.findById(userId).orElseThrow(() -> new Exception("Không tìm thấy người dùng!"));

        if (oldPassword == null || newPassword == null || newPassword.trim().length() < 6) {
            throw new Exception("Mật khẩu mới phải từ 6 ký tự trở lên!");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new Exception("Mật khẩu hiện tại không đúng!");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
