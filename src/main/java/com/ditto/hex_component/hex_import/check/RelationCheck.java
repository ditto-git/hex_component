package com.ditto.hex_component.hex_import.check;


import com.ditto.hex_component.hex_import.annotation.HEX_Relation;
import com.ditto.hex_component.hex_import.importTemp.Dict;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RelationCheck {
    private static  final ThreadLocal<RelationCheck> threadLocal = new ThreadLocal<>();


    private final Class<?> clazz;

    // 标记是否已解析过表头，避免重复执行
    private boolean isHeadAnalyzed = false;

    private final Map<String,List<Dict>> dicts;

    private final Map<String,Map<String,Map<String,Dict>>> dictsMap=new HashMap<>();
    //子节对应父节点
    private final Map<String,String> childParent=new HashMap<>();
    //子节对应dict
    private final Map<String,String> childProperty=new HashMap<>();

    public RelationCheck(Class<?> clazz, Map<String, List<Dict>> dicts) {
       this.clazz = clazz;
        this.dicts = dicts;
    }

    public static void create(Class<?> clazz,Map<String,List<Dict>> dicts){
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        threadLocal.set(new RelationCheck(clazz,dicts));
    }

    public static RelationCheck check(){
        RelationCheck relationCheck = threadLocal.get();
        if (relationCheck == null) {
            throw new IllegalStateException("RelationCheck not initialized, please call create() first");
        }
        return relationCheck;
    }

    private void  relationAnalyzed(){
        if (isHeadAnalyzed || clazz == null) {
            return;
        }

        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            if (declaredField.isAnnotationPresent(HEX_Relation.class)) {
                HEX_Relation annotation = declaredField.getAnnotation(HEX_Relation.class);
                childParent.put(declaredField.getName(),annotation.parentField());
                String property =annotation.property();
                property=StringUtils.hasText(property)?property:declaredField.getName();
                childProperty.put(declaredField.getName(),property);
            }

        }

        childProperty.forEach((k,v)-> {
            List<Dict> dictList = dicts.get(v);
            if (!CollectionUtils.isEmpty(dictList)) {
                Map<String, Map<String, Dict>> relationMap = new HashMap<>();
                dictList.forEach(dict -> {
                    String parentCode = dict.getParentCode();
                    if (HEX_Relation.RELATION_TOP.equals(childParent.get(k))) {
                        parentCode = HEX_Relation.RELATION_TOP;
                    }
                    relationMap.computeIfAbsent(parentCode, key -> new HashMap<>()).put(dict.getCode(), dict);
                });
                dictsMap.put(k,relationMap);

            }

        });


        isHeadAnalyzed=true;
    }


    public  <T>  void throwsCheckEasyExcel(T data) throws RuntimeException {
       if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }

        relationAnalyzed();

        //clazz.getField()
        String childValue;
        String parentValue;

        //读取数据
        for (Map.Entry<String, String> entry : childParent.entrySet()) {
            try {
                Field field = data.getClass().getDeclaredField(entry.getKey());
                field.setAccessible(true);
                Object o = field.get(data);
                if (o == null || o.equals("")) {
                    continue;
                }
                childValue = "" + o;
                if (HEX_Relation.RELATION_TOP.equals(entry.getValue())) {
                    parentValue = HEX_Relation.RELATION_TOP;
                }else {
                    field = data.getClass().getDeclaredField(entry.getValue());
                    field.setAccessible(true);
                    parentValue = "" + field.get(data);
                }


            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            //获取Dict
            Map<String, Map<String, Dict>> childParentMap = dictsMap.get(entry.getKey());
            if (childParentMap.get(parentValue) == null||childParentMap.get(parentValue).get(childValue) == null) {
                threadLocal.remove();
                throw new RuntimeException("校验失败 : "+parentValue+"--->"+childValue);
            }


        }


    }









}
