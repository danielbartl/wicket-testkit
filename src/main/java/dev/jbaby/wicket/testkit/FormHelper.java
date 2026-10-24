package dev.jbaby.wicket.testkit;

import static dev.jbaby.wicket.testkit.WicketTestkit.check;
import static dev.jbaby.wicket.testkit.WicketTestkit.notNull;
import static dev.jbaby.wicket.testkit.WicketTestkit.prefix;
import static dev.jbaby.wicket.testkit.WicketTestkit.requireAjaxBehavior;
import static dev.jbaby.wicket.testkit.WicketTestkit.requireVisible;

import dev.jbaby.wicket.testkit.locator.Locator;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;

/// Fills in and submits one form through a `FormTester`, with fields and buttons found by
/// locators inside that form. [WicketTestkit#form(Locator)] creates it.
///
/// ```java
/// kit.form(locate(Form.class).withId("order"))
///     .set(locate(TextField.class).withId("customer"), "ACME")
///     .select(locate(DropDownChoice.class).withId("country"), 2)
///     .submit(locate(AjaxButton.class).withId("save"));
/// ```
///
/// A form helper lives for one request. Until the tester handles another one, `form` returns the
/// same helper for the same form, so page objects can fill in a form step by step. Once a submit,
/// an Ajax event or anything else has sent a request, the helper fails; call `form` again.
public final class FormHelper {

    private final WicketTester tester;
    @SuppressWarnings("rawtypes")
    private final Locator<? extends Form> formLocator;
    private final Form<?> form;
    private final FormTester formTester;
    private final Object request;

    FormHelper(WicketTester tester, @SuppressWarnings("rawtypes") Locator<? extends Form> formLocator,
            Form<?> form, FormTester formTester) {
        this.tester = tester;
        this.formLocator = formLocator;
        this.form = form;
        this.formTester = formTester;
        this.request = tester.getRequest();
    }

    /// Sets a field's input, as `FormTester.setValue` does.
    ///
    /// ```java
    /// form.set(locate(TextField.class).withId("customer"), "ACME");
    /// ```
    ///
    /// @param field the locator of the field, inside this form
    /// @param value the input, as a user would type it
    /// @return this helper
    /// @throws AssertionError also if the field is not enabled in hierarchy
    public FormHelper set(@SuppressWarnings("rawtypes") Locator<? extends FormComponent> field,
            String value) {
        requireCurrent();
        notNull(value, "value");
        var located = locateUsable(notNull(field, "field"));
        check(field, located, () -> formTester.setValue(located, value));
        return this;
    }

    /// Selects the choice at an index, as `FormTester.select` does.
    ///
    /// ```java
    /// form.select(locate(DropDownChoice.class).withId("country"), 2);
    /// ```
    ///
    /// A field with a `FormComponentUpdatingBehavior` can't be selected this way, because
    /// `FormTester` would send a request for it at once and lose the values set so far. Use
    /// [#formTester()] to select it anyway.
    ///
    /// @param field the locator of the choice field, inside this form
    /// @param index the index of the choice, from zero
    /// @return this helper
    /// @throws AssertionError also if the field is not enabled in hierarchy or has a
    ///     `FormComponentUpdatingBehavior`
    public FormHelper select(@SuppressWarnings("rawtypes") Locator<? extends FormComponent> field,
            int index) {
        requireCurrent();
        var located = locateUsable(notNull(field, "field"));
        if (!located.getBehaviors(FormComponentUpdatingBehavior.class).isEmpty()) {
            throw new AssertionError(prefix(field, located) + "the component has a "
                + "FormComponentUpdatingBehavior, which FormTester would run at once, losing the values set so far;"
                + " use formTester() to select it anyway");
        }
        var formRelativePath = located.getPageRelativePath()
            .substring(form.getPageRelativePath().length() + 1);
        check(field, located, () -> formTester.select(formRelativePath, index));
        return this;
    }

    /// Fires an Ajax event on a component inside the form, sending the values set so far, as
    /// `WicketTester.executeAjaxEvent` does. An `AjaxFormComponentUpdatingBehavior` then sees the
    /// field's new input. This sends a request, so call `form` again afterwards.
    ///
    /// ```java
    /// kit.form(locate(Form.class).withId("order"))
    ///     .select(locate(DropDownChoice.class).withId("country"), 1)
    ///     .ajax(locate(DropDownChoice.class).withId("country"), "change");
    /// ```
    ///
    /// @param component the locator of the component, inside this form
    /// @param event the event, such as `change` or `blur`
    /// @throws AssertionError also if the component has no Ajax behavior for the event
    public void ajax(Locator<?> component, String event) {
        requireCurrent();
        notNull(event, "event");
        var located = requireVisible(component, notNull(component, "component").one(form));
        requireAjaxBehavior(component, located, event);
        check(component, located, () -> tester.executeAjaxEvent(located, event));
    }

    /// Submits the form with the values set so far, as `FormTester.submit()` does.
    ///
    /// ```java
    /// form.set(locate(TextField.class).withId("customer"), "ACME").submit();
    /// ```
    public void submit() {
        requireCurrent();
        check(formLocator, form, formTester::submit);
    }

    /// Submits the form through a button with the values set so far, as
    /// `FormTester.submit(Component)` does: through Ajax if the button is an `AjaxButton` or an
    /// `AjaxSubmitLink`.
    ///
    /// ```java
    /// form.set(locate(TextField.class).withId("customer"), "ACME")
    ///     .submit(locate(Button.class).withId("save"));
    /// ```
    ///
    /// @param button the locator of the button, inside this form
    /// @throws AssertionError also if the button is not enabled in hierarchy
    public void submit(Locator<?> button) {
        requireCurrent();
        var located = locateUsable(notNull(button, "button"));
        check(button, located, () -> formTester.submit(located));
    }

    /// Returns the `FormTester` behind this helper, with the values set so far, for what the
    /// helper doesn't offer: files, multiple selections, check boxes as booleans.
    ///
    /// ```java
    /// kit.form(locate(Form.class).withId("upload")).formTester().setFile("file", file, "text/plain");
    /// ```
    ///
    /// @return the form tester
    public FormTester formTester() {
        requireCurrent();
        return formTester;
    }

    /// Whether the tester is still on the request this helper started with.
    boolean isCurrent() {
        return tester.getRequest() == request;
    }

    private void requireCurrent() {
        if (!isCurrent()) {
            throw new AssertionError(prefix(formLocator, form)
                + "the tester has handled a request since this form helper was created; call form again");
        }
    }

    /// The one component the locator matches inside the form, which has to be visible and enabled
    /// in hierarchy: Wicket ignores the input of any other field, and nobody can click a disabled
    /// button.
    private Component locateUsable(Locator<?> locator) {
        var located = requireVisible(locator, locator.one(form));
        if (!located.isEnabledInHierarchy()) {
            throw new AssertionError(prefix(locator, located) + "the component is not enabled in hierarchy");
        }
        return located;
    }
}
