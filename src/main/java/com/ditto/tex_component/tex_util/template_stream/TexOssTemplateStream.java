package com.ditto.tex_component.tex_util.template_stream;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.OSSObject;
import com.ditto.tex_component.tex_util.oss.TexOssProperties;
import com.ditto.tex_component.tex_util.request.ImportFileMultipartUtil;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class TexOssTemplateStream implements TexTemplateStream, DisposableBean {

    @Resource
    private OSS ossClient;
    @Autowired
    private TexOssProperties properties;
    @Value("${tex.oss.bucket-name}")
    private String bucketName;
    @Value("${tex.oss.object-name}")
    private String objectName;



    /**
     * 本地文件上传到OSS
     * @param localFilePath 本地文件全路径
     * @param fileUrl OSS存储的文件路径/文件名
     */
    public void upload(String localFilePath, String fileUrl) {
        // ✅ try-with-resources 自动关闭FileInputStream，无需手动close，JVM保证释放
        try (FileInputStream fileInputStream = new FileInputStream(localFilePath)) {
            upload(fileInputStream, fileUrl);
        } catch (Exception e) {
            throw new RuntimeException("本地文件转输入流失败 -> " + localFilePath, e);
        }
    }

    /**
     * 输入流上传到OSS（核心上传方法）
     * @param inputStream 文件输入流
     * @param fileUrl OSS存储的文件路径/文件名
     */
    public void upload(InputStream inputStream, String fileUrl) {
        try {
            String fullObjectName = objectName + fileUrl;
            // 执行OSS上传
            ossClient.putObject(bucketName, fullObjectName, inputStream);
        } catch (OSSException oe) {
            // OSS服务端异常：请求到达OSS但被拒绝（权限、文件不存在等）
            throw new RuntimeException("OSS上传服务端异常 -> 文件路径：" + fileUrl, oe);
        } catch (ClientException ce) {
            // OSS客户端异常：网络不通、客户端配置错误等
            throw new RuntimeException("OSS上传客户端异常 -> 网络/配置问题", ce);
        }finally {
            ImportFileMultipartUtil.closeInputStream(inputStream);
        }

    }

    /**
     * OSS文件下载并写入HttpServletResponse，返回给前端下载
     * @param fileUrl OSS存储的文件路径/文件名
     * @param response 响应对象
     */
    public void downloadResponse(String fileUrl, HttpServletResponse response) {
        String fullObjectName =objectName + fileUrl;
        // ✅ 链式try-with-resources：自动关闭 所有实现AutoCloseable的资源，顺序：后声明先关闭
        // 自动关闭顺序：ServletOutputStream → InputStream → OSSObject 完全符合阿里云规范
        try (OSSObject ossObject = ossClient.getObject(bucketName, fullObjectName);
             InputStream inputStream = ossObject.getObjectContent();
             ServletOutputStream outputStream = response.getOutputStream()) {

            // 流式读写，✅ 仅用1024字节缓冲区，无内存堆积，大文件下载也不会OOM
            byte[] readBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(readBuffer)) != -1) {
                outputStream.write(readBuffer, 0, bytesRead);
            }
            // 强制刷出缓冲区数据到前端，避免数据残留
            outputStream.flush();

        } catch (OSSException oe) {
            throw new RuntimeException("OSS下载服务端异常 -> 文件路径：" + fileUrl, oe);
        } catch (ClientException ce) {
            throw new RuntimeException("OSS下载客户端异常 -> 网络/配置问题", ce);
        } catch (IOException e) {
            throw new RuntimeException("OSS文件流写入响应失败", e);
        }
    }

    /**
     * OSS文件下载并自定义处理输入流（核心推荐用法）
     * @param fileUrl OSS存储的文件路径/文件名
     * @param operateOssInputStream 自定义流处理逻辑
     */
    public void downloadInput(String fileUrl, TexInputStreamOperate operateOssInputStream) {
        String fullObjectName = objectName + fileUrl;
        // ✅ try-with-resources 自动关闭：InputStream → OSSObject，杜绝连接泄漏
        try (OSSObject ossObject = ossClient.getObject(bucketName, fullObjectName);
             InputStream inputStream = ossObject.getObjectContent()) {

            // 执行业务自定义的流处理逻辑
            operateOssInputStream.closeBefore(inputStream);

        } catch (OSSException oe) {
            throw new RuntimeException("OSS下载服务端异常 -> 文件路径：" + fileUrl, oe);
        } catch (ClientException ce) {
            throw new RuntimeException("OSS下载客户端异常 -> 网络/配置问题", ce);
        } catch (Exception e) {
            throw new RuntimeException("OSS文件流自定义处理失败", e);
        } finally {
            // 执行后置收尾操作，有异常也能执行，保证收尾逻辑不落空
            try {
                operateOssInputStream.closeAfter();
            } catch (Exception e) {
                throw new RuntimeException("OSS流处理后置操作失败", e);
            }
        }
    }

    /**
     * ✅ 核心优化：实现DisposableBean接口，Spring容器销毁时 执行一次关闭
     * 全局OSS客户端仅关闭一次，彻底杜绝 重复关闭导致的客户端失效问题
     */
    @Override
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }
}


