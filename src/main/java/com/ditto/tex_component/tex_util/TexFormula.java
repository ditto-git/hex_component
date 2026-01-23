package com.ditto.tex_component.tex_util;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
@Data
@AllArgsConstructor
public class TexFormula {


   private String  property;
   private Map<String, String> cellFormulas;
   private int weight ;

}
