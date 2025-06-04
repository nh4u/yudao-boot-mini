package cn.bitlinks.ems.module.acquisition.starrocks;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.STREAM_LOAD_PREFIX;

@Slf4j
@Service
public class StarRocksStreamLoadService {


    @Value("${spring.datasource.dynamic.datasource.starrocks.username}")
    private String feUsername;

    @Value("${spring.datasource.dynamic.datasource.starrocks.password}")
    private String fePassword;

    @Value("${ems.starrocks.db}")
    private String db;

    @Value("${ems.starrocks.api-url}")
    private String baseUrl;


    private static final String API_BEGIN = "/api/transaction/begin";
    private static final String API_LOAD = "/api/transaction/load";
    private static final String API_COMMIT = "/api/transaction/commit";
    private static final String API_ROLLBACK = "/api/transaction/rollback";

    private static final String HEADER_LABEL = "label";
    private static final String HEADER_EXPECT = "Expect";
    private static final String HEADER_DB = "db";
    private static final String HEADER_TABLE = "table";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_FORMAT = "format";
    private static final String HEADER_STRIP_OUTER_ARRAY = "strip_outer_array";

    private static final String VALUE_EXPECT_CONTINUE = "100-continue";
    private static final String VALUE_FORMAT_JSON = "json";
    private static final String VALUE_STRIP_TRUE = "true";

    public void streamLoadData(List<?> data, String label, String tableName) throws IOException {
        Path tempFile = null;
        String authHeader = generateBasicAuthHeader(feUsername, fePassword);

        try {
            tempFile = createTempJsonFile(data);

            // 1. 开启事务
            Map<String, String> beginHeaders = buildBaseHeaders(label, tableName, authHeader);
            String beginResult = sendPost(API_BEGIN, beginHeaders, null);
            log.info("[StarRocks] 开启事务 label={}，结果={}", label, beginResult);

            // 2. 加载数据
            beginHeaders.put(HEADER_FORMAT, VALUE_FORMAT_JSON);
            beginHeaders.put(HEADER_STRIP_OUTER_ARRAY, VALUE_STRIP_TRUE);
            String loadResult = sendPut(API_LOAD, beginHeaders, FileUtil.readBytes(tempFile.toFile()));
            log.info("[StarRocks] 加载数据 label={}，结果={}", label, loadResult);

            // 3. 提交事务
            Map<String, String> commitHeaders = buildControlHeaders(label, authHeader);
            String commitResult = sendPost(API_COMMIT, commitHeaders, null);
            log.info("[StarRocks] 提交事务 label={}，结果={}", label, commitResult);

        } catch (Exception e) {
            log.error("[StarRocks] Stream Load 失败 label={}：{}", label, e.getMessage(), e);
            Map<String, String> rollbackHeaders = buildControlHeaders(label, authHeader);
            String rollbackResult = sendPost(API_ROLLBACK, rollbackHeaders, null);
            log.warn("[StarRocks] 回滚事务 label={}，结果={}", label, rollbackResult);
        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
            log.debug("[StarRocks] 删除临时文件: {}", tempFile);
        }
    }

    private Map<String, String> buildBaseHeaders(String label, String tableName, String authHeader) {
        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER_LABEL, label);
        headers.put(HEADER_EXPECT, VALUE_EXPECT_CONTINUE);
        headers.put(HEADER_DB, db);
        headers.put(HEADER_TABLE, tableName);
        headers.put(HEADER_AUTHORIZATION, authHeader);
        return headers;
    }

    private Map<String, String> buildControlHeaders(String label, String authHeader) {
        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER_LABEL, label);
        headers.put(HEADER_DB, db);
        headers.put(HEADER_AUTHORIZATION, authHeader);
        return headers;
    }

    private String sendPost(String uri, Map<String, String> headers, byte[] body) {
        HttpRequest req = HttpUtil.createPost(baseUrl + uri).headerMap(headers, false);
        req.setFollowRedirects(true);
        if (body != null) req.body(body);
        return req.execute().body();
    }

    private String sendPut(String uri, Map<String, String> headers, byte[] body) {
        HttpRequest req = HttpRequest.put(baseUrl + uri).headerMap(headers, false);
        req.setFollowRedirects(true);
        if (body != null) req.body(body);
        return req.execute().body();
    }

    private Path createTempJsonFile(List<?> data) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // 注册 Java 8 时间模块
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        File tempFile = File.createTempFile(STREAM_LOAD_PREFIX, ".json");
        try (FileOutputStream fos = new FileOutputStream(tempFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            mapper.writeValue(writer, data);
        }
        return tempFile.toPath();
    }

    public static String generateBasicAuthHeader(String username, String password) {
        String auth = username + ":" + password;
        return "Basic " + Base64.encodeBase64String(auth.getBytes(StandardCharsets.UTF_8));
    }

    public static MultipartFile createMultipartFile(Path path) throws IOException {
        File file = path.toFile();
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found: " + path);
        }

        String fileName = file.getName();
        String contentType = HttpUtil.getMimeType(fileName);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        byte[] fileContent = FileUtil.readBytes(file);
        org.apache.commons.fileupload.disk.DiskFileItem fileItem = new org.apache.commons.fileupload.disk.DiskFileItem(
                "file", contentType, false, fileName, (int) file.length(), file.getParentFile());

        try (OutputStream os = fileItem.getOutputStream()) {
            os.write(fileContent);
        }

        return new CommonsMultipartFile(fileItem);
    }
}
