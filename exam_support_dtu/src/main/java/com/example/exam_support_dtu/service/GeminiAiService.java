package com.example.exam_support_dtu.service;

import com.example.exam_support_dtu.entity.AiChatLog;
import com.example.exam_support_dtu.entity.Users;
import com.example.exam_support_dtu.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiAiService {

    private final SystemSettingRepository systemSettingRepository;
    private final AiChatLogRepository aiChatLogRepository;
    private final UserRepository userRepository;
    private final ExamStudentRepository examStudentRepository;
    private final DocumentRepository documentRepository;

    @Value("${gemini.api.key:}")
    private String configApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=";

    public List<AiChatLog> getChatHistory(Long userId) {
        return aiChatLogRepository.findByUserIdOrderByCreatedAtAsc(userId);
    }

    public String chatAndSave(Long userId, String userMessage) {
        Users user = userRepository.findById(userId).orElse(null);
        String context = "";

        if (user != null) {
            context = buildUserContext(user);
        }

        String aiResponse = chat(userMessage, context);

        try {
            if (user != null) {
                AiChatLog log = new AiChatLog();
                log.setUser(user);
                log.setUserMessage(userMessage);
                log.setAiResponse(aiResponse);
                aiChatLogRepository.save(log);
            }
        } catch (Exception e) {
            System.err.println("Lỗi lưu chat log: " + e.getMessage());
        }

        return aiResponse;
    }

    private String buildUserContext(Users user) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- THÔNG TIN NGƯỜI DÙNG ---\n");
        sb.append("Họ tên: ").append(user.getFullName()).append("\n");
        sb.append("MSSV: ").append(user.getStudentCode()).append("\n");

        try {
            // Truy vấn lịch thi thông qua ExamStudent bằng studentCode
            var studentExams = examStudentRepository.findByStudentCode(user.getStudentCode());
            if (studentExams != null && !studentExams.isEmpty()) {
                sb.append("--- LỊCH THI CỦA BẠN ---\n");
                for (var es : studentExams) {
                    var room = es.getExamRoom();
                    if (room != null && room.getExamSchedule() != null) {
                        var schedule = room.getExamSchedule();
                        sb.append("- Môn: ").append(schedule.getCourseName())
                                .append(", Ngày: ").append(room.getExamDate())
                                .append(", Giờ: ").append(room.getExamTime())
                                .append(", Phòng: ").append(room.getRoomName())
                                .append(", SBD: ").append(es.getSeatNumber()).append("\n");
                    }
                }
            }
        } catch (Exception e) {
        }

        sb.append("--- THÔNG TIN HỆ THỐNG ---\n");
        sb.append("- Website: DTU Supports\n");
        sb.append("- Tính năng: Xem lịch thi, tải tài liệu học tập, theo dõi môn học.\n");

        return sb.toString();
    }

    private String chat(String userMessage, String context) {
        String apiKey = systemSettingRepository.findById("gemini.api_key")
                .map(com.example.exam_support_dtu.entity.SystemSetting::getValue)
                .orElse(configApiKey);

        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            return "Hệ thống AI chưa được cấu hình API Key. Vui lòng liên hệ Admin.";
        }

        try {
            String url = GEMINI_API_URL + apiKey;

            Map<String, Object> contents = new HashMap<>();
            Map<String, Object> parts = new HashMap<>();

            String systemPrompt = "Bạn là Trợ lý ảo của hệ thống DTU Supports, chuyên giải đáp thắc mắc về lịch thi, tài liệu học tập của Đại học Duy Tân. "
                    +
                    "Hãy trả lời ngắn gọn, thân thiện và chính xác. Tránh trả lời lan man không liên quan đến DTU.\n" +
                    "Dưới đây là ngữ cảnh liên quan đến người dùng hiện tại:\n" + context;

            String fullPrompt = systemPrompt + "\nNgười dùng hỏi: " + userMessage;

            parts.put("text", fullPrompt);
            contents.put("parts", List.of(parts));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(contents));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> resParts = (List<Map<String, Object>>) content.get("parts");
                    if (!resParts.isEmpty()) {
                        return (String) resParts.get(0).get("text");
                    }
                }
            }
            return "Rất tiếc, AI không phản hồi vào lúc này.";
        } catch (Exception e) {
            return "Lỗi kết nối AI: " + e.getMessage();
        }
    }
}
