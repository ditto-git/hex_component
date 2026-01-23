package com.ditto.tex_component.tex_util;





import com.ditto.tex_component.tex_console.entity.TexTemplate;
import com.ditto.tex_component.tex_console.entity.TexTemplateCell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TexThreadLocal_bak {

    private static  final  ThreadLocal<TexThread> threadLocal=new ThreadLocal<>();


    private static TexThread getExThread() {
       return  threadLocal.get();
    }

    public static void setExTemplate(TexTemplate texTemplate) {
        threadLocal.set(new TexThread());
        getExThread().setEx(texTemplate);
    }

    public static TexTemplate getExTemplate() {
        return threadLocal.get().getEx();
    }

    public static List<TexTemplateCell> getExCells() {
        return threadLocal.get().getCells();
    }

    public static Map<String,String>  getExHead() {return threadLocal.get().getHead();}

    public static Map<String, Map<String,  Map<String, String>>> getExFormulas() {return threadLocal.get().getFormulas();}



    public static void clear() {
        threadLocal.remove();
    }

}

class TexThread {
    //模板
    public TexTemplate texTemplate;
    //模板单元格/列/行
    public List<TexTemplateCell> hexTemplateCells =new ArrayList<>();
    //模板表头
    public Map<String,String> exTemplateHead=new HashMap<>();
    //模板公式
    public Map<String, Map<String,  Map<String, String>>> formulas=null;


    public void  setEx(TexTemplate texTemplate){
        this.texTemplate = texTemplate;
    }
    public TexTemplate getEx(){
         return  this.texTemplate;
    }


    public List<TexTemplateCell>  getCells(){
        return this.hexTemplateCells;
    }
    public Map<String,String>  getHead(){return this.exTemplateHead;}
    public Map<String, Map<String,  Map<String, String>>>  getFormulas(){
        if(formulas==null){
         //   formulas=TexFormulaUtil.readFormula(this.texTemplate);
        }
        return this.formulas;
    }


}

