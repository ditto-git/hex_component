package com.ditto.hex_component.hex_console.exListener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.ditto.hex_component.hex_console.entity.HexTemplateCell;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;


@Slf4j
public class ExTemplateCellListener implements ReadListener<HexTemplateCell> {

    /**
     * 缓存的数据
     */
    private List<HexTemplateCell> dataList=new ArrayList<HexTemplateCell>();


    public ExTemplateCellListener() {

    }


    /**
     * 这个每一条数据解析都会来调用
     *
     * @param data    one row value. Is is same as {@link AnalysisContext#readRowHolder()}
     * @param context
     */
    @Override
    public void invoke(HexTemplateCell data, AnalysisContext context) {
        dataList.add(data);
    }

    /**
     * 所有数据解析完成了 都会来调用
     *
     * @param context
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("所有数据解析完成！");
    }

    /**
     * 加上存储数据库
     */
    public  List<HexTemplateCell> getDataList() {
        log.info("{}条数据，开始存储数据库！", dataList.size());
        return dataList;
    }
}
