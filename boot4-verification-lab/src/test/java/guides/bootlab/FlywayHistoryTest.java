package guides.bootlab;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FlywayHistoryTest {
    @TempDir Path migrations;
    final String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
    static final String ORIGINAL = "ALTER TABLE notes ADD COLUMN tag VARCHAR(30);\n";
    static final String EDITED = "ALTER TABLE notes ADD COLUMN tag VARCHAR(30) DEFAULT 'new' NOT NULL;\n";

    Flyway flyway(String database) {
        return Flyway.configure().dataSource(database, "sa", "")
                .locations("filesystem:" + migrations.toAbsolutePath()).load();
    }
    void installOriginal() throws Exception {
        Files.writeString(migrations.resolve("V1__create_notes.sql"),
                "CREATE TABLE notes (id INTEGER PRIMARY KEY, body VARCHAR(100));\n"
                + "INSERT INTO notes (id, body) VALUES (1, 'existing row');\n");
        Files.writeString(migrations.resolve("V2__add_tag.sql"), ORIGINAL);
        flyway(url).migrate();
    }
    void editAppliedMigration() throws Exception {
        Files.writeString(migrations.resolve("V2__add_tag.sql"), EDITED);
    }
    String scalar(String database, String sql) throws Exception {
        try (var connection = DriverManager.getConnection(database, "sa", "");
             var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            assertTrue(rows.next());
            return rows.getString(1);
        }
    }
    String nullable(String database) throws Exception {
        return scalar(database, "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'NOTES' AND COLUMN_NAME = 'TAG'");
    }

    @Test void editedAppliedMigrationFailsValidation() throws Exception {
        installOriginal();
        editAppliedMigration();
        var error = assertThrows(FlywayValidateException.class, () -> flyway(url).validate());
        assertTrue(error.getMessage().contains("checksum mismatch"), error.getMessage());
        assertEquals("YES", nullable(url));
        System.out.println("OBSERVATION edited_applied_migration=checksum_mismatch");
    }
    @Test void repairAlignsMetadataButLeavesDifferentSchemas() throws Exception {
        installOriginal();
        editAppliedMigration();
        flyway(url).repair();
        flyway(url).validate();
        String fresh = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        flyway(fresh).migrate();
        assertEquals("YES", nullable(url));
        assertEquals("NO", nullable(fresh));
        assertNull(scalar(url, "SELECT tag FROM notes WHERE id = 1"));
        assertEquals("new", scalar(fresh, "SELECT tag FROM notes WHERE id = 1"));
        System.out.printf("OBSERVATION repair: existing_nullable=%s fresh_nullable=%s%n", nullable(url), nullable(fresh));
    }
    @Test void restoreOriginalAndAddV3MakesFreshAndUpgradeAgree() throws Exception {
        installOriginal();
        editAppliedMigration();
        assertThrows(FlywayValidateException.class, () -> flyway(url).validate());
        Files.writeString(migrations.resolve("V2__add_tag.sql"), ORIGINAL);
        flyway(url).validate();
        Files.writeString(migrations.resolve("V3__require_tag.sql"),
                "UPDATE notes SET tag = 'new' WHERE tag IS NULL;\n"
                + "ALTER TABLE notes ALTER COLUMN tag SET DEFAULT 'new';\n"
                + "ALTER TABLE notes ALTER COLUMN tag SET NOT NULL;\n");
        flyway(url).migrate();
        flyway(url).validate();
        String fresh = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        flyway(fresh).migrate();
        for (String database : new String[]{url, fresh}) {
            assertEquals("NO", nullable(database));
            assertEquals("new", scalar(database, "SELECT tag FROM notes WHERE id = 1"));
            assertEquals(3, flyway(database).info().applied().length);
        }
        System.out.printf("OBSERVATION forward_migration: existing_nullable=%s fresh_nullable=%s%n", nullable(url), nullable(fresh));
    }
}
