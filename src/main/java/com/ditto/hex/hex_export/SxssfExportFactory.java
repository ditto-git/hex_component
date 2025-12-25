package com.ditto.hex.hex_export;

import java.io.InputStream;

import static com.ditto.hex.hex_util.ExConstants.*;


public class SxssfExportFactory {


    private SxssfExportFactory() {}

    public static SxssfExport create(InputStream inputStream, String templateType){

        if(TEMPLATE_TYPE_COlUMN.equals(templateType)){
            return  new SxssfExportColumn(inputStream);

        } else if (TEMPLATE_TYPE_ROW.equals(templateType)) {
            return  new SxssfExportRow(inputStream);
        }else {
            return null;
        }

    }

}
