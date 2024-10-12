package cn.bitlinks.ems.gateway.util;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 项目启动成功后，提供文档相关的地址
 *
 * @author bitlinks
 */
@Component
@Slf4j
public class BannerApplicationRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        ThreadUtil.execute(() -> {
            ThreadUtil.sleep(1, TimeUnit.SECONDS); // 延迟 1 秒，保证输出到结尾
            log.info("\n----------------------------------------------------------\n\t" +
                            "项目启动成功！\n\t" +
                            "接口文档: \t{} \n\t" +
                            "开发文档: \t{} \n\t" +
                            "视频教程: \t{} \n" +
                            "----------------------------------------------------------",
                    "https://cloud.bitlinks.cn/api-doc/",
                    "https://cloud.bitlinks.cn",
                    "https://t.zsxq.com/02Yf6M7Qn");

            // 数据报表
            System.out.println("[报表模块 ems-module-report 教程][参考 https://cloud.bitlinks.cn/report/ 开启]");
            // 工作流
            System.out.println("[工作流模块 ems-module-bpm 教程][参考 https://cloud.bitlinks.cn/bpm/ 开启]");
            // 商城系统
            System.out.println("[商城系统 ems-module-mall 教程][参考 https://cloud.bitlinks.cn/mall/build/ 开启]");
            // ERP 系统
            System.out.println("[ERP 系统 ems-module-erp - 教程][参考 https://cloud.bitlinks.cn/erp/build/ 开启]");
            // CRM 系统
            System.out.println("[CRM 系统 ems-module-crm - 教程][参考 https://cloud.bitlinks.cn/crm/build/ 开启]");
            // 微信公众号
            System.out.println("[微信公众号 ems-module-mp 教程][参考 https://cloud.bitlinks.cn/mp/build/ 开启]");
            // 支付平台
            System.out.println("[支付系统 ems-module-pay - 教程][参考 https://doc.bitlinks.cn/pay/build/ 开启]");
            // AI 大模型
            System.out.println("[AI 大模型 ems-module-ai - 教程][参考 https://cloud.bitlinks.cn/ai/build/ 开启]");
            // IOT 物联网
            System.out.println("[IOT 物联网 ems-module-iot - 教程][参考 https://doc.bitlinks.cn/iot/build/ 开启]");
        });
    }

}
