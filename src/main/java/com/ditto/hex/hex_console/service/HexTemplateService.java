package com.ditto.hex.hex_console.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ditto.hex.hex_console.entity.HexTemplate;
import com.ditto.hex.hex_util.request.ImportFileMultipartUtil;

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

     void replaceExTemplate(ImportFileMultipartUtil importFileMultipartUtil);

}
