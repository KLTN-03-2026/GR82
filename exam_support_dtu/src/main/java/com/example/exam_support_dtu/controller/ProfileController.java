package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.entity.Users;
import com.example.exam_support_dtu.service.ProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@Controller
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping({"/profile", "/user/profile"})
    public String showProfilePage(@AuthenticationPrincipal Users currentUser, Model model) {
        Users user = profileService.getProfile(currentUser.getId());
        if (user == null) user = currentUser;
        model.addAttribute("user", user);

        // Kiểm tra role để trả về view tương ứng
        if ("admin".equalsIgnoreCase(user.getRole())) {
            return "admin-profile";
        } else {
            return "user-profile";
        }
    }

    @PostMapping("/api/profile/update")
    @ResponseBody
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal Users currentUser,
            @RequestBody Map<String, String> payload) {
        try {
            profileService.updateProfileInfo(currentUser.getId(), payload);
            return ResponseEntity.ok().body("{\"message\": \"Cập nhật thông tin thành công!\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/api/profile/avatar")
    @ResponseBody
    public ResponseEntity<?> uploadAvatar(
            @AuthenticationPrincipal Users currentUser,
            @RequestParam("file") MultipartFile file) {
        try {
            String webPath = profileService.updateAvatar(currentUser.getId(), file);
            return ResponseEntity.ok().body("{\"message\": \"Upload thành công!\", \"avatarUrl\": \"" + webPath + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/api/profile/change-password")
    @ResponseBody
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal Users currentUser,
            @RequestBody Map<String, String> payload) {
        try {
            String oldPassword = payload.get("oldPassword");
            String newPassword = payload.get("newPassword");
            profileService.changePassword(currentUser.getId(), oldPassword, newPassword);
            return ResponseEntity.ok().body("{\"message\": \"Đổi mật khẩu thành công!\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"message\": \"" + e.getMessage() + "\"}");
        }
    }
}
