package com.citics.glxtapi.web.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TenantInterfaceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long authId;

    private Long apiId;

    private String apiName;

    private String apiCode;

    private String previewSql;

}
