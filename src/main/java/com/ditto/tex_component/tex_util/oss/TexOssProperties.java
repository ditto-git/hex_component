package com.ditto.tex_component.tex_util.oss;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OSS配置属性类 - 批量绑定配置，无硬编码
 */
@Component
@ConfigurationProperties(prefix = "tex.oss")
@Data
public class TexOssProperties {
    // Endpoint以华东1（杭州）为例，其它Region请按实际情况填写。
    private String endpoint ;

    // 填写Bucket所在地域。以华东1（杭州）为例，Region填写为cn-hangzhou。
    private String region;

    //填写Bucket名称，例如examplebucket。
    private String bucketName;

    // 填写Object完整路径，完整路径中不能包含Bucket名称，例如exampledir/。
    private String objectName;

    // 如果未指定本地路径，则默认从示例程序所属项目对应本地路径中上传文件流。
    //private static String filePath= "D:\\examplefile.txt";


}
