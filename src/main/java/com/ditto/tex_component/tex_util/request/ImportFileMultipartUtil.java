package com.ditto.tex_component.tex_util.request;

import com.ditto.tex_component.tex_exception.TexException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.io.InputStream;

import static com.ditto.tex_component.tex_exception.TexExceptionEnum.TEMP_IMPORT_ERROR;
import static com.ditto.tex_component.tex_exception.TexExceptionEnum.TEMP_IO_ERROR;


@Slf4j
public class ImportFileMultipartUtil {




    private MultipartFile multipartFile;
    @Getter
    private String fileName;
    @Getter
    private String suffix;

    private InputStream inputStream=null;


    public ImportFileMultipartUtil(MultipartHttpServletRequest request, String fileParam){
        this.multipartFile= request.getFile(fileParam);
        String originalFilenameA = multipartFile.getOriginalFilename();
        if(!StringUtils.hasText(originalFilenameA)){
            throw new TexException(TEMP_IMPORT_ERROR);
        }
        int last = originalFilenameA.lastIndexOf(".");
        this.fileName=originalFilenameA.substring(0,last);
        this.suffix=originalFilenameA.substring(last);

    }


    public InputStream getInputStream (){
        try {
            inputStream=multipartFile.getInputStream();
        } catch (IOException e) {
            log.error("multipartFileInputStream 读取失败{}", e.getMessage());
            throw new TexException(TEMP_IO_ERROR);
        }
        return  inputStream;
    }


    public void closeInputStream (){
        try {
            this.inputStream.close();
        } catch (IOException e) {
            log.error("multipartFileInputStream 释放失败{}", e.getMessage());
            throw new TexException(TEMP_IO_ERROR);
        }
    }

    public static void closeInputStream (InputStream inputStream){
        try {
            inputStream.close();
        } catch (IOException e) {
            log.error("multipartFileInputStream 释放失败{}", e.getMessage());
            throw new TexException(TEMP_IO_ERROR);
        }
    }



}
