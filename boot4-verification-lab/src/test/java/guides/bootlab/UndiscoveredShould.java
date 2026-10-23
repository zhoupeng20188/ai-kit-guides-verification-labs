package guides.bootlab;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

// Intentionally outside Surefire's default class-name patterns.
class UndiscoveredShould {
    @Test void explicitSelectionRevealsTheFailure() {
        fail("DISCOVERY_SENTINEL: this intentionally failing test was explicitly selected");
    }
}
