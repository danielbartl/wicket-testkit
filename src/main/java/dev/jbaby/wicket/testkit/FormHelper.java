package dev.jbaby.wicket.testkit;

import dev.jbaby.wicket.testkit.locator.Locator;
import org.apache.wicket.markup.html.form.FormComponent;

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

    private FormHelper() {
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
        throw new UnsupportedOperationException("not implemented yet");
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
        throw new UnsupportedOperationException("not implemented yet");
    }

    /// Submits the form with the values set so far, as `FormTester.submit()` does.
    ///
    /// ```java
    /// form.set(locate(TextField.class).withId("customer"), "ACME").submit();
    /// ```
    public void submit() {
        throw new UnsupportedOperationException("not implemented yet");
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
        throw new UnsupportedOperationException("not implemented yet");
    }
}
