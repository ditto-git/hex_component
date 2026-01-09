package com.ditto.tex_component.tex_util.template_stream;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

public interface TexTemplateStream {
    public void upload (InputStream inputStream, String fileUrl);
    public void upload (String localFilePath,String fileUrl);
    public void downloadResponse(String fileUrl, HttpServletResponse response);
    public void downloadInput(String fileUrl, TexInputStreamOperate operateOssInputStream);


}
