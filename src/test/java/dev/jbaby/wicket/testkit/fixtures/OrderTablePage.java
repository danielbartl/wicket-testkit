package dev.jbaby.wicket.testkit.fixtures;

import java.util.List;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

/// A page whose rows to compare with tables:
///
/// ```
/// orders       ListView of ORDERS, rows 0 to 2
///   customer   Label with the order's customer
///   number     Label with the order's id
///   vip        Label "VIP", visible for Globex only
///   remark     Label "  Call\n    first  " for ACME, empty otherwise
///   tags       ListView of "new", "rush"
///     tag      Label with the tag
/// nothing      ListView without rows
/// some         ListView of ACME_41, OBRIEN_45 and WAYNE_44, whose row is invisible
///   customer   Label with the order's customer
///   amount     Label "1\u202F234" for ACME, with a narrow no-break space, "56" otherwise
///   box        container with a model that throws when it is read
/// hidden       ListView of ORDERS, invisible
///   customer   Label with the order's customer
/// ```
public class OrderTablePage extends WebPage {

    public static final Order WAYNE_44 = new Order(44, "Wayne | Sons");
    public static final Order OBRIEN_45 = new Order(45, "O'Brien & Sons");

    /// The orders the page lists, in this order.
    public static final List<Order> ORDERS = List.of(Order.ACME_41, Order.GLOBEX_42, WAYNE_44);

    public OrderTablePage() {
        add(new ListView<>("orders", ORDERS) {
            @Override
            protected void populateItem(ListItem<Order> item) {
                var order = item.getModelObject();
                item.add(new Label("customer", order.customer()));
                item.add(new Label("number", order.id()));
                item.add(new Label("vip", "VIP").setVisible(order.equals(Order.GLOBEX_42)));
                item.add(new Label("remark", order.equals(Order.ACME_41) ? "  Call\n    first  " : ""));
                item.add(new ListView<>("tags", List.of("new", "rush")) {
                    @Override
                    protected void populateItem(ListItem<String> tag) {
                        tag.add(new Label("tag", tag.getModelObject()));
                    }
                });
            }
        });
        add(new ListView<>("nothing", List.<Order>of()) {
            @Override
            protected void populateItem(ListItem<Order> item) {
            }
        });
        add(new ListView<>("some", List.of(Order.ACME_41, OBRIEN_45, WAYNE_44)) {
            @Override
            protected void populateItem(ListItem<Order> item) {
                var order = item.getModelObject();
                item.setVisible(!order.equals(WAYNE_44));
                item.add(new Label("customer", order.customer()));
                item.add(new Label("amount", order.equals(Order.ACME_41) ? "1\u202F234" : "56"));
                IModel<String> failing = () -> {
                    throw new IllegalStateException("no such property");
                };
                item.add(new WebMarkupContainer("box", failing));
            }
        });
        var hidden = new ListView<>("hidden", ORDERS) {
            @Override
            protected void populateItem(ListItem<Order> item) {
                item.add(new Label("customer", item.getModelObject().customer()));
            }
        };
        add(hidden.setVisible(false));
    }
}
