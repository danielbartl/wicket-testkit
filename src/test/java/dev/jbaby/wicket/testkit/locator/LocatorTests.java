package dev.jbaby.wicket.testkit.locator;

import static dev.jbaby.wicket.testkit.locator.Locator.locate;
import static org.assertj.core.api.Assertions.assertThat;

import dev.jbaby.wicket.testkit.fixtures.Headline;
import dev.jbaby.wicket.testkit.fixtures.NoOpLink;
import dev.jbaby.wicket.testkit.fixtures.Order;
import dev.jbaby.wicket.testkit.fixtures.ShopPage;
import dev.jbaby.wicket.testkit.fixtures.UserPanel;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/// Each test starts with the id of the requirement it proves.
/// The requirements live in this package's `package-info.java`.
/// Most tests search a rendered `ShopPage`, whose Javadoc shows its component tree.
class LocatorTests {

    WicketTester tester;
    Page page;

    @BeforeEach
    void renderShopPage() {
        tester = new WicketTester();
        tester.startPage(ShopPage.class);
        page = tester.getLastRenderedPage();
    }

    @AfterEach
    void destroyTester() {
        tester.destroy();
    }

    static List<String> paths(List<? extends Component> components) {
        return components.stream().map(Component::getPageRelativePath).toList();
    }

    @Nested
    @DisplayName("R1 locate by type")
    class LocateByType {

        @Test
        @DisplayName("R1.1 a locator matches every component of its type, subtypes included")
        void matchesEveryComponentOfItsType() {
            assertThat(paths(locate(Label.class).all(page)))
                .contains("title", "headline", "note", "closed:reason", "user:name",
                    "orders:0:customer", "table:1:customer")
                .doesNotContain("refresh", "user", "orders", "orders:0");
            assertThat(paths(locate(Headline.class).all(page))).containsExactly("headline");
        }

        @Test
        @DisplayName("R1.2 the root is not a candidate, only its descendants are")
        void rootIsNotACandidate() {
            var user = (MarkupContainer) page.get("user");

            assertThat(locate(UserPanel.class).all(user)).isEmpty();
            assertThat(paths(locate(Component.class).all(user))).containsExactly("user:name");
            assertThat(locate(Page.class).all(page)).isEmpty();
        }
    }

    @Nested
    @DisplayName("R2 refine by the component itself")
    class RefineByTheComponentItself {

        @Test
        @DisplayName("R2.1 withId matches only components with that wicket:id")
        void withIdMatchesTheWicketId() {
            assertThat(paths(locate(Label.class).withId("customer").all(page))).containsExactly(
                "orders:0:customer", "orders:1:customer", "orders:2:customer",
                "table:1:customer", "table:2:customer", "table:3:customer");
        }

        @Test
        @DisplayName("R2.2 withModelObject matches only components whose model object equals the argument")
        void withModelObjectMatchesEqualModelObjects() {
            assertThat(paths(locate(Label.class).withModelObject("ACME").all(page))).containsExactly(
                "user:name", "orders:0:customer", "orders:2:customer",
                "table:1:customer", "table:3:customer");
            assertThat(paths(locate(Label.class).withModelObject(42).all(page)))
                .containsExactly("orders:1:number");
        }

        @Test
        @DisplayName("R2.3 visible matches only components that are visible in hierarchy")
        void visibleMatchesOnlyComponentsVisibleInHierarchy() {
            assertThat(paths(locate(Label.class).visible().all(page)))
                .contains("title", "locked:hint", "orders:0:customer")
                .doesNotContain("note", "closed:reason");
        }

        @Test
        @DisplayName("R2.4 without visible, visible and invisible components match alike")
        void withoutVisibleInvisibleComponentsMatchToo() {
            assertThat(paths(locate(Label.class).all(page)))
                .contains("title", "note", "closed:reason");
        }

        @Test
        @DisplayName("R2.5 enabled matches only components that are enabled in hierarchy")
        void enabledMatchesOnlyComponentsEnabledInHierarchy() {
            assertThat(paths(locate(NoOpLink.class).enabled().all(page))).containsExactly("refresh");
            assertThat(paths(locate(NoOpLink.class).all(page)))
                .containsExactly("locked:unlock", "refresh", "archive");
        }

        @Test
        @DisplayName("R2.6 matching matches only components that the predicate accepts")
        void matchingMatchesWhatThePredicateAccepts() {
            var startsWithH = locate(Label.class)
                .matching(label -> label.getDefaultModelObjectAsString().startsWith("H"), "starts with H");

            assertThat(paths(startsWithH.all(page))).containsExactly("closed:reason");
        }
    }

    @Nested
    @DisplayName("R3 refine by position")
    class RefineByPosition {

        @Test
        @DisplayName("R3.1 inside matches only components with an ancestor that the other locator matches")
        void insideMatchesComponentsWithAMatchingAncestor() {
            var user = (MarkupContainer) page.get("user");
            var closed = (MarkupContainer) page.get("closed");

            assertThat(paths(locate(Label.class).inside(locate(UserPanel.class)).all(page)))
                .containsExactly("user:name");
            assertThat(paths(locate(Label.class).inside(locate(WebMarkupContainer.class).withId("locked")).all(page)))
                .containsExactly("locked:hint");
            assertThat(locate(UserPanel.class).inside(locate(UserPanel.class)).all(page))
                .as("the component itself is not its own ancestor")
                .isEmpty();
            assertThat(paths(locate(Label.class).inside(locate(UserPanel.class)).all(user)))
                .as("ancestor at the root")
                .containsExactly("user:name");
            assertThat(paths(locate(Label.class).inside(locate(ShopPage.class)).all(closed)))
                .as("ancestor above the root")
                .containsExactly("closed:reason");
        }

        @Test
        @DisplayName("R3.2 inRowOf matches only components inside a ListView row whose model object equals the argument")
        void inRowOfMatchesListViewRows() {
            assertThat(paths(locate(Label.class).inRowOf(Order.GLOBEX_42).all(page)))
                .filteredOn(path -> path.startsWith("orders:"))
                .containsExactly("orders:1:customer", "orders:1:number");
        }

        @Test
        @DisplayName("R3.3 inRowOf matches only components inside a RefreshingView row whose model object equals the argument")
        void inRowOfMatchesRefreshingViewRows() {
            assertThat(paths(locate(Label.class).inRowOf(Order.GLOBEX_42).all(page)))
                .filteredOn(path -> path.startsWith("table:"))
                .containsExactly("table:2:customer");
        }
    }
}
