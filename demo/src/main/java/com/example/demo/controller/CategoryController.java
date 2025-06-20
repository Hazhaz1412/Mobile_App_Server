package com.example.demo.controller;

import com.example.demo.entity.Category;
import com.example.demo.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Get all active categories
     */
    @GetMapping("/listings/categories")
    public ResponseEntity<Map<String, Object>> getAllCategories() {
        try {
            List<Category> categories = categoryRepository.findByIsActiveTrueOrderByName();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categories);
            response.put("count", categories.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving categories: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get category by ID
     */
    @GetMapping("/categories/{id}")
    public ResponseEntity<Map<String, Object>> getCategoryById(@PathVariable Long id) {
        try {
            Category category = categoryRepository.findById(id).orElse(null);
            
            Map<String, Object> response = new HashMap<>();
            
            if (category != null) {
                response.put("success", true);
                response.put("data", category);
            } else {
                response.put("success", false);
                response.put("message", "Category not found");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving category: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Create new category (admin only)
     */
    @PostMapping("/categories")
    public ResponseEntity<Map<String, Object>> createCategory(@RequestBody Category category) {
        try {
            Category savedCategory = categoryRepository.save(category);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", savedCategory);
            response.put("message", "Category created successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error creating category: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Update category (admin only)
     */
    @PutMapping("/categories/{id}")
    public ResponseEntity<Map<String, Object>> updateCategory(@PathVariable Long id, @RequestBody Category categoryDetails) {
        try {
            Category category = categoryRepository.findById(id).orElse(null);
            
            Map<String, Object> response = new HashMap<>();
            
            if (category != null) {
                category.setName(categoryDetails.getName());
                category.setDescription(categoryDetails.getDescription());
                category.setIconUrl(categoryDetails.getIconUrl());
                category.setIsActive(categoryDetails.getIsActive());
                
                Category updatedCategory = categoryRepository.save(category);
                
                response.put("success", true);
                response.put("data", updatedCategory);
                response.put("message", "Category updated successfully");
            } else {
                response.put("success", false);
                response.put("message", "Category not found");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating category: " + e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}
