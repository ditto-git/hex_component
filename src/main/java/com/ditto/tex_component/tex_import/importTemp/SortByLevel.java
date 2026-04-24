package com.ditto.tex_component.tex_import.importTemp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SortByLevel {

    //哈希表递归+双抽快排
    public static List<String> sort(Map<String, String> relationMap ){
        Map<String, Integer> levelMap = new HashMap<>();
        relationMap.forEach((k,v)->{
            int level = calculateHierarchyLevel(relationMap, k, 1);
            levelMap.put(k,level);
        });
        //以levelMap v排序
        return levelMap.entrySet().stream().sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).collect(Collectors.toList());

    }

    private static int  calculateHierarchyLevel(Map <String, String> relationMap,String children,int level ){
        String parent=relationMap.get(children);
        if (parent!=null&&!"TOP".equals(parent)){
            level=calculateHierarchyLevel(relationMap,parent,++level);
        }
        return level;
    }

}
