package dev.jbaby.wicket.testkit.fixtures;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.Model;

/// A page with a bit of everything a locator looks at:
///
/// ```
/// title       Label "Shop"
/// headline    Headline "Today"
/// note        Label "Closed", invisible
/// closed      container, invisible
///   reason    Label "Holiday"
/// locked      container, disabled
///   hint      Label "Ask the manager"
///   unlock    Link
/// refresh     Link
/// archive     Link, disabled
/// user        UserPanel "ACME"
///   name      Label "ACME"
/// orders      ListView of Order.ALL, rows 0 to 2
///   customer  Label with the order's customer
///   number    Label with the order's id, an Integer
/// table       DataView of Order.ALL, rows 1 to 3
///   customer  Label with the order's customer
/// ```
///
/// The links are `NoOpLink`s.
public class ShopPage extends WebPage {

    public ShopPage() {
        add(new Label("title", "Shop"));
        add(new Headline("headline", "Today"));
        add(new Label("note", "Closed").setVisible(false));

        var closed = new WebMarkupContainer("closed");
        closed.setVisible(false);
        closed.add(new Label("reason", "Holiday"));
        add(closed);

        var locked = new WebMarkupContainer("locked");
        locked.setEnabled(false);
        locked.add(new Label("hint", "Ask the manager"));
        locked.add(new NoOpLink("unlock"));
        add(locked);

        add(new NoOpLink("refresh"));
        add(new NoOpLink("archive").setEnabled(false));
        add(new UserPanel("user", Model.of("ACME")));

        add(new ListView<>("orders", Order.ALL) {
            @Override
            protected void populateItem(ListItem<Order> item) {
                item.add(new Label("customer", item.getModelObject().customer()));
                item.add(new Label("number", item.getModelObject().id()));
            }
        });
        add(new DataView<>("table", new ListDataProvider<>(Order.ALL)) {
            @Override
            protected void populateItem(Item<Order> item) {
                item.add(new Label("customer", item.getModelObject().customer()));
            }
        });
    }
}
