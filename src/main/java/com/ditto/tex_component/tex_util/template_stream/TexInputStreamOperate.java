package com.ditto.tex_component.tex_util.template_stream;

import java.io.InputStream;

public interface TexInputStreamOperate {

   public  void closeBefore(InputStream inputStream) throws Exception;

   public void closeAfter()  throws Exception;
}
