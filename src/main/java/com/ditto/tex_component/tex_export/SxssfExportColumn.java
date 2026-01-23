package com.ditto.tex_component.tex_export;



import com.ditto.tex_component.tex_console.entity.TexTemplateCell;
import com.ditto.tex_component.tex_exception.TexException;
import com.ditto.tex_component.tex_util.ExCellUtil;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ditto.tex_component.tex_exception.TexExceptionEnum.TEMP_IMPORT_ERROR;


@Slf4j
public class SxssfExportColumn extends SxssfExport {


    protected SxssfExportColumn(InputStream inputStream) {
        try {
            this.xssfWorkbook = new XSSFWorkbook(inputStream);
            this.workbook = new SXSSFWorkbook(xssfWorkbook, -1);
            //初始行(样式,参数)
            this.initRow = this.xssfWorkbook.getSheetAt(styleSheetIndex).getRow(Integer.parseInt(TexThreadLocal.getExTemplate().getTexTemplateCells().get(0).getCellStartRow()));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new TexException(TEMP_IMPORT_ERROR);
        }

    }

    public SxssfExportColumnWrite dataList(List<Map<String, Object>> dataList) {
        this.dataList = dataList;
        return new SxssfExportColumnWrite();
    }


    public class SxssfExportColumnWrite extends SxssfExportWrite {

        public SxssfExportColumnWrite otherCellData(List<IndexSetCellData> indexSetCellDataList) {
            if (!CollectionUtils.isEmpty(indexSetCellDataList)) {
                for (IndexSetCellData indexSetCellData : indexSetCellDataList) {
                    otherCellData(indexSetCellData);
                }
            }
            return this;
        }

        public SxssfExportColumnWrite otherCellData(IndexSetCellData indexSetCellData) {
            int r = indexSetCellData.isDownward()&&rowIndex !=initRow.getRowNum() ?
                    indexSetCellData.getRowIndex() + rowIndex
                    : indexSetCellData.getRowIndex();
            otherCellData.computeIfAbsent(r, kv -> new HashMap<>());
            otherCellData.get(r).put(indexSetCellData.getCellIndex(), indexSetCellData.getValue());
            return this;
        }


        public void write() {

            if (CollectionUtils.isEmpty(dataList) || CollectionUtils.isEmpty(TexThreadLocal.getExTemplate().getTexTemplateCells())) {
                return;
            }
            //默认首页,新生成页复制首页样式
            SXSSFSheet sxssfSheet = sheetName == null ? workbook.getSheetAt(styleSheetIndex): workbook.getSheet(sheetName);
            if (sxssfSheet == null) {
                sxssfSheet=workbook.createSheet(sheetName);

                // 复制列宽
                for (int i = 0; i <= initRow.getLastCellNum(); i++) { // 遍历行，确保所有行都被考虑在内
                    sxssfSheet.setColumnWidth(i, workbook.getSheetAt(styleSheetIndex).getColumnWidth(i));
                }

                //设置目标sheet冻结对应行列
                PaneInformation paneInformation =  workbook.getSheetAt(styleSheetIndex).getPaneInformation();
                if(paneInformation!=null&&paneInformation.isFreezePane()){
                    sxssfSheet.createFreezePane(paneInformation.getHorizontalSplitPosition(), paneInformation.getVerticalSplitPosition()
                            , paneInformation.getHorizontalSplitTopRow(), paneInformation.getVerticalSplitLeftColumn());
                }

            }


            //确立起始行
            sheetRowIndex(sxssfSheet);
            //复制表头
            if (copyHeed) {
                copyHead(sxssfSheet, ExCellUtil.getCellRangeAddress(xssfWorkbook.getSheetAt(styleSheetIndex)));
            }

            //插入数据
            Row dataRow ;
            Cell cell;
            while (!dataList.isEmpty()) {
                // 获取当前批次数据（cacheCount条）
                List<Map<String, Object>> batch = dataList.subList(0, Math.min(cacheCount, dataList.size()));

                //解析EX公式
                TexFormulaUtil.cellFormulaBatch(TexThreadLocal.getExFormulas(), batch);

                //插入数据到文档
                for (Map<String, Object> data : batch) {
                    dataRow = rowIndex == initRow.getRowNum() && initRow.getSheet().getSheetName().equals(sxssfSheet.getSheetName()) ? initRow : sxssfSheet.createRow(rowIndex);
                    //从初始(样式)行,复制样式
                    if (!(rowIndex == initRow.getRowNum() && initRow.getSheet().getSheetName().equals(sxssfSheet.getSheetName()))) {
                        ExCellUtil.copyRowPOI(initRow, dataRow, false, false);
                    }
                    for (TexTemplateCell hexTemplateCell : TexThreadLocal.getExTemplate().getTexTemplateCells()) {
                        //添加序号
                        if ("XH".equals(hexTemplateCell.getCellProperty())) {
                            data.put("XH", count++);
                        }
                        //添加插入值
                        ExCellUtil.setCellValue(dataRow.getCell(Integer.parseInt(hexTemplateCell.getCellIndex())), data.get(hexTemplateCell.getCellProperty()));
                    }
                    rowIndex++;
                }

                //插入单元格自定数据
                if (!CollectionUtils.isEmpty(otherCellData)) {
                    for (Integer key : otherCellData.keySet()) {
                        if (key > rowIndex - 1) {
                            continue;
                        }

                        dataRow = key <= initRow.getRowNum() && initRow.getSheet().getSheetName().equals(sxssfSheet.getSheetName()) ? initRow.getSheet().getRow(key) :
                                sxssfSheet.getRow(key) == null ? sxssfSheet.createRow(key) : sxssfSheet.getRow(key);

                        for (Map.Entry<Integer, String> entry : otherCellData.get(key).entrySet()) {
                            cell = dataRow.getCell(entry.getKey()) == null ? dataRow.createCell(entry.getKey()) : dataRow.getCell(entry.getKey());
                            cell.setCellValue(entry.getValue());
                        }
                        otherCellData.remove(key);
                    }
                }


                clearBatch(batch);

                //写到临时文件
                flushRows(sxssfSheet);
            }


            //分段间隔行数
            rowIndex += interval;

            //刷新文档公式
            sxssfSheet.setForceFormulaRecalculation(true);

            //刷新参数
            flushParam();
        }

        /*核心缓存数cacheCount   最小缓存数cacheCount*0.5   最大缓存数cacheCount*1.5 */
//        private void flushRows(SXSSFSheet sxssfSheet) {
//            if (rowIndex - sxssfSheet.getLastFlushedRowNum() < cacheCount * 0.5) {
//                return;
//            }
//            // System.out.println("上次刷新位置： "+sxssfSheet.getLastFlushedRowNum());
//            try {
//                sxssfSheet.flushRows();
//            } catch (IOException e) {
//                log.error(e.getMessage(), e);
//                throw new HEX_Exception(FILE_EXPORT_ERROR);
//            }
//
//        }

        /**
         * @description  首页（样式页） 从插数位置开始，其他页都是从0开始
         */
        private void sheetRowIndex(SXSSFSheet sxssfSheet) {
            if(rowIndex==0&&sxssfSheet.getSheetName().equals(workbook.getSheetAt(styleSheetIndex).getSheetName())){rowIndex = initRow.getRowNum();}
        }


        private void copyHead(SXSSFSheet sxssfSheet,List<CellRangeAddress> cellRangeAddress) {
            //不存在表头, 首页（样式页）首个插数位置,不需要复制表头
            if(initRow.getRowNum() < 1 || (sxssfSheet.getSheetName().equals(initRow.getSheet().getSheetName())&&rowIndex <= initRow.getRowNum())){return;}
            int startRowIndex = copyHeedRange == null ? 0 : copyHeedRange[0];
            int endRowIndex = copyHeedRange == null ? initRow.getRowNum() - 1 : copyHeedRange[1];
            ExCellUtil.copyRowsPOI(startRowIndex, endRowIndex, rowIndex, initRow.getSheet(), sxssfSheet, true, cellRangeAddress);
            rowIndex += endRowIndex - startRowIndex + 1;
        }

    }


}
