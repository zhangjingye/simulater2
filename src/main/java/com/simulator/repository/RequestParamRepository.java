package com.simulator.repository;

import com.simulator.entity.RequestParam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 请求参数Repository
 * 
 * @author simulator
 * @date 2024
 */
@Repository
public interface RequestParamRepository extends JpaRepository<RequestParam, Long> {

    /**
     * 根据接口ID查询所有请求参数
     */
    List<RequestParam> findByApiId(Long apiId);

    /**
     * 根据接口ID和位置查询请求参数
     */
    List<RequestParam> findByApiIdAndLocation(Long apiId, String location);

    /**
     * 根据接口ID和参数类型查询请求参数
     */
    List<RequestParam> findByApiIdAndParamType(Long apiId, String paramType);

    /**
     * 根据接口ID删除所有请求参数
     */
    void deleteByApiId(Long apiId);
}


