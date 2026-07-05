package com.interviewprep.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing {@link Category} entities in the hierarchical topic structure.
 * <p>
 * Categories form a tree-like hierarchy where each category can have a parent category, enabling
 * organization of interview topics into broad subject areas (e.g., "Data Structures" → "Arrays", "Trees").
 * This repository provides methods to query categories by type, search within parent categories,
 * and retrieve hierarchical relationships.
 * </p>
 * <p>
 * The category hierarchy is used throughout the application:
 * <ul>
 *   <li>Root categories represent broad subject areas (e.g., "Data Structures")</li>
 *   <li>Child categories with type {@code TOPIC} represent specific topics within each area</li>
 *   <li>The hierarchy enables filtering and searching of interview questions by topic</li>
 * </ul>
 * </p>
 *
 * @see Category
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Finds all categories with the specified type (e.g., "TOPIC", "SUBJECT").
     * <p>
     * This method is used to retrieve all categories of a particular classification level.
     * For example, finding all topics within a subject area by querying for type {@code "TOPIC"}.
     * </p>
     *
     * @param type the category type to filter by (e.g., "TOPIC", "SUBJECT")
     * @return list of categories matching the specified type
     */
    List<Category> findByType(String type);

    /**
     * Finds child topics within a parent category, filtered by name (case-insensitive).
     * <p>
     * This method supports search functionality by returning topics whose names contain the
     * search string, regardless of case. Results are ordered alphabetically by name.
     * </p>
     * <p>
     * Example: Searching for "array" within parent category ID 5 would return all TOPIC categories
     * under that parent with "array" in their name (e.g., "Arrays", "Array Operations").
     * </p>
     *
     * @param parentId the ID of the parent category to search within
     * @param search   the search string to match against topic names (case-insensitive)
     * @return list of matching TOPIC categories ordered by name
     */
    @Query("SELECT c FROM Category c WHERE c.type = 'TOPIC' AND c.parent.id = :parentId ORDER BY c.name")
    List<Category> findByParentIdAndNameContainingIgnoreCase(@Param("parentId") Long parentId, @Param("search") String search);

    /**
     * Finds all child topics within a parent category, ordered by type and name.
     * <p>
     * This method retrieves all TOPIC categories under the specified parent, sorted first by type
     * (for grouping) and then alphabetically by name. Used for displaying topic lists in the UI.
     * </p>
     *
     * @param parentId the ID of the parent category to retrieve topics for
     * @return list of TOPIC categories under the parent, ordered by type and name
     */
    @Query("SELECT c FROM Category c WHERE c.type = 'TOPIC' AND c.parent.id = :parentId ORDER BY c.name")
    List<Category> findByParentIdOrderByTypeThenName(@Param("parentId") Long parentId);

    /**
     * Finds all distinct parent categories that have child TOPIC categories.
     * <p>
     * This method identifies root-level subject areas (e.g., "Data Structures", "Algorithms") by
     * finding unique parents of TOPIC-type categories. Results are ordered alphabetically by name.
     * </p>
     * <p>
     * Used to populate the main navigation or subject selection UI, showing users the available
     * broad topic areas they can practice.
     * </p>
     *
     * @return list of distinct parent categories with TOPIC children, ordered by name
     */
    @Query("SELECT DISTINCT p FROM Category c JOIN c.parent p WHERE c.type = 'TOPIC' AND p IS NOT NULL ORDER BY p.name")
    List<Category> findDistinctParents();

    /**
     * Finds categories by their IDs, eagerly loading parent relationships.
     * <p>
     * This method uses a JOIN FETCH to load parent entities in the same query, avoiding N+1
     * query problems when accessing parent references. Results are ordered by type and name.
     * </p>
     * <p>
     * Used when displaying category details that require both the category and its parent information.
     * </p>
     *
     * @param ids list of category IDs to retrieve
     * @return list of categories with their parents loaded, ordered by type and name
     */
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent WHERE c.id IN :ids ORDER BY c.type, c.name")
    List<Category> findByIdIn(@Param("ids") List<Long> ids);

    /**
     * Finds categories by type and ID list.
     * <p>
     * This method filters categories by both their type classification and a specific set of IDs.
     * Results are ordered alphabetically by name. Used for retrieving specific categories within
     * a particular type (e.g., all TOPIC categories with given IDs).
     * </p>
     *
     * @param type the category type to filter by (e.g., "TOPIC")
     * @param ids  list of category IDs to retrieve
     * @return list of matching categories ordered by name
     */
    @Query("SELECT c FROM Category c WHERE c.type = :type AND c.id IN :ids ORDER BY c.name")
    List<Category> findByTypeAndIdList(@Param("type") String type, @Param("ids") List<Long> ids);

    /**
     * Finds all distinct parent categories ordered by type and name.
     * <p>
     * This method retrieves all root-level categories (those that have TOPIC children), sorted
     * first by type for grouping and then alphabetically by name. Used for displaying the main
     * subject navigation in the application UI.
     * </p>
     *
     * @return list of distinct parent categories ordered by type and name
     */
    @Query("SELECT DISTINCT p FROM Category c JOIN c.parent p ORDER BY p.type, p.name")
    List<Category> findAllDistinctParentsOrdered();
}
