package com.citics.glxtapi.web.entity.dto;

import com.citics.glxtapi.common.page.PageRequest;
import com.citics.glxtapi.web.entity.TenantInterface;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value="租户权限dto", description="")
public class TenantInterfaceDTO extends PageRequest {

    private static final long serialVersionUID = 1L;

    private Long authId;

    private List<TenantInterface> tenantInterfaceList;

}
