package com.ditto.hex_component.hex_util.request;

import com.ditto.hex_component.hex_exception.HexException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static com.ditto.hex_component.hex_exception.HexExceptionEnum.TEMP_IO_ERROR;


@Slf4j
public class ExportFileResponseUtil {



    @Getter
    private String fileName;
    @Getter
    private String suffix;

    private HttpServletResponse response;

    private ServletOutputStream servletOutputStream=null;

    public ExportFileResponseUtil(HttpServletResponse response, String fileName, String suffix){
        this.fileName = fileName;
        this.suffix = suffix;
        this.response = response;
        ResponseBuilder(this.response,this.fileName,this.suffix);
    }


    public ExportFileResponseUtil(HttpServletResponse response,String suffix){
        this.fileName = "file";
        this.suffix = suffix;
        this.response = response;
        ResponseBuilder(this.response,this.fileName,this.suffix);
    }


    public ExportFileResponseUtil(HttpServletResponse response){
        this.fileName = "file";
        this.suffix = "xlsx";
        this.response = response;
        ResponseBuilder(this.response,this.fileName,this.suffix);
    }


    public void setFileName(String fileName) {
        this.fileName = fileName;
        ResponseBuilder(this.response,this.fileName,this.suffix);
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
        ResponseBuilder(this.response,this.fileName,this.suffix);
    }


    public static void ResponseBuilder(HttpServletResponse response, String fileName, String suffix){
        // 设置响应的内容类型，根据文件类型设置，例如：application/pdf, text/plain等
        // response.setContentType("application/octet-stream");
        // response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        // response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(fileName, "UTF-8"));

        // response.setContentType("application/ms-excel;charset=UTF-8");
        // response.setHeader("Content-Disposition", "attachment;filename="+ new String(fileName.getBytes("utf-8"), "iso-8859-1"));

        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
        try {
            response.setHeader("Content-Disposition", "attachment;filename=".concat(String.valueOf(URLEncoder.encode(fileName+"."+suffix, "UTF-8"))));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

    }




    public ServletOutputStream getOutputStream (){
        try {
            servletOutputStream =response.getOutputStream();
        } catch (IOException e) {
            log.error("{}servletOutputStream 读取失败", e.getMessage());
            throw new HexException(TEMP_IO_ERROR);
        }
        return  servletOutputStream;
    }

    public void closeOutputStream () {
        try {
            servletOutputStream.close();
        } catch (IOException e) {
            log.error("{}servletOutputStream 释放失败", e.getMessage());
            throw new HexException(TEMP_IO_ERROR);
        }
    }

}
