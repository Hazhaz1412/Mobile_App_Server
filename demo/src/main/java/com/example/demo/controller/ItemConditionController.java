package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.ItemCondition;
import com.example.demo.service.ItemConditionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conditions")
@CrossOrigin(origins = "*")
public class ItemConditionController {
    
    @Autowired
    private ItemConditionService itemConditionService;
    
    @GetMapping
    public ResponseEntity<ApiResponse> getAllConditions() {
        try {
            List<ItemCondition> conditions = itemConditionService.getAllConditions();
            return ResponseEntity.ok(new ApiResponse(
                true,
                "Lấy danh sách tình trạng thành công!",
                conditions
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(
                false,
                "Lỗi lấy danh sách tình trạng: " + e.getMessage()
            ));
        }
    }
}