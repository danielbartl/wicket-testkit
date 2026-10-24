package dev.jbaby.wicket.testkit;

import static dev.jbaby.wicket.testkit.locator.Locator.locate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import dev.jbaby.wicket.testkit.fixtures.OrderFormPage;
import dev.jbaby.wicket.testkit.locator.Locator;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.WicketTester;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// Each test starts with the id of the requirement it proves.
/// The requirements live in this package's `package-info.java`.
/// The tests fill in the forms of a rendered `OrderFormPage`, whose Javadoc shows its component tree.
@SuppressWarnings("rawtypes")
class FormHelperTests {

    final Locator<Form> order = locate(Form.class).withId("order");
    final Locator<TextField> customer = locate(TextField.class).withId("customer");
    final Locator<DropDownChoice> country = locate(DropDownChoice.class).withId("country");

    WicketTester tester;
    WicketTestkit kit;

    @BeforeEach
    void renderOrderFormPage() {
        tester = new WicketTester();
        kit = WicketTestkit.of(tester);
        tester.startPage(OrderFormPage.class);
    }

    @AfterEach
    void destroyTester() {
        tester.destroy();
    }

    @Test
    @DisplayName("R4.1 form returns a helper that starts from the form's current values")
    void formStartsFromTheCurrentValues() {
        kit.form(order).submit();

        tester.assertLabel("result", "ACME from Austria via form");
    }

    @Test
    @DisplayName("R4.2 the form helper locates fields among the form's descendants only, at any depth")
    void fieldsAreLocatedInsideTheForm() {
        assertThatThrownBy(() -> kit.form(order).set(locate(TextField.class).withId("query"), "Globex"))
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessageStartingWith(
                "Expected exactly one match for a TextField with id \"query\" in Form \"order\", but found none.");

        kit.form(order).select(country, 2).submit();

        tester.assertLabel("result", "ACME from Switzerland via form");
    }

    @Test
    @DisplayName("R4.3 set sets the field's input as FormTester.setValue does")
    void setSetsTheInput() {
        kit.form(order).set(customer, "Globex").submit();

        tester.assertModelValue("order:customer", "Globex");
    }

    @Test
    @DisplayName("R4.4 select selects the choice at the index as FormTester.select does")
    void selectSelectsTheChoice() {
        kit.form(order).select(country, 1).submit();

        tester.assertModelValue("order:details:country", "Germany");
    }

    @Test
    @DisplayName("R4.5 submit without a button submits the form with the values set so far")
    void submitSubmitsTheForm() {
        kit.form(order).set(customer, "Globex").select(country, 1).submit();

        tester.assertLabel("result", "Globex from Germany via form");
    }

    @Test
    @DisplayName("R4.6 submit with a button submits the form through that button")
    void submitWithAButtonSubmitsThroughIt() {
        kit.form(order).set(customer, "Globex").submit(locate(Button.class).withId("save"));

        tester.assertLabel("result", "Globex from Austria via save");
    }

    @Test
    @DisplayName("R4.7 an AjaxButton or AjaxSubmitLink submits the form through Ajax with the values set so far")
    void ajaxButtonsSubmitThroughAjax() {
        kit.form(order).set(customer, "Globex").submit(locate(AjaxButton.class).withId("saveAjax"));

        tester.assertLabel("result", "Globex from Austria via saveAjax");
        tester.assertComponentOnAjaxResponse("result");

        kit.form(order).set(customer, "Initech").submit(locate(AjaxSubmitLink.class).withId("send"));

        tester.assertLabel("result", "Initech from Austria via send");
        tester.assertComponentOnAjaxResponse("result");
    }

    @Test
    @DisplayName("R4.8 set, select and submit with a button fail on a field or button that is not enabled in hierarchy")
    void disabledFieldsAndButtonsAreReported() {
        assertThatThrownBy(() -> kit.form(order).submit(locate(Button.class).withId("restore")))
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessage("a Button with id \"restore\" (at order:archived:restore): "
                + "the component is not enabled in hierarchy");
        tester.assertLabel("result", "not submitted");
        assertThatThrownBy(() -> kit.form(order).set(locate(TextField.class).withId("note"), "fragile"))
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessage("a TextField with id \"note\" (at order:archived:note): "
                + "the component is not enabled in hierarchy");
        assertThatThrownBy(() -> kit.form(order).select(locate(DropDownChoice.class).withId("region"), 1))
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessageEndingWith("): the component is not enabled in hierarchy");
    }

    @Test
    @DisplayName("R4.9 form returns the same helper until the tester handles another request")
    void formReturnsTheSameHelperWithinARequest() {
        kit.form(order).set(customer, "Globex");
        kit.form(order).submit();

        tester.assertLabel("result", "Globex from Austria via form");
        assertThat(kit.form(order)).isSameAs(kit.form(order));
    }

    @Test
    @DisplayName("R4.10 a form helper fails once the tester has handled another request")
    void aHelperFailsAfterAnotherRequest() {
        var form = kit.form(order);
        form.set(customer, "Globex").submit(locate(AjaxButton.class).withId("saveAjax"));

        assertThatThrownBy(() -> form.set(customer, "Initech"))
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessage("a Form with id \"order\" (at order): "
                + "the tester has handled a request since this form helper was created; call form again");
        assertThat(kit.form(order)).isNotSameAs(form);
    }

    @Test
    @DisplayName("R4.11 select fails on a field with a FormComponentUpdatingBehavior")
    void selectFailsOnAFieldThatWouldSubmitAtOnce() {
        assertThatThrownBy(() -> kit.form(order).select(locate(DropDownChoice.class).withId("speed"), 1))
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessage("a DropDownChoice with id \"speed\" (at order:speed): the component has a "
                + "FormComponentUpdatingBehavior, which FormTester would run at once, losing the values set so far;"
                + " use formTester() to select it anyway");
    }

    @Test
    @DisplayName("R4.12 the form helper's ajax fires the event with the values set so far")
    void ajaxFiresTheEventWithTheValuesSetSoFar() {
        kit.form(order).select(country, 1).ajax(country, "change");

        tester.assertLabel("shipping", "Shipping to Germany");
        tester.assertComponentOnAjaxResponse("shipping");
    }

    @Test
    @DisplayName("R4.13 formTester returns the FormTester with the values set so far")
    void formTesterHoldsTheValuesSetSoFar() {
        var formTester = kit.form(order).set(customer, "Globex").formTester();

        formTester.submit();

        tester.assertLabel("result", "Globex from Austria via form");
    }

    @Test
    @DisplayName("R3.4 the form helper's ajax fails if the component has no Ajax behavior for the event")
    void ajaxFailsWithoutABehaviorForTheEvent() {
        assertThatThrownBy(() -> kit.form(order).ajax(customer, "change"))
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessage("a TextField with id \"customer\" (at order:customer): "
                + "the component has no Ajax behavior for the event \"change\"");
    }

    @Test
    @DisplayName("R1.4 a null argument to the form helper throws a NullPointerException that names the parameter")
    void nullArgumentsAreRejected() {
        var form = kit.form(order);

        assertAll(
            () -> assertThatNullPointerException().isThrownBy(() -> form.set(null, "Globex"))
                .withMessageContaining("field"),
            () -> assertThatNullPointerException().isThrownBy(() -> form.set(customer, null))
                .withMessageContaining("value"),
            () -> assertThatNullPointerException().isThrownBy(() -> form.select(null, 1))
                .withMessageContaining("field"),
            () -> assertThatNullPointerException().isThrownBy(() -> form.submit(null))
                .withMessageContaining("button"),
            () -> assertThatNullPointerException().isThrownBy(() -> form.ajax(null, "change"))
                .withMessageContaining("component"),
            () -> assertThatNullPointerException().isThrownBy(() -> form.ajax(country, null))
                .withMessageContaining("event"));
    }

    @Test
    @DisplayName("R2.1 a form helper locator that matches no component or several fails with the locator's AssertionError")
    void formHelperLocatorsMustMatchExactlyOne() {
        assertThatThrownBy(() -> kit.form(order).set(locate(TextField.class).withId("missing"), "Globex"))
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessageStartingWith(
                "Expected exactly one match for a TextField with id \"missing\" in Form \"order\", but found none.");
        assertThatThrownBy(() -> kit.form(order).submit(locate(Button.class)))
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessageStartingWith("Expected exactly one match for a Button in Form \"order\", but found 4:");
    }

    @Test
    @DisplayName("R7.3 form and the form helper's methods fail on an invisible component with a message that says so")
    void invisibleFormsFieldsAndButtonsAreReported() {
        assertThatThrownBy(() -> kit.form(locate(Form.class).withId("hidden")))
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessage("a Form with id \"hidden\" (at hidden): the component is not visible in hierarchy");
        assertAll(
            () -> assertInvisible(() -> kit.form(order).set(locate(TextField.class).withId("coupon"), "SAVE10")),
            () -> assertInvisible(() -> kit.form(order).select(locate(DropDownChoice.class).withId("gift"), 0)),
            () -> assertInvisible(() -> kit.form(order).submit(locate(Button.class).withId("apply"))),
            () -> assertInvisible(() -> kit.form(order).ajax(locate(DropDownChoice.class).withId("gift"), "change")));
    }

    static void assertInvisible(ThrowingCallable call) {
        assertThatThrownBy(call)
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessageEndingWith("): the component is not visible in hierarchy");
    }
}
