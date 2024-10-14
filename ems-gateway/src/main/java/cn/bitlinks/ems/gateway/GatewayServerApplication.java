package cn.bitlinks.ems.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.TimeZone;

@Slf4j
@SpringBootApplication
public class GatewayServerApplication {

    public static void main(String[] args) throws UnknownHostException {
        // 启动 Spring Boot 应用
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));
        ConfigurableApplicationContext application = SpringApplication.run(GatewayServerApplication.class, args);
        Environment env = application.getEnvironment();
        log.info("\n----------------------------------------------------------\n\t" +
                        "应用 '{}' 启动成功! 访问连接:\n\t" +
                        "Swagger文档: \t\thttp://{}:{}/doc.html\n\t" +
                        "数据库监控: \t\thttp://{}:{}/druid\n" +
                        "----------------------------------------------------------",
                env.getProperty("spring.application.name"),
                InetAddress.getLocalHost().getHostAddress(),
                env.getProperty("server.port", "8080"),
                "127.0.0.1",
                env.getProperty("server.port", "8080"));
    }

}
