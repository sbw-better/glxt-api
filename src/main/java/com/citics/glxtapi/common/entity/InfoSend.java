package com.citics.glxtapi.common.entity;

import lombok.Data;
import java.io.Serializable;

@Data
public class InfoSend implements Serializable {

    private static final long serialVersionUID = 4988262811381478313L;

    // 1|正式客户  99|抄送邮件
    private Long recvUserType;

    private String infoTo;

    private String infoFrom;

    private String title;

    private String content;

    private Long contentSize;

    private Long batchId;

}
