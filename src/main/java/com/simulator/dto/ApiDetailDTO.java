package com.simulator.dto;

import lombok.Data;
import java.util.List;

/**
 * 接口详情DTO
 * 
 * @author simulator
 * @date 2024
 */
@Data
public class ApiDetailDTO {

    /**
     * 接口基础信息
     */
    private ApiInfoDTO apiInfo;

    /**
     * 请求参数列表
     */
    private List<RequestParamDTO> requestParams;

    /**
     * 响应参数列表
     */
    private List<ResponseParamDTO> responseParams;
}

