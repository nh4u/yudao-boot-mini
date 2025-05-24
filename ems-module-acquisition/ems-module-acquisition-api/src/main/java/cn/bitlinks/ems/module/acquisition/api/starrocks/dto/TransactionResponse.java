package cn.bitlinks.ems.module.acquisition.api.starrocks.dto;

import lombok.Data;

// 响应对象
@Data
public class TransactionResponse {
    private String Status;
    private String TxnId;
    private String Message;
}