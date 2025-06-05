package com.example.demo.service;

import com.example.demo.entity.ItemCondition;
import com.example.demo.repository.ItemConditionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemConditionService {
    
    @Autowired
    private ItemConditionRepository itemConditionRepository;
    
    public List<ItemCondition> getAllConditions() {
        return itemConditionRepository.findAllByOrderByDisplayOrder();
    }
    
    public ItemCondition findById(Long id) {
        return itemConditionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tình trạng không tồn tại!"));
    }
}