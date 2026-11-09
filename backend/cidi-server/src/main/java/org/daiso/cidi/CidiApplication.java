package org.daiso.cidi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Cidi 景点信息与社区服务的 Spring Boot 启动入口。
 *
 * <p>从 {@code org.daiso.cidi} 包开始扫描组件，装配景点资料、社区互动、微信认证及客流数据接口。
 *
 * <p>客流历史与预测结果由接口接收和查询，预测计算由外部服务提供；运行参数从 Spring 配置环境加载。
 */
@SpringBootApplication
public class CidiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CidiApplication.class, args);
    }
}
