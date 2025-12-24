package com.simulator.repository;

import com.simulator.entity.ServerInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 服务器信息Repository
 * 
 * @author simulator
 */
@Repository
public interface ServerInfoRepository extends JpaRepository<ServerInfo, Long> {

    /**
     * 根据Swagger文档ID查询所有服务器
     */
    List<ServerInfo> findBySwaggerId(Long swaggerId);

    /**
     * 根据Swagger文档ID和服务器URL查询
     */
    Optional<ServerInfo> findBySwaggerIdAndServerUrl(Long swaggerId, String serverUrl);
}


