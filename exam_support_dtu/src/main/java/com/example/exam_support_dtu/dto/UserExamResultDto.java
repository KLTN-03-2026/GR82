package com.example.exam_support_dtu.dto;

import java.time.LocalDate;

public class UserExamResultDto {
    private Long scheduleId;
    private Long roomId;

    private String studentCode;
    private String fullName;
    private String studentClass;
    private String classCode;
    private String seatNumber;

    private String courseCode;
    private String courseName;
    private Integer credit;
    private String semester;
    private Integer attempt;

    private String roomName;
    private String location;
    private LocalDate examDate;
    private String examTime;

    private String note;

    public UserExamResultDto() {
    }

    public UserExamResultDto(Long scheduleId, Long roomId, String studentCode, String fullName,
                             String studentClass, String classCode, String seatNumber,
                             String courseCode, String courseName, Integer credit, String semester,
                             Integer attempt, String roomName, String location,
                             LocalDate examDate, String examTime, String note) {
        this.scheduleId = scheduleId;
        this.roomId = roomId;
        this.studentCode = studentCode;
        this.fullName = fullName;
        this.studentClass = studentClass;
        this.classCode = classCode;
        this.seatNumber = seatNumber;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.credit = credit;
        this.semester = semester;
        this.attempt = attempt;
        this.roomName = roomName;
        this.location = location;
        this.examDate = examDate;
        this.examTime = examTime;
        this.note = note;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public String getFullName() {
        return fullName;
    }

    public String getStudentClass() {
        return studentClass;
    }

    public String getClassCode() {
        return classCode;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public Integer getCredit() {
        return credit;
    }

    public String getSemester() {
        return semester;
    }

    public Integer getAttempt() {
        return attempt;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getLocation() {
        return location;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public String getExamTime() {
        return examTime;
    }

    public String getNote() {
        return note;
    }
}