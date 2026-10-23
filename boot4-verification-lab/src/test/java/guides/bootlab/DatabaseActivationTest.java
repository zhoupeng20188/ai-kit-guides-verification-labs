package guides.bootlab;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DatabaseActivationTest {
    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;

    @Test void bootRunsTheClasspathMigrationAtStartup() {
        assertNotNull(flyway);
        assertEquals("migration-ran", jdbc.queryForObject(
                "SELECT label FROM lab_marker WHERE id = 1", String.class));
        assertEquals(1, flyway.info().applied().length);
        System.out.println("OBSERVATION startup_migrations=" + flyway.info().applied().length);
    }
}
