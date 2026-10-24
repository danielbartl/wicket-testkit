package dev.jbaby.wicket.testkit.fixtures;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;

/// One order's details: Labels `customer` and `id`.
public class OrderPage extends WebPage {

    public OrderPage(Order order) {
        add(new Label("customer", order.customer()));
        add(new Label("id", order.id()));
    }
}
