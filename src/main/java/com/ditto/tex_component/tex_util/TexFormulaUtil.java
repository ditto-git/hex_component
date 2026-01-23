package com.ditto.tex_component.tex_util;



import com.ditto.tex_component.tex_console.entity.TexTemplate;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.poi.ss.util.CellReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.wltea.expression.ExpressionEvaluator;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class TexFormulaUtil {
    private static Logger logger = LoggerFactory.getLogger(TexFormulaUtil.class);

    //空白字符
    public final static String BLANK = "";
    //公式分割符
    public final static String SPLIT_FORMULA = "\\$";
    //节点分割符
    public final static String SPLIT_NODE = ":";


    //DATA_UNION公式数据链接符
    public final static String SPLIT_OP = "#";
    //DATA_UNION合并拼接符
    public final static String SPLIT_UNION = "&";
    //DATA_UNION替换符
    public final static String SPLIT_BRACKET_L = "\\(";
    public final static String SPLIT_BRACKET_R  = ")";
    public final static int DEFAULT_WEIGHT  = 0;

    //当前字段的公式执行权重
    public final static String CELL_FORMULA_WT = "WT";
    //数据运算
    public final static String DATA_CALCULATION = "DATA_CALCULATION";
    //数据合并及补替
    public final static String DATA_UNION = "DATA_UNION";
    //数据直取不做任何处理
    public final static String DATA_N = "DATA_N";
    //取值保留一位小数
    public final static String DATA_AC1 = "AC1";
    //取值保留两位小数(默认)
    public final static String DATA_AC2 = "AC2";
    //表头公式
    public final static String FORMULA_HEAD = "FORMULA_HEAD";
    //数据公式
    public final static String FORMULA_CELL = "FORMULA_CELL";

    /**
     * @description 读取数据公式
     * @author wdx
     */
    public static  List<TexFormula> readFormula(TexTemplate texTemplate) {

        List<TexFormula> texFormulaList = new ArrayList<>();

        texTemplate.getTexTemplateCells().forEach(sxtc -> {

                    //读取cellFormula
                    if (StringUtils.hasText(sxtc.getCellFormula()) && sxtc.getCellFormula().contains(SPLIT_NODE)) {
                       Map<String, String> cellFormulas = new HashMap<>();new HashMap<>();


                        //读取成多条独立公式
                        Arrays.stream(sxtc.getCellFormula().split(SPLIT_FORMULA)).forEach(formula -> {
                            try {
                                //key=公式类型，value=实际公式
                                if (formula.contains(SPLIT_NODE)) {
                                    cellFormulas.put(formula.split(SPLIT_NODE)[0], formula.split(SPLIT_NODE)[1]);
                                }
                            } catch (Exception e) {
                                logger.error("公式读取失败：" + formula, e);
                            }

                        });

                        if(!CollectionUtils.isEmpty(cellFormulas)){
                            String weight = cellFormulas.get(CELL_FORMULA_WT);
                            int weightInt= StringUtils.hasText(weight)&&weight.matches("^-?[\\d.]+$") ? Integer.parseInt(weight):DEFAULT_WEIGHT;
                            texFormulaList.add(new TexFormula(sxtc.getCellProperty(),cellFormulas,weightInt)) ;
                        }

                    }
                }

        );

        if(!CollectionUtils.isEmpty(texFormulaList)){
            texFormulaList.sort(Comparator.comparing(TexFormula::getWeight));
        }
        return texFormulaList;
    }

    /**
     * @description 公式解析
     * @author wdx
     */
    public static void cellFormulaBatch(List<TexFormula>texFormulaList, List<Map<String, Object>> dataList) {
        //读取cellFormula
        if (CollectionUtils.isEmpty(texFormulaList)) {return;}
        //遍历每条数据，
        dataList.forEach(data -> {
            texFormulaList.forEach(texFormula-> {
                Object o = cellFormulaAnalysis(texFormula.getProperty(), texFormula.getCellFormulas(), data);
                data.put(texFormula.getProperty(), o);
            });
        });

    }

    /**
     * @description 公式解析
     * @author wdx
     */
    public static void cellFormula(List<TexFormula>texFormulaList, Map<String, Object> data) {
        //读取cellFormula
        if (CollectionUtils.isEmpty(texFormulaList)) {return;}
        texFormulaList.forEach(texFormula -> {
                    Object o = cellFormulaAnalysis(texFormula.getProperty(), texFormula.getCellFormulas(), data);
                    data.put(texFormula.getProperty(), o);
                });

    }



    /**
     * @description 解析数据公式
     * @author wdx
     */
    private static Object cellFormulaAnalysis(String property, Map<String, String> propertyFormulas, Map<String, Object> data) {

        Object value = data.get(property);
        //为null返回
        if (value == null) {
            value= BLANK;
        }

        /** 公式有序执行*/


        //数据直取不做任何处理
        if (propertyFormulas.get(DATA_N) != null) {
            value.toString().trim();
            return value;
        }

        //数据合并及补替
        if (propertyFormulas.get(DATA_UNION) != null) {
            value = unionData(propertyFormulas.get(DATA_UNION), data);
        }
        //数据运算
        if (propertyFormulas.get(DATA_CALCULATION) != null) {
            value = calculationData(propertyFormulas.get(DATA_CALCULATION), data);
        }


        //**小数保留两位/一位  多配置取小位 *//*
        if (propertyFormulas.get(DATA_AC1) != null) {
            value = ac1(value);
        } else  {
            //默认小数保留两位
            value = ac2(value);
        }
        value = valueFloatAccuracy(value);
        return value;

    }


    /**
     * @description 数据合并及补替 DATA_UNION 例：CLO5(CLO4)&CLO2#-
     * @author wdx
     */
    private static String unionData(String formula, Map<String, Object> data) {

        //"#"右侧取连接符union 默认连接符为空白字符
        String[] unionFormula = formula.split(SPLIT_OP);
        String union = unionFormula.length > 1 ? unionFormula[1] : BLANK;


        //字典倒序  避免错误替换 CL15=12 CL152=34  CL15+CL152  正确替换12+34  错误替换  12+122
        String[] cloumns = unionFormula[0].split(SPLIT_UNION);
        Arrays.sort(cloumns, Comparator.reverseOrder());
        formula = unionFormula[0];

        String value;
        for (String cloumn : cloumns) {
            //补替 如果（）外为空，取括号内
            String[] bts = cloumn.split(SPLIT_BRACKET_L);
            value = data.get(bts[0]) == null ? BLANK : data.get(bts[0]).toString();
            value = !StringUtils.hasText(value) && bts.length > 1 && data.get(bts[1].replace(SPLIT_BRACKET_R, BLANK)) != null ? data.get(bts[1].replace(SPLIT_BRACKET_R, BLANK)).toString() : value;
            //数据替换cloumn
            formula = formula.replace(cloumn, value);
            //为空处理无效拼接
            if (!StringUtils.hasText(value)) {
                formula = formula.replaceAll(SPLIT_UNION + SPLIT_UNION, SPLIT_UNION);
            }
        }

        return formula.replaceAll(SPLIT_UNION, StringEscapeUtils.unescapeJava(union));
    }

    /**
     * @description 数据运算  DATA_OPERATION 例：CLO1*(CLO2+CLO3-CL16) /CLO5
     * @author wdx
     */
    private static Object calculationData(String formula, Map<String, Object> data) {
        Object evaluate = BLANK;

       /*遍历转换法
            检索到运算符时, 将前面内容做为key获得具体value,如果本身就是数字，直接用做运算。
            最后一位单独处理
        */

        //初始化cloumn拼接
        String cloumn;
        StringBuffer cloumnBuffer = new StringBuffer();
        //初始化算数拼接
        StringBuffer formartBuffer = new StringBuffer();
        try {
            final char[] chars = formula.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                //遇到运算符，查数拼接运算符
                if ("+-*/()".contains(chars[i] + BLANK)) {
                    if (StringUtils.hasText(cloumnBuffer.toString())) {
                        //数字直接加入运算，非数字进行获取具体值，值为空设置为0
                        cloumn = cloumnBuffer.toString();
                        formartBuffer.append(Double.parseDouble(cloumn.matches("^-?[\\d.]+$") ? cloumn : data.get(cloumn) == null || !StringUtils.hasText(data.get(cloumn).toString()) ? "0" : data.get(cloumn).toString()));
                        cloumnBuffer.delete(0, cloumnBuffer.length());
                    }
                    //拼接运算符
                    formartBuffer.append(chars[i]);
                } else {
                    //拼接cloumn
                    cloumnBuffer.append(chars[i]);
                }

                //到最后字符且最后一位不为符号
                if (i == chars.length - 1 && StringUtils.hasText(cloumnBuffer.toString())) {
                    //数字直接加入运算，非数字进行获取
                    cloumn = cloumnBuffer.toString();
                    formartBuffer.append(Double.parseDouble(cloumn.matches("^-?[\\d.]+$") ? cloumn : data.get(cloumn) == null || !StringUtils.hasText(data.get(cloumn).toString()) ? "0" : data.get(cloumn).toString()));
                    cloumnBuffer.delete(0, cloumnBuffer.length());
                }

            }

            //运算
            evaluate = ExpressionEvaluator.evaluate(formartBuffer.toString());
        } catch (Exception e) {
            logger.error("[calculation]公式运算有误：" + formula + "=" + formartBuffer);
        }
        return evaluate;
    }

    /**
     * @description 小数保留两位 DATA_AC2 数值为Double  序号为Integer 其他数据为String类型
     * @author wdx
     */
    private static Object ac2(Object value) {
        if (value instanceof Double) {
            //小于0.01的数强制算作0.01
            Double o = (Double) value;
            if (0.00 < o && o < 0.01) {
                return 0.01;
            }
            DecimalFormat df = new DecimalFormat("#.##");
            return Double.parseDouble(df.format(value));
        }

        return value;
    }

    /**
     * @description 小数保留一位 DATA_AC1  数值为Double  序号为Integer 其他数据为String类型
     * @author wdx
     */
    private static Object ac1(Object value) {
        if (value instanceof Double) {
            Double o = (Double) value;
            //小于0.01的数强制算作0.1
            if (0 < o && o < 0.1) {
                return 0.1;
            }
            DecimalFormat df = new DecimalFormat("#.#");
            return Double.parseDouble(df.format(value));
        }
        return value;
    }


    /**
     * @description 数值为Double  序号为Integer 其他数据为String类型
     * 目的： 数值强转字符串在生成Excel文档时会出现 警示角标：数字为文本类型
     * @author wdx
     */
    private static Object valueFloatAccuracy(Object value) {
        //处理数据:返空字符串
        if (value == null) {
            return BLANK;
        }

        if (value instanceof String) {

            String s = value.toString();
            //正数或小数转为Double
            if (s.matches("^-?[\\d.]+$")) {
                value = Double.parseDouble(s);
            } else {
                //处理数据:去空格
                return s.trim();
            }
        }

        //处理数据:==0返空
        if (value instanceof Double) {
            if ((Double) value == 0) {
                return BLANK;
            }
        }

        return value;
    }


    /**
     * @description DateFormatter
     * @author wdx
     */
    public  static  String getDateFormatter(){
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日");
        return formatter.format(date);}
    /**
     * @description DateFormatter
     * @author wdx
     */
    public  static  String getDateFormatter(String ymonth){
        if(StringUtils.hasText(ymonth)){return ymonth;}
        return ymonth.substring(0,4)+"年"+ymonth.substring(4,6)+"月";
    }



    /**
     * @description 组排序
     * @author wdx
     */
//    public  static  List<String> groupKeySort(List<String> keys){
//        //&分割
//        String groupSplit="&";
//        //多级排序 如：10 10-1 ，10-1-1 ，10-2 ,10-2-2  11
//        String sortNe="-";
//        //预留两位 支持[10-100] 、[10-100-100]    不支持[10-1000]、[10-1000-1000]
//        double d=0.001;
//
//        List<HashMap<String, Object> > sortList=new ArrayList<>();
//        keys.forEach(s-> {
//            //分割符在首位 说明排序号为空,给999排最后
//            if (s.indexOf(groupSplit) == 0) {s = "999" + s;}
//
//            //取排序码
//            String sort = s.split(groupSplit)[0];
//            //拆分 [0]=一级 [1]=二级 [2]=三级。。。。
//            String[] sorts = sort.split(sortNe);
//
//            //初始化运算公式
//            String operation = "";
//            //生成运算公式    [10-1]= 10+1*0.001  [10-100]= 10+100*0.001
//            for (int i = 0; i < sorts.length; i++) {
//                //首位初始化整数位
//                if (i == 0) {
//                    operation = sorts[i] + "+0.00";
//                } else {
//                    //相加小数位置   d的i次幂：[10-2-8]  2*0.001  +8*0.000001
//                    operation = operation + "+" + sorts[i] + "*" + Math.pow(d, i);
//                }
//            }
//            HashMap<String, Object> sortMap = new HashMap<>();
//            sortMap.put("sort",ExpressionEvaluator.evaluate(operation));
//            sortMap.put("key",s);
//            sortList.add(sortMap);
//        });
//        //以排序码排序
//        sortList.sort(Comparator.comparingDouble(m -> (Double) m.get("sort")));
//        keys.clear();
//        sortList.forEach(s->{
//           keys.add(s.get("key").toString());});
//
//        return keys;
//    }

    /**
     * @description 返回单元格Index
     * 例A=0 B=1
     * @author wdx
     */
    private static void printIndex(String cellName) {
        System.out.print(cellName + "=" + new CellReference(cellName).getCol());
    }


    public static void main(String[] args) {
        printIndex("AF");
    }


}
