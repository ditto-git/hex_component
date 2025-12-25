package com.ditto.hex_component.hex_console.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ditto.hex_component.hex_console.entity.HexTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author ditto
 * @since 2025-08-17
 */
@Mapper
public interface HexTemplateMapper extends BaseMapper<HexTemplate> {

    HexTemplate getExTemplate(@Param("t_code")String t_code);

    int maintenance(@Param("t_code")String t_code);

}
