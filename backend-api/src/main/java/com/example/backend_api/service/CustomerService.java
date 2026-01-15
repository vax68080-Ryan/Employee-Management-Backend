package com.example.backend_api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.backend_api.entity.Customer;
import com.example.backend_api.repository.CustomerRepository;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    public Customer addCustomer(Customer customer) {
        // 1. 取得目前資料庫中最大的 ID (例如 "C000000005")
        String lastId = customerRepository.findMaxId();

        // 2. 產出下一個編號 (例如 "C000000006")
        String nextId = generateId(lastId);

        // 3. 設定進實體類別並存檔
        customer.setId(nextId);
        return customerRepository.save(customer);
    }

    private String generateId(String lastId) {
        if (lastId == null || lastId.isEmpty()) {
            return "C000000001"; // 第一筆資料
        }

        // 截取數字部分 (跳過開頭的 C)，加 1 後格式化回 9 位數字並補零
        long num = Long.parseLong(lastId.substring(1)) + 1;
        return String.format("C%09d", num); // 格式化為 C + 9 位數，總長 10
    }
}