package cn.bitlinks.ems.module.acquisition.starrocks;

import cn.bitlinks.ems.module.acquisition.api.starrocks.StarRocksFeignClient;
import cn.hutool.core.io.FileUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.STREAM_LOAD_PREFIX;

@Slf4j
@Service
public class StarRocksStreamLoadService {

    @Resource
    private StarRocksFeignClient starRocksFeignClient;

    @Value("${spring.datasource.dynamic.datasource.starrocks.username}")
    private String fe_username;

    @Value("${spring.datasource.dynamic.datasource.starrocks.password}")
    private String fe_password;
    @Value("${ems.starrocks.db}")
    private String db;

    /**
     * 使用 Stream Load 事务接口导入数据
     *
     * @param data 要导入的数据列表
     * @throws IOException
     */
    public void streamLoadData(List<?> data, String label, String tableName) throws IOException {
        Path tempFile = null;
        String authorizationHeader = generateBasicAuthHeader(fe_username, fe_password); // 生成 Authorization header
        String transactionId = null; // 存储事务 ID

        try {
            // 1. 创建临时 JSON 文件
            tempFile = createTempJsonFile(data);
            // 2. 开启事务
           // String a = starRocksFeignClient.beginTransaction(authorizationHeader, label, db, tableName);
            HttpRequest post = HttpUtil.createPost("http://192.168.1.139:8040/api/transaction/begin");
            Map<String,String> header = new HashMap<>();
            header.put("label",label);
            header.put("Expect","100-continue");
            header.put("db",db);
            header.put("table",tableName);
            header.put("Authorization",authorizationHeader);
            post.headerMap(header, false);
            HttpResponse execute = post.execute();

            String a = execute.body();
            //transactionId = transactionResponse.getTxnId();
            log.info("开启事务:" + post.toString());
            log.info("开启事务结果:" + a);
            // 3. Stream Load 数据
            MultipartFile multipartFile = createMultipartFile(tempFile);
          /*  String b = starRocksFeignClient.loadData(authorizationHeader, label, db, tableName,
                    multipartFile);*/


            header.put("format","json");
            header.put("strip_outer_array","true");
            HttpRequest load = HttpRequest.put("http://192.168.1.139:8040/api/transaction/load")
                    .headerMap(header, false)
                    .body(FileUtil.readBytes(tempFile.toFile()));


            String b = load.execute().body();


            log.info("插入数据:" + b);
            // 4. 提交事务
            //String c = starRocksFeignClient.commitTransaction(authorizationHeader, label, db);

            HttpRequest commit = HttpUtil.createRequest(Method.POST, "http://192.168.1.139:8040/api/transaction/commit");
            Map<String,String> commitHeader = new HashMap<>();
            commitHeader.put("label",label);
            //commitHeader.put("Expect","100-continue");
            commitHeader.put("db",db);
           // commitHeader.put("table",tableName);
            commitHeader.put("Authorization",authorizationHeader);
            commit.headerMap(commitHeader, false);
            String c = commit.execute().body();

            log.info("提交事务:" + c);
        } catch (Exception e) {
            log.error("Stream Load failed: " + e.getMessage(), e);
            // 5. 回滚事务
           // String d = starRocksFeignClient.rollbackTransaction(authorizationHeader, label,db);

            HttpRequest rollback = HttpUtil.createRequest(Method.POST, "http://192.168.1.139:8040/api/transaction/commit");
            Map<String,String> rollbackHeader = new HashMap<>();
            rollbackHeader.put("label",label);
            //commitHeader.put("Expect","100-continue");
            rollbackHeader.put("db",db);
            // commitHeader.put("table",tableName);
            rollbackHeader.put("Authorization",authorizationHeader);
            rollback.headerMap(rollbackHeader, false);
            String d = rollback.execute().body();

            log.info("回滚:" + d);
        } finally {
            // 6. 清理临时文件
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * 创建临时 JSON 文件
     *
     * @param data 数据列表
     * @return 临时文件路径
     * @throws IOException
     */
    private Path createTempJsonFile(List<?> data) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        File tempFile = File.createTempFile(STREAM_LOAD_PREFIX, ".json");
        try (FileOutputStream fos = new FileOutputStream(tempFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            mapper.writeValue(writer, data);
        }

        return tempFile.toPath();
    }

    /**
     * 模拟从BeginResult提取事务ID
     *
     * @param beginResult begin API返回值
     * @return 事务ID
     */
    private String extractTransactionId(String beginResult) {
        // 模拟返回值
        return UUID.randomUUID().toString();
    }

    public static String generateBasicAuthHeader(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.UTF_8));
        return "Basic " + new String(encodedAuth);
    }

    public static MultipartFile createMultipartFile(Path path) throws IOException {
        File file = path.toFile();
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found: " + path);
        }

        String fileName = file.getName();
        String contentType = HttpUtil.getMimeType(fileName); // 获取文件 MIME 类型

        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE; // 默认 MIME 类型
        }

        byte[] fileContent = FileUtil.readBytes(file);

        org.apache.commons.fileupload.disk.DiskFileItem fileItem = new org.apache.commons.fileupload.disk.DiskFileItem(
                "file", // 表单字段名
                contentType,
                false,
                fileName,
                (int) file.length(),
                file.getParentFile()
        );

        try {
            fileItem.getOutputStream().write(fileContent);
        } catch (IOException e) {
            throw new IOException("Error writing file content to DiskFileItem: " + e.getMessage(), e);
        }

        return new CommonsMultipartFile(fileItem);
    }
}