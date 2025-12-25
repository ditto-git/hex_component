package com.ditto.hex.hex_util.oss;

import java.io.InputStream;

public interface OSSInputOperate {

   public  void closeBefore(InputStream inputStream) throws Exception;

   public void closeAfter()  throws Exception;
}
