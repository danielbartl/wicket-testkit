package dev.jbaby.wicket.testkit.fixtures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

/// A page with forms to fill in:
///
/// ```
/// order            Form; submitting it sets result to "<customer> from <country> via <submitter>"
///   customer       TextField, "ACME" at first, required, labelled "Customer" by setLabel
///   details        container
///     country      DropDownChoice of COUNTRIES, "Austria" at first; on Ajax "change" updates shipping;
///                  a plain <label wicket:for> in the markup, so no label
///   speed          DropDownChoice of "standard", "express", with a FormComponentUpdatingBehavior
///   giftWrap       CheckBox, unchecked at first
///   extras         CheckBoxMultipleChoice of EXTRAS, "tracking" at first
///   quantity       DropDownChoice of QUANTITIES, 1 at first
///   slot           DropDownChoice without choices
///   reorder        DropDownChoice of Order.ALL
///   archived       container, disabled
///     note         TextField
///     region       DropDownChoice of COUNTRIES
///     insured      CheckBox
///     services     ListMultipleChoice of EXTRAS
///     restore      Button
///   extra          container, invisible
///     coupon       TextField
///     gift         DropDownChoice of COUNTRIES
///     fragile      CheckBox
///     addons       ListMultipleChoice of EXTRAS
///     apply        Button
///   save           Button
///   saveAjax       AjaxButton, updates result
///   send           AjaxSubmitLink, updates result
/// result           Label, "not submitted" at first
/// shipping         Label "Shipping to <country>"
/// search           Form
///   query          TextField, labelled "Search term" by a <wicket:label> spread over lines
///   terms          CheckBox, labelled by the <wicket:label> "Terms &amp; <b>conditions</b>"
/// hidden           Form, invisible
/// ```
///
/// The submitter is the id of the button the form was submitted through, or `form` without one.
public class OrderFormPage extends WebPage {

    public static final List<String> COUNTRIES = List.of("Austria", "Germany", "Switzerland");
    public static final List<String> EXTRAS = List.of("insurance", "tracking", "signature");
    public static final List<Integer> QUANTITIES = IntStream.rangeClosed(1, 12).boxed().toList();

    private String customer = "ACME";
    private String country = "Austria";
    private String speed = "standard";
    private boolean giftWrap;
    private Collection<String> extras = new ArrayList<>(List.of("tracking"));
    private Integer quantity = 1;
    private String slot;
    private Order reorder;
    private String note;
    private String region;
    private String coupon;
    private String gift;
    private String result = "not submitted";
    private String query;
    private boolean terms;

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
        var customerField = new TextField<>("customer", PropertyModel.<String>of(this, "customer"));
        order.add(customerField.setRequired(true).setLabel(Model.of("Customer")));

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
        order.add(new CheckBox("giftWrap", PropertyModel.of(this, "giftWrap")));
        order.add(new CheckBoxMultipleChoice<>("extras", PropertyModel.<Collection<String>>of(this, "extras"), EXTRAS));
        order.add(new DropDownChoice<>("quantity", PropertyModel.<Integer>of(this, "quantity"), QUANTITIES));
        order.add(new DropDownChoice<>("slot", PropertyModel.<String>of(this, "slot"), List.<String>of()));
        order.add(new DropDownChoice<>("reorder", PropertyModel.<Order>of(this, "reorder"), Order.ALL));

        var archived = new WebMarkupContainer("archived");
        archived.setEnabled(false);
        archived.add(new TextField<>("note", PropertyModel.<String>of(this, "note")));
        archived.add(new DropDownChoice<>("region", PropertyModel.<String>of(this, "region"), COUNTRIES));
        archived.add(new CheckBox("insured", Model.of(false)));
        archived.add(new ListMultipleChoice<>("services", new ListModel<>(new ArrayList<>()), EXTRAS));
        archived.add(new Button("restore"));
        order.add(archived);

        var extra = new WebMarkupContainer("extra");
        extra.setVisible(false);
        extra.add(new TextField<>("coupon", PropertyModel.<String>of(this, "coupon")));
        extra.add(new DropDownChoice<>("gift", PropertyModel.<String>of(this, "gift"), COUNTRIES));
        extra.add(new CheckBox("fragile", Model.of(false)));
        extra.add(new ListMultipleChoice<>("addons", new ListModel<>(new ArrayList<>()), EXTRAS));
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
        search.add(new CheckBox("terms", PropertyModel.of(this, "terms")));
        add(search);

        var hidden = new Form<Void>("hidden");
        hidden.setVisible(false);
        add(hidden);
    }
}
