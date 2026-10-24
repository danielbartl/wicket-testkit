package dev.jbaby.wicket.testkit;

import dev.jbaby.wicket.testkit.locator.Locator;
import java.io.Serializable;
import java.util.Objects;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.tester.WicketTesterHelper;

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
/// component was found, its path.
public final class WicketTestkit {

    private final WicketTester tester;

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
    /// kit.ajax(locate(DropDownChoice.class).withId("country"), "change");
    /// ```
    ///
    /// @param component the locator of the component
    /// @param event the event, such as `click` or `change`
    /// @throws AssertionError also if the component has no Ajax behavior for the event
    public void ajax(Locator<?> component, String event) {
        notNull(event, "event");
        var located = locateVisible(notNull(component, "component"));
        if (WicketTesterHelper.findAjaxEventBehaviors(located, event).isEmpty()) {
            throw new AssertionError(prefix(component, located)
                + "the component has no Ajax behavior for the event \"" + event + "\"");
        }
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
        notNull(form, "form");
        throw new UnsupportedOperationException("not implemented yet");
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
        notNull(component, "component");
        throw new UnsupportedOperationException("not implemented yet");
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
        notNull(component, "component");
        throw new UnsupportedOperationException("not implemented yet");
    }

    /// Asserts that a component is enabled in hierarchy, as `WicketTester.assertEnabled` does.
    ///
    /// ```java
    /// kit.assertEnabled(locate(Button.class).withId("save"));
    /// ```
    ///
    /// @param component the locator of the component
    public void assertEnabled(Locator<?> component) {
        notNull(component, "component");
        throw new UnsupportedOperationException("not implemented yet");
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
        notNull(component, "component");
        throw new UnsupportedOperationException("not implemented yet");
    }

    /// Asserts a component's model object, as `WicketTester.assertModelValue` does.
    ///
    /// ```java
    /// kit.assertModelObject(locate(TextField.class).withId("customer"), "ACME");
    /// ```
    ///
    /// @param component the locator of the component
    /// @param expected the model object it should have, compared with `equals`
    public void assertModelObject(Locator<?> component, Object expected) {
        notNull(component, "component");
        notNull(expected, "expected");
        throw new UnsupportedOperationException("not implemented yet");
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
        notNull(panel, "panel");
        notNull(expectedMessages, "expectedMessages");
        throw new UnsupportedOperationException("not implemented yet");
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
        notNull(component, "component");
        throw new UnsupportedOperationException("not implemented yet");
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
        page();
        throw new UnsupportedOperationException("not implemented yet");
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
        var component = locate(locator);
        if (!component.isVisibleInHierarchy()) {
            throw new AssertionError(prefix(locator, component) + "the component is not visible in hierarchy");
        }
        return component;
    }

    /// Runs a WicketTester check for the component and puts the prefix in front of its failure.
    private static void check(Locator<?> locator, Component component, Runnable check) {
        try {
            check.run();
        } catch (AssertionError failure) {
            throw new AssertionError(prefix(locator, component) + failure.getMessage(), failure);
        }
    }

    /// The locator's description and the component's path, to start a failure message with.
    static String prefix(Locator<?> locator, Component component) {
        return locator + " (at " + component.getPageRelativePath() + "): ";
    }

    static <V> V notNull(V value, String parameter) {
        return Objects.requireNonNull(value, () -> parameter + " must not be null");
    }
}
