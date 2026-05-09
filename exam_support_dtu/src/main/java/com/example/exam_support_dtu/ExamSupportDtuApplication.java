package com.example.exam_support_dtu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ExamSupportDtuApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExamSupportDtuApplication.class, args);
	}

}
