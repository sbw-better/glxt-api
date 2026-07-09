package com.citics.glxtapi.common.entity;

import lombok.Data;
import java.io.Serializable;

@Data
public class MassMessage implements Serializable {

    private static final long serialVersionUID = -7251242223961347856L;

    private Long id;

    private Long userId;

    private String title;

    private String content;

}
