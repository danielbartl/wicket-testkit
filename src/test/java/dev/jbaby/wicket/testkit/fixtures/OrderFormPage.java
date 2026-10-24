package dev.jbaby.wicket.testkit.fixtures;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/// A page with forms to fill in:
///
/// ```
/// order            Form; submitting it sets result to "<customer> from <country> via <submitter>"
///   customer       TextField, "ACME" at first
///   details        container
///     country      DropDownChoice of COUNTRIES, "Austria" at first; on Ajax "change" updates shipping
///   speed          DropDownChoice of "standard", "express", with a FormComponentUpdatingBehavior
///   archived       container, disabled
///     note         TextField
///     region       DropDownChoice of COUNTRIES
///     restore      Button
///   extra          container, invisible
///     coupon       TextField
///     gift         DropDownChoice of COUNTRIES
///     apply        Button
///   save           Button
///   saveAjax       AjaxButton, updates result
///   send           AjaxSubmitLink, updates result
/// result           Label, "not submitted" at first
/// shipping         Label "Shipping to <country>"
/// search           Form
///   query          TextField
/// hidden           Form, invisible
/// ```
///
/// The submitter is the id of the button the form was submitted through, or `form` without one.
public class OrderFormPage extends WebPage {

    public static final List<String> COUNTRIES = List.of("Austria", "Germany", "Switzerland");

    private String customer = "ACME";
    private String country = "Austria";
    private String speed = "standard";
    private String note;
    private String region;
    private String coupon;
    private String gift;
    private String result = "not submitted";
    private String query;

    public OrderFormPage() {
        IModel<String> resultModel = () -> result;
        var resultLabel = new Label("result", resultModel);
        add(resultLabel.setOutputMarkupId(true));
        IModel<String> shippingModel = () -> "Shipping to " + country;
        var shipping = new Label("shipping", shippingModel);
        add(shipping.setOutputMarkupId(true));

        var order = new Form<Void>("order") {
            @Override
            protected void onSubmit() {
                var submitter = findSubmitter();
                var submittedBy = submitter == null ? "form" : ((Component) submitter).getId();
                result = customer + " from " + country + " via " + submittedBy;
            }
        };
        order.add(new TextField<>("customer", PropertyModel.<String>of(this, "customer")));

        var details = new WebMarkupContainer("details");
        var countryChoice = new DropDownChoice<>("country", PropertyModel.<String>of(this, "country"), COUNTRIES);
        countryChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(shipping);
            }
        });
        details.add(countryChoice);
        order.add(details);

        var speedChoice = new DropDownChoice<>("speed", PropertyModel.<String>of(this, "speed"),
            List.of("standard", "express"));
        order.add(speedChoice.add(new FormComponentUpdatingBehavior()));

        var archived = new WebMarkupContainer("archived");
        archived.setEnabled(false);
        archived.add(new TextField<>("note", PropertyModel.<String>of(this, "note")));
        archived.add(new DropDownChoice<>("region", PropertyModel.<String>of(this, "region"), COUNTRIES));
        archived.add(new Button("restore"));
        order.add(archived);

        var extra = new WebMarkupContainer("extra");
        extra.setVisible(false);
        extra.add(new TextField<>("coupon", PropertyModel.<String>of(this, "coupon")));
        extra.add(new DropDownChoice<>("gift", PropertyModel.<String>of(this, "gift"), COUNTRIES));
        extra.add(new Button("apply"));
        order.add(extra);

        order.add(new Button("save"));
        order.add(new AjaxButton("saveAjax") {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                target.add(resultLabel);
            }
        });
        order.add(new AjaxSubmitLink("send") {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                target.add(resultLabel);
            }
        });
        add(order);

        var search = new Form<Void>("search");
        search.add(new TextField<>("query", PropertyModel.<String>of(this, "query")));
        add(search);

        var hidden = new Form<Void>("hidden");
        hidden.setVisible(false);
        add(hidden);
    }
}
