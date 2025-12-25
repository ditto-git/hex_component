package com.ditto.hex.hex_export;

import com.ditto.hex.hex_exception.HexException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.ditto.hex.hex_exception.HexExceptionEnum.FILE_EXPORT_ERROR;


@Slf4j
public abstract class SxssfExport {
    //数据记数
    protected Integer count = 1;
    //缓存限数
    protected Integer cacheCount = 10000;
    //缓存记数
    protected int processedCount=0;

    @Getter
    protected SXSSFWorkbook workbook;

    protected XSSFWorkbook xssfWorkbook;

    protected int styleSheetIndex=0 ;

    protected Row initRow;

    protected Map<String, Cell> initColumn = new HashMap<>();

    protected List<Map<String, Object>> dataList;

    protected Map<Integer, Map<Integer, String>> otherCellData = new ConcurrentHashMap<Integer, Map<Integer, String>>();

    @Getter
    protected Integer rowIndex=0;


    protected String sheetName;

    protected boolean copyHeed;

    protected int[] copyHeedRange;

    protected Integer interval = 0;


    public SxssfExport copyHeed(int start,  int end ) {
        this.copyHeedRange = new int[2];
        this.copyHeedRange[0] = start;
        this.copyHeedRange[1] = end;
        this.copyHeed = true;
        return this;
    }

    public SxssfExport count(Integer count) {
        this.count = count;
        return this;
    }

    public SxssfExport copyHeed(boolean copyHeed) {
        this.copyHeed = copyHeed;
        return this;
    }

    public SxssfExport rowIndex(Integer rowIndex) {
        this.rowIndex = rowIndex;
        return this;
    }



    public SxssfExport interval(Integer interval) {
        this.interval = interval;
        return this;
    }

    public SxssfExport sheetName(String sheetName) {
        this.sheetName = sheetName;
        return this;
    }

    protected void flushParam(){
        this.dataList.clear();
        this.otherCellData.clear();
        this.copyHeedRange=null;
        this.sheetName =null;
        this.copyHeed=false;
        this.interval=0;

    }

    /**核心缓存数cacheCount   最小缓存数cacheCount*0.5   最大缓存数cacheCount*1.5 */
    protected void flushRows(SXSSFSheet sxssfSheet) {
        if (rowIndex - sxssfSheet.getLastFlushedRowNum() < cacheCount * 0.5) {
            return;
        }
        log.info("上次刷新位置: " + sxssfSheet.getLastFlushedRowNum());
        try {
            sxssfSheet.flushRows();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new HexException(FILE_EXPORT_ERROR);
        }
    }

    /**分段清除List*/
    protected void clearBatch(List<Map<String, Object>> batch) {
        processedCount += batch.size();
        // 移除已处理数据（关键内存释放点）
        dataList.subList(0, batch.size()).clear();
        // 显式触发GC（可选）
        if (processedCount % cacheCount == 0) {
            System.gc();
        }
        log.info("已处理: "+processedCount+" 条 | 剩余: "+dataList.size()+" 条\n");

    }


    public abstract SxssfExportWrite dataList(List<Map<String, Object>> dataList);


    public abstract static class SxssfExportWrite {

        public abstract SxssfExportWrite otherCellData(IndexSetCellData indexSetCellDataList);

        public abstract SxssfExportWrite otherCellData(List<IndexSetCellData> indexSetCellDataList);

        public abstract void write();

    }


}
