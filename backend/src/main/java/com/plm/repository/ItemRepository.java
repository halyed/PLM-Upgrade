package com.plm.repository;

import com.plm.entity.Item;
import com.plm.entity.LifecycleState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    Optional<Item> findByItemNumber(String itemNumber);
    boolean existsByItemNumber(String itemNumber);
    List<Item> findByLifecycleState(LifecycleState lifecycleState);
    List<Item> findByNameContainingIgnoreCase(String name);

    @org.springframework.data.jpa.repository.Query(
        "SELECT i FROM Item i WHERE LOWER(i.itemNumber) LIKE LOWER(CONCAT('%',:q,'%')) " +
        "OR LOWER(i.name) LIKE LOWER(CONCAT('%',:q,'%')) " +
        "OR LOWER(i.description) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<Item> search(@org.springframework.data.repository.query.Param("q") String q);
}
