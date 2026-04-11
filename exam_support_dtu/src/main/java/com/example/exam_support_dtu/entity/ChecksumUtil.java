package com.example.exam_support_dtu.entity;


import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public class ChecksumUtil {

    public static String sha256(String filePath) {

        try {
            // 1. Tạo bộ máy tính checksum (SHA-256)
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

            // 2. Mở file
            Path path = Path.of(filePath);
            InputStream input = Files.newInputStream(path);

            // 3. Đọc file từng phần nhỏ
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                // 4. Đưa dữ liệu vừa đọc vào bộ tính checksum
                sha256.update(buffer, 0, bytesRead);
            }

            input.close();

            // 5. Lấy kết quả checksum (dạng byte[])
            byte[] hashBytes = sha256.digest();

            // 6. Chuyển byte[] → String để lưu DB
            return toHexString(hashBytes);

        } catch (Exception e) {
            throw new RuntimeException("Checksum failed", e);
        }
    }

    // Hàm phụ: đổi byte[] thành chuỗi dễ lưu
    private static String toHexString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}

