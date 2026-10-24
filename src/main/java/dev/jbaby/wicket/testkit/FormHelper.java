package dev.jbaby.wicket.testkit;

import static dev.jbaby.wicket.testkit.WicketTestkit.check;
import static dev.jbaby.wicket.testkit.WicketTestkit.notNull;
import static dev.jbaby.wicket.testkit.WicketTestkit.prefix;
import static dev.jbaby.wicket.testkit.WicketTestkit.requireVisible;

import dev.jbaby.wicket.testkit.locator.Locator;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.tester.FormTester;

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
/// Like a `FormTester`, it submits once; after a submit, start again with
/// [WicketTestkit#form(Locator)].
public final class FormHelper {

    @SuppressWarnings("rawtypes")
    private final Locator<? extends Form> formLocator;
    private final Form<?> form;
    private final FormTester formTester;

    FormHelper(@SuppressWarnings("rawtypes") Locator<? extends Form> formLocator, Form<?> form,
            FormTester formTester) {
        this.formLocator = formLocator;
        this.form = form;
        this.formTester = formTester;
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
    /// @param field the locator of the choice field, inside this form
    /// @param index the index of the choice, from zero
    /// @return this helper
    /// @throws AssertionError also if the field is not enabled in hierarchy
    public FormHelper select(@SuppressWarnings("rawtypes") Locator<? extends FormComponent> field,
            int index) {
        var located = locateUsable(notNull(field, "field"));
        var formRelativePath = located.getPageRelativePath()
            .substring(form.getPageRelativePath().length() + 1);
        check(field, located, () -> formTester.select(formRelativePath, index));
        return this;
    }

    /// Submits the form with the values set so far, as `FormTester.submit()` does.
    ///
    /// ```java
    /// form.set(locate(TextField.class).withId("customer"), "ACME").submit();
    /// ```
    public void submit() {
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
    public void submit(Locator<?> button) {
        var located = requireVisible(button, notNull(button, "button").one(form));
        check(button, located, () -> formTester.submit(located));
    }

    /// The one field the locator matches inside the form, which has to be visible and enabled in
    /// hierarchy: Wicket ignores the input of any other field.
    private Component locateUsable(Locator<?> field) {
        var located = requireVisible(field, field.one(form));
        if (!located.isEnabledInHierarchy()) {
            throw new AssertionError(prefix(field, located) + "the component is not enabled in hierarchy");
        }
        return located;
    }
}
