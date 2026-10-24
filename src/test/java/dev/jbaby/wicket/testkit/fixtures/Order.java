package dev.jbaby.wicket.testkit.fixtures;

import java.io.Serializable;
import java.util.List;

/// An order, as the fixture pages list it.
public record Order(int id, String customer) implements Serializable {

    public static final Order ACME_41 = new Order(41, "ACME");
    public static final Order GLOBEX_42 = new Order(42, "Globex");
    public static final Order ACME_43 = new Order(43, "ACME");

    /// The orders the fixture pages list, in this order.
    public static final List<Order> ALL = List.of(ACME_41, GLOBEX_42, ACME_43);
}
