package dev.jbaby.wicket.testkit.fixtures;

import java.util.HashSet;
import java.util.Set;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;

/// A page to act on:
///
/// ```
/// title            Label "Orders"
/// feedback         FeedbackPanel
/// orders           ListView of Order.ALL, rows 0 to 2
///   customer       Label with the order's customer
///   status         Label "open", "shipped" once shipped
///   ship           AjaxLink: ships the order, says "Shipped order <id>", updates status and feedback
///   details        Link to the order's OrderPage
/// bumps            Label with how often bump was double-clicked
/// bump             container with an Ajax behavior for "dblclick" only
/// frozen           NoOpLink, disabled
/// vault            container, invisible
///   secret         Label "42"
///   open           NoOpLink
///   vaultFeedback  FeedbackPanel
/// boom             Link that throws an IllegalStateException "boom"
/// ```
public class OrdersPage extends WebPage {

    private final Set<Integer> shipped = new HashSet<>();
    private int bumps;

    public OrdersPage() {
        add(new Label("title", "Orders"));
        var feedback = new FeedbackPanel("feedback");
        add(feedback.setOutputMarkupId(true));

        add(new ListView<>("orders", Order.ALL) {
            @Override
            protected void populateItem(ListItem<Order> item) {
                var order = item.getModelObject();
                IModel<String> statusText = () -> shipped.contains(order.id()) ? "shipped" : "open";
                var status = new Label("status", statusText);
                item.add(new Label("customer", order.customer()));
                item.add(status.setOutputMarkupId(true));
                item.add(new AjaxLink<Void>("ship") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        shipped.add(order.id());
                        info("Shipped order " + order.id());
                        target.add(status, feedback);
                    }
                });
                item.add(new Link<Void>("details") {
                    @Override
                    public void onClick() {
                        setResponsePage(new OrderPage(order));
                    }
                });
            }
        });

        IModel<Integer> bumpCount = () -> bumps;
        var counter = new Label("bumps", bumpCount);
        add(counter.setOutputMarkupId(true));
        var bump = new WebMarkupContainer("bump");
        bump.add(new AjaxEventBehavior("dblclick") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                bumps++;
                target.add(counter);
            }
        });
        add(bump);

        add(new NoOpLink("frozen").setEnabled(false));

        var vault = new WebMarkupContainer("vault");
        vault.setVisible(false);
        vault.add(new Label("secret", "42"), new NoOpLink("open"), new FeedbackPanel("vaultFeedback"));
        add(vault);

        add(new Link<Void>("boom") {
            @Override
            public void onClick() {
                throw new IllegalStateException("boom");
            }
        });
    }
}
