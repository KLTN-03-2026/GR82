////package com.example.exam_support_dtu.repository;
////
////import com.example.exam_support_dtu.entity.ExamStudent;
////import org.springframework.data.jpa.repository.JpaRepository;
////import org.springframework.stereotype.Repository;
////
////@Repository
////public interface ExamStudentRepository extends JpaRepository<ExamStudent, Long> {
////}
//package com.example.exam_support_dtu.repository;
//
//import com.example.exam_support_dtu.entity.ExamStudent;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
//@Repository
//public interface ExamStudentRepository extends JpaRepository<ExamStudent, Long> {
//
//    // Tìm chính xác theo mã sinh viên
//    List<ExamStudent> findByStudentCode(String studentCode);
//
//    // Tìm gần đúng, tiện cho trường hợp nhập thiếu/ký tự hoa thường khác nhau
//    List<ExamStudent> findByStudentCodeContainingIgnoreCase(String studentCode);
//}

//package com.example.exam_support_dtu.repository;
//
//import com.example.exam_support_dtu.entity.ExamStudent;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
//@Repository
//public interface ExamStudentRepository extends JpaRepository<ExamStudent, Long> {
//
//    List<ExamStudent> findByStudentCodeContainingIgnoreCase(String studentCode);
//}

package com.example.exam_support_dtu.repository;

import com.example.exam_support_dtu.entity.ExamStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamStudentRepository extends JpaRepository<ExamStudent, Long> {

    // Tìm gần đúng theo mã sinh viên, không phân biệt hoa/thường
    List<ExamStudent> findByStudentCodeContainingIgnoreCase(String studentCode);

    // Tìm chính xác theo mã sinh viên
    List<ExamStudent> findByStudentCode(String studentCode);
}