package com.ditto.hex.hex_util.oss;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


public class OSSUtil {

    // Endpoint以华东1（杭州）为例，其它Region请按实际情况填写。
    private static String endpoint = "";
    // 填写Bucket所在地域。以华东1（杭州）为例，Region填写为cn-hangzhou。
    private static String region = "";
    // 填写Bucket名称，例如examplebucket。
    private static String bucketName = "";
    // 填写Object完整路径，完整路径中不能包含Bucket名称，例如exampledir/。
    private static String objectName = "";
    // 如果未指定本地路径，则默认从示例程序所属项目对应本地路径中上传文件流。
    //private static String filePath= "D:\\examplefile.txt";
    // 从环境变量中获取访问凭证。运行本代码示例之前，请确保已设置环境变量OSS_ACCESS_KEY_ID和OSS_ACCESS_KEY_SECRET。
    private static EnvironmentVariableCredentialsProvider credentialsProvider;


   static {
       try {
           credentialsProvider=CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
       } catch (com.aliyuncs.exceptions.ClientException e) {
           throw new RuntimeException("Failed to initialize OSS credentialsProvider", e);
       }
   }


    private static OSS buildOSSClient(){
            // 创建OSSClient实例。
            // 当OSSClient实例不再使用时，调用shutdown方法以释放资源。
            ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
            clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);

            return OSSClientBuilder.create()
                    .endpoint(endpoint)
                    .credentialsProvider(credentialsProvider)
                    .clientConfiguration(clientBuilderConfiguration)
                    .region(region)
                    .build();
    }


    public  void uploadOSS (String localFilePath,String fileUrl) {
       try (FileInputStream fileInputStream = new FileInputStream(localFilePath)){
            uploadOSS(fileInputStream,fileUrl);
       }catch (Exception e){
           throw new RuntimeException("filePath to fileInputStream  error", e);
       }

    }

    public static void uploadOSS (InputStream inputStream,String fileUrl){
        OSS ossClient=null;
        try {
        //获取oss实例
        ossClient = buildOSSClient();
        // 创建PutObjectRequest对象。
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName+fileUrl, inputStream);
        // 创建PutObject请求。
         ossClient.putObject(putObjectRequest);
         inputStream.close();
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } catch (IOException e) {
            System.out.println("Error Message:" + e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }


    public static void downloadOSSResponse(String fileUrl, HttpServletResponse response) {

        OSS ossClient = buildOSSClient();
        try {
            // ossObject包含文件所在的存储空间名称、文件名称、文件元数据以及一个输入流。
            OSSObject ossObject = ossClient.getObject(bucketName, objectName + fileUrl);
            InputStream inputStream = ossObject.getObjectContent();

            ServletOutputStream outputStream = response.getOutputStream();

            // 读取文件内容到字节数组。
            byte[] readBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(readBuffer)) != -1) {
                outputStream.write(readBuffer, 0, bytesRead);
            }
            // 获取最终的字节数组。
            //byte[] fileBytes = byteArrayOutputStream.toByteArray();
            // 打印字节数组的长度。
            // System.out.println("Downloaded file size: " + fileBytes.length + " bytes");
            // 数据读取完成后，获取的流必须关闭，否则会造成连接泄漏，导致请求无连接可用，程序无法正常工作。
            inputStream.close();
            outputStream.close();
            // ossObject对象使用完毕后必须关闭，否则会造成连接泄漏，导致请求无连接可用，程序无法正常工作。
            ossObject.close();

        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (Throwable ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

    }


    public  static void downloadOSSInput (String fileUrl, OSSInputOperate operateOssInputStream){
        OSS ossClient = buildOSSClient();
        try {
            // ossObject包含文件所在的存储空间名称、文件名称、文件元数据以及一个输入流。
            OSSObject ossObject = ossClient.getObject(bucketName, objectName+fileUrl);
            InputStream inputStream =ossObject.getObjectContent();
            operateOssInputStream.closeBefore(inputStream);
            //todo 先关闭oss呢？
            // 数据读取完成后，获取的流必须关闭，否则会造成连接泄漏，导致请求无连接可用，程序无法正常工作。
            inputStream.close();
            // ossObject对象使用完毕后必须关闭，否则会造成连接泄漏，导致请求无连接可用，程序无法正常工作。
            ossObject.close();

        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (Throwable ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            ce.printStackTrace();
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        try {
            operateOssInputStream.closeAfter();
        }catch (Exception e){
            e.printStackTrace();
        }

    }








/*    public  static void downloadOSSResponse (String fileUrl, HttpServletResponse response){

        OSS ossClient = buildOSSClient();
        try {
            // ossObject包含文件所在的存储空间名称、文件名称、文件元数据以及一个输入流。
            OSSObject ossObject = ossClient.getObject(bucketName, objectName+fileUrl);
            InputStream inputStream = ossObject.getObjectContent();

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // 读取文件内容到字节数组。
//            byte[] readBuffer = new byte[1024];
//            int bytesRead;
//            while ((bytesRead = inputStream.read(readBuffer)) != -1) {
//                byteArrayOutputStream.write(readBuffer, 0, bytesRead);
//            }
            // 获取最终的字节数组。
            //byte[] fileBytes = byteArrayOutputStream.toByteArray();
            // 打印字节数组的长度。
            // System.out.println("Downloaded file size: " + fileBytes.length + " bytes");
            // 数据读取完成后，获取的流必须关闭，否则会造成连接泄漏，导致请求无连接可用，程序无法正常工作。
            //inputStream.close();
            //byteArrayOutputStream.close();
            // ossObject对象使用完毕后必须关闭，否则会造成连接泄漏，导致请求无连接可用，程序无法正常工作。
            //ossObject.close();

        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (Throwable ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        try {
            ossInputStream.CloseAfter();
        }catch (Exception e){
            e.printStackTrace();
        }

    }*/













}
