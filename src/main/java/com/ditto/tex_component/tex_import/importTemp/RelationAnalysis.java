package com.ditto.tex_component.tex_import.importTemp;


import com.ditto.tex_component.tex_import.annotation.TEX_Relation;
import com.ditto.tex_component.tex_import.annotation.TEX_Template;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationConstraint;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.lang.reflect.Field;
import java.util.*;


public class RelationAnalysis {

    private  final static String RELATION_TOP= TEX_Relation.RELATION_TOP;

    private   Map<String,List<Dict>> dicts;
    private   Workbook workbook;
    private   String dictSheetName="码表";
    private   Integer dataSheetAt=0;
    private   Integer cellStartRow=0;
    private   Integer lastRow=1000;




    public  Map<String,Map <String,Long>> relationSort = new HashMap<>();

    public RelationAnalysis(Workbook workbook,Map<String,List<Dict>> dicts) {
        this.dicts=dicts;
        this.workbook=workbook;
    }

    /**
     * 生成带级联下拉框的Excel
     * @param clazz 实体类（含CascadeParent/CascadeChild注解）
     * @return Workbook Excel工作簿
     * @throws Exception 反射/POI操作异常
     */
    public  <T> Workbook generateCascadeExcel(Class<T> clazz)  {
        return generateCascadeExcel(clazz,dictSheetName,dataSheetAt,lastRow);
    }

    public  <T> Workbook generateCascadeExcel(Class<T> clazz,String dictSheetName,Integer dataSheetAt,Integer lastRow)  {
        this.dictSheetName=dictSheetName;
        this.dataSheetAt=dataSheetAt;
        this.lastRow=lastRow;
        if (clazz.isAnnotationPresent(TEX_Template.class)){
            this.cellStartRow=clazz.getAnnotation(TEX_Template.class).cellStartRow();
        }

        Sheet dictSheet = workbook.getSheet(dictSheetName);
        dictSheet=dictSheet==null? workbook.createSheet(dictSheetName):dictSheet;
        workbook.setSheetHidden(workbook.getSheetIndex(dictSheetName), true);
        CreationHelper creationHelper = workbook.getCreationHelper();

        Map<String, String> relationMap =new HashMap<>();
        Map<String, Field> fieldMap = new HashMap<>();


        for (Field field : clazz.getDeclaredFields()){
            if (field.isAnnotationPresent(TEX_Relation.class)) {
                TEX_Relation annotation = field.getAnnotation(TEX_Relation.class);
                fieldMap.put(field.getName(),field);
                relationMap.put(field.getName(),annotation.parentField());
            }
        }
         List<String> fieldRelationSort = SortByLevel.sort(relationMap);



        //将子节点数据按父节点sort排序
        Sheet finalDictSheet = dictSheet;
        fieldRelationSort.forEach(k -> {
            Field field = fieldMap.get(k);

            TEX_Relation v = field.getAnnotation(TEX_Relation.class);
                    //获取父节点
                    String parentField = v.parentField();
                    String parentProperty = parentField;
                    if (!RELATION_TOP.equals(parentField)) {
                        parentProperty=fieldMap.get(parentField).getAnnotation(TEX_Relation.class).property();
                        //property为空使用FieldName
                        parentProperty = parentProperty.isEmpty() ? parentField : parentProperty;
                    }

                    //property为空使用FieldName
                    String property = v.property();
                    property = property.isEmpty() ? k: property;
                    //将子节点数据按父节点sort排序
                    relationSort(parentProperty, property, dicts.get(property));

                    /**创建级联关系 */
                    relationToSheet(clazz,field,parentField, finalDictSheet, dicts.get(property));

                }
        );


        dictSheet.setForceFormulaRecalculation(true);

        return workbook;

    }

    private <T> void relationToSheet(Class clazz,Field field,String parentField, Sheet dictSheet, List<Dict> dictList){

        /**创建级联关系 */
        Integer cellIndex = 0;
        Integer parentCellIndex = 0;
        if (field.isAnnotationPresent(TEX_Template.class)) {
            cellIndex = field.getAnnotation(TEX_Template.class).cellIndex();

        } else { //todo ex_Template
        }


        //List<Dict> dictList = dicts.get(property);
        String nameManage =dictList.get(0).getParentCode() ;
        int startIndex = 0;
        for (int endIndex = startIndex; endIndex < dictList.size(); endIndex++) {
            Dict dict = dictList.get(endIndex);

            //插入到dict页
            Row row = dictSheet.getRow(endIndex);
            row=row == null?dictSheet.createRow(endIndex):row;


            row.createCell(cellIndex).setCellValue(dict.getCode());


            //创建名称选择器
            if (!RELATION_TOP.equals(parentField) && !dict.getParentCode().equals(nameManage)) {
                this.creatDataConstraintName(nameManage, creatConstraintNameFormulaIndirect(CellReference.convertNumToColString(cellIndex), startIndex+1, endIndex));
                nameManage = dict.getParentCode();
                startIndex = endIndex;
            }
            if (!RELATION_TOP.equals(parentField) && endIndex==dictList.size()-1) {
                this.creatDataConstraintName(nameManage, creatConstraintNameFormulaIndirect(CellReference.convertNumToColString(cellIndex), startIndex+1, endIndex+1));
            }

        }


        if (RELATION_TOP.equals(parentField)) //todo 横表
            //顶级下拉框
            this.creatDataConstraint((XSSFSheet) workbook.getSheet(workbook.getSheetName(dataSheetAt)), cellIndex, creatConstraintFormulaTop(CellReference.convertNumToColString(cellIndex), dictList.size()));
        else {
            try {
                parentCellIndex = clazz.getDeclaredField(parentField).getAnnotation(TEX_Template.class).cellIndex();
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
            //下级有效性
            this.creatDataConstraint((XSSFSheet) workbook.getSheet(workbook.getSheetName(dataSheetAt)), cellIndex, creatConstraintFormulaIndirect(CellReference.convertNumToColString(parentCellIndex)));

        }


    }

    private  void relationSort(String parentProperty, String property, List<Dict> cascadeFields){

        //获取父节点sort
        Map<String, Long> parentSrortMap = relationSort.get(parentProperty);

        Map<String, Long> sortMap= Collections.emptyMap();
        //已经存在无需创建
        boolean creatSortMap = relationSort.get(property) == null;
        if(creatSortMap){sortMap= new HashMap<>();}
        long sort=0;
        for (Dict cascadeField : cascadeFields) {
            //插入父节点排序号
            if(!RELATION_TOP.equals(property)&&parentSrortMap!=null &&!parentSrortMap.isEmpty()){
                try {
                    cascadeField.setParentSort(parentSrortMap.get(cascadeField.getParentCode()));
                }catch (Exception e){
                    System.err.println("关联关系失效");
                }

            }
            //插入当前节点sort
           if(creatSortMap){ sortMap.put(cascadeField.getCode(),sort++);}
        }
        cascadeFields.sort(Comparator.comparingLong(Dict ::getParentSort));

        relationSort.put(property,sortMap);


    }



    /**
     * @description 顶级下拉框
     * @author wdx
     */
    public String creatConstraintFormulaTop(String repCol,int endIndex){
        return  creatConstraintNameFormulaIndirect(repCol,1, endIndex);
    }


    /**
     * @description 子级下拉框/有效性
     * @author wdx
     */
    public String creatConstraintFormulaIndirect(String repColParent){
        return "INDIRECT($"+repColParent+":$"+repColParent+")";
    }



    /**
     * @description 创建下拉有效性
     * @author wdx
     */
    public void creatDataConstraint(XSSFSheet dataSheet, int repColIndex, String formula){

        //数据有效性对象
        XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper(dataSheet);
        XSSFDataValidationConstraint dataValidationConstraint = (XSSFDataValidationConstraint) dvHelper.createFormulaListConstraint(formula);

        //应用下拉框的区域边界(行列范围)
        CellRangeAddressList addressList = new CellRangeAddressList(cellStartRow, lastRow, repColIndex, repColIndex);
        //下拉列表值验证
        XSSFDataValidation dataValidation = (XSSFDataValidation) dvHelper.createValidation(dataValidationConstraint, addressList);
        dataValidation.setSuppressDropDownArrow(true);
        dataValidation.setShowErrorBox(true);
        dataValidation.createErrorBox("输入值有误", "请从下拉框选择");
        dataSheet.addValidationData(dataValidation);
    }

    /**
     * @description 子级名称管理器
     * @author wdx
     */
    public String creatConstraintNameFormulaIndirect(String repCol,int startIndex, int endIndex){
        return dictSheetName+"!"+"$"+repCol+"$"+startIndex+":"+"$"+repCol+"$"+endIndex;
    }


    /**
     * @description  名称管理器
     * @author wdx
     */
    public void creatDataConstraintName(String name,String constraintNameFormula){
      System.out.println(name +"   :  "+constraintNameFormula);
        //创建名称管理器
        try {
            Name xssfName = workbook.createName();
            xssfName.setNameName(name);
            xssfName.setRefersToFormula(constraintNameFormula);
        }catch (Exception e){
           // logger.error("名称管理器创建失败:"+name+"重复或无效",e);
            }

    }








}
