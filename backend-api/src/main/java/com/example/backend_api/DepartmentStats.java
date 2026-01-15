package com.example.backend_api;

public class DepartmentStats {
    private String department;
    private Long count;

    public DepartmentStats(String department, Long count) {
        this.department = department;
        this.count = count;
    }

    // Getters and Setters
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
}