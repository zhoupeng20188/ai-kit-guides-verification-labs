package guides;
import java.util.List;
import java.util.Optional;
public final class OrderAccess {
    public record Order(long id, String owner) {}
    private final List<Order> orders;
    public OrderAccess(List<Order> orders) { this.orders = List.copyOf(orders); }
    public Optional<Order> find(long id, String authenticatedOwner) {
        if (authenticatedOwner == null) { return Optional.empty(); }
        return orders.stream().filter(o -> o.id() == id && authenticatedOwner.equals(o.owner())).findFirst();
    }
}
