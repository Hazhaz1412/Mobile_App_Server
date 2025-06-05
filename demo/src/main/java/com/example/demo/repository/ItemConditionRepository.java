package com.example.demo.repository;

import com.example.demo.entity.ItemCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ItemConditionRepository extends JpaRepository<ItemCondition, Long> {
    List<ItemCondition> findAllByOrderByDisplayOrder();
}