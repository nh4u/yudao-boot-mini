package cn.bitlinks.ems.module.acquisition.api.starrocks;

import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "starRocksFeignClient", url = "${ems.starrocks.api-url}")
public interface StarRocksFeignClient {
    /**
     * 开启 事务
     *
     * @param label 事务 label
     * @param table 表名
     * @return 事务开始的响应
     */
    @PostMapping(value = "/api/transaction/begin", produces = "application/json")
    @Headers({"Expect:100-continue"})
    String beginTransaction(
            @RequestHeader("Authorization") String authorization, // 使用 Basic Auth,  用户名和密码
            @RequestHeader("label") String label,
            @RequestHeader("db") String db,
            @RequestHeader("table") String table
    );

    /**
     * 查数据
     *
     * @param label Transaction label.
     * @param table Table name.
     * @param file  Data file to load.
     * @return Response from Doris FE.
     */
    @PutMapping(value = "/api/transaction/load", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/json")
    @Headers({"Expect:100-continue"})
    String loadData(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("label") String label,
            @RequestHeader("db") String db,
            @RequestHeader("table") String table,
            @RequestPart(value = "file") MultipartFile file // Use @RequestPart for file upload
    );

    /**
     * Commit a Doris transaction.
     *
     * @param label Transaction label.
     * @param db    Database name.
     * @return Response from Doris FE.
     */
    @PostMapping(value = "/api/transaction/commit", produces = "application/json")
    @Headers({"Expect:100-continue"})
    String commitTransaction(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("label") String label,
            @RequestHeader("db") String db
    );

    /**
     * 回滚事务
     *
     * @param label Transaction label.
     * @return Response from Doris FE.
     */
    @PostMapping(value = "/api/transaction/rollback", produces = "application/json")
    @Headers({"Expect:100-continue"})
    String rollbackTransaction(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("label") String label,
            @RequestHeader("db") String db
    );
}