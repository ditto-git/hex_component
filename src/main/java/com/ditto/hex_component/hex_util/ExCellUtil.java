package com.ditto.hex_component.hex_util;


import org.apache.poi.ss.formula.*;
import org.apache.poi.ss.formula.ptg.AreaPtg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.RefPtgBase;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTMergeCell;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTMergeCells;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorksheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class ExCellUtil {
    private static Logger logger = LoggerFactory.getLogger(ExCellUtil.class);




//    public static  <T extends Object> T  getExcelTemplate(String url,String excelTemplate,Class<T> clazz){
//        /**读取模板  为空不读  非空无后缀拼接.xlsx         .xls .xlsx  使用POI  xml使用FreeMarker */
//        if(!StringUtils.isEmpty(excelTemplate)&&excelTemplate.split("\\.").length<2){excelTemplate+=".xlsx";}
//
//        /**  FreeMarker template  or  POI workbook*/
//        if("ftl".equals(excelTemplate.split("\\.")[1])&&clazz==Template.class ||"xml".equals(excelTemplate.split("\\.")[1])&&clazz==Template.class){
//            try {
//                logger.info("type:FreeMarker");
//                Configuration configuration = new Configuration();
//                configuration.setDefaultEncoding("utf-8");
//                configuration.setDirectoryForTemplateLoading(new File(url));
//                return (T) configuration.getTemplate(excelTemplate);
//            }catch (IOException e){
//                logger.error("FreeMarker模板获取失败:"+excelTemplate,e);}
//        }
//
//        if(!"ftl".equals(excelTemplate.split("\\.")[1])&&clazz==Workbook.class&&!"xml".equals(excelTemplate.split("\\.")[1])&&clazz==Workbook.class){
//            logger.info("type:POI");
//            try(FileInputStream inputStream=new FileInputStream(url + File.separator + excelTemplate)){
//                return (T) new XSSFWorkbook(inputStream);
//            }catch (IOException e){
//                logger.error("POI模板获取失败:"+excelTemplate,e);}
//        }
//
//        return  null;
//    }
//
//
//    public  static void  responseTemplateExcel(Workbook workbook, Template template,Map<String,Object> templateData, ServletOutputStream outputStream) throws TemplateException, IOException {
//        /**  FreeMarker template  or  POI workbook*/
//        if(workbook==null){
//            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, Charset.forName("utf-8")));
//            template.setEncoding("utf-8");
//            template.process(templateData, writer);
//        }else {
//            workbook.write(outputStream);}
//    }


    /**
     * @description 批量复制行
     * @param startRowIndex  原始行Index
     * @param toRowIndex    目标行Index
     * @param toSheet       目标工作表
     * @param copyValueFlag 是否复制内容
     * @param merged        是否合并单元格
     */
    public static void copyRowsPOI(Integer startRowIndex, Integer endRowIndex, Integer toRowIndex,Integer startCellIndex,Integer endCellIndex,Sheet fromSheet, Sheet toSheet,boolean copyValueFlag,boolean merged){
        /**复制行*/
        Row toRow;
        Row fromRow;
        for(int i= startRowIndex;i<=endRowIndex;i++ ){
            fromRow=fromSheet.getRow(i);
            fromRow=fromRow==null?fromSheet.createRow(i):fromRow;
            toRow=toSheet.getRow(toRowIndex);
            toRow=toRow==null?toSheet.createRow(toRowIndex):toRow;
            toRowIndex++;
            copyRowPOI(fromRow, toRow,startCellIndex,endCellIndex,copyValueFlag,false);
        }

        /**合并单元格*/
        if(merged){
            copyMergedRegionPOI(startRowIndex,endRowIndex, toRowIndex, fromSheet, toSheet,null);
        }

    }

    /**
     * @description 批量复制行
     * @param startRowIndex  原始行Index
     * @param toRowIndex    目标行Index
     * @param toSheet       目标工作表
     * @param copyValueFlag 是否复制内容
     * @param merged        是否合并单元格
     */
    public static void copyRowsPOI(Integer startRowIndex, Integer endRowIndex, Integer toRowIndex,Sheet fromSheet, Sheet toSheet,boolean copyValueFlag,boolean merged){
        /**获取原始行。没有就创建一个*/
        List<Row> fromRows = new ArrayList<>();
        Row fromRow;
        for(int i= startRowIndex;i<=endRowIndex;i++ ){
            fromRow=fromSheet.getRow(i);
            fromRow=fromRow==null?fromSheet.createRow(i):fromRow;
            fromRows.add(fromRow);
        }
        copyRowsPOI(fromRows,toRowIndex, toSheet, copyValueFlag, merged);
    }


    /**
     * @description 批量复制行
     * @param startRowIndex  原始行Index
     * @param toRowIndex    目标行Index
     * @param toSheet       目标工作表
     * @param copyValueFlag 是否复制内容
     * @param mergedRegions        合并单元格样式
     */
    public static void copyRowsPOI(Integer startRowIndex, Integer endRowIndex, Integer toRowIndex,Sheet fromSheet, Sheet toSheet,boolean copyValueFlag,List<CellRangeAddress> mergedRegions){
        /**获取原始行。没有就创建一个*/
        List<Row> fromRows = new ArrayList<>();
        Row fromRow;
        for(int i= startRowIndex;i<=endRowIndex;i++ ){
            fromRow=fromSheet.getRow(i);
            fromRow=fromRow==null?fromSheet.createRow(i):fromRow;
            fromRows.add(fromRow);
        }
        copyRowsPOI(fromRows,toRowIndex, toSheet, copyValueFlag, mergedRegions);
    }



    /**
     * @description 批量复制行
     * @param fromRows      原始行
     * @param toRowIndex    目标行Index
     * @param toSheet       目标工作表
     * @param copyValueFlag 是否复制内容

     * @param merged        是否合并单元格
     */
    public static void copyRowsPOI(List<Row> fromRows, int toRowIndex, Sheet toSheet,boolean copyValueFlag,boolean merged){
        //取行号
        List<Integer> fromRowsIndex = fromRows.stream().map(Row::getRowNum).collect(Collectors.toList());
        Collections.sort(fromRowsIndex);
        if(fromRowsIndex==null){return;}


        /**校验连贯*/
        for (int s=0; s<fromRowsIndex.size();s++){
            if(s!=0&&fromRowsIndex.get(s)-fromRowsIndex.get(s-1)!=1){logger.error("批量复制行失败：数据行不连贯");return;}
        }

        /**复制行*/
        Row toRow;
        int rowIndex=toRowIndex;
        for(Row fromRow:fromRows){
            toRow=toSheet.getRow(rowIndex);
            toRow=toRow==null?toSheet.createRow(rowIndex):toRow;
            rowIndex++;
            copyRowPOI(fromRow, toRow,copyValueFlag,false);

        }
        /**合并单元格*/
        if(merged){
            copyMergedRegionPOI(fromRowsIndex.get(0),fromRowsIndex.get(fromRowsIndex.size()-1), toRowIndex, fromRows.get(0).getSheet(), toSheet,null);
        }

    }

    /**
     * @description 批量复制行
     * @param fromRows      原始行
     * @param toRowIndex    目标行Index
     * @param toSheet       目标工作表
     * @param copyValueFlag 是否复制内容
     * @param mergedRegions 否合并单元格
     */
    public static void copyRowsPOI(List<Row> fromRows, int toRowIndex, Sheet toSheet,boolean copyValueFlag,List<CellRangeAddress> mergedRegions){
        //取行号
        List<Integer> fromRowsIndex = fromRows.stream().map(Row::getRowNum).collect(Collectors.toList());
        Collections.sort(fromRowsIndex);
        if(fromRowsIndex==null){return;}


        /**校验连贯*/
        for (int s=0; s<fromRowsIndex.size();s++){
            if(s!=0&&fromRowsIndex.get(s)-fromRowsIndex.get(s-1)!=1){
                logger.error("批量复制行失败：数据行不连贯");return;
            }
        }

        /**复制行*/
        Row toRow;
        int rowIndex=toRowIndex;
        for(Row fromRow:fromRows){
            toRow=toSheet.getRow(rowIndex);
            toRow=toRow==null?toSheet.createRow(rowIndex):toRow;
            rowIndex++;
            copyRowPOI(fromRow, toRow,copyValueFlag,false);

        }


        /**合并单元格*/
        if(!CollectionUtils.isEmpty(mergedRegions)){
            copyMergedRegionPOI(fromRowsIndex.get(0),fromRowsIndex.get(fromRowsIndex.size()-1), toRowIndex, fromRows.get(0).getSheet(), toSheet,mergedRegions);
        }


    }

    /**
     * @description 复制行
     * @param fromRow       原始行
     * @param toRow         目标行
     * @param copyValueFlag 是否复制内容
     * @param merged        是否合并单元格
     * @author wdx
     */
    public static void copyRowPOI(Row fromRow, Row toRow,boolean copyValueFlag, boolean merged) {
        copyRowPOI(fromRow,toRow, null, null, copyValueFlag,  merged);
    }




    /**
     * @description 复制行
     * @param fromRow               原始行
     * @param toRow                 目标行
     * @param startCellIndex        起始行
     * @param endCellIndex          结束行
     * @param copyValueFlag         是否复制内容
     * @param merged                是否合并单元格
     * @author wdx
     */
    public static void copyRowPOI(Row fromRow, Row toRow,Integer startCellIndex,Integer endCellIndex,boolean copyValueFlag, boolean merged) {
        if(fromRow==null||toRow==null){
           String fromRowIndex=  fromRow==null?"":""+fromRow.getRowNum();
           String toRowIndex=  toRow==null?"":""+toRow.getRowNum();
           logger.error("复制行失败  fromRowIndex:"+fromRowIndex+"---toRowIndex:"+toRowIndex);return;
        }
        startCellIndex=startCellIndex==null? 0:startCellIndex;
        endCellIndex=endCellIndex==null?fromRow.getLastCellNum()-1:endCellIndex;

        //复制行高
        toRow.setHeight(fromRow.getHeight());

        //复制单元格
        for (Iterator cellIt = fromRow.cellIterator(); cellIt.hasNext(); ) {
            Cell tmpCell = (Cell) cellIt.next();
            if(startCellIndex>tmpCell.getColumnIndex()||endCellIndex<tmpCell.getColumnIndex()){continue;}
            Cell toCell = toRow.getCell(tmpCell.getColumnIndex());
            toCell =toCell==null?toRow.createCell(tmpCell.getColumnIndex()):toCell;
            copyCellPOI(tmpCell, toCell, copyValueFlag);
        }

        //合并单元格
        if (merged) {
            copyMergedRegionPOI(fromRow.getRowNum(),fromRow.getRowNum(), toRow.getRowNum(), fromRow.getSheet(),toRow.getSheet(),null);
        }

    }




    /**
     * @description 复制单元格
     * @param srcCell   原始单元格
     * @param distCell  目标单元格
     */
    public static void copyCellPOI(Cell srcCell,Cell distCell,boolean copyValueFlag){
        if(srcCell==null||distCell==null){return;}
        //复制样式
        distCell.setCellStyle(srcCell.getCellStyle());

        //复制公式
        if (srcCell.getCellType()==CellType.FORMULA){
            distCell.setCellFormula(srcCell.getCellFormula());
        }

        //复制批注
        if(srcCell.getCellComment() != null){distCell.setCellComment(srcCell.getCellComment());}

        //不复制内容
        if(!copyValueFlag){return;}

        if(srcCell.getCellType()==CellType.NUMERIC){
          if(DateUtil.isCellDateFormatted(srcCell)){
              distCell.setCellValue(srcCell.getDateCellValue());
            } else{
                distCell.setCellValue(srcCell.getNumericCellValue());
          }
        } else if(srcCell.getCellType()==CellType.STRING){
            distCell.setCellValue(srcCell.getRichStringCellValue());
        } else if(srcCell.getCellType()==CellType.BLANK){

        } else if(srcCell.getCellType()==CellType.BOOLEAN){
            distCell.setCellValue(srcCell.getBooleanCellValue());
        } else if(srcCell.getCellType()== CellType.ERROR){
            distCell.setCellErrorValue(srcCell.getErrorCellValue());
        } else if(srcCell.getCellType()==CellType.FORMULA){
            distCell.setCellFormula(srcCell.getCellFormula());
        } else{

        }
    }

    /**
     * @description 合并单元格
     * @param startRowIndex     原始行Index
     * @param endRowIndex       结束行Index
     * @param toRowIndex        目标行Index
     * @param fromSheet         原始页
     * @param toSheet           目标页
     */
    public static void copyMergedRegionPOI(Integer startRowIndex,Integer endRowIndex,Integer toRowIndex,Sheet fromSheet,Sheet toSheet,List<CellRangeAddress> mergedRegions){
        if(startRowIndex==null||endRowIndex==null||toRowIndex==null||fromSheet==null||toSheet==null){return;}
        XSSFSheet xs;
        if(toSheet instanceof SXSSFSheet){
            SXSSFSheet sx = (SXSSFSheet) toSheet;
            xs=sx.getWorkbook().getXSSFWorkbook().getSheet(toSheet.getSheetName());
        } else {
            xs = (XSSFSheet) toSheet;}

        //设定合并样式  减少迭代次降低时间复杂度   无传入取整页合并样式
        //xssfWorkbook.getSheetAt(sheetDataIndex).getMergedRegion(i)
        if(CollectionUtils.isEmpty(mergedRegions)){mergedRegions=fromSheet.getMergedRegions();}
        //取自源码 org.apache.poi.xssf.usermodel.XSSFSheet 2921
        for (CellRangeAddress region : mergedRegions) {
            if(region.getFirstRow() > endRowIndex){return;}
            if(region.getFirstRow() >= startRowIndex&& region.getLastRow() <= endRowIndex) {
                CellRangeAddress newRegion=region.copy();
                newRegion.setFirstRow(region.getFirstRow()-startRowIndex+toRowIndex);
                newRegion.setFirstColumn(region.getFirstColumn());
                newRegion.setLastRow(region.getLastRow()-startRowIndex+toRowIndex);
                newRegion.setLastColumn(region.getLastColumn());

                //取自源码 org.apache.poi.xssf.usermodel.XSSFSheet 416 只保留合并操作
                CTWorksheet ctWorksheet = xs.getCTWorksheet();
                CTMergeCells ctMergeCells = ctWorksheet.isSetMergeCells()  ?ctWorksheet.getMergeCells():ctWorksheet.addNewMergeCells();
                CTMergeCell ctMergeCell = ctMergeCells.addNewMergeCell();
                ctMergeCell.setRef(newRegion.formatAsString());
                //     toSheet.addMergedRegionUnsafe(newRegion);
            }
        }


    }


    public static List<CellRangeAddress> getCellRangeAddress(Sheet sheet){
        return sheet.getMergedRegions();
    }



    /**
     * 复制单元格公式
     */
    public static void copyFormulaPOI(Cell srcCell, Cell destCell, Workbook book) {
        String formula = srcCell.getCellFormula();
        EvaluationWorkbook ew;
        FormulaRenderingWorkbook rw;
        Ptg[] ptgs;
        ew = XSSFEvaluationWorkbook.create((XSSFWorkbook) book);
        ptgs = FormulaParser.parse(formula, (XSSFEvaluationWorkbook) ew, FormulaType.CELL, 0);
        rw = (XSSFEvaluationWorkbook) ew;
        for (Ptg ptg : ptgs) {
            int shiftRows = destCell.getRowIndex() - srcCell.getRowIndex();
            int shiftCols = destCell.getColumnIndex() - srcCell.getColumnIndex();
            if (ptg instanceof RefPtgBase) {
                RefPtgBase ref = (RefPtgBase) ptg;
                if (ref.isColRelative()) ref.setColumn(ref.getColumn() + shiftCols);
                if (ref.isRowRelative()) ref.setRow(ref.getRow() + shiftRows);
            } else if (ptg instanceof AreaPtg) {
                AreaPtg ref = (AreaPtg) ptg;
                if (ref.isFirstColRelative()) ref.setFirstColumn(ref.getFirstColumn() + shiftCols);
                if (ref.isLastColRelative()) ref.setLastColumn(ref.getLastColumn() + shiftCols);
                if (ref.isFirstRowRelative()) ref.setFirstRow(ref.getFirstRow() + shiftRows);
                if (ref.isLastRowRelative()) ref.setLastRow(ref.getLastRow() + shiftRows);
            }
        }
        destCell.setCellFormula(FormulaRenderer.toFormulaString(rw, ptgs));
        // 强制重新计算所有公式
        //workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
    }


    /**
     * @description  向行的指定单元格插入数据
     * @author wdx
     */
    public static void rowSlotInsertData(Row dataRow,Map<Integer,Map<Integer,String>>slotData){
        Cell cell;
        //插入单元格自定数据
        if(!CollectionUtils.isEmpty(slotData)&&slotData.get(dataRow.getRowNum())!=null){
            for (Map.Entry<Integer, String> entry : slotData.get(dataRow).entrySet()) {
                cell= dataRow.getCell(entry.getKey())==null?dataRow.createCell(entry.getKey()):dataRow.getCell(entry.getKey());
                cell.setCellValue(entry.getValue());
            }
        }
    }

    /**
     * @description  向单元格插入数据
     * @author wdx
     */
    public  static void  setCellValue(Cell cell,Object value){
        if(cell==null){

        } else if(cell.getCellType()==CellType.FORMULA){

        } else if(value==null){
            cell.setCellValue("");
        } else if(value instanceof String){
            cell.setCellValue(value.toString());
        }else if(value instanceof Integer){
            cell.setCellValue(((Integer) value).doubleValue());
        }else if(value instanceof BigDecimal){
            cell.setCellValue(((BigDecimal) value).doubleValue());
        }else {
            Double d=(Double) value;
            cell.setCellValue(d);}
    }

    /**
     * 表格中指定位置插入行
     * @param sheet 工作表对象
     * @param rowIndex 指定的行数
     * @return 当前行对象
     */
    public static XSSFRow insertRowPOI(XSSFSheet sheet, int rowIndex) {
        XSSFRow row=null;
        if(sheet.getRow(rowIndex) != null) {
            int lastRowNo=sheet.getLastRowNum();
            sheet.shiftRows(rowIndex,lastRowNo,1);
        }
        row=sheet.createRow(rowIndex);
        return row;
    }





}
