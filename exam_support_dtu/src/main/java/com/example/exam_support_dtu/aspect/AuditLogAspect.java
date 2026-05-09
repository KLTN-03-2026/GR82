package com.example.exam_support_dtu.aspect;

import com.example.exam_support_dtu.annotation.LoggableAction;
import com.example.exam_support_dtu.entity.AuditLog;
import com.example.exam_support_dtu.entity.Users;
import com.example.exam_support_dtu.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;
    private final ExpressionParser parser = new SpelExpressionParser();

    @AfterReturning(pointcut = "@annotation(loggableAction)", returning = "result")
    public void logAction(JoinPoint joinPoint, LoggableAction loggableAction, Object result) {
        try {
            AuditLog log = new AuditLog();
            log.setAction(loggableAction.action());
            log.setTargetType(loggableAction.targetType());
            log.setLogLevel("SUCCESS");

            // 1. Lấy User hiện tại
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Users user) {
                log.setUserId(user.getId());
            }

            // 2. Lấy Target ID từ SpEL hoặc kết quả trả về
            Long targetId = extractTargetId(joinPoint, loggableAction, result);
            log.setTargetId(targetId);

            // 3. Xây dựng nội dung chi tiết (Details) từ SpEL nếu có
            String detailsExpr = loggableAction.details();
            String details = "";
            if (!detailsExpr.isEmpty()) {
                details = evaluateSpel(joinPoint, detailsExpr, result);
            } else {
                details = String.format("Thực hiện %s trên %s (ID: %s)", 
                        loggableAction.action(), loggableAction.targetType(), targetId);
            }
            log.setDetails(details);

            auditLogRepository.save(log);
        } catch (Exception e) {
            // Không để lỗi Logging làm hỏng Business Logic chính
            System.err.println("Lỗi khi ghi Audit Log: " + e.getMessage());
        }
    }

    private String evaluateSpel(JoinPoint joinPoint, String expression, Object result) {
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
            Object[] args = joinPoint.getArgs();
            
            for (int i = 0; i < args.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
            context.setVariable("result", result);
            
            return parser.parseExpression(expression).getValue(context, String.class);
        } catch (Exception e) {
            return expression; // Fallback trả về nguyên bản nếu lỗi parse
        }
    }

    private Long extractTargetId(JoinPoint joinPoint, LoggableAction annotation, Object result) {
        String spEl = annotation.targetId();
        
        // Nếu dùng SpEL
        if (!spEl.isEmpty()) {
            StandardEvaluationContext context = new StandardEvaluationContext();
            String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
            Object[] args = joinPoint.getArgs();
            
            for (int i = 0; i < args.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
            context.setVariable("result", result);
            
            try {
                Object value = parser.parseExpression(spEl).getValue(context);
                if (value instanceof Number n) return n.longValue();
            } catch (Exception e) {
                return null;
            }
        }

        // Fallback 1: Nếu kết quả trả về là Number (VD: Long id)
        if (result instanceof Number n) return n.longValue();

        // Fallback 2: Thử tìm trong tham số đầu tiên nếu là Long
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Number n) {
            return n.longValue();
        }

        return null;
    }
}
