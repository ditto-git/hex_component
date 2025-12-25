package com.ditto.hex_component.hex_console.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ditto.hex_component.hex_console.entity.HexTemplateCell;
import com.ditto.hex_component.hex_util.request.ExportFileResponseUtil;
import com.ditto.hex_component.hex_util.request.ImportFileMultipartUtil;


/**
 * <p>
 *  服务类
 * </p>
 *
 * @author ditto
 * @since 2025-08-18
 */
public interface HexTemplateCellService extends IService<HexTemplateCell> {

     void replaceExTemplate(ImportFileMultipartUtil importFileMultipartUtil) ;

     void importExTemplateCell(ImportFileMultipartUtil importFileMultipartUtil) ;

     void exportExTemplateCell(ExportFileResponseUtil exportFileResponseUtil) ;

}
