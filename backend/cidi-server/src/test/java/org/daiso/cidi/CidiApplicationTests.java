package org.daiso.cidi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-mysql-test.properties")
class CidiApplicationTests {

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertNotNull(applicationContext, "Spring 应用上下文应成功加载");
        assertTrue(applicationContext.isActive(), "Spring 应用上下文应处于活跃状态");
    }
}
