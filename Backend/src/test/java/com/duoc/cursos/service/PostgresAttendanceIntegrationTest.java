package com.duoc.cursos.service;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@EnabledIfEnvironmentVariable(named = "TEST_POSTGRES_URL", matches = "jdbc:postgresql:.*")
class PostgresAttendanceIntegrationTest extends AttendanceFlowIntegrationTest {
    @DynamicPropertySource static void postgres(DynamicPropertyRegistry properties) {
        PostgresTestProperties.configure(properties);
    }
}
