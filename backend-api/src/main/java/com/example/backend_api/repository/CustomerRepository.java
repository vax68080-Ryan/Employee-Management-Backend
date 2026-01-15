package com.example.backend_api.repository;

import com.example.backend_api.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // 👈 必須引入
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
// ⭐ 修正處：繼承 JpaSpecificationExecutor 以支援 Specification 查詢
public interface CustomerRepository extends JpaRepository<Customer, String>, JpaSpecificationExecutor<Customer> {
    
    @Query("SELECT MAX(c.id) FROM Customer c")
    String findMaxId();
}