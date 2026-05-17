/**
 * CidiApplicationTests
 *
 * 说明：本文件定义了 CidiApplicationTests，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class CidiApplicationTests {

    @Test
    void contextLoads() {
        // 测试 Spring 应用上下文是否能正常加载
    }

}
