package dev.jbaby.wicket.testkit;

import static dev.jbaby.wicket.testkit.locator.Locator.locate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import dev.jbaby.wicket.testkit.fixtures.CounterPanel;
import dev.jbaby.wicket.testkit.fixtures.Order;
import dev.jbaby.wicket.testkit.fixtures.OrderPage;
import dev.jbaby.wicket.testkit.fixtures.OrdersPage;
import dev.jbaby.wicket.testkit.locator.Locator;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.util.tester.WicketTester;
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
                () -> assertThatNullPointerException().isThrownBy(() -> kit.assertModelObject(label, null))
                    .withMessageContaining("expected"),
                () -> assertThatNullPointerException().isThrownBy(() -> kit.assertFeedback(null, "Shipped"))
                    .withMessageContaining("panel"),
                () -> assertThatNullPointerException()
                    .isThrownBy(() -> kit.assertFeedback(feedback, (java.io.Serializable[]) null))
                    .withMessageContaining("expectedMessages"),
                () -> assertThatNullPointerException().isThrownBy(() -> kit.assertOnAjaxResponse(null))
                    .withMessageContaining("component"),
                () -> assertThatNullPointerException().isThrownBy(() -> kit.assertCount(null, 1))
                    .withMessageContaining("locator"),
                () -> assertThatNullPointerException().isThrownBy(() -> kit.pathOf(null))
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
    @DisplayName("R8 path for other WicketTester methods")
    class PathForOtherWicketTesterMethods {

        @Test
        @DisplayName("R8.1 pathOf returns the page-relative path, which the tester's own methods accept")
        void pathOfReturnsThePageRelativePath() {
            var customer = locate(Label.class).withId("customer").inRowOf(Order.GLOBEX_42);

            assertThat(kit.pathOf(customer)).isEqualTo("orders:1:customer");
            tester.assertLabel(kit.pathOf(customer), "Globex");
        }
    }
}
