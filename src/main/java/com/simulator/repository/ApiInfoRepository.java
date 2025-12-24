package com.simulator.repository;

import com.simulator.entity.ApiInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 接口信息Repository
 * 
 * @author simulator
 * @date 2024
 */
@Repository
public interface ApiInfoRepository extends JpaRepository<ApiInfo, Long> {

    /**
     * 根据路径和方法查询接口
     */
    Optional<ApiInfo> findByPathAndMethod(String path, String method);

    /**
     * 根据路径模糊查询
     */
    Page<ApiInfo> findByPathContaining(String path, Pageable pageable);

    /**
     * 根据方法查询
     */
    Page<ApiInfo> findByMethod(String method, Pageable pageable);

    /**
     * 根据标签查询
     */
    Page<ApiInfo> findByTagsContaining(String tags, Pageable pageable);

    /**
     * 根据Swagger文档ID查询
     */
    List<ApiInfo> findBySwaggerId(Long swaggerId);
}


