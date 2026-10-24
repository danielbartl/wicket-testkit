package dev.jbaby.wicket.testkit.examples;

import static dev.jbaby.wicket.testkit.locator.Locator.locate;

import dev.jbaby.wicket.testkit.WicketTestkit;
import dev.jbaby.wicket.testkit.fixtures.Order;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;

/// The page object that the README shows: `OrdersPage` in the words of the domain.
class OrdersPageObject {

    private final WicketTestkit kit;

    OrdersPageObject(WicketTestkit kit) {
        this.kit = kit;
    }

    Row rowOf(Order order) {
        return new Row(order);
    }

    class Row {

        private final Order order;

        Row(Order order) {
            this.order = order;
        }

        void ship() {
            kit.click(locate(AjaxLink.class).withId("ship").inRowOf(order));
        }

        void assertCustomer(String customer) {
            kit.assertLabel(locate(Label.class).withId("customer").inRowOf(order), customer);
        }

        void assertStatus(String status) {
            kit.assertLabel(locate(Label.class).withId("status").inRowOf(order), status);
        }
    }
}
