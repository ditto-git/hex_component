package com.ditto.hex_component.hex_import.importTemp;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Dict {

    public Dict(String code, String parentCode, String type) {
        this.code = code;
        this.type = type;
        this.parentCode = parentCode;
    }
    public Dict(String code, String parentCode) {
        this.code = code;
        this.parentCode = parentCode;
    }

    private String code;

    private String type;

    private long sort;

    private String parentCode;

    private long parentSort;




}
