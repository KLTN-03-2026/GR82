package com.example.exam_support_dtu.interceptor;

import com.example.exam_support_dtu.entity.PageVisit;
import com.example.exam_support_dtu.repository.PageVisitRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PageVisitInterceptor implements HandlerInterceptor {

    private final PageVisitRepository pageVisitRepository;

    public PageVisitInterceptor(PageVisitRepository pageVisitRepository) {
        this.pageVisitRepository = pageVisitRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        
        // Chỉ lưu các lượt truy cập vào các trang HTML chính (không lưu file tĩnh, API hoặc CSS)
        if (uri.startsWith("/user/") || uri.startsWith("/admin/") || uri.equals("/") || uri.equals("/index")) {
            if (!uri.startsWith("/api/")) {
                // Kiểm tra xem session hiện tại đã được tính lượt truy cập chưa
                var session = request.getSession(true);
                if (session.getAttribute("VISIT_RECORDED") == null) {
                    String ip = request.getRemoteAddr();
                    pageVisitRepository.save(new PageVisit(uri, ip));
                    
                    // Đánh dấu đã ghi nhận để không đếm lại trong cùng 1 lần đăng nhập
                    session.setAttribute("VISIT_RECORDED", true);
                }
            }
        }
        return true;
    }
}
