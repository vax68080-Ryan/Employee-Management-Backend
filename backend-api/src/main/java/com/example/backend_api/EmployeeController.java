package com.example.backend_api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "http://localhost:4200")
public class EmployeeController {

    private final EmployeeRepository repo;
    private final PasswordEncoder passwordEncoder;

    public EmployeeController(EmployeeRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    // 👇 新增這個 API：取得所有員工 (供下拉選單使用，不分頁)
    @GetMapping("/all")
    public List<Employee> getAllEmployeesList() {
        // 使用 findAll() 直接回傳所有資料
        return repo.findAll();
    }

    // 💡 批次加密 API：保留此功能以便未來若有大量明文資料導入時可修復
    @GetMapping("/batch-encrypt")
    public ResponseEntity<String> batchEncryptPasswords() {
        List<Employee> employees = repo.findAll();
        int count = 0;
        for (Employee emp : employees) {
            if (emp.getPassword() != null && !emp.getPassword().startsWith("$2a$")) {
                emp.setPassword(passwordEncoder.encode(emp.getPassword()));
                repo.save(emp);
                count++;
            }
        }
        return ResponseEntity.ok("成功將 " + count + " 筆員工密碼轉換為加密格式！");
    }

    // 💡 新增員工：統一進行密碼加密與預設等級設定
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Employee p) {
        if (p.getPassword() == null || p.getPassword().isEmpty()) {
            p.setPassword(passwordEncoder.encode("1234")); // 預設密碼
        } else {
            p.setPassword(passwordEncoder.encode(p.getPassword()));
        }

        if (p.getLevel() == null) {
            p.setLevel(2); // 預設一般員工
        }

        return ResponseEntity.ok(repo.save(p));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!repo.existsById(id))
            return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // 💡 更新員工：處理密碼加密邏輯
    @PutMapping("/{id}")
    public ResponseEntity<Employee> update(@PathVariable String id, @RequestBody Employee req) {
        return repo.findById(id).map(existingEmployee -> {
            req.setId(id);
            // 只有當密碼欄位被修改（且非隱藏符號）時才重新加密
            if (req.getPassword() != null && !req.getPassword().isEmpty() && !req.getPassword().equals("******")) {
                req.setPassword(passwordEncoder.encode(req.getPassword()));
            } else {
                // 若未改密碼，保留原有的加密雜湊值
                req.setPassword(existingEmployee.getPassword());
            }
            return ResponseEntity.ok(repo.save(req));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Page<Employee> getAllEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) LocalDate hireDate,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return repo.searchByCriteria(id, name, hireDate, department, address, phone, email, pageable);
    }

    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportExcel(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) LocalDate hireDate,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email) throws IOException {

        Pageable unpaged = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("id"));
        List<Employee> employees = repo.searchByCriteria(id, name, hireDate, department, address, phone, email, unpaged)
                .getContent();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("員工資料");
            Row headerRow = sheet.createRow(0);
            String[] headers = { "ID", "姓名", "部門", "等級", "入職日", "電話", "Email" };

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Employee p : employees) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getId());
                row.createCell(1).setCellValue(p.getName());
                row.createCell(2).setCellValue(p.getDepartment());
                row.createCell(3).setCellValue(p.getLevel() != null ? p.getLevel().toString() : "2");
                row.createCell(4).setCellValue(p.getHireDate() != null ? p.getHireDate().toString() : "");
                row.createCell(5).setCellValue(p.getPhone());
                row.createCell(6).setCellValue(p.getEmail());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            ByteArrayResource resource = new ByteArrayResource(out.toByteArray());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employees.xlsx")
                    .contentType(MediaType
                            .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(resource);
        }
    }

    @GetMapping("/stats")
    public List<DepartmentStats> getStats() {
        return repo.getDepartmentStats();
    }
}