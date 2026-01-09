package com.ditto.tex_component.tex_export;

import com.ditto.tex_component.tex_exception.TexException;
import com.ditto.tex_component.tex_util.TexThreadLocal;
import com.ditto.tex_component.tex_util.template_stream.TexInputStreamOperate;
import com.ditto.tex_component.tex_util.request.ExportFileResponseUtil;
import com.ditto.tex_component.tex_util.template_stream.TexOssTemplateStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

import static com.ditto.tex_component.tex_exception.TexExceptionEnum.FILE_EXPORT_ERROR;


@Component("SxssfExportOrdinary1")
@Slf4j
public class SxssfExportOrdinaryColumn implements SxssfExportOrdinary{

    @Autowired
    private TexOssTemplateStream TexOssTemplateStream;

    @Override
    public void export( HttpServletResponse response, GoExport goExport) {
        ExportFileResponseUtil responseUtil = new ExportFileResponseUtil(response, TexThreadLocal.getExTemplate().getFileName(), "xlsx");
        TexOssTemplateStream.downloadInput(TexThreadLocal.getExTemplate().getTemplateUrl(), new TexInputStreamOperate() {
            @Override
            public void closeBefore(InputStream inputStream) throws Exception {
                SxssfExport exportColum = SxssfExportFactory.create(inputStream, TexThreadLocal.getExTemplate().getTemplateType());
                goExport.exportData(exportColum);
                try (ServletOutputStream outputStream = responseUtil.getOutputStream()) {
                    exportColum.getWorkbook().write(outputStream);
                    exportColum.getWorkbook().dispose();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    throw new TexException(FILE_EXPORT_ERROR);
                }finally {
                    TexThreadLocal.clear();
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
