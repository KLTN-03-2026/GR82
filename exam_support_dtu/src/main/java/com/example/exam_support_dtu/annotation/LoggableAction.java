package com.example.exam_support_dtu.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation dùng để đánh dấu các method cần ghi Audit Log tự động qua AOP.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoggableAction {
    /**
     * Tên hành động (Ví dụ: CREATE_DOCUMENT, DELETE_SCHEDULE)
     */
    String action();

    /**
     * Loại thực thể (Ví dụ: DOCUMENT, EXAM_ROOM)
     */
    String targetType();

    /**
     * SpEL Expression để lấy ID của đối tượng từ tham số (Ví dụ: "#id" hoặc "#doc.id")
     * Nếu để trống, Aspect sẽ cố gắng lấy từ kết quả trả về hoặc tham số đầu tiên.
     */
    String targetId() default "";

    /**
     * Nội dung chi tiết bổ sung (Tùy chọn)
     */
    String details() default "";
}
