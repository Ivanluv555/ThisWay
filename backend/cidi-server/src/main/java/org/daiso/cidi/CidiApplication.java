/**
 * CidiApplication
 *
 * 说明：本文件定义了 CidiApplication，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CidiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CidiApplication.class, args);
    }
}
