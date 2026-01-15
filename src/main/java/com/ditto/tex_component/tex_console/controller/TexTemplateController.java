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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

import static com.ditto.tex_component.tex_console.constants.TexConstants.*;


@RestController
@RequestMapping("/tex-templateConsole")
public class TexTemplateController {

    @Autowired
    private TexTemplateService texTemplateService;

    @Autowired
    private TexTemplateCellService texTemplateCellService;

    @Autowired
    private TexOssTemplateStream texOssTemplateStream;

    @PostMapping("/init-template")
    public void  initTexTemplate (@RequestBody TexTemplate texTemplate){
        if (!StringUtils.hasText(texTemplate.getTemplateCode())){
            throw  new TexException(TexExceptionEnum.TEMP_CODE_NULL);
        }
        texTemplateService.initExTemplate(texTemplate);
    }

    @PutMapping("/update-template")
    public void  updateTexTemplate (@RequestBody TexTemplate texTemplate){
        if (!StringUtils.hasText(texTemplate.getTemplateCode())){
            throw  new TexException(TexExceptionEnum.TEMP_CODE_NULL);
        }
        texTemplateService.updateById(texTemplate);
    }

    @PutMapping("/update-template-status")
    public void  updateTexTemplateStatus (@RequestBody TexTemplate texTemplate){
        if (!StringUtils.hasText(texTemplate.getTemplateCode())){
            throw  new TexException(TexExceptionEnum.TEMP_CODE_NULL);
        }
        texTemplateService.lambdaUpdate().set(TexTemplate::getTemplateStatus, texTemplate.getTemplateStatus())
                .eq(TexTemplate::getTemplateCode, texTemplate.getTemplateCode()).update();
    }


    @DeleteMapping("/del-template")
    public void delTexTemplate (@RequestBody List<String> ids){
        texTemplateService.lambdaUpdate().set(TexTemplate::getTemplateStatus,DEL).in(TexTemplate::getTemplateCode,ids).update();

    }


    @GetMapping("/query-template")
    public List<TexTemplate> queryTexTemplate (String searchValue){
        LambdaQueryWrapper<TexTemplate> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(TexTemplate::getTemplateStatus, Arrays.asList(MAINTAIN, USE));
        if (StringUtils.hasText(searchValue)) {
            lambdaQueryWrapper.and(queryWrapper -> {
                queryWrapper.like(TexTemplate::getTemplateCode, searchValue).or().like(TexTemplate::getTemplateName, searchValue);
            });
        }
        return texTemplateService.list(lambdaQueryWrapper);
    }

    @GetMapping("/upload-template/{templateCode}")
    public void  uploadTexTemplate(@PathVariable()String templateCode , MultipartHttpServletRequest request) {
        uploadTexTemplate(request,(ImportFileMultipartUtil importFileMultipartUtil)->{
            if(!importFileMultipartUtil.getFileName().equals(templateCode)){
                throw  new TexException(TexExceptionEnum.TEMP_MATCH_ERROR);
            }
        });
    }
    public void  uploadTexTemplate(MultipartHttpServletRequest request, TexTemplateFileCheck texTemplateFileCheck)  {
        ImportFileMultipartUtil multipart = new ImportFileMultipartUtil(request, FILE_PARAM);
        if(texTemplateFileCheck !=null){
            texTemplateFileCheck.check(multipart);
        }
       texTemplateService.replaceExTemplate(multipart);

    }

    @GetMapping("/download-template/{templateCode}")
    public void  downloadTexTemplate(@PathVariable String templateCode, HttpServletResponse response)  {
        ExportFileResponseUtil.ResponseBuilder(response,templateCode,FILE_TYPE);
        TexTemplate texTemplate = texTemplateService.getExTemplate(templateCode);
        texOssTemplateStream.downloadResponse(texTemplate.getTemplateUrl(),response);
    }

    @GetMapping("/query-template-info")
    public List<TexTemplateCell>  queryTexTemplateInfo(String texTemplateCode,String searchValue)  {
        if (!StringUtils.hasText(texTemplateCode)){
            throw  new TexException(TexExceptionEnum.TEMP_CODE_NULL);
        }
        LambdaQueryWrapper<TexTemplateCell> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(TexTemplateCell::getTemplateCode, texTemplateCode);
        if (StringUtils.hasText(searchValue)) {
            lambdaQueryWrapper.and(queryWrapper -> {
                queryWrapper.like(TexTemplateCell::getCellCode, searchValue)
                        .or().like(TexTemplateCell::getCellProperty, searchValue)
                        .or().like(TexTemplateCell::getHeadContent, searchValue);
            });
        }
        return texTemplateCellService.list(lambdaQueryWrapper);
    }

    @PutMapping("/update-templateCell")
    public void updateTexTemplate(@RequestBody TexTemplateCell texTemplateCell)  {
        if (!StringUtils.hasText(texTemplateCell.getTemplateCode())){
            throw  new TexException(TexExceptionEnum.CELL_CODE_NULL);
        }
         texTemplateCellService.updateById(texTemplateCell);
    }


    @GetMapping("/download-templateCells")
    public void exportTexTemplateCell(String templateCode,HttpServletResponse response){
        texTemplateCellService.exportExTemplateCell(new ExportFileResponseUtil(response,templateCode,FILE_TYPE));
    }

    @PostMapping("/upload-templateCells")
    public void importTexTemplateCell(MultipartHttpServletRequest request) {
        ImportFileMultipartUtil multipart = new ImportFileMultipartUtil(request, FILE_PARAM);
        texTemplateCellService.importExTemplateCell(multipart);
    }








}
