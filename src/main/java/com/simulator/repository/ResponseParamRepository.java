package com.simulator.repository;

import com.simulator.entity.ResponseParam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 响应参数Repository
 * 
 * @author simulator
 * @date 2024
 */
@Repository
public interface ResponseParamRepository extends JpaRepository<ResponseParam, Long> {

    /**
     * 根据接口ID查询所有响应参数
     */
    List<ResponseParam> findByApiId(Long apiId);

    /**
     * 根据接口ID和状态码查询响应参数
     */
    List<ResponseParam> findByApiIdAndStatusCode(Long apiId, String statusCode);

    /**
     * 根据接口ID和位置查询响应参数
     */
    List<ResponseParam> findByApiIdAndLocation(Long apiId, String location);

    /**
     * 根据接口ID、状态码和位置查询响应参数
     */
    List<ResponseParam> findByApiIdAndStatusCodeAndLocation(Long apiId, String statusCode, String location);

    /**
     * 根据接口ID删除所有响应参数
     */
    void deleteByApiId(Long apiId);
}


