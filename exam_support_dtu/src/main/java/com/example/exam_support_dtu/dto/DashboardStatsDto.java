package com.example.exam_support_dtu.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    // Stat Boxes
    private long totalPageViews;
    private long newUsers;
    private long newInterests;
    private long pendingDocuments;
    private long failedEmails;
    private long crawlErrors;

    // Line Chart Data
    private List<String> labels; // Dates
    private List<Long> pageViewData;
    private List<Long> userData;
    private List<Long> interestData;

    // Doughnut Chart Data
    private long approvedDocs;
    private long rejectedDocs;
    private long pendingDocs;
}
