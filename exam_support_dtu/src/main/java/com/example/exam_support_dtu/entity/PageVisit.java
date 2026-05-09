package com.example.exam_support_dtu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "page_visits")
@Getter
@Setter
@NoArgsConstructor
public class PageVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "page_url")
    private String pageUrl;

    @Column(name = "visitor_ip")
    private String visitorIp;

    @Column(name = "visit_time")
    private LocalDateTime visitTime = LocalDateTime.now();
    
    public PageVisit(String pageUrl, String visitorIp) {
        this.pageUrl = pageUrl;
        this.visitorIp = visitorIp;
    }
}
