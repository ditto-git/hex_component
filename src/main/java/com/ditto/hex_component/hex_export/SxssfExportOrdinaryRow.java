package com.ditto.hex_component.hex_export;

import com.ditto.hex_component.hex_exception.HexException;
import com.ditto.hex_component.hex_util.ExThreadLocal;
import com.ditto.hex_component.hex_util.oss.OSSInputOperate;
import com.ditto.hex_component.hex_util.oss.OSSUtil;
import com.ditto.hex_component.hex_util.request.ExportFileResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

import static com.ditto.hex_component.hex_exception.HexExceptionEnum.FILE_EXPORT_ERROR;


@Component("SxssfExportOrdinary2")
@Slf4j
public class SxssfExportOrdinaryRow implements SxssfExportOrdinary{



    @Override
    public void export( HttpServletResponse response, GoExport goExport) {
        ExportFileResponseUtil responseUtil = new ExportFileResponseUtil(response, ExThreadLocal.getExTemplate().getFileName(), "xlsx");
        OSSUtil.downloadOSSInput(ExThreadLocal.getExTemplate().getTemplateUrl(), new OSSInputOperate() {
            @Override
            public void closeBefore(InputStream inputStream) throws Exception {
                SxssfExport exportColum = SxssfExportFactory.create(inputStream, ExThreadLocal.getExTemplate().getTemplateType());
                goExport.exportData(exportColum);
                ExThreadLocal.clear();
                try (ServletOutputStream outputStream = responseUtil.getOutputStream()) {
                    exportColum.getWorkbook().write(outputStream);
                    exportColum.getWorkbook().dispose();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    throw new HexException(FILE_EXPORT_ERROR);
                }
            }

            @Override
            public void closeAfter() throws Exception {

            }
        });




    }

    @Override
    public void exportLocalFile(HttpServletResponse response, GoExport goExport) {

    }


}
