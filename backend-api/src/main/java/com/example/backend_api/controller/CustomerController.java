package com.example.backend_api.controller;

import com.example.backend_api.EmployeeRepository;
import com.example.backend_api.entity.Customer;
import com.example.backend_api.repository.CustomerRepository;
import com.example.backend_api.service.CustomerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "http://localhost:4200")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    // 1. 新增 API (自動編號)
    @PostMapping
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customer) {
        Customer savedCustomer = customerService.addCustomer(customer);
        return ResponseEntity.ok(savedCustomer);
    }

    // 2. 查詢 + 分頁 API
    @GetMapping
    public Page<Customer> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "id,asc") String sort,
            @RequestParam Map<String, String> allParams) {

        String[] sortParams = sort.split(",");
        Sort.Direction direction = Sort.Direction.fromString(sortParams[1]);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        Specification<Customer> spec = createSpec(allParams);
        Page<Customer> customerPage = customerRepository.findAll(spec, pageable);

        customerPage.getContent().forEach(this::fillSalesEmployeeName);
        return customerPage;
    }

    // 3. 單筆查詢
    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable String id) {
        return customerRepository.findById(id)
                .map(customer -> {
                    fillSalesEmployeeName(customer);
                    return ResponseEntity.ok(customer);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // 4. 修改 API
    @PutMapping("/{id}")
    public Customer updateCustomer(@PathVariable String id, @RequestBody Customer customer) {
        customer.setId(id);
        return customerRepository.save(customer);
    }

    // 5. 刪除 API (單筆)
    @DeleteMapping("/{id}")
    public void deleteCustomer(@PathVariable String id) {
        customerRepository.deleteById(id);
    }

    // 6. 批次刪除 API
    @DeleteMapping("/batch")
    public ResponseEntity<?> deleteBatch(@RequestBody List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.badRequest().body("請選擇要刪除的編號");
        }
        customerRepository.deleteAllByIdInBatch(ids);
        return ResponseEntity.ok().build();
    }

    // ⭐⭐⭐ 新增：批次更新等級 API (解決 405 Method Not Allowed) ⭐⭐⭐
    @PatchMapping("/batch-level")
    public ResponseEntity<?> updateLevelBatch(@RequestBody Map<String, Object> payload) {
        try {
            List<String> ids = (List<String>) payload.get("ids");
            String newLevel = (String) payload.get("level");
            Boolean selectAllPages = (Boolean) payload.get("selectAllPages");
            Map<String, String> criteria = (Map<String, String>) payload.get("criteria");

            if (Boolean.TRUE.equals(selectAllPages)) {
                // 情境 A：全選所有頁面，根據搜尋條件更新
                List<Customer> allMatch = customerRepository.findAll(createSpec(criteria));
                allMatch.forEach(c -> c.setLevel(newLevel));
                customerRepository.saveAll(allMatch);
            } else {
                // 情境 B：僅更新勾選的特定 ID
                if (ids != null) {
                    ids.forEach(id -> customerRepository.findById(id).ifPresent(c -> {
                        c.setLevel(newLevel);
                        customerRepository.save(c);
                    }));
                }
            }
            return ResponseEntity.ok(Map.of("message", "更新成功"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "批次更新失敗: " + e.getMessage()));
        }
    }

    // 7. Excel 匯出 API
    @GetMapping("/export")
    public void exportToExcel(@RequestParam Map<String, String> allParams, HttpServletResponse response)
            throws IOException {
        List<Customer> customers = customerRepository.findAll(createSpec(allParams));

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("客戶資料清單");

        String[] columns = { "客戶編號", "聯絡人", "公司", "等級", "業務員", "電話", "Email", "最後聯絡日" };
        Row headerRow = sheet.createRow(0);

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
        }

        int rowNum = 1;
        for (Customer c : customers) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(c.getId());
            row.createCell(1).setCellValue(c.getName());
            row.createCell(2).setCellValue(c.getCompany());
            row.createCell(3).setCellValue(c.getLevel());
            row.createCell(4).setCellValue(c.getSalesEmployee());
            row.createCell(5).setCellValue(c.getPhone());
            row.createCell(6).setCellValue(c.getEmail());
            row.createCell(7).setCellValue(c.getLastContactDate() != null ? c.getLastContactDate().toString() : "");
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Customers_Report.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    // 💡 輔助方法：填入業務員姓名
    private void fillSalesEmployeeName(Customer customer) {
        if (StringUtils.hasText(customer.getSalesEmployee())) {
            employeeRepository.findById(customer.getSalesEmployee())
                    .ifPresent(emp -> customer.setSalesEmployeeName(emp.getName()));
        }
    }

    private Specification<Customer> createSpec(Map<String, String> params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (params != null) {
                if (StringUtils.hasText(params.get("ids"))) {
                    // 前端傳來的可能是 "C001,C002,C003" 這樣的字串
                    String[] ids = params.get("ids").split(",");
                    // 使用 SQL 的 IN 語法: WHERE id IN ('C001', 'C002'...)
                    predicates.add(root.get("id").in((Object[]) ids));
                }
                if (StringUtils.hasText(params.get("id")))
                    predicates.add(cb.like(root.get("id"), "%" + params.get("id") + "%"));
                if (StringUtils.hasText(params.get("name")))
                    predicates.add(cb.like(root.get("name"), "%" + params.get("name") + "%"));
                if (StringUtils.hasText(params.get("company")))
                    predicates.add(cb.like(root.get("company"), "%" + params.get("company") + "%"));
                if (StringUtils.hasText(params.get("level")))
                    predicates.add(cb.equal(root.get("level"), params.get("level")));
                if (StringUtils.hasText(params.get("salesEmployee"))) {
                    predicates.add(cb.equal(root.get("salesEmployee"), params.get("salesEmployee")));
                }
                if (StringUtils.hasText(params.get("lastContactDate"))) {
                    predicates
                            .add(cb.equal(root.get("lastContactDate").as(String.class), params.get("lastContactDate")));
                }
                if (StringUtils.hasText(params.get("phone")))
                    predicates.add(cb.like(root.get("phone"), "%" + params.get("phone") + "%"));
                if (StringUtils.hasText(params.get("email")))
                    predicates.add(cb.like(root.get("email"), "%" + params.get("email") + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}