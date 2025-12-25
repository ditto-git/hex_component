package com.ditto.hex_component.hex_export;

import javax.servlet.http.HttpServletResponse;

public interface SxssfExportOrdinary {

    public void  export( HttpServletResponse response, GoExport goExport);
    public void  exportLocalFile( HttpServletResponse response, GoExport goExport);

}
