package com.citics.glxtapi.web.entity.vo;

import com.citics.glxtapi.common.annotation.Dict;
import com.citics.glxtapi.web.entity.ApiInterface;
import com.citics.glxtapi.web.entity.ApiParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiInterfaceVO extends ApiInterface {
    private static final long serialVersionUID = 1L;

    @Dict(childConvert = "List", convertMode = "child")
    private List<ApiParam> apiParamList;
}
