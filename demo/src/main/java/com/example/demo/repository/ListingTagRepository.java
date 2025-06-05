package com.example.demo.repository;

import com.example.demo.entity.ListingTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ListingTagRepository extends JpaRepository<ListingTag, Long> {
    List<ListingTag> findByListingId(Long listingId);
    void deleteByListingId(Long listingId);
}