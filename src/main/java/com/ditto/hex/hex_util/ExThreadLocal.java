package com.ditto.hex.hex_util;


import com.ditto.hex.hex_console.entity.HexTemplate;
import com.ditto.hex.hex_console.entity.HexTemplateCell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExThreadLocal {

    private static  final  ThreadLocal<ExThread> threadLocal=new ThreadLocal<>();


    private static ExThread getExThread() {
       return  threadLocal.get();
    }

    public static void setExTemplate(HexTemplate hexTemplate) {
        threadLocal.set(new ExThread());
        getExThread().setEx(hexTemplate);
    }

    public static HexTemplate getExTemplate() {
        return threadLocal.get().getEx();
    }

    public static List<HexTemplateCell> getExCells() {
        return threadLocal.get().getCells();
    }

    public static Map<String,String>  getExHead() {return threadLocal.get().getHead();}

    public static Map<String, Map<String,  Map<String, String>>> getExFormulas() {return threadLocal.get().getFormulas();}



    public static void clear() {
        threadLocal.remove();
    }

}

class ExThread{
    //模板
    public HexTemplate hexTemplate;
    //模板单元格/列/行
    public List<HexTemplateCell> hexTemplateCells =new ArrayList<>();
    //模板表头
    public Map<String,String> exTemplateHead=new HashMap<>();
    //模板公式
    public Map<String, Map<String,  Map<String, String>>> formulas=null;


    public void  setEx(HexTemplate hexTemplate){
        this.hexTemplate = hexTemplate;
    }
    public HexTemplate getEx(){
         return  this.hexTemplate;
    }


    public List<HexTemplateCell>  getCells(){
        return this.hexTemplateCells;
    }
    public Map<String,String>  getHead(){return this.exTemplateHead;}
    public Map<String, Map<String,  Map<String, String>>>  getFormulas(){
        if(formulas==null){
            formulas=ExFormula.readFormula(this.hexTemplate);
        }
        return this.formulas;
    }


}

