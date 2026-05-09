package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.entity.AiChatLog;
import com.example.exam_support_dtu.entity.Users;
import com.example.exam_support_dtu.repository.UserRepository;
import com.example.exam_support_dtu.service.GeminiAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final GeminiAiService geminiAiService;
    private final UserRepository userRepository;

    @GetMapping("/history")
    public List<Map<String, String>> getHistory() {
        Users currentUser = getCurrentUser();
        if (currentUser == null) return new ArrayList<>();

        List<AiChatLog> history = geminiAiService.getChatHistory(currentUser.getId());
        return history.stream().flatMap(log -> {
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "text", log.getUserMessage()));
            messages.add(Map.of("role", "ai", "text", log.getAiResponse()));
            return messages.stream();
        }).collect(Collectors.toList());
    }

    @PostMapping
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        Users currentUser = getCurrentUser();
        
        if (currentUser == null) {
            return Map.of("reply", "Bạn cần đăng nhập để sử dụng tính năng này.");
        }

        String response = geminiAiService.chatAndSave(currentUser.getId(), message);
        return Map.of("reply", response);
    }

    private Users getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String email = auth.getName();
        return userRepository.findByEmail(email).orElse(null);
    }
}
