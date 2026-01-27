package com.ditto.tex_component.tex_export;

import com.ditto.tex_component.tex_console.entity.TexTemplateCell;
import com.ditto.tex_component.tex_exception.TexException;
import com.ditto.tex_component.tex_util.TexCellUtil;
import com.ditto.tex_component.tex_util.TexFormulaUtil;
import com.ditto.tex_component.tex_util.TexThreadLocal;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.PaneInformation;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.ditto.tex_component.tex_exception.TexExceptionEnum.TEMP_IMPORT_ERROR;


@Slf4j
public class SxssfExportRow extends SxssfExport {

    private Integer heedHeight=0;


    public SxssfExportWrite dataList(List<Map<String, Object>> dataList) {
        this.dataList = dataList;
        return new SxssfExportRowWrite();
    }

    protected SxssfExportRow(InputStream inputStream) {
        try {
            this.xssfWorkbook = new XSSFWorkbook(inputStream);
            this.workbook = new SXSSFWorkbook(xssfWorkbook, -1);
            //初始(样式)列
            List<TexTemplateCell> hexTemplateCells = TexThreadLocal.getExTemplate().getTexTemplateCells();
            hexTemplateCells.sort(Comparator.comparing(TexTemplateCell::getCellIndex));
            for (TexTemplateCell cell : hexTemplateCells) {
                initColumn.put(cell.getCellProperty(), xssfWorkbook.getSheetAt(styleSheetIndex).getRow(Integer.parseInt(cell.getCellIndex())).getCell(Integer.parseInt(cell.getCellStartCol())));
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new TexException(TEMP_IMPORT_ERROR);
        }

    }

    public class SxssfExportRowWrite extends SxssfExportWrite {

        public SxssfExportRowWrite otherCellData(List<IndexSetCellData> indexSetCellDataList) {
            for (IndexSetCellData indexSetCellData : indexSetCellDataList) {
                int r = indexSetCellData.isDownward() ? indexSetCellData.getRowIndex() + rowIndex : indexSetCellData.getRowIndex();
                otherCellData.computeIfAbsent(r, kv -> new HashMap<>());
                otherCellData.get(r).put(indexSetCellData.getCellIndex(), indexSetCellData.getValue());
            }
            return this;
        }

        public SxssfExportRowWrite otherCellData(IndexSetCellData indexSetCellData) {
            int r = indexSetCellData.isDownward() ? indexSetCellData.getRowIndex() + rowIndex : indexSetCellData.getRowIndex();
            otherCellData.computeIfAbsent(r, kv -> new HashMap<>());
            otherCellData.get(r).put(indexSetCellData.getCellIndex(), indexSetCellData.getValue());
            return this;
        }


        public void write() {

            if (CollectionUtils.isEmpty(dataList) || CollectionUtils.isEmpty(TexThreadLocal.getExTemplate().getTexTemplateCells())) {
                return;
            }

            //解析EX公式
            TexFormulaUtil.cellFormulaBatch(TexThreadLocal.getExFormulas(), dataList);
            List<TexTemplateCell> hexTemplateCells = TexThreadLocal.getExTemplate().getTexTemplateCells();

            //首行末行信息
            Row firstRow = initColumn.get(hexTemplateCells.get(0).getCellProperty()).getRow();
            Row lastRow = initColumn.get(hexTemplateCells.get(hexTemplateCells.size() - 1).getCellProperty()).getRow();
            int lastCellNum = firstRow.getLastCellNum();
            int initCell = Integer.parseInt(hexTemplateCells.get(0).getCellStartCol());
            int cellIndex = initCell;


            //默认首页,新生成页复制首页样式
            SXSSFSheet sxssfSheet = sheetName == null ? workbook.getSheetAt(styleSheetIndex): workbook.getSheet(sheetName);
            if (sxssfSheet == null) {
                sxssfSheet=workbook.createSheet(sheetName);

                // 复制列宽
                for (int i = 0; i <= lastCellNum; i++) { // 遍历行，确保所有行都被考虑在内
                    sxssfSheet.setColumnWidth(i, workbook.getSheetAt(styleSheetIndex).getColumnWidth(i));
                }

                //设置目标sheet冻结对应行列
                PaneInformation paneInformation =  workbook.getSheetAt(styleSheetIndex).getPaneInformation();
                if(paneInformation!=null&&paneInformation.isFreezePane()){
                    sxssfSheet.createFreezePane(paneInformation.getHorizontalSplitPosition(), paneInformation.getVerticalSplitPosition()
                            , paneInformation.getHorizontalSplitTopRow(), paneInformation.getVerticalSplitLeftColumn());
                }

            }

            List<CellRangeAddress> cellRangeAddress = TexCellUtil.getCellRangeAddress(xssfWorkbook.getSheetAt(styleSheetIndex));

            //复制表头
            if (copyHeed && (rowIndex > lastRow.getRowNum() ||!sxssfSheet.getSheetName().equals(workbook.getSheetAt(styleSheetIndex).getSheetName()))) {
                copyHead(sxssfSheet,cellRangeAddress);
            }

            if(!sxssfSheet.getSheetName().equals(workbook.getSheetAt(styleSheetIndex).getSheetName())||rowIndex!=0){
                TexCellUtil.copyRowsPOI(firstRow.getRowNum(), lastRow.getRowNum(), rowIndex+heedHeight, firstRow.getSheet(), sxssfSheet, true, cellRangeAddress);
            }

            //用空字符串补全最后一个表
            int dataSize = dataList.size();
            int plugSize = (dataSize / lastCellNum == 0 ? 0 : (lastCellNum - initCell) - (dataSize % (lastCellNum - initCell)));
            for (int p = 0; p < plugSize; p++) {
                dataList.add(Collections.emptyMap());
            }

            //插入数据
            Cell dataCell;
            Row dataRow = null;
            while (!dataList.isEmpty()) {
                // 获取当前批次数据（cacheCount条）
                List<Map<String, Object>> batch = dataList.subList(0, Math.min(cacheCount, dataList.size()));

                for (Map<String, Object> data : batch) {

                    if (cellIndex > lastCellNum - 1) {
                        //换行
                        rowIndex = dataRow.getSheet().getLastRowNum() + 1;
                        heedHeight=0;
                        //插入单元格自定数据
                        if (!CollectionUtils.isEmpty(otherCellData)) {
                            for (Integer key : otherCellData.keySet()) {
                                if (key > rowIndex - 1) {
                                    continue;
                                }
                                dataRow = key <= lastRow.getRowNum() && lastRow.getSheet().getSheetName().equals(sxssfSheet.getSheetName()) ?
                                        xssfWorkbook.getSheetAt(0).getRow(key)
                                        : sxssfSheet.getRow(key) == null ? sxssfSheet.createRow(key) : sxssfSheet.getRow(key);
                                for (Map.Entry<Integer, String> entry : otherCellData.get(key).entrySet()) {
                                    dataCell = dataRow.getCell(entry.getKey()) == null ? dataRow.createCell(entry.getKey()) : dataRow.getCell(entry.getKey());
                                    dataCell.setCellValue(entry.getValue());
                                }
                                otherCellData.remove(key);
                            }
                        }


                        //写到临时文件
                        flushRows(sxssfSheet);

                        //分段间隔行数
                        rowIndex += interval;

                        //重置列索引
                        cellIndex = initCell;
                        if (rowIndex != 0) {
                            TexCellUtil.copyRowsPOI(firstRow.getRowNum(), lastRow.getRowNum(), rowIndex, firstRow.getSheet(), sxssfSheet, true, cellRangeAddress);
                            // TexCellUtil.copyRowsPOI(firstRow.getRowNum(), lastRow.getRowNum(), rowIndex, 0, cellIndex, firstRow.getSheet(), sxssfSheet, true, true);
                        }
                    }

                    for (TexTemplateCell hexTemplateCell : hexTemplateCells) {
                        //添加序号
                        if ("XH".equals(hexTemplateCell.getCellProperty())) {
                            data.put("XH", count++);
                        }
                        //添加插入值
                        try {
                            if(lastRow.getSheet().getSheetName().equals(sxssfSheet.getSheetName()) ){
                                dataRow = rowIndex <= lastRow.getRowNum() ?
                                        xssfWorkbook.getSheetAt(styleSheetIndex).getRow(Integer.parseInt(hexTemplateCell.getCellIndex()))
                                        : sxssfSheet.getRow(heedHeight+Integer.parseInt(hexTemplateCell.getCellIndex()) + rowIndex - firstRow.getRowNum());
                            }else {

                                dataRow =sxssfSheet.getRow(heedHeight+Integer.parseInt(hexTemplateCell.getCellIndex()) + rowIndex - firstRow.getRowNum());
                            }


                            dataCell = dataRow.getCell(cellIndex);
                            TexCellUtil.setCellValue(dataCell, CollectionUtils.isEmpty(data) ? "" : data.get(hexTemplateCell.getCellProperty()));
                        }catch (Exception e){
                            System.out.println(heedHeight+Integer.parseInt(hexTemplateCell.getCellIndex()) + rowIndex - firstRow.getRowNum());
                            e.printStackTrace();
                        }

                    /*    dataCell = dataRow.getCell(cellIndex) == null ? dataRow.createCell(cellIndex) : dataRow.getCell(cellIndex);
                        if (cellIndex > initCell) {
                            TexCellUtil.copyCellPOI(initColumn.get(exTemplateCell.getCellProperty()), dataCell, false);
                        }*/



                    }
                    cellIndex++;


                }

                clearBatch(batch);
            }

            rowIndex = dataRow.getSheet().getLastRowNum() + 1;
            //分段间隔行数
            rowIndex += interval;

            //刷新文档公式
            sxssfSheet.setForceFormulaRecalculation(true);

            //刷新参数
            flushParam();
        }




        private void copyHead(SXSSFSheet sxssfSheet,List<CellRangeAddress> cellRangeAddress) {
            Row firstRow = initColumn.get(TexThreadLocal.getExTemplate().getTexTemplateCells().get(0).getCellProperty()).getRow();
            if(firstRow.getRowNum() <1 ){return;}
            int startRowIndex = copyHeedRange == null ? 0 : copyHeedRange[0];
            int endRowIndex = copyHeedRange == null ? firstRow.getRowNum() - 1 : copyHeedRange[1];
            TexCellUtil.copyRowsPOI(startRowIndex, endRowIndex, rowIndex, firstRow.getSheet(), sxssfSheet, true, cellRangeAddress);
            heedHeight=endRowIndex - startRowIndex + 1;
           // rowIndex += heedHeight;
        }


    }
}
