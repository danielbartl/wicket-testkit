package dev.jbaby.wicket.testkit;

import static dev.jbaby.wicket.testkit.locator.Locator.locate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import dev.jbaby.wicket.testkit.fixtures.OrderFormPage;
import dev.jbaby.wicket.testkit.locator.Locator;
import java.util.Collection;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
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
    final Locator<DropDownChoice> quantity = locate(DropDownChoice.class).withId("quantity");
    final Locator<CheckBox> giftWrap = locate(CheckBox.class).withId("giftWrap");
    final Locator<CheckBoxMultipleChoice> extras = locate(CheckBoxMultipleChoice.class).withId("extras");

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
    @DisplayName("R4.8 set, select, choose, chooseAll, check, uncheck and submit with a button fail on a field or button that is not enabled in hierarchy")
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
        var insured = locate(CheckBox.class).withId("insured");
        assertAll(
            () -> assertDisabled(() -> kit.form(order).choose(locate(DropDownChoice.class).withId("region"), "Germany")),
            () -> assertDisabled(() -> kit.form(order).chooseAll(locate(ListMultipleChoice.class).withId("services"),
                "tracking")),
            () -> assertDisabled(() -> kit.form(order).check(insured)),
            () -> assertDisabled(() -> kit.form(order).uncheck(insured)));
    }

    static void assertDisabled(ThrowingCallable call) {
        assertThatThrownBy(call)
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
    @DisplayName("R4.11 select and choose fail on a field with a FormComponentUpdatingBehavior")
    void selectFailsOnAFieldThatWouldSubmitAtOnce() {
        var speed = locate(DropDownChoice.class).withId("speed");
        var message = "a DropDownChoice with id \"speed\" (at order:speed): the component has a "
            + "FormComponentUpdatingBehavior, which FormTester would run at once, losing the values set so far;"
            + " use formTester() to select it anyway";

        assertThatThrownBy(() -> kit.form(order).select(speed, 1))
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessage(message);
        assertThatThrownBy(() -> kit.form(order).choose(speed, "express"))
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessage(message);
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
    @DisplayName("R4.14 choose selects the choice that equals the given one")
    void chooseSelectsTheEqualChoice() {
        kit.form(order).choose(country, "Switzerland").choose(quantity, 12).submit();

        tester.assertModelValue("order:details:country", "Switzerland");
        tester.assertModelValue("order:quantity", 12);
    }

    @Test
    @DisplayName("R4.15 choose and chooseAll fail on a choice the field doesn't have, and list the choices it has")
    void chooseFailsOnAnUnknownChoice() {
        assertThatThrownBy(() -> kit.form(order).choose(country, "France"))
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessage("a DropDownChoice with id \"country\" (at order:details:country): the component has no "
                + "choice \"France\"; its choices are \"Austria\", \"Germany\", \"Switzerland\"");
        assertThatThrownBy(() -> kit.form(order).choose(quantity, 13))
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessageEndingWith("): the component has no choice 13; its choices are "
                + "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, and 2 more");
        assertThatThrownBy(() -> kit.form(order).choose(locate(DropDownChoice.class).withId("slot"), "morning"))
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessageEndingWith("): the component has no choice \"morning\"; its choices are none");
        assertThatThrownBy(() -> kit.form(order).chooseAll(extras, "tracking", "gift card"))
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessageEndingWith("): the component has no choice \"gift card\"; its choices are "
                + "\"insurance\", \"tracking\", \"signature\"");
    }

    @Test
    @DisplayName("R4.20 when no choice is of the given choice's class, the message names the classes")
    void chooseNamesTheClassesOnATypeMismatch() {
        assertThatThrownBy(() -> kit.form(order).choose(quantity, 12L))
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessageEndingWith("): the component has no choice 12 of type Long; its choices, of type Integer, are "
                + "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, and 2 more");
    }

    @Test
    @DisplayName("R4.21 classes with the same simple name appear by their full names")
    void classesWithTheSameSimpleNameAppearByTheirFullNames() {
        assertThatThrownBy(() -> kit.form(order).choose(locate(DropDownChoice.class).withId("reorder"), new Order(41)))
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessageContaining(" of type dev.jbaby.wicket.testkit.FormHelperTests.Order; its choices, of type "
                + "dev.jbaby.wicket.testkit.fixtures.Order, are ");
    }

    /// An order of another package, with the same simple name as the fixtures' `Order`.
    record Order(int id) implements java.io.Serializable {
    }

    @Test
    @DisplayName("R4.16 chooseAll replaces the selection with exactly the given choices")
    void chooseAllReplacesTheSelection() {
        kit.form(order).chooseAll(extras, "signature", "insurance").submit();

        assertThat(selected(extras)).containsExactlyInAnyOrder("insurance", "signature");
    }

    @SuppressWarnings("unchecked")
    Collection<Object> selected(Locator<CheckBoxMultipleChoice> field) {
        return (Collection<Object>) kit.one(field).getModelObject();
    }

    @Test
    @DisplayName("R4.17 chooseAll without choices clears the selection")
    void chooseAllWithoutChoicesClearsTheSelection() {
        kit.form(order).chooseAll(extras).submit();

        assertThat(selected(extras)).isEmpty();
    }

    @Test
    @DisplayName("R4.18 check checks the check box")
    void checkChecksTheCheckBox() {
        kit.form(order).check(giftWrap).submit();

        tester.assertModelValue("order:giftWrap", true);
    }

    @Test
    @DisplayName("R4.19 uncheck unchecks the check box")
    void uncheckUnchecksTheCheckBox() {
        kit.form(order).check(giftWrap).submit();
        kit.form(order).uncheck(giftWrap).submit();

        tester.assertModelValue("order:giftWrap", false);
    }

    @Test
    @DisplayName("R5.10 assertFeedbackOn checks the component's own feedback messages")
    void assertFeedbackOnChecksTheComponentsMessages() {
        kit.form(order).set(customer, "").submit();

        kit.assertFeedbackOn(customer, "'Customer' is required.");
        assertThatThrownBy(() -> kit.assertFeedbackOn(customer, "Customer is missing."))
            .isInstanceOf(AssertionError.class)
            .hasMessageStartingWith("a TextField with id \"customer\" (at order:customer): ")
            .hasMessageContaining("'Customer' is required.");
    }

    @Test
    @DisplayName("R5.11 assertFeedbackOn without messages checks that the component has none")
    void assertFeedbackOnWithoutMessagesChecksForNone() {
        kit.form(order).set(customer, "").submit();

        kit.assertFeedbackOn(country);
        assertThatThrownBy(() -> kit.assertFeedbackOn(customer))
            .isInstanceOf(AssertionError.class)
            .hasMessageStartingWith("a TextField with id \"customer\" (at order:customer): ")
            .hasMessageContaining("'Customer' is required.");
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
            () -> assertThatNullPointerException().isThrownBy(() -> form.choose(null, "Germany"))
                .withMessageContaining("field"),
            () -> assertThatNullPointerException().isThrownBy(() -> form.choose(country, null))
                .withMessageContaining("choice"),
            () -> assertThatNullPointerException().isThrownBy(() -> form.chooseAll(null, "tracking"))
                .withMessageContaining("field"),
            () -> assertThatNullPointerException().isThrownBy(() -> form.chooseAll(extras, (Object[]) null))
                .withMessageContaining("choices"),
            () -> assertThatNullPointerException().isThrownBy(() -> form.chooseAll(extras, "tracking", null))
                .withMessageContaining("choices"),
            () -> assertThatNullPointerException().isThrownBy(() -> form.check(null))
                .withMessageContaining("field"),
            () -> assertThatNullPointerException().isThrownBy(() -> form.uncheck(null))
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
            () -> assertInvisible(() -> kit.form(order).choose(locate(DropDownChoice.class).withId("gift"), "Germany")),
            () -> assertInvisible(() -> kit.form(order).chooseAll(locate(ListMultipleChoice.class).withId("addons"),
                "tracking")),
            () -> assertInvisible(() -> kit.form(order).check(locate(CheckBox.class).withId("fragile"))),
            () -> assertInvisible(() -> kit.form(order).uncheck(locate(CheckBox.class).withId("fragile"))),
            () -> assertInvisible(() -> kit.form(order).submit(locate(Button.class).withId("apply"))),
            () -> assertInvisible(() -> kit.form(order).ajax(locate(DropDownChoice.class).withId("gift"), "change")));
    }

    @Test
    @DisplayName("R1.4 a null argument to a form helper of an earlier request throws a NullPointerException too")
    void nullArgumentsAreRejectedBeforeTheRequestIsChecked() {
        var stale = kit.form(order);
        stale.submit();

        assertAll(
            () -> assertThatNullPointerException().isThrownBy(() -> stale.choose(country, null))
                .withMessageContaining("choice"),
            () -> assertThatNullPointerException().isThrownBy(() -> stale.chooseAll(extras, (Object[]) null))
                .withMessageContaining("choices"),
            () -> assertThatNullPointerException().isThrownBy(() -> stale.set(customer, null))
                .withMessageContaining("value"),
            () -> assertThatNullPointerException().isThrownBy(() -> stale.select(null, 1))
                .withMessageContaining("field"),
            () -> assertThatNullPointerException().isThrownBy(() -> stale.check(null))
                .withMessageContaining("field"),
            () -> assertThatNullPointerException().isThrownBy(() -> stale.ajax(country, null))
                .withMessageContaining("event"),
            () -> assertThatNullPointerException().isThrownBy(() -> stale.submit(null))
                .withMessageContaining("button"));
    }

    static void assertInvisible(ThrowingCallable call) {
        assertThatThrownBy(call)
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessageEndingWith("): the component is not visible in hierarchy");
    }
}
