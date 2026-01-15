package com.example.backend_api;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin(origins = "http://localhost:4200")
public class DepartmentController {

    private final DepartmentRepo repo;

    public DepartmentController(DepartmentRepo repo) {
        this.repo = repo;
    }

    // 提供給前端抓取所有部門
    @GetMapping
    public List<Department> getAllDepartments() {
        return repo.findAll();
    }
}