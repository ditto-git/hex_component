package com.ditto.hex.hex_util.request;

import com.ditto.hex.hex_exception.HexException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.io.InputStream;

import static com.ditto.hex.hex_exception.HexExceptionEnum.TEMP_IMPORT_ERROR;
import static com.ditto.hex.hex_exception.HexExceptionEnum.TEMP_IO_ERROR;


@Slf4j
public class ImportFileMultipartUtil {



    @Getter
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
            throw new HexException(TEMP_IMPORT_ERROR);
        }
        int last = originalFilenameA.lastIndexOf(".");
        this.fileName=originalFilenameA.substring(0,last);
        this.suffix=originalFilenameA.substring(last);

    }


    public InputStream getInputStream (){
        try {
            inputStream = multipartFile.getInputStream();
        } catch (IOException e) {
            log.error("multipartFileInputStream 读取失败{}", e.getMessage());
            throw new HexException(TEMP_IO_ERROR);
        }
        return  inputStream;
    }

    public void closeInputStream (){
        try {
            inputStream.close();
        } catch (IOException e) {
            log.error("multipartFileInputStream 释放失败{}", e.getMessage());
            throw new HexException(TEMP_IO_ERROR);
        }
    }



}
