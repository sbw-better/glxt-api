package com.citics.glxtapi.web.entity.dto;

import com.citics.glxtapi.common.page.PageRequest;
import io.swagger.annotations.ApiModel;
import lombok.Data;

@Data
@ApiModel(value="租户权限dto", description="")
public class TenantAuthDTO extends PageRequest {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String tenant;

    private String name;

    private String token;

    private String ipConfig;

    private String apiName;

    private String apiCode;

    private String description;

}
