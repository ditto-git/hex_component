package com.ditto.tex_component.tex_util.oss;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.comm.SignVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
/**
 * OSS配置类 - 初始化OSS客户端，全局单例Bean
 */
@Configuration
public class TexOssClient {

    @Resource
    private TexOssProperties ossProperties;

    // 从环境变量中获取访问凭证。运行本代码示例之前，请确保已设置环境变量OSS_ACCESS_KEY_ID和OSS_ACCESS_KEY_SECRET。
    private static EnvironmentVariableCredentialsProvider credentialsProvider;


    static {
        try {
            credentialsProvider= CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
        } catch (com.aliyuncs.exceptions.ClientException e) {
            throw new RuntimeException("Failed to initialize OSS credentialsProvider", e);
        }
    }

    /**
     * 初始化OSS客户端，交给Spring管理，全局唯一
     */
    @Bean
    @ConditionalOnMissingBean
    public OSS ossClient() {
        // 创建OSSClient实例。
        // 当OSSClient实例不再使用时，调用shutdown方法以释放资源。
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);

        return OSSClientBuilder.create()
                .endpoint(ossProperties.getEndpoint())
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(ossProperties.getRegion())
                .build();
    }
}
