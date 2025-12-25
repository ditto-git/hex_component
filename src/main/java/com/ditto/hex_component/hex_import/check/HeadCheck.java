package com.ditto.hex_component.hex_import.check;


import com.ditto.hex_component.hex_import.annotation.HEX_Template;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;


public class HeadCheck{

    private static  final ThreadLocal<HeadCheck> threadLocal = new ThreadLocal<>();

    private static  final  String ROW_CELL_SEPARATOR="&";

    private final Class<?> clazz;

    private final Map<Integer,Map<Integer,String>> headMap= new HashMap<>();

    // 标记是否已解析过表头，避免重复执行
    private boolean isHeadAnalyzed = false;


    public HeadCheck(Class<?> clazz) {
        this.clazz = clazz;
    }

    public static void create(Class<?> clazz){
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        threadLocal.set(new HeadCheck(clazz));
    }

    public static HeadCheck check(){
        HeadCheck headCheck = threadLocal.get();
        if (headCheck == null) {
            throw new IllegalStateException("HeadCheck not initialized, please call create() first");
        }
        return headCheck;
    }

    public static void remove() {
        threadLocal.remove();
    }

    private void analysisHead (){
        if (isHeadAnalyzed || clazz == null) {
            return;
        }
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            if(field.isAnnotationPresent(HEX_Template.class)){
                HEX_Template annotation = field.getAnnotation(HEX_Template.class);
                CellReference cellRef = new CellReference(annotation.cellHead());
                headMap.computeIfAbsent(cellRef.getRow(), k -> new HashMap<>())
                        .put((int) cellRef.getCol(),annotation.headContent());
            }
        }
        isHeadAnalyzed = true; // 标记为已解析
    }



    public void throwsCheckEasyExcel(Integer rowIndex,Map<Integer,String> cellMap) throws RuntimeException {
        if (rowIndex == null || cellMap == null) {
            throw new IllegalArgumentException("rowIndex or cellMap cannot be null");
        }

        analysisHead();

        Map<Integer, String> contentMap = headMap.get(rowIndex);
        if (contentMap == null) return;
        contentMap.forEach((k, v) -> {
            String stringCellValue=cellMap.get(k);
            if (!v.equals(stringCellValue)) {
                threadLocal.remove();
                throw new RuntimeException(String.format("%d%s%d:%s---%s", rowIndex, ROW_CELL_SEPARATOR,  k, stringCellValue, v));
            }
        });
    }




    public CheckResult interruptCheckPoi(XSSFSheet sheet) {
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field field : declaredFields) {
            if(field.isAnnotationPresent(HEX_Template.class)){
                HEX_Template annotation = field.getAnnotation(HEX_Template.class);
                CellReference cellRef = new CellReference(annotation.cellHead());
                String stringCellValue;
                try {
                     stringCellValue = sheet.getRow(cellRef.getRow()).getCell(cellRef.getCol()).getStringCellValue();
                }catch (Exception e){
                    threadLocal.remove();
                    return new CheckResult(false,field.getName()+": interruptCheckPoi 解析异常");
                }

                if(!annotation.headContent().equals(stringCellValue)){
                    return new CheckResult(false,String.format("%d%s%d:%s---%s", cellRef.getRow(), ROW_CELL_SEPARATOR, cellRef.getCol(), stringCellValue, annotation.headContent()));
                }

            }

        }
        threadLocal.remove();
        return new CheckResult(true,"检验成功");
    }




}



/*    private  int match=0;

  public CheckResult checkResult(){
     return   new CheckResult(match == headMap.size() && !"".equals(CHECK_MSG.toString()),CHECK_MSG.toString());
    }

      public void check(int rowIndex, int CellIndex, String head) {
        String s = headMap.get(rowIndex + ROW_CELL_SLIP + CellIndex);
        if (!StringUtils.hasText(s)) {return ;}
        if (s.equals(head)){
            match++;
        }else {
            CHECK_MSG.append(rowIndex + ROW_CELL_SLIP + CellIndex +":"+head+"---"+s+CHECK_MSG_SLIP);
        }
    }*/


