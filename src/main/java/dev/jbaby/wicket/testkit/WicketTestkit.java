package dev.jbaby.wicket.testkit;

import dev.jbaby.wicket.testkit.locator.Locator;
import java.io.Serializable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.tester.WicketTesterHelper;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.ValueWrapper;

/// Acts on and asserts about the components that locators find, through a `WicketTester` the
/// test already has.
///
/// Each method resolves its locator against the last rendered page to exactly one component,
/// then hands that component, or its path, to the matching `WicketTester` or `FormTester`
/// method. The tester stays usable as before.
///
/// ```java
/// WicketTestkit kit = WicketTestkit.of(tester);
/// tester.startPage(OrdersPage.class);
///
/// kit.click(locate(AjaxLink.class).withId("ship").inRowOf(order));
/// kit.assertLabel(locate(Label.class).withId("status").inRowOf(order), "shipped");
/// ```
///
/// Every failure is an `AssertionError` whose message names the locator's criteria and, once a
/// component was found, its path. When a `WicketTester` assertion fails with expected and actual
/// values, they are kept, so IDEs can show their diff.
public final class WicketTestkit {

    /// How many matches a failure message lists, as the locator's own messages do.
    private static final int MAX_MATCHES = 10;

    private final WicketTester tester;

    /// The latest form helper for each form; [FormHelper#isCurrent()] tells whether it still holds.
    private final Map<Form<?>, FormHelper> formHelpers = new IdentityHashMap<>();

    private WicketTestkit(WicketTester tester) {
        this.tester = tester;
    }

    /// Wraps a tester. The testkit always works on the page the tester rendered last.
    ///
    /// ```java
    /// WicketTestkit kit = WicketTestkit.of(new WicketTester(new MyApplication()));
    /// ```
    ///
    /// @param tester the tester to act and assert through
    /// @return a testkit for that tester
    public static WicketTestkit of(WicketTester tester) {
        return new WicketTestkit(notNull(tester, "tester"));
    }

    /// Clicks a link, through Ajax if it is an Ajax link, as `WicketTester.clickLink` does.
    ///
    /// ```java
    /// kit.click(locate(Link.class).withId("details").inRowOf(order));
    /// ```
    ///
    /// @param link the locator of the link
    public void click(Locator<?> link) {
        var component = locateVisible(notNull(link, "link"));
        check(link, component, () -> tester.clickLink(component));
    }

    /// Fires an Ajax event on a component, as `WicketTester.executeAjaxEvent` does.
    ///
    /// ```java
    /// kit.ajax(locate(WebMarkupContainer.class).withId("row").inRowOf(order), "dblclick");
    /// ```
    ///
    /// It sends no form input. For a form field with an `AjaxFormComponentUpdatingBehavior`, use
    /// [FormHelper#ajax(Locator, String)], which sends the field's new input; this method would
    /// leave the field's model empty.
    ///
    /// @param component the locator of the component
    /// @param event the event, such as `click` or `change`
    /// @throws AssertionError also if the component has no Ajax behavior for the event
    public void ajax(Locator<?> component, String event) {
        notNull(event, "event");
        var located = locateVisible(notNull(component, "component"));
        requireAjaxBehavior(component, located, event);
        check(component, located, () -> tester.executeAjaxEvent(located, event));
    }

    /// Starts filling in a form, from its current values, as `WicketTester.newFormTester` does.
    ///
    /// ```java
    /// kit.form(locate(Form.class).withId("order"))
    ///     .set(locate(TextField.class).withId("customer"), "ACME")
    ///     .select(locate(DropDownChoice.class).withId("country"), 2)
    ///     .submit(locate(Button.class).withId("save"));
    /// ```
    ///
    /// @param form the locator of the form
    /// @return a helper that locates fields and buttons inside that form
    public FormHelper form(@SuppressWarnings("rawtypes") Locator<? extends Form> form) {
        var located = (Form<?>) locateVisible(notNull(form, "form"));
        var current = formHelpers.get(located);
        if (current != null && current.isCurrent()) {
            return current;
        }
        var formTester = checked(form, located, () -> tester.newFormTester(located.getPageRelativePath()));
        var helper = new FormHelper(tester, form, located, formTester);
        formHelpers.put(located, helper);
        return helper;
    }

    /// Asserts a label's text, as `WicketTester.assertLabel` does.
    ///
    /// ```java
    /// kit.assertLabel(locate(Label.class).withId("customer").inRowOf(order), "ACME");
    /// ```
    ///
    /// @param label the locator of the label
    /// @param expectedText the text the label should show
    public void assertLabel(Locator<? extends Label> label, String expectedText) {
        notNull(expectedText, "expectedText");
        var component = locateVisible(notNull(label, "label"));
        check(label, component, () -> tester.assertLabel(component.getPageRelativePath(), expectedText));
    }

    /// Asserts that a component is visible in hierarchy, as `WicketTester.assertVisible` does.
    ///
    /// ```java
    /// kit.assertVisible(locate(Link.class).withId("ship").inRowOf(order));
    /// ```
    ///
    /// @param component the locator of the component
    public void assertVisible(Locator<?> component) {
        var located = locate(notNull(component, "component"));
        check(component, located, () -> tester.assertVisible(located.getPageRelativePath()));
    }

    /// Asserts that a component is not visible in hierarchy, as `WicketTester.assertInvisible`
    /// does. The component has to exist; to assert that nothing matches, use
    /// [#assertCount(Locator, int)] with zero.
    ///
    /// ```java
    /// kit.assertInvisible(locate(Link.class).withId("ship").inRowOf(shippedOrder));
    /// ```
    ///
    /// @param component the locator of the component
    public void assertInvisible(Locator<?> component) {
        var located = locate(notNull(component, "component"));
        check(component, located, () -> tester.assertInvisible(located.getPageRelativePath()));
    }

    /// Asserts that a component is enabled in hierarchy, as `WicketTester.assertEnabled` does.
    ///
    /// ```java
    /// kit.assertEnabled(locate(Button.class).withId("save"));
    /// ```
    ///
    /// @param component the locator of the component
    public void assertEnabled(Locator<?> component) {
        var located = locateVisible(notNull(component, "component"));
        check(component, located, () -> tester.assertEnabled(located.getPageRelativePath()));
    }

    /// Asserts that a component is not enabled in hierarchy, as `WicketTester.assertDisabled`
    /// does.
    ///
    /// ```java
    /// kit.assertDisabled(locate(Button.class).withId("save"));
    /// ```
    ///
    /// @param component the locator of the component
    public void assertDisabled(Locator<?> component) {
        var located = locateVisible(notNull(component, "component"));
        check(component, located, () -> tester.assertDisabled(located.getPageRelativePath()));
    }

    /// Asserts a component's model object, as `WicketTester.assertModelValue` does.
    ///
    /// ```java
    /// kit.assertModelObject(locate(TextField.class).withId("customer"), "ACME");
    /// ```
    ///
    /// @param component the locator of the component
    /// @param expected the model object it should have, compared with `equals`; `null` asserts
    ///     that it has none
    public void assertModelObject(Locator<?> component, Object expected) {
        var located = locateVisible(notNull(component, "component"));
        check(component, located, () -> tester.assertModelValue(located.getPageRelativePath(), expected));
    }

    /// Asserts the messages a feedback panel shows, as `WicketTester.assertFeedback` does.
    ///
    /// ```java
    /// kit.assertFeedback(locate(FeedbackPanel.class), "Customer is required.");
    /// ```
    ///
    /// @param panel the locator of the feedback panel
    /// @param expectedMessages every message the panel should show, in any order
    public void assertFeedback(Locator<? extends FeedbackPanel> panel,
            Serializable... expectedMessages) {
        notNull(expectedMessages, "expectedMessages");
        var located = locateVisible(notNull(panel, "panel"));
        check(panel, located, () -> tester.assertFeedback(located.getPageRelativePath(), expectedMessages));
    }

    /// Asserts the feedback messages of one component, such as a form field, as
    /// `WicketTester.assertFeedbackMessages` does with a `ComponentFeedbackMessageFilter` for it.
    ///
    /// ```java
    /// kit.assertFeedbackOn(locate(TextField.class).withId("customer"), "'Customer' is required.");
    /// kit.assertFeedbackOn(locate(TextField.class).withId("country"));
    /// ```
    ///
    /// @param component the locator of the component
    /// @param expectedMessages every message the component should have, in any order; none
    ///     asserts that it has no messages
    public void assertFeedbackOn(Locator<?> component, Serializable... expectedMessages) {
        notNull(expectedMessages, "expectedMessages");
        var located = locate(notNull(component, "component"));
        check(component, located,
            () -> tester.assertFeedbackMessages(new ComponentFeedbackMessageFilter(located), expectedMessages));
    }

    /// Asserts that the last Ajax response rendered a component, as
    /// `WicketTester.assertComponentOnAjaxResponse` does.
    ///
    /// ```java
    /// kit.click(locate(AjaxLink.class).withId("ship").inRowOf(order));
    /// kit.assertOnAjaxResponse(locate(Label.class).withId("status").inRowOf(order));
    /// ```
    ///
    /// @param component the locator of the component
    public void assertOnAjaxResponse(Locator<?> component) {
        var located = locate(notNull(component, "component"));
        check(component, located, () -> tester.assertComponentOnAjaxResponse(located));
    }

    /// Asserts how many components a locator matches on the last rendered page.
    ///
    /// ```java
    /// kit.assertCount(locate(Label.class).withId("customer"), 3);
    /// kit.assertCount(locate(Link.class).withId("ship").visible(), 0);
    /// ```
    ///
    /// @param locator the locator
    /// @param expected how many components it should match, zero included
    public void assertCount(Locator<?> locator, int expected) {
        notNull(locator, "locator");
        var page = page();
        var matches = locator.all(page);
        if (matches.size() == expected) {
            return;
        }
        var message = new StringBuilder("Expected ").append(expected).append(expected == 1 ? " match" : " matches")
            .append(" for ").append(locator).append(" in ").append(className(page.getClass()))
            .append(", but found ");
        if (matches.isEmpty()) {
            message.append("none.");
        } else {
            message.append(matches.size()).append(':');
            matches.stream().limit(MAX_MATCHES)
                .forEach(match -> message.append("\n  - ").append(match.getPageRelativePath()));
            if (matches.size() > MAX_MATCHES) {
                message.append("\n  - and ").append(matches.size() - MAX_MATCHES).append(" more");
            }
        }
        throw new AssertionError(message.toString());
    }

    /// Returns the component a locator finds, for any check this class doesn't wrap.
    ///
    /// ```java
    /// TextField<?> customer = kit.one(locate(TextField.class).withId("customer"));
    /// assertThat(customer.isRequired()).isTrue();
    /// ```
    ///
    /// @param locator the locator
    /// @param <T> the type of the component
    /// @return the component, typed as the locator's type
    public <T extends Component> T one(Locator<T> locator) {
        return notNull(locator, "locator").one(page());
    }

    /// Returns the page-relative path of the component a locator finds, for any `WicketTester`
    /// method this class doesn't wrap.
    ///
    /// ```java
    /// tester.assertComponent(kit.pathOf(locate(Panel.class).withId("details")), DetailsPanel.class);
    /// ```
    ///
    /// @param locator the locator
    /// @return the path, as `WicketTester` takes it
    public String pathOf(Locator<?> locator) {
        return locate(notNull(locator, "locator")).getPageRelativePath();
    }

    /// The page the tester rendered last.
    private Page page() {
        var page = tester.getLastRenderedPage();
        if (page == null) {
            throw new AssertionError("The tester has not rendered a page yet.");
        }
        return page;
    }

    /// The one component the locator matches on the last rendered page.
    private Component locate(Locator<?> locator) {
        return locator.one(page());
    }

    /// The one component the locator matches, which has to be visible in hierarchy, because
    /// WicketTester only looks up visible components by path.
    private Component locateVisible(Locator<?> locator) {
        return requireVisible(locator, locate(locator));
    }

    /// The component, if it is visible in hierarchy.
    static Component requireVisible(Locator<?> locator, Component component) {
        if (!component.isVisibleInHierarchy()) {
            throw new AssertionError(prefix(locator, component) + "the component is not visible in hierarchy");
        }
        return component;
    }

    /// Fails unless the component has an Ajax behavior for the event, where
    /// `WicketTester.executeAjaxEvent` would silently do nothing.
    static void requireAjaxBehavior(Locator<?> locator, Component component, String event) {
        if (WicketTesterHelper.findAjaxEventBehaviors(component, event).isEmpty()) {
            throw new AssertionError(prefix(locator, component)
                + "the component has no Ajax behavior for the event \"" + event + "\"");
        }
    }

    /// Runs a WicketTester check for the component and puts the prefix in front of its failure.
    static void check(Locator<?> locator, Component component, Runnable check) {
        checked(locator, component, () -> {
            check.run();
            return null;
        });
    }

    /// Like [#check(Locator, Component, Runnable)], for a check that returns a result. An
    /// `AssertionFailedError` comes back as one, with its expected and actual values, so IDEs
    /// can show their diff.
    static <R> R checked(Locator<?> locator, Component component, Supplier<R> check) {
        try {
            return check.get();
        } catch (AssertionFailedError failure) {
            var message = prefix(locator, component) + failure.getMessage();
            if (failure.isExpectedDefined() || failure.isActualDefined()) {
                throw new AssertionFailedError(message, value(failure.getExpected()), value(failure.getActual()),
                    failure);
            }
            throw new AssertionFailedError(message, failure);
        } catch (AssertionError failure) {
            throw new AssertionError(prefix(locator, component) + failure.getMessage(), failure);
        }
    }

    private static Object value(ValueWrapper wrapper) {
        return wrapper == null ? null : wrapper.getEphemeralValue();
    }

    /// The locator's description and the component's path, to start a failure message with.
    static String prefix(Locator<?> locator, Component component) {
        return locator + " (at " + component.getPageRelativePath() + "): ";
    }

    /// The simple name of the class, or of its nearest named superclass if it is anonymous, as
    /// the locator's messages name classes.
    private static String className(Class<?> type) {
        var named = type;
        while (named.isAnonymousClass()) {
            named = named.getSuperclass();
        }
        return named.getSimpleName();
    }

    static <V> V notNull(V value, String parameter) {
        return Objects.requireNonNull(value, () -> parameter + " must not be null");
    }
}
