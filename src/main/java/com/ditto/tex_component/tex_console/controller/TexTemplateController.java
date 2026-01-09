package com.ditto.tex_component.tex_console.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ditto.tex_component.tex_console.entity.TexTemplate;
import com.ditto.tex_component.tex_console.entity.TexTemplateCell;
import com.ditto.tex_component.tex_console.service.TexTemplateCellService;
import com.ditto.tex_component.tex_console.service.TexTemplateFileCheck;
import com.ditto.tex_component.tex_console.service.TexTemplateService;
import com.ditto.tex_component.tex_exception.TexException;
import com.ditto.tex_component.tex_exception.TexExceptionEnum;
import com.ditto.tex_component.tex_util.request.ExportFileResponseUtil;
import com.ditto.tex_component.tex_util.request.ImportFileMultipartUtil;
import com.ditto.tex_component.tex_util.template_stream.TexOssTemplateStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

import static com.ditto.tex_component.tex_console.constants.TexConstants.*;


@RestController
@RequestMapping("/ExTemplateConsole")
public class TexTemplateController {

    @Autowired
    private TexTemplateService texTemplateService;

    @Autowired
    private TexTemplateCellService texTemplateCellService;

    @Autowired
    private TexOssTemplateStream TexOssTemplateStream;

    @RequestMapping("/initExTemplate")
    public void  initExTemplate (@RequestBody TexTemplate texTemplate){
        if (StringUtils.isEmpty(texTemplate.getTemplateCode())){
            throw  new TexException(TexExceptionEnum.TEMP_CODE_NULL);
        }
        texTemplateService.initExTemplate(texTemplate);
    }

    @RequestMapping("/updateExTemplate")
    public void  updateExTemplate (@RequestBody TexTemplate texTemplate){
        if (StringUtils.isEmpty(texTemplate.getTemplateCode())){
            throw  new TexException(TexExceptionEnum.TEMP_CODE_NULL);
        }
        texTemplateService.updateById(texTemplate);
    }

    @RequestMapping("/useExTemplate")
    public void  useExTemplate (@RequestBody TexTemplate texTemplate){
        if (StringUtils.isEmpty(texTemplate.getTemplateCode())){
            throw  new TexException(TexExceptionEnum.TEMP_CODE_NULL);
        }
        texTemplateService.lambdaUpdate().set(TexTemplate::getTemplateStatus, texTemplate.getTemplateStatus())
                .eq(TexTemplate::getTemplateCode, texTemplate.getTemplateCode()).update();
    }


    @RequestMapping("/delExTemplate")
    public void delExTemplate (@RequestBody List<String> ids){
        texTemplateService.lambdaUpdate().set(TexTemplate::getTemplateStatus,DEL).in(TexTemplate::getTemplateCode,ids).update();

    }



    @RequestMapping("/selectExTemplate")
    public List<TexTemplate> selectExTemplate (String select){
        LambdaQueryWrapper<TexTemplate> lambdaQueryWrapper = new LambdaQueryWrapper();
        lambdaQueryWrapper.in(TexTemplate::getTemplateStatus, Arrays.asList(MAINTAIN, USE));
        if (StringUtils.hasText(select)) {
            lambdaQueryWrapper.and(queryWrapper -> {
                queryWrapper.like(TexTemplate::getTemplateCode, select).or().like(TexTemplate::getTemplateName, select);
            });
        }
        return texTemplateService.list(lambdaQueryWrapper);
    }

    @RequestMapping("/uploadExTemplate/{templateCode}")
    public void  uploadExTemplate(@PathVariable()String templateCode , MultipartHttpServletRequest request) {
        uploadExTemplate(request,(ImportFileMultipartUtil importFileMultipartUtil)->{
            if(!importFileMultipartUtil.getFileName().equals(templateCode)){
                throw  new TexException(TexExceptionEnum.TEMP_MATCH_ERROR);
            }
        });
    }
    public void  uploadExTemplate(MultipartHttpServletRequest request, TexTemplateFileCheck texTemplateFileCheck)  {
        ImportFileMultipartUtil multipart = new ImportFileMultipartUtil(request, "file");
        if(texTemplateFileCheck !=null){
            texTemplateFileCheck.check(multipart);
        }
       texTemplateService.replaceExTemplate(multipart);

    }

    @RequestMapping("/downloadExTemplate/{templateCode}")
    public void  downloadExTemplate(@PathVariable String templateCode, HttpServletResponse response)  {
        ExportFileResponseUtil.ResponseBuilder(response,templateCode,"xlsx");
        TexTemplate texTemplate = texTemplateService.getExTemplate(templateCode);
        TexOssTemplateStream.downloadResponse(texTemplate.getTemplateUrl(),response);
    }

    @RequestMapping("/exTemplateInfo")
    public List<TexTemplateCell>  exTemplateInfo(@RequestBody TexTemplateCell texTemplateCell)  {
        if (StringUtils.isEmpty(texTemplateCell.getTemplateCode())){
            throw  new TexException(TexExceptionEnum.TEMP_CODE_NULL);
        }
        LambdaQueryWrapper<TexTemplateCell> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(TexTemplateCell::getTemplateCode, texTemplateCell.getTemplateCode());
        if (StringUtils.hasText(texTemplateCell.getCellCode())) {
            lambdaQueryWrapper.and(queryWrapper -> {
                queryWrapper.like(TexTemplateCell::getCellCode, texTemplateCell.getCellCode())
                        .or().like(TexTemplateCell::getCellProperty, texTemplateCell.getCellProperty())
                        .or().like(TexTemplateCell::getHeadContent, texTemplateCell.getHeadContent());


            });
        }
        return texTemplateCellService.list(lambdaQueryWrapper);
    }

    @RequestMapping("/updateExTemplateCell")
    public void updateExTemplate(@RequestBody TexTemplateCell texTemplateCell)  {
        if (StringUtils.isEmpty(texTemplateCell.getTemplateCode())){
            throw  new TexException(TexExceptionEnum.CELL_CODE_NULL);
        }
         texTemplateCellService.updateById(texTemplateCell);
    }


    @RequestMapping("/downloadExCells")
    public void exportExTemplateCell(@RequestBody TexTemplateCell texTemplateCell,HttpServletResponse response)throws Exception {
        texTemplateCellService.exportExTemplateCell(new ExportFileResponseUtil(response,texTemplateCell.getTemplateCode(),"xlsx"));
    }
    @RequestMapping("/uploadExCells")
    public void importExTemplateCell(MultipartHttpServletRequest request) throws Exception {
        ImportFileMultipartUtil multipart = new ImportFileMultipartUtil(request, "file");
        texTemplateCellService.importExTemplateCell(multipart);
    }








}
