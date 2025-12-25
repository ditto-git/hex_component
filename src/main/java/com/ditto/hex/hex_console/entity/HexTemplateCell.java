package com.ditto.hex.hex_console.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentStyle;
import com.alibaba.excel.annotation.write.style.HeadFontStyle;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.alibaba.excel.enums.poi.BorderStyleEnum;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @author ditto
 * @since 2025-08-18
 */
@TableName("ex_template_cell")
@Data
@HeadRowHeight(20)
@ContentStyle(borderBottom = BorderStyleEnum.THIN,borderTop = BorderStyleEnum.THIN,borderRight = BorderStyleEnum.THIN)
public class HexTemplateCell implements Serializable {

    private static final long serialVersionUID = 1L;


    @TableId
    @ExcelProperty(value = "cellCode",order = 2)
    @ColumnWidth(14)
    private String cellCode;

    @ExcelProperty(value = "templateCode",order = 1)
    @ColumnWidth(0)
    private String templateCode;

    @ExcelProperty(value = "模板参数",order = 3)
    @ColumnWidth(14)
    private String cellProperty;

    @ExcelProperty(value = "表头位置",order = 4)
    @ColumnWidth(14)
    private String cellHead;

    @ExcelProperty(value = "表头内容",order = 5)
    @ColumnWidth(14)
    private String headContent;

    @HeadFontStyle(fontHeightInPoints = 8)
    @ExcelProperty(value = "纵表起始行",order = 6)
    @ColumnWidth(15)
    private String cellStartRow;

    @ColumnWidth(15)
    @ExcelProperty(value = "横表起始列",order = 7)
    @HeadFontStyle(fontHeightInPoints = 8)
    private String cellStartCol;

    @ColumnWidth(15)
    @ExcelProperty(value = "所在列/行",order = 8)
    @HeadFontStyle(fontHeightInPoints = 8)
    private String cellIndex;


    @ColumnWidth(25)
    @ExcelProperty(value = "数据公式",order = 9)
    private String cellFormula;


    @ColumnWidth(25)
    @ExcelProperty(value = "表头公式",order = 10)
    private String headFormula;




}
