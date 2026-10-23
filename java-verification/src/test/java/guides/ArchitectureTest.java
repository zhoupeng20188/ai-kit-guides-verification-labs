package guides;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static org.junit.jupiter.api.Assertions.assertThrows;
class ArchitectureTest {
    static class BadFixture { @Deprecated String field; }
    @Test void provesAnnotationGateRejectsKnownViolation() {
        var classes = new ClassFileImporter().importClasses(BadFixture.class);
        var rule = noFields().should().beAnnotatedWith(Deprecated.class);
        assertThrows(AssertionError.class, () -> rule.check(classes));
    }
    @Test void acceptsCleanFixture() {
        noFields().should().beAnnotatedWith(Deprecated.class)
                .check(new ClassFileImporter().importClasses(OrderAccess.class));
    }
}
