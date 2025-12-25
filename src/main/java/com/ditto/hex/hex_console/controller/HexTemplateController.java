package com.ditto.hex.hex_console.controller;

import com.ditto.hex.hex_console.entity.HexTemplate;
import com.ditto.hex.hex_console.service.HexTemplateCellService;
import com.ditto.hex.hex_console.service.HexTemplateFileCheck;
import com.ditto.hex.hex_console.service.HexTemplateService;
import com.ditto.hex.hex_exception.HexException;
import com.ditto.hex.hex_exception.HexExceptionEnum;
import com.ditto.hex.hex_util.oss.OSSUtil;
import com.ditto.hex.hex_util.request.ExportFileResponseUtil;
import com.ditto.hex.hex_util.request.ImportFileMultipartUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/ExTemplateController")
public class HexTemplateController {

    @Autowired
    private HexTemplateService hexTemplateService;

    @Autowired
    private HexTemplateCellService hexTemplateCellService;




    @RequestMapping("/uploadExTemplate/{templateCode}")
    public void  uploadExTemplate(@PathVariable()String templateCode , MultipartHttpServletRequest request) {
        uploadExTemplate(request,(ImportFileMultipartUtil importFileMultipartUtil)->{
            if(!importFileMultipartUtil.getFileName().equals(templateCode)){
                throw  new HexException(HexExceptionEnum.TEMP_MATCH_ERROR);
            }
        });
    }
    public void  uploadExTemplate(MultipartHttpServletRequest request, HexTemplateFileCheck hexTemplateFileCheck)  {
        ImportFileMultipartUtil multipart = new ImportFileMultipartUtil(request, "file");
        if(hexTemplateFileCheck !=null){
            hexTemplateFileCheck.check(multipart);
        }
       hexTemplateService.replaceExTemplate(multipart);

    }

    @RequestMapping("/downloadExTemplate/{templateCode}")
    public void  downloadExTemplate(@PathVariable String templateCode, HttpServletResponse response)  {
        ExportFileResponseUtil.ResponseBuilder(response,templateCode,"xlsx");
        HexTemplate hexTemplate = hexTemplateService.getExTemplate(templateCode);
        OSSUtil.downloadOSSResponse(hexTemplate.getTemplateUrl(),response);
    }



    @RequestMapping("/exportExCells")
    public void exportExTemplateCell(HttpServletResponse response, String templateCode)throws Exception {
        hexTemplateCellService.exportExTemplateCell(new ExportFileResponseUtil(response,templateCode,"xlsx"));
    }
    @RequestMapping("/importExCells")
    public void importExTemplateCell(MultipartHttpServletRequest request) throws Exception {
        ImportFileMultipartUtil multipart = new ImportFileMultipartUtil(request, "file");
        hexTemplateCellService.importExTemplateCell(multipart);
    }








}
