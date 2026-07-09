package com.citics.glxtapi.web.entity.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class InterfaceDemoVo implements Serializable {

    private static final long serialVersionUID = -5003208214854560082L;

    /**
     * 接口调用demo的url
     */
    private String url;

    /**
     * 接口调用demo的body
     */
    private String body;
}
