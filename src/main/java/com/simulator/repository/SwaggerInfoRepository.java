package com.simulator.repository;

import com.simulator.entity.SwaggerInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Swagger文档信息Repository
 * 
 * @author simulator
 * @date 2024
 */
@Repository
public interface SwaggerInfoRepository extends JpaRepository<SwaggerInfo, Long> {

    /**
     * 根据标题查询
     */
    List<SwaggerInfo> findByTitle(String title);

    /**
     * 根据版本查询
     */
    List<SwaggerInfo> findByVersion(String version);
}

