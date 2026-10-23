package guides;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OrderAccessTest {
    final OrderAccess access = new OrderAccess(List.of(new OrderAccess.Order(1, "alice"), new OrderAccess.Order(2, "bob")));
    @Test void ownerCanRead() { assertTrue(access.find(1, "alice").isPresent()); }
    @Test void otherOwnerCannotRead() { assertTrue(access.find(1, "bob").isEmpty()); }
    @Test void absentAndAnonymousCannotRead() {
        assertTrue(access.find(9, "alice").isEmpty());
        assertTrue(access.find(1, null).isEmpty());
    }
}
