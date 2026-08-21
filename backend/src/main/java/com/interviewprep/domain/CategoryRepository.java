package com.interviewprep.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByType(String type);

    @Query("SELECT c FROM Category c WHERE c.type = 'TOPIC' AND c.parent.id = :parentId ORDER BY c.name")
    List<Category> findByParentIdAndNameContainingIgnoreCase(@Param("parentId") Long parentId, @Param("search") String search);

    @Query("SELECT c FROM Category c WHERE c.type = 'TOPIC' AND c.parent.id = :parentId ORDER BY c.name")
    List<Category> findByParentIdOrderByTypeThenName(@Param("parentId") Long parentId);

    @Query("SELECT DISTINCT p FROM Category c JOIN c.parent p WHERE c.type = 'TOPIC' AND p IS NOT NULL ORDER BY p.name")
    List<Category> findDistinctParents();

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent WHERE c.id IN :ids ORDER BY c.type, c.name")
    List<Category> findByIdIn(@Param("ids") List<Long> ids);

    @Query("SELECT c FROM Category c WHERE c.type = :type AND c.id IN :ids ORDER BY c.name")
    List<Category> findByTypeAndIdList(@Param("type") String type, @Param("ids") List<Long> ids);

    @Query("SELECT DISTINCT p FROM Category c JOIN c.parent p ORDER BY p.type, p.name")
    List<Category> findCategoriesOrderedByTypeThenName();

    long countByParentIsNullAndType(String type);
}
