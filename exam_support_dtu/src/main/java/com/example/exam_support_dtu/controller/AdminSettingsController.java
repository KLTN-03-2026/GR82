package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.annotation.LoggableAction;
import com.example.exam_support_dtu.entity.SystemSetting;
import com.example.exam_support_dtu.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {

    private final SystemSettingRepository systemSettingRepository;

    @GetMapping
    public String showSettings(Model model) {
        List<SystemSetting> settings = systemSettingRepository.findAll();

        // Map settings to model for easier access in Thymeleaf
        for (SystemSetting setting : settings) {
            model.addAttribute(setting.getKey().replace(".", "_"), setting.getValue());
        }

        // Default values if not in DB
        if (!model.containsAttribute("mail_default_notify_days"))
            model.addAttribute("mail_default_notify_days", "4");
        if (!model.containsAttribute("mail_auto_enabled"))
            model.addAttribute("mail_auto_enabled", "true");
        if (!model.containsAttribute("crawl_auto_pages"))
            model.addAttribute("crawl_auto_pages", "5");
        if (!model.containsAttribute("crawl_cron"))
            model.addAttribute("crawl_cron", "0 0 2 * * ?");
        if (!model.containsAttribute("mail_cron"))
            model.addAttribute("mail_cron", "0 0 6 * * ?");

        return "admin-settings";
    }

    @PostMapping("/save")
    @ResponseBody
    @LoggableAction(action = "UPDATE_SYSTEM_SETTINGS", targetType = "SYSTEM", details = "'Cập nhật cấu hình hệ thống: ' + #payload")
    public ResponseEntity<?> saveSettings(@RequestBody Map<String, String> payload) {
        try {
            payload.forEach((dbKey, value) -> {
                SystemSetting setting = systemSettingRepository.findById(dbKey)
                        .orElse(new SystemSetting());
                setting.setKey(dbKey);
                setting.setValue(value);

                // Cập nhật mô tả nếu là bản ghi mới
                if (setting.getDescription() == null || setting.getDescription().isEmpty()) {
                    if (dbKey.equals("mail.default_notify_days"))
                        setting.setDescription("Số ngày báo trước mặc định");
                    if (dbKey.equals("mail.auto_enabled"))
                        setting.setDescription("Bật/Tắt gửi mail tự động");
                    if (dbKey.equals("crawl.auto_pages"))
                        setting.setDescription("Số trang cào tự động");
                    if (dbKey.equals("crawl.cron"))
                        setting.setDescription("Lịch chạy cào dữ liệu");
                    if (dbKey.equals("mail.cron"))
                        setting.setDescription("Lịch chạy gửi mail");
                }

                systemSettingRepository.save(setting);
            });
            return ResponseEntity.ok(Map.of("status", "success", "message", "Cấu hình đã được lưu thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Lỗi: " + e.getMessage()));
        }
    }
}
