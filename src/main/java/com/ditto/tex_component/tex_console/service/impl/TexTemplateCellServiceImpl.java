package com.ditto.tex_component.tex_console.service.impl;


import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ditto.tex_component.tex_console.entity.TexTemplate;
import com.ditto.tex_component.tex_console.entity.TexTemplateCell;
import com.ditto.tex_component.tex_console.exListener.ExTemplateCellListener;
import com.ditto.tex_component.tex_console.mapper.TexTemplateCellMapper;
import com.ditto.tex_component.tex_console.mapper.TexTemplateMapper;
import com.ditto.tex_component.tex_console.service.TexTemplateCellService;
import com.ditto.tex_component.tex_exception.TexException;
import com.ditto.tex_component.tex_util.TexThreadLocal;
import com.ditto.tex_component.tex_util.request.ExportFileResponseUtil;
import com.ditto.tex_component.tex_util.request.ImportFileMultipartUtil;
import com.ditto.tex_component.tex_util.template_stream.TexOssTemplateStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.ditto.tex_component.tex_exception.TexExceptionEnum.*;
import static com.ditto.tex_component.tex_console.constants.TexConstants.*;


/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author ditto
 * @since 2025-08-18
 */
@Service
@Slf4j
public class TexTemplateCellServiceImpl extends ServiceImpl<TexTemplateCellMapper, TexTemplateCell> implements TexTemplateCellService {

    private static final String CONTENTS="/ex_template/";
    private static final String FILE_NAME_CONNECT="_";
    private static final String VERSION_TIME_FORMATTER="yyMMddHHmmss";

    private static final String BLANK="";
    private static final String RC_CONNECT="&";
    private static final String CODE_CONNECT="_";
    private static final String PROPERTY_STARTS_WITH="{";
    private static final String PROPERTY_ENDS_WITH="}";
    private static final String PROPERTY_CONTINUE=".";
    private static final int PROPERTY_MIN_SIZE=3;



    @Autowired
    private TexTemplateMapper texTemplateMapper;

    @Autowired
    private TexTemplateCellMapper hexTemplateCellMapper;

    @Autowired
    private TexOssTemplateStream TexOssTemplateStream;

    @Transactional
    public void replaceExTemplate(ImportFileMultipartUtil importFileMultipartUtil) {
        TexTemplate texTemplate = TexThreadLocal.getExTemplate();

        /*文件名定义     templateCode+version+加线程号+文件后缀         TEST_25083014120856.xlsx*/
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(VERSION_TIME_FORMATTER);
        String fileUrl = CONTENTS + texTemplate.getTemplateCode() + FILE_NAME_CONNECT + LocalDateTime.now().format(timeFormatter) + FILE_NAME_CONNECT + Thread.currentThread().getId() + importFileMultipartUtil.getSuffix();
        texTemplate.setTemplateUrl(fileUrl);

        /*读取模板内容 到ExTemplateCell*/
        log.info("{}...模板解析中............", texTemplate.getTemplateUrl());
        if(!TEMPLATE_TYPE_X.equals(texTemplate.getTemplateType())){
            try (XSSFWorkbook workbook = new XSSFWorkbook(importFileMultipartUtil.getInputStream())) {
                XSSFSheet sheet = workbook.getSheetAt(0);
                XSSFRow row;
                for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                    row = sheet.getRow(i);
                    for (int j = 0; j < row.getLastCellNum(); j++) {
                        invoke(i, j, row.getCell(j) == null || CellType.FORMULA == row.getCell(j).getCellType() ? BLANK : row.getCell(j).toString());
                    }
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new TexException(TEMP_IMPORT_ERROR);
            }finally {
                importFileMultipartUtil.closeInputStream();
            }
        }

        //存储ExTemplate
        doAfterAllAnalysed();

        /*更新OSS模板   模板路径变更*/
        TexOssTemplateStream.upload(importFileMultipartUtil.getInputStream(), fileUrl);
    }

    /**
     * ExTemplate解析模板数据
     */
    private void invoke(int rIndex, int cIndex, String value) {
        TexTemplate texTemplate = TexThreadLocal.getExTemplate();
        if (!StringUtils.hasText(value)) {
            return;
        }
        boolean contains = value.startsWith(PROPERTY_STARTS_WITH) && value.endsWith(PROPERTY_ENDS_WITH);
        if (contains) {
            if (value.length() < PROPERTY_MIN_SIZE) {
                return;
            }
            //正则
            value = value.substring(1, value.length() - 1);
            TexTemplateCell exc = new TexTemplateCell();
            exc.setTemplateCode(texTemplate.getTemplateCode());
            exc.setCellStartRow(BLANK + rIndex);
            exc.setCellStartCol(BLANK + cIndex);
            exc.setCellProperty(value);
            TexThreadLocal.addExCell(exc);
        } else if (!value.trim().isEmpty()) {
            TexThreadLocal.putExHead(rIndex + RC_CONNECT + cIndex, value);
        }
    }

    /**
     * ExTemplate判断模板类型
     */
    private void doAfterAllAnalysed() {
        String v;
        String headCell_r;
        String headCell_c;
        String head_r;
        String head_c;
        int cCount = 0;int rCount = 0;
        TexTemplate texTemplate = TexThreadLocal.getExTemplate();
        List<TexTemplateCell> hexTemplateCells = TexThreadLocal.getExCells();
        Map<String, String> exTemplateHead = TexThreadLocal.getExHead();

        for (TexTemplateCell exc : hexTemplateCells) {
            v = exc.getCellProperty();
            headCell_r = exc.getCellStartRow() + RC_CONNECT + (Integer.parseInt(exc.getCellStartCol()) - 1);
            headCell_c = Integer.parseInt(exc.getCellStartRow()) - 1 + RC_CONNECT + exc.getCellStartCol();
            head_r = exTemplateHead.get(headCell_r);
            head_c = exTemplateHead.get(headCell_c);
            //集合数据（列/横）
            if (v.startsWith(PROPERTY_CONTINUE)) {
                if (head_c != null) {
                    exc.setHeadContent(head_c);
                    exc.setCellHead(headCell_c);
                    exc.setCellIndex(exc.getCellStartCol());
                    exc.setCellStartCol(BLANK);
                }
                if (head_r != null || head_c == null) {
                    exc.setHeadContent(head_r);
                    exc.setCellHead(headCell_r);
                    exc.setCellIndex(exc.getCellStartRow());
                    exc.setCellStartRow(BLANK);
                }
                exc.setCellProperty(v.substring(1));
            } else {
                //单元数据
                if (head_r != null) {
                    exc.setHeadContent(head_r);
                    exc.setCellHead(headCell_r);
                }
                if (head_c != null) {
                    exc.setHeadContent(head_c);
                    exc.setCellHead(headCell_c);
                }
            }
            exc.setCellCode(exc.getTemplateCode() + CODE_CONNECT + exc.getCellProperty());

            //起始（列/横）计数
            if (StringUtils.hasText(exc.getCellStartCol())) {
                rCount++;
            }
            if (StringUtils.hasText(exc.getCellStartRow())) {
                cCount++;
            }
        }


        //判断模板类型
        if (cCount == hexTemplateCells.size()) {
            TexThreadLocal.getExTemplate().setTemplateType(TEMPLATE_TYPE_COlUMN);
        } else if (rCount == hexTemplateCells.size()) {
            TexThreadLocal.getExTemplate().setTemplateType(TEMPLATE_TYPE_ROW);
        } else {
            TexThreadLocal.getExTemplate().setTemplateType(TEMPLATE_TYPE_X);
        }
        log.info("{}...模板解析完成！", texTemplate.getTemplateUrl());
        saveData();
    }

    /**
     * ExTemplate存储数据库
     */
    private void saveData() {
        List<TexTemplateCell> hexTemplateCells = TexThreadLocal.getExCells();
        TexTemplate texTemplate = TexThreadLocal.getExTemplate();
        if(hexTemplateCells.isEmpty()){log.info("{}...无参数的模板............", texTemplate.getTemplateUrl());return;}


        //查看现有TemplateCell
        QueryWrapper<TexTemplateCell> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(TexTemplateCell::getTemplateCode, texTemplate.getTemplateCode());
        List<TexTemplateCell> selectCells = hexTemplateCellMapper.selectList(queryWrapper);

        List<String> deleteIds = new ArrayList<>();
        //insertOrUpdate不识别updateStrategy   遍历,只更新下列参数
        String cell_r;
        String cell_c;
        String cell_i;
        String cell_head;
        String headContent;
        A:
        for (TexTemplateCell sexc : selectCells) {
            /* if(exTemplateCells.stream().noneMatch(e -> e.getCellCode().equals(exTemplateCell.getCellCol()))){}*/
            for (TexTemplateCell exc : hexTemplateCells) {
                if (sexc.getCellCode().equals(exc.getCellCode())) {
                    cell_c = exc.getCellStartCol();
                    cell_r = exc.getCellStartRow();
                    cell_i = exc.getCellIndex();
                    cell_head = exc.getCellHead();
                    headContent = exc.getHeadContent();
                    BeanUtils.copyProperties(sexc, exc);
                    exc.setCellStartCol(cell_c);
                    exc.setCellStartRow(cell_r);
                    exc.setCellIndex(cell_i);
                    exc.setCellHead(cell_head);
                    exc.setHeadContent(headContent);
                    continue A;
                }
            }
            //TemplateCell不在新的模板中 就删除
            deleteIds.add(sexc.getCellCode());

        }
        /*DCU*/
        if(!deleteIds.isEmpty()){
            hexTemplateCellMapper.deleteByIds(deleteIds);}
        hexTemplateCellMapper.insertOrUpdate(hexTemplateCells);
        texTemplateMapper.updateById(texTemplate);
        log.info("{}...存储数据库成功！", texTemplate.getTemplateUrl());
    }



    @Override
    @Transactional
    public void importExTemplateCell(ImportFileMultipartUtil importFileMultipartUtil) {
        ExTemplateCellListener exTemplateCellListener = new ExTemplateCellListener();
        EasyExcel.read(importFileMultipartUtil.getInputStream(), TexTemplateCell.class,exTemplateCellListener)
                // 在 read 方法之后， 在 sheet方法之前都是设置ReadWorkbook的参数
                .sheet()
                .doRead();
        if(!exTemplateCellListener.getDataList().isEmpty()){
            updateBatchById(exTemplateCellListener.getDataList());
        }
    }


    @Override
    public void exportExTemplateCell(ExportFileResponseUtil exportFileResponseUtil) {
        String templateCode = exportFileResponseUtil.getFileName();
        exportFileResponseUtil.setFileName(exportFileResponseUtil.getFileName()+"_cells");
        EasyExcel.write(exportFileResponseUtil.getOutputStream(), TexTemplateCell.class)
                // 在 write 方法之后， 在 sheet方法之前都是设置WriteWorkbook的参数
                .sheet("模板")
                //件流会自动关闭
                .doWrite(() -> {
                    // 分页查询数据
                    return this.lambdaQuery().eq(TexTemplateCell::getTemplateCode,templateCode ).list();
                });

    }



}
