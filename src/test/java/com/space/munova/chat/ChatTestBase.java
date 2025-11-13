package com.space.munova.chat;

import com.space.munova.config.YamlPropertySourceFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // 테스트용 임시 포트 등록
@ActiveProfiles("test") // 스프링의 profile을 지정하는 어노테이션
@Testcontainers
//@TestPropertySource(locations = "classpath:application-test.yml", factory = YamlPropertySourceFactory.class)    // 테스트에서 명시적으로 특정 파일을 로딩
public abstract class ChatTestBase {

    private static final String USERNAME = "root";
    private static final String PASSWORD = "0000";
    private static final String DATABASE_NAME = "munova-test";

    @Container  // Junit 전 자동 시작, 테스트 종료 후 자동 종료
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0.43")
            .withUsername(USERNAME)
            .withPassword(PASSWORD)
            .withDatabaseName(DATABASE_NAME);

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);


    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // mysql
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        // Redis
        registry.add("spring.redis.host", () -> redis.getHost());
        registry.add("spring.redis.port", () -> redis.getFirstMappedPort());
    }
}
