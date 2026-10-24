package dev.jbaby.wicket.testkit.examples;

import dev.jbaby.wicket.testkit.WicketTestkit;
import dev.jbaby.wicket.testkit.fixtures.Order;
import dev.jbaby.wicket.testkit.fixtures.OrdersPage;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// Keeps the README's page-object example compiling and working. It proves no requirement of its
/// own, so it names no id.
class PageObjectExampleTests {

    WicketTester tester;
    OrdersPageObject ordersPage;

    @BeforeEach
    void renderOrdersPage() {
        tester = new WicketTester();
        tester.startPage(OrdersPage.class);
        ordersPage = new OrdersPageObject(WicketTestkit.of(tester));
    }

    @AfterEach
    void destroyTester() {
        tester.destroy();
    }

    @Test
    @DisplayName("the README's page object ships an order")
    void shipsAnOrder() {
        var order = Order.GLOBEX_42;

        ordersPage.rowOf(order).assertCustomer("Globex");
        ordersPage.rowOf(order).assertStatus("open");

        ordersPage.rowOf(order).ship();

        ordersPage.rowOf(order).assertStatus("shipped");
    }
}
