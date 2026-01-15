package com.example.backend_api;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

        // ✅ 修改：在 WHERE 子句中加入 department 的判斷
        @Query("SELECT p FROM Employee p WHERE " +
                        "(:id IS NULL OR p.id LIKE %:id%) AND " +
                        "(:name IS NULL OR p.name LIKE %:name%) AND " +
                        "(:hireDate IS NULL OR p.hireDate = :hireDate) AND " + // 👈 新增這行 (注意是用 = )
                        "(:department IS NULL OR p.department LIKE %:department%) AND " +
                        "(:address IS NULL OR p.address LIKE %:address%) AND " +
                        "(:phone IS NULL OR p.phone LIKE %:phone%) AND " +
                        "(:email IS NULL OR p.email LIKE %:email%)")
        Page<Employee> searchByCriteria(
                        @Param("id") String id,
                        @Param("name") String name,
                        @Param("hireDate") LocalDate hireDate, // 👈 新增參數 (注意型別是 LocalDate)
                        @Param("department") String department,
                        @Param("address") String address,
                        @Param("phone") String phone,
                        @Param("email") String email,
                        Pageable pageable);

        // 👇 新增這段統計查詢
        // 語法意思：依照部門分組，算出每個部門有幾個人
        @Query("SELECT new com.example.backend_api.DepartmentStats(p.department, COUNT(p)) " +
                        "FROM Employee p GROUP BY p.department")
        List<DepartmentStats> getDepartmentStats();
}