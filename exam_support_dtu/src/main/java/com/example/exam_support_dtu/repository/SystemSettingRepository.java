package com.example.exam_support_dtu.repository;

import com.example.exam_support_dtu.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting,String> {
}
