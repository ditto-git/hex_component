package com.ditto.hex_component.hex_console.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ditto.hex_component.hex_console.entity.HexTemplate;
import com.ditto.hex_component.hex_util.request.ImportFileMultipartUtil;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author ditto
 * @since 2025-08-17
 */
public interface HexTemplateService extends IService<HexTemplate> {
     HexTemplate getExTemplate(String templateCode);
     void initExTemplate(HexTemplate hexTemplate);
     void replaceExTemplate(ImportFileMultipartUtil importFileMultipartUtil);

}
