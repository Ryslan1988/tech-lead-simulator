package com.techleadsim.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
public abstract class AbstractPostgresIntegrationTest {

    // Deliberately NOT annotated with @Container/@Testcontainers: the "singleton container"
    // pattern (https://java.testcontainers.org/test_framework_integration/junit_5/#singleton-containers).
    // With @Container on a static field shared via inheritance, the JUnit5 Testcontainers
    // extension stops the container after each subclass finishes and starts a NEW one (new
    // port) for the next subclass — but Spring's test-context cache reuses the OLD cached
    // ApplicationContext/DataSource from the first subclass, which still points at the dead
    // container's port. Every subclass after the first then fails with "Connection refused".
    // Starting it once here and relying on the Ryuk reaper for JVM-exit cleanup avoids that.
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static {
        postgres.start();
    }
}
