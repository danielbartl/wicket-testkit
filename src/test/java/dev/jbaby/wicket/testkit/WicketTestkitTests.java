package dev.jbaby.wicket.testkit;

import static dev.jbaby.wicket.testkit.locator.Locator.locate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.api.Assertions.assertAll;

import dev.jbaby.wicket.testkit.fixtures.CounterPanel;
import dev.jbaby.wicket.testkit.fixtures.Order;
import dev.jbaby.wicket.testkit.fixtures.OrderPage;
import dev.jbaby.wicket.testkit.fixtures.OrdersPage;
import dev.jbaby.wicket.testkit.locator.Locator;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.util.tester.WicketTester;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.opentest4j.AssertionFailedError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/// Each test starts with the id of the requirement it proves.
/// The requirements live in this package's `package-info.java`.
/// Most tests act on a rendered `OrdersPage`, whose Javadoc shows its component tree.
class WicketTestkitTests {

    WicketTester tester;
    WicketTestkit kit;

    @BeforeEach
    void renderOrdersPage() {
        tester = new WicketTester();
        kit = WicketTestkit.of(tester);
        tester.startPage(OrdersPage.class);
    }

    @AfterEach
    void destroyTester() {
        tester.destroy();
    }

    @Nested
    @DisplayName("R1 wrap a tester")
    class WrapATester {

        @Test
        @DisplayName("R1.1 locators are resolved against the page the tester rendered last")
        void resolvesAgainstTheLastRenderedPage() {
            tester.clickLink("orders:1:details");

            assertThat(kit.pathOf(locate(Label.class).withId("customer"))).isEqualTo("customer");
        }

        @Test
        @DisplayName("R1.2 a component started on its own is located and acted on like a page")
        void worksOnAComponentStartedOnItsOwn() {
            tester.startComponentInPage(new CounterPanel("counter"));
            var count = locate(Label.class).withId("count");

            kit.click(locate(AjaxLink.class).withId("increment"));

            kit.assertLabel(count, "1");
            assertThat(kit.pathOf(count)).isEqualTo("counter:count");
        }

        @Test
        @DisplayName("R1.3 without a rendered page, the testkit throws an AssertionError that says so")
        void failsWithoutARenderedPage() {
            var fresh = new WicketTester();
            try {
                var freshKit = WicketTestkit.of(fresh);

                assertThatThrownBy(() -> freshKit.pathOf(locate(Label.class)))
                    .isExactlyInstanceOf(AssertionError.class)
                    .hasMessage("The tester has not rendered a page yet.");
                assertThatThrownBy(() -> freshKit.assertCount(locate(Label.class), 0))
                    .isExactlyInstanceOf(AssertionError.class)
                    .hasMessage("The tester has not rendered a page yet.");
                assertThatThrownBy(() -> freshKit.one(locate(Label.class)))
                    .isExactlyInstanceOf(AssertionError.class)
                    .hasMessage("The tester has not rendered a page yet.");
            } finally {
                fresh.destroy();
            }
        }

        @Test
        @DisplayName("R1.4 a null argument throws a NullPointerException that names the parameter")
        void nullArgumentsAreRejected() {
            Locator<Label> label = locate(Label.class);
            Locator<FeedbackPanel> feedback = locate(FeedbackPanel.class);

            assertAll(
                () -> assertThatNullPointerException().isThrownBy(() -> WicketTestkit.of(null))
                    .withMessageContaining("tester"),
                () -> assertThatNullPointerException().isThrownBy(() -> kit.click(null)).withMessageContaining("link"),
                () -> assertThatNullPointerException().isThrownBy(() -> kit.ajax(null, "click"))
                    .withMessageContaining("component"),
                () -> assertThatNullPointerException().isThrownBy(() -> kit.ajax(label, null))
                    .withMessageContaining("event"),
                () -> assertThatNullPointerException().isThrownBy(() -> kit.form(null)).withMessageContaining("form"),
                () -> assertThatNullPointerException().isThrownBy(() -> kit.assertLabel(null, "Orders"))
                    .withMessageContaining("label"),
                () -> assertThatNullPointerException().isThrownBy(() -> kit.assertLabel(label, null))
                    .withMessageContaining("expectedText"),
                () -> assertThatNullPointerException().isThrownBy(() -> kit.assertVisible(null))
                    .withMessageContaining("component"),
                () -> assertThatNullPointerException().isThrownBy(() -> kit.assertInvisible(null))
                    .withMessageContaining("component"),
                () -> assertThatNullPointerException().isThrownBy(() -> kit.assertEnabled(null))
                    .withMessageContaining("component"),
                () -> assertThatNullPointerException().isThrownBy(() -> kit.assertDisabled(null))
                    .withMessageContaining("component"),
                () -> assertThatNullPointerException().isThrownBy(() -> kit.assertModelObject(null, "Orders"))
                    .withMessageContaining("component"),
                () -> assertThatNullPointerException().isThrownBy(() -> kit.assertFeedback(null, "Shipped"))
                    .withMessageContaining("panel"),
                () -> assertThatNullPointerException()
                    .isThrownBy(() -> kit.assertFeedback(feedback, (java.io.Serializable[]) null))
                    .withMessageContaining("expectedMessages"),
                () -> assertThatNullPointerException().isThrownBy(() -> kit.assertFeedbackOn(null))
                    .withMessageContaining("component"),
                () -> assertThatNullPointerException()
                    .isThrownBy(() -> kit.assertFeedbackOn(label, (java.io.Serializable[]) null))
                    .withMessageContaining("expectedMessages"),
                () -> assertThatNullPointerException().isThrownBy(() -> kit.assertOnAjaxResponse(null))
                    .withMessageContaining("component"),
                () -> assertThatNullPointerException().isThrownBy(() -> kit.assertCount(null, 1))
                    .withMessageContaining("locator"),
                () -> assertThatNullPointerException().isThrownBy(() -> kit.pathOf(null))
                    .withMessageContaining("locator"),
                () -> assertThatNullPointerException().isThrownBy(() -> kit.one(null))
                    .withMessageContaining("locator"));
        }
    }

    @Nested
    @DisplayName("R2 exactly one")
    class ExactlyOne {

        @Test
        @DisplayName("R2.1 a locator that matches no component or several fails with the locator's AssertionError")
        void failsUnlessExactlyOneMatches() {
            assertThatThrownBy(() -> kit.click(locate(Link.class).withId("missing")))
                .isExactlyInstanceOf(AssertionError.class)
                .hasMessageStartingWith(
                    "Expected exactly one match for a Link with id \"missing\" in OrdersPage, but found none.");
            assertThatThrownBy(() -> kit.assertLabel(locate(Label.class).withId("customer"), "ACME"))
                .isExactlyInstanceOf(AssertionError.class)
                .hasMessageStartingWith(
                    "Expected exactly one match for a Label with id \"customer\" in OrdersPage, but found 3:");
        }
    }

    @Nested
    @DisplayName("R3 act")
    class Act {

        @Test
        @DisplayName("R3.1 click clicks a link as WicketTester.clickLink does")
        void clickClicksALink() {
            kit.click(locate(Link.class).withId("details").inRowOf(Order.GLOBEX_42));

            tester.assertRenderedPage(OrderPage.class);
            tester.assertLabel("customer", "Globex");
        }

        @Test
        @DisplayName("R3.2 click clicks an Ajax link through Ajax as WicketTester.clickLink does")
        void clickClicksAnAjaxLinkThroughAjax() {
            kit.click(locate(AjaxLink.class).withId("ship").inRowOf(Order.GLOBEX_42));

            tester.assertLabel("orders:1:status", "shipped");
            tester.assertComponentOnAjaxResponse("orders:1:status");
        }

        @Test
        @DisplayName("R3.3 ajax fires the event on the component as WicketTester.executeAjaxEvent does")
        void ajaxFiresTheEvent() {
            kit.ajax(locate(WebMarkupContainer.class).withId("bump"), "dblclick");

            tester.assertLabel("bumps", "1");
            tester.assertComponentOnAjaxResponse("bumps");
        }

        @Test
        @DisplayName("R3.4 ajax fails if the component has no Ajax behavior for the event")
        void ajaxFailsWithoutABehaviorForTheEvent() {
            assertThatThrownBy(() -> kit.ajax(locate(WebMarkupContainer.class).withId("bump"), "click"))
                .isExactlyInstanceOf(AssertionError.class)
                .hasMessage("a WebMarkupContainer with id \"bump\" (at bump): "
                    + "the component has no Ajax behavior for the event \"click\"");
        }
    }

    @Nested
    @DisplayName("R5 assert about one component")
    class AssertAboutOneComponent {

        final Locator<Label> globex = locate(Label.class).withId("customer").inRowOf(Order.GLOBEX_42);
        final Locator<Label> title = locate(Label.class).withId("title");
        final Locator<Label> secret = locate(Label.class).withId("secret");
        final Locator<Link> boom = locate(Link.class).withId("boom");
        final Locator<Link> frozen = locate(Link.class).withId("frozen");

        @Test
        @DisplayName("R5.1 assertLabel checks the label's text as WicketTester.assertLabel does")
        void assertLabelChecksTheText() {
            kit.assertLabel(globex, "Globex");

            assertThatThrownBy(() -> kit.assertLabel(globex, "ACME")).isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("R5.2 assertVisible checks that the component is visible in hierarchy")
        void assertVisibleChecksVisibility() {
            kit.assertVisible(title);

            assertThatThrownBy(() -> kit.assertVisible(secret)).isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("R5.3 assertInvisible checks that the component is not visible in hierarchy")
        void assertInvisibleChecksInvisibility() {
            kit.assertInvisible(secret);

            assertThatThrownBy(() -> kit.assertInvisible(title)).isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("R5.4 assertEnabled checks that the component is enabled in hierarchy")
        void assertEnabledChecksEnabledState() {
            kit.assertEnabled(boom);

            assertThatThrownBy(() -> kit.assertEnabled(frozen)).isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("R5.5 assertDisabled checks that the component is not enabled in hierarchy")
        void assertDisabledChecksDisabledState() {
            kit.assertDisabled(frozen);

            assertThatThrownBy(() -> kit.assertDisabled(boom)).isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("R5.6 assertModelObject checks the model object as WicketTester.assertModelValue does")
        void assertModelObjectChecksTheModelObject() {
            kit.assertModelObject(title, "Orders");

            assertThatThrownBy(() -> kit.assertModelObject(title, "Shop")).isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("R5.9 assertModelObject with null checks that the model object is null")
        void assertModelObjectWithNullChecksForNull() {
            kit.assertModelObject(locate(WebMarkupContainer.class).withId("bump"), null);

            assertThatThrownBy(() -> kit.assertModelObject(title, null)).isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("R5.7 assertFeedback checks the feedback panel's messages as WicketTester.assertFeedback does")
        void assertFeedbackChecksTheMessages() {
            var feedback = locate(FeedbackPanel.class).visible();
            kit.click(locate(AjaxLink.class).withId("ship").inRowOf(Order.GLOBEX_42));

            kit.assertFeedback(feedback, "Shipped order 42");

            assertThatThrownBy(() -> kit.assertFeedback(feedback, "Shipped order 41"))
                .isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("R5.8 assertOnAjaxResponse checks that the last Ajax response rendered the component")
        void assertOnAjaxResponseChecksTheResponse() {
            kit.click(locate(AjaxLink.class).withId("ship").inRowOf(Order.GLOBEX_42));

            kit.assertOnAjaxResponse(locate(Label.class).withId("status").inRowOf(Order.GLOBEX_42));

            assertThatThrownBy(() -> kit.assertOnAjaxResponse(
                locate(Label.class).withId("status").inRowOf(Order.ACME_41))).isInstanceOf(AssertionError.class);
        }
    }

    @Nested
    @DisplayName("R6 count")
    class Count {

        @Test
        @DisplayName("R6.1 assertCount passes when exactly that many components match, zero included")
        void assertCountPassesOnTheRightNumber() {
            kit.assertCount(locate(Label.class).withId("customer"), 3);
            kit.assertCount(locate(Label.class).withId("missing"), 0);
            kit.assertCount(locate(Link.class).withId("open").visible(), 0);
        }

        @Test
        @DisplayName("R6.2 assertCount fails on another number, with the expected and found matches")
        void assertCountFailsOnAnotherNumber() {
            assertThatThrownBy(() -> kit.assertCount(locate(Label.class).withId("customer"), 2))
                .isExactlyInstanceOf(AssertionError.class)
                .hasMessage("""
                    Expected 2 matches for a Label with id "customer" in OrdersPage, but found 3:
                      - orders:0:customer
                      - orders:1:customer
                      - orders:2:customer""");
            assertThatThrownBy(() -> kit.assertCount(locate(Label.class).withId("missing"), 1))
                .isExactlyInstanceOf(AssertionError.class)
                .hasMessage("Expected 1 match for a Label with id \"missing\" in OrdersPage, but found none.");
            assertThatThrownBy(() -> kit.assertCount(locate(Component.class), 1))
                .isExactlyInstanceOf(AssertionError.class)
                .satisfies(failure -> assertThat(failure.getMessage().lines())
                    .hasSize(12)
                    .last().asString().matches("  - and \\d+ more"));
        }
    }

    @Nested
    @DisplayName("R7 failure messages")
    class FailureMessages {

        @Test
        @DisplayName("R7.1 a WicketTester failure gets the locator's description and path in front, and stays the cause")
        void wicketTesterFailuresGetThePrefix() {
            var globex = locate(Label.class).withId("customer").inRowOf(Order.GLOBEX_42);
            var frozen = locate(Link.class).withId("frozen");

            assertThatThrownBy(() -> kit.assertLabel(globex, "ACME"))
                .isInstanceOf(AssertionError.class)
                .hasMessage("a Label with id \"customer\" in the row of Order[id=42, customer=Globex]"
                    + " (at orders:1:customer): expected: <ACME> but was: <Globex>")
                .cause().isInstanceOf(AssertionError.class).hasMessage("expected: <ACME> but was: <Globex>");
            assertThatThrownBy(() -> kit.click(frozen))
                .isExactlyInstanceOf(AssertionError.class)
                .hasMessageStartingWith("a Link with id \"frozen\" (at frozen): The component is currently not enabled")
                .cause().isInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("R7.4 an AssertionFailedError from WicketTester comes back as one, with its expected and actual values")
        void assertionFailedErrorsKeepExpectedAndActual() {
            var globex = locate(Label.class).withId("customer").inRowOf(Order.GLOBEX_42);

            var failure = catchThrowableOfType(AssertionFailedError.class, () -> kit.assertLabel(globex, "ACME"));

            assertThat(failure).hasMessageStartingWith("a Label with id \"customer\"");
            assertThat(failure.getExpected().getEphemeralValue()).isEqualTo("ACME");
            assertThat(failure.getActual().getEphemeralValue()).isEqualTo("Globex");
        }

        @Test
        @DisplayName("R7.2 any other exception passes through unchanged")
        void otherExceptionsPassThrough() {
            assertThatThrownBy(() -> kit.click(locate(Link.class).withId("boom")))
                .isNotInstanceOf(AssertionError.class)
                .satisfies(thrown -> assertThat(rootCause(thrown))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("boom"));
        }

        static Throwable rootCause(Throwable thrown) {
            var cause = thrown;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            return cause;
        }

        @Test
        @DisplayName("R7.3 acting on or asserting about an invisible component fails with a message that says so")
        void invisibleComponentsAreReported() {
            var secret = locate(Label.class).withId("secret");
            var open = locate(Link.class).withId("open");
            var vaultFeedback = locate(FeedbackPanel.class).withId("vaultFeedback");

            assertThatThrownBy(() -> kit.assertLabel(secret, "42"))
                .isExactlyInstanceOf(AssertionError.class)
                .hasMessage("a Label with id \"secret\" (at vault:secret): the component is not visible in hierarchy");
            assertAll(
                () -> assertInvisible(() -> kit.click(open)),
                () -> assertInvisible(() -> kit.ajax(open, "click")),
                () -> assertInvisible(() -> kit.assertModelObject(secret, "42")),
                () -> assertInvisible(() -> kit.assertEnabled(open)),
                () -> assertInvisible(() -> kit.assertDisabled(open)),
                () -> assertInvisible(() -> kit.assertFeedback(vaultFeedback)));
        }

        static void assertInvisible(ThrowingCallable call) {
            assertThatThrownBy(call)
                .isExactlyInstanceOf(AssertionError.class)
                .hasMessageEndingWith("): the component is not visible in hierarchy");
        }
    }

    @Nested
    @DisplayName("R8 hand over to code the testkit doesn't wrap")
    class HandOver {

        @Test
        @DisplayName("R8.1 pathOf returns the page-relative path, which the tester's own methods accept")
        void pathOfReturnsThePageRelativePath() {
            var customer = locate(Label.class).withId("customer").inRowOf(Order.GLOBEX_42);

            assertThat(kit.pathOf(customer)).isEqualTo("orders:1:customer");
            tester.assertLabel(kit.pathOf(customer), "Globex");
        }

        @Test
        @DisplayName("R8.2 one returns the located component, typed as the locator's type")
        void oneReturnsTheTypedComponent() {
            Label title = kit.one(locate(Label.class).withId("title"));

            assertThat(title).isSameAs(tester.getComponentFromLastRenderedPage("title"));
        }
    }
}
