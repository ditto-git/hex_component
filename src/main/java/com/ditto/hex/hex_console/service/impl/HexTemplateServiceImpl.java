package com.ditto.hex.hex_console.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ditto.hex.hex_console.entity.HexTemplate;
import com.ditto.hex.hex_console.mapper.HexTemplateMapper;
import com.ditto.hex.hex_console.service.HexTemplateCellService;
import com.ditto.hex.hex_console.service.HexTemplateService;
import com.ditto.hex.hex_util.ExThreadLocal;
import com.ditto.hex.hex_util.request.ImportFileMultipartUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author ditto
 * @since 2025-08-17
 */
@Service
@Slf4j
public class HexTemplateServiceImpl extends ServiceImpl<HexTemplateMapper, HexTemplate> implements HexTemplateService {



    private final static  String defaultStatus="0";



    @Autowired
    private HexTemplateCellService hexTemplateCellService;

    @Autowired
    private HexTemplateMapper hexTemplateMapper;


    public void initExTemplateConfig(HexTemplate hexTemplate){
        hexTemplate.setTemplateStatus(defaultStatus);
        save(hexTemplate);
    }


    public boolean notDuplicate(HexTemplate hexTemplate){
        return lambdaQuery().eq(HexTemplate::getTemplateCode, hexTemplate.getTemplateCode()).list().isEmpty();
    }



    public void replaceExTemplate(ImportFileMultipartUtil importFileMultipartUtil) {
        HexTemplate hexTemplate =getById(importFileMultipartUtil.getFileName());

        //是否初始化
        if(hexTemplate ==null){ return;}

        //模板状态0维护中 乐观锁
        if(!this.maintenance(importFileMultipartUtil.getFileName())){return;}
        log.info("{}...乐观锁..........");

        //缓存exTemplate
        ExThreadLocal.setExTemplate(hexTemplate);

        /*OSS插入模板文档, 数据库插入模板内容*/
        try {
            hexTemplateCellService.replaceExTemplate(importFileMultipartUtil);
        }catch (Exception e){
            log.error("{}...模板解析失败.....", hexTemplate.getTemplateUrl(),e);
        }finally {
            //清楚缓存exTemplate
            ExThreadLocal.clear();
            //模板状态1使用中 解乐观锁
            this.lambdaUpdate().set(HexTemplate::getTemplateStatus,1).eq(HexTemplate::getTemplateCode, hexTemplate.getTemplateCode()).update();
            log.info("{}...释放乐观锁..........", hexTemplate.getTemplateUrl());
        }

    }








    public HexTemplate getExTemplate(String templateCode){
         return hexTemplateMapper.getExTemplate(templateCode);
    }


    private boolean maintenance(String templateCode){
      //  return this.lambdaUpdate().set(ExTemplate::getTemplateStatus, 0).eq(ExTemplate::getTemplateStatus, 1).update();
        return hexTemplateMapper.maintenance(templateCode)>0;
    }





}
