package cn.bitlinks.ems.module.acquisition.mq.config;

import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class CustomAuthRPCHook implements RPCHook {

    private final String accessKey;
    private final String secretKey;

    public CustomAuthRPCHook(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    @Override
    public void doBeforeRequest(String remoteAddr, RemotingCommand request) {
        // 在请求中添加鉴权信息，比如签名和accessKey
        try {
            long timestamp = System.currentTimeMillis();
            String nonce = String.valueOf(timestamp); // 简单示例用时间戳做nonce

            // 待签名字符串格式：accessKey + nonce + secretKey（你可以根据实际签名规则调整）
            String signData = accessKey + nonce + secretKey;
            String signature = sha256Base64(signData);

            // 把鉴权参数放入请求头（自定义Header，Broker端也需配合支持）
            request.addExtField("AccessKey", accessKey);
            request.addExtField("Nonce", nonce);
            request.addExtField("Signature", signature);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doAfterResponse(String remoteAddr, RemotingCommand request, RemotingCommand response) {
        // 可做响应后处理，通常鉴权不需要
    }

    private String sha256Base64(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
