package guides;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;
// Deliberately omitted by Surefire's default class-name patterns.
// Select explicitly with -Dtest=UndiscoveredShould to demonstrate the failure.
class UndiscoveredShould {
    @Test void deliberateFailure() { fail("Explicit selection discovered this intentionally failing test"); }
}
