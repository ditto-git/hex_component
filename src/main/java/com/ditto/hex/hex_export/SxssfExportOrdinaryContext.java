package com.ditto.hex.hex_export;

import com.ditto.hex.hex_util.ExThreadLocal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class SxssfExportOrdinaryContext {

    @Autowired
    Map<String,SxssfExportOrdinary> SxssfExportOrdinary;


    public SxssfExportOrdinary sxssfExportOrdinary(){
        return SxssfExportOrdinary.get("SxssfExportOrdinary"+ ExThreadLocal.getExTemplate().getTemplateType());
    }


}
