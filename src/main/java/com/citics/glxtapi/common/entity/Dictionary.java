package com.citics.glxtapi.common.entity;

import lombok.Data;
import java.io.Serializable;

@Data
public class Dictionary implements Serializable {

    private static final long serialVersionUID = -6699282914474817247L;

    /**
     * txtdm
     */
    private String fldm;

    private Integer ibm;

    private String cbm;

    private String note;

}
