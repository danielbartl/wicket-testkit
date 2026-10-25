package dev.jbaby.wicket.testkit;

import static dev.jbaby.wicket.testkit.WicketTestkit.className;
import static dev.jbaby.wicket.testkit.WicketTestkit.delegate;
import static dev.jbaby.wicket.testkit.WicketTestkit.notNull;
import static dev.jbaby.wicket.testkit.WicketTestkit.prefix;
import static dev.jbaby.wicket.testkit.WicketTestkit.requireAjaxBehavior;
import static dev.jbaby.wicket.testkit.WicketTestkit.requireVisible;

import static java.util.stream.Collectors.joining;

import dev.jbaby.wicket.testkit.locator.Locator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.AbstractChoice;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;

/// Fills in and submits one form through a `FormTester`, with fields and buttons found by
/// locators inside that form. [WicketTestkit#form(Locator)] creates it.
///
/// ```java
/// kit.form(locate(Form.class).withId("order"))
///     .set(locate(TextField.class).withId("customer"), "ACME")
///     .choose(locate(DropDownChoice.class).withId("country"), "Switzerland")
///     .check(locate(CheckBox.class).withId("giftWrap"))
///     .submit(locate(AjaxButton.class).withId("save"));
/// ```
///
/// A form helper lives for one request. Until the tester handles another one, `form` returns the
/// same helper for the same form, so page objects can fill in a form step by step. Once a submit,
/// an Ajax event or anything else has sent a request, the helper fails; call `form` again.
public final class FormHelper {

    /// How many choices a failure message lists.
    private static final int MAX_CHOICES = 10;

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
        delegate(field, located, () -> formTester.setValue(located, value));
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
        requireNoUpdatingBehavior(field, located);
        delegate(field, located, () -> formTester.select(formRelativePath(located), index));
        return this;
    }

    /// Selects a choice, as `FormTester.select` does with the index of the first of the field's
    /// choices that equals it. Unlike [#select(Locator, int)], it still selects the right choice
    /// once the choices are reordered.
    ///
    /// ```java
    /// form.choose(locate(DropDownChoice.class).withId("country"), "Switzerland");
    /// ```
    ///
    /// Like `select`, it fails on a field with a `FormComponentUpdatingBehavior`.
    ///
    /// @param field the locator of the choice field, inside this form
    /// @param choice the choice, compared with `equals`
    /// @return this helper
    /// @throws AssertionError also if the field has no such choice; the message lists the choices
    ///     it has
    public FormHelper choose(@SuppressWarnings("rawtypes") Locator<? extends AbstractChoice> field,
            Object choice) {
        requireCurrent();
        notNull(choice, "choice");
        var located = (AbstractChoice<?, ?>) locateUsable(notNull(field, "field"));
        requireNoUpdatingBehavior(field, located);
        var index = indexOf(field, located, located.getChoices(), choice);
        delegate(field, located, () -> formTester.select(formRelativePath(located), index));
        return this;
    }

    /// Replaces the selection of a multiple-choice field with exactly the given choices, as
    /// `FormTester.selectMultiple` does with their indexes. Without choices, it clears the
    /// selection.
    ///
    /// ```java
    /// form.chooseAll(locate(CheckBoxMultipleChoice.class).withId("extras"), "insurance", "tracking");
    /// ```
    ///
    /// @param field the locator of the multiple-choice field, such as a `CheckBoxMultipleChoice`,
    ///     inside this form
    /// @param choices the choices to select, compared with `equals`
    /// @return this helper
    /// @throws AssertionError also if the field lacks one of the choices; the message lists the
    ///     choices it has
    public FormHelper chooseAll(@SuppressWarnings("rawtypes") Locator<? extends ListMultipleChoice> field,
            Object... choices) {
        requireCurrent();
        Stream.of(notNull(choices, "choices")).forEach(choice -> notNull(choice, "choices"));
        var located = (ListMultipleChoice<?>) locateUsable(notNull(field, "field"));
        var available = located.getChoices();
        var indexes = Stream.of(choices).mapToInt(choice -> indexOf(field, located, available, choice)).toArray();
        delegate(field, located,
            () -> formTester.selectMultiple(formRelativePath(located), indexes, true));
        return this;
    }

    /// Checks a check box, as `FormTester.setValue` does with `true`.
    ///
    /// ```java
    /// form.check(locate(CheckBox.class).withId("giftWrap"));
    /// ```
    ///
    /// @param field the locator of the check box, inside this form
    /// @return this helper
    /// @throws AssertionError also if the check box is not enabled in hierarchy
    public FormHelper check(Locator<? extends CheckBox> field) {
        return setChecked(field, true);
    }

    /// Unchecks a check box, as `FormTester.setValue` does with `false`.
    ///
    /// ```java
    /// form.uncheck(locate(CheckBox.class).withId("giftWrap"));
    /// ```
    ///
    /// @param field the locator of the check box, inside this form
    /// @return this helper
    /// @throws AssertionError also if the check box is not enabled in hierarchy
    public FormHelper uncheck(Locator<? extends CheckBox> field) {
        return setChecked(field, false);
    }

    private FormHelper setChecked(Locator<? extends CheckBox> field, boolean checked) {
        requireCurrent();
        var located = locateUsable(notNull(field, "field"));
        delegate(field, located, () -> formTester.setValue(located, Boolean.toString(checked)));
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
        delegate(component, located, () -> tester.executeAjaxEvent(located, event));
    }

    /// Submits the form with the values set so far, as `FormTester.submit()` does.
    ///
    /// ```java
    /// form.set(locate(TextField.class).withId("customer"), "ACME").submit();
    /// ```
    public void submit() {
        requireCurrent();
        delegate(formLocator, form, formTester::submit);
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
        delegate(button, located, () -> formTester.submit(located));
    }

    /// Returns the `FormTester` behind this helper, with the values set so far, for what the
    /// helper doesn't offer, such as files.
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

    /// Fails if the field has a `FormComponentUpdatingBehavior`, which `FormTester.select` would
    /// run at once.
    private static void requireNoUpdatingBehavior(Locator<?> field, Component located) {
        if (!located.getBehaviors(FormComponentUpdatingBehavior.class).isEmpty()) {
            throw new AssertionError(prefix(field, located) + "the component has a "
                + "FormComponentUpdatingBehavior, which FormTester would run at once, losing the values set so far;"
                + " use formTester() to select it anyway");
        }
    }

    /// The index of the first of the field's choices that equals the choice.
    private static int indexOf(Locator<?> field, AbstractChoice<?, ?> located, List<?> choices, Object choice) {
        for (var index = 0; index < choices.size(); index++) {
            if (choice.equals(choices.get(index))) {
                return index;
            }
        }
        var types = choices.stream().filter(Objects::nonNull).map(each -> className(each.getClass())).distinct()
            .toList();
        var type = className(choice.getClass());
        var choicesAre = choices.isEmpty() || types.contains(type) ? "; its choices are "
            : " of type " + type + "; its choices, of type " + String.join(", ", types) + ", are ";
        throw new AssertionError(prefix(field, located) + "the component has no choice " + WicketTestkit.shown(choice)
            + choicesAre + listed(choices));
    }

    /// The choices separated by commas, at most `MAX_CHOICES` of them, or `none`.
    private static String listed(List<?> choices) {
        if (choices.isEmpty()) {
            return "none";
        }
        var shown = choices.stream().limit(MAX_CHOICES).map(WicketTestkit::shown).collect(joining(", "));
        return choices.size() > MAX_CHOICES ? shown + ", and " + (choices.size() - MAX_CHOICES) + " more" : shown;
    }

    /// The component's path relative to the form, as `FormTester` takes it.
    private String formRelativePath(Component component) {
        return component.getPageRelativePath().substring(form.getPageRelativePath().length() + 1);
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
