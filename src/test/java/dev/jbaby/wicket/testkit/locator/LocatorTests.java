package dev.jbaby.wicket.testkit.locator;

import static dev.jbaby.wicket.testkit.locator.Locator.locate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import dev.jbaby.wicket.testkit.fixtures.Headline;
import dev.jbaby.wicket.testkit.fixtures.NoOpLink;
import dev.jbaby.wicket.testkit.fixtures.Order;
import dev.jbaby.wicket.testkit.fixtures.ShopPage;
import dev.jbaby.wicket.testkit.fixtures.UserPanel;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
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

        @Test
        @DisplayName("R2.7 a component whose model object cannot be read does not match withModelObject")
        void unreadableModelObjectsDoNotMatch() {
            var root = new WebMarkupContainer("root", new CompoundPropertyModel<>(new Customer("ACME")));
            root.add(new Label("name"));
            root.add(new WebMarkupContainer("box"));

            assertThat(paths(locate(Component.class).withModelObject("ACME").all(root))).containsExactly("name");
        }
    }

    /// A bean for a `CompoundPropertyModel`: it has a `name` property and nothing else.
    public static class Customer implements Serializable {

        private final String name;

        Customer(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
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

    @Nested
    @DisplayName("R4 combine refinements")
    class CombineRefinements {

        @Test
        @DisplayName("R4.1 a locator matches only components that meet all its criteria")
        void allCriteriaHaveToBeMet() {
            assertThat(paths(locate(Label.class).withId("customer").withModelObject("ACME").all(page)))
                .containsExactly("orders:0:customer", "orders:2:customer", "table:1:customer", "table:3:customer");
            assertThat(locate(Label.class).withId("customer").withId("number").all(page)).isEmpty();
        }

        @Test
        @DisplayName("R4.2 refining returns a new locator and leaves the original unchanged")
        void refiningLeavesTheOriginalUnchanged() {
            var labels = locate(Label.class);
            var title = labels.withId("title");

            assertThat(title).isNotSameAs(labels);
            assertThat(paths(title.all(page))).containsExactly("title");
            assertThat(paths(labels.all(page))).contains("title", "headline", "user:name");
        }

        @Test
        @DisplayName("R4.3 a null argument throws a NullPointerException that names the parameter")
        void nullArgumentsAreRejected() {
            var labels = locate(Label.class);

            assertAll(
                () -> assertThatNullPointerException().isThrownBy(() -> locate(null)).withMessageContaining("type"),
                () -> assertThatNullPointerException().isThrownBy(() -> labels.withId(null)).withMessageContaining("id"),
                () -> assertThatNullPointerException().isThrownBy(() -> labels.withModelObject(null))
                    .withMessageContaining("modelObject"),
                () -> assertThatNullPointerException().isThrownBy(() -> labels.inside(null))
                    .withMessageContaining("ancestor"),
                () -> assertThatNullPointerException().isThrownBy(() -> labels.inRowOf(null))
                    .withMessageContaining("element"),
                () -> assertThatNullPointerException().isThrownBy(() -> labels.matching(null, "is bold"))
                    .withMessageContaining("predicate"),
                () -> assertThatNullPointerException().isThrownBy(() -> labels.matching(label -> true, null))
                    .withMessageContaining("description"),
                () -> assertThatNullPointerException().isThrownBy(() -> labels.one(null)).withMessageContaining("root"),
                () -> assertThatNullPointerException().isThrownBy(() -> labels.all(null)).withMessageContaining("root"),
                () -> assertThatNullPointerException().isThrownBy(() -> labels.existsIn(null))
                    .withMessageContaining("root"));
        }

        @Test
        @DisplayName("R4.4 model objects are read and predicates applied only once the type and all other criteria are met")
        void modelsAndPredicatesComeLast() {
            var root = new WebMarkupContainer("root");
            root.add(new WebMarkupContainer("broken", failingModel()));
            root.add(new Label("brokenLabel", failingModel()));
            root.add(new Label("customer", "ACME"));
            var seen = new ArrayList<Component>();
            var seenWhenGivenFirst = new ArrayList<Component>();

            assertThat(paths(locate(Label.class).withId("customer").withModelObject("ACME").all(root)))
                .containsExactly("customer");
            assertThat(paths(locate(Label.class).withModelObject("ACME").withId("customer").all(root)))
                .containsExactly("customer");
            locate(Label.class).withId("customer").matching(seen::add, "is recorded").all(root);
            locate(Label.class).matching(seenWhenGivenFirst::add, "is recorded").withId("customer").all(root);
            assertThat(paths(seen)).containsExactly("customer");
            assertThat(paths(seenWhenGivenFirst)).containsExactly("customer");
        }

        static IModel<String> failingModel() {
            return () -> {
                throw new IllegalStateException("this model must not be read");
            };
        }

        @Test
        @DisplayName("R4.5 matching with a blank description throws an IllegalArgumentException")
        void blankDescriptionsAreRejected() {
            var labels = locate(Label.class);

            assertThatIllegalArgumentException().isThrownBy(() -> labels.matching(label -> true, ""));
            assertThatIllegalArgumentException().isThrownBy(() -> labels.matching(label -> true, "  "));
        }
    }

    @Nested
    @DisplayName("R5 resolve")
    class Resolve {

        @Test
        @DisplayName("R5.1 one returns the component when exactly one matches")
        void oneReturnsTheOnlyMatch() {
            Label title = locate(Label.class).withId("title").one(page);

            assertThat(title).isSameAs(page.get("title"));
        }

        @Test
        @DisplayName("R5.2 one throws an AssertionError when nothing matches")
        void oneFailsWithoutAMatch() {
            assertThatThrownBy(() -> locate(Label.class).withId("missing").one(page))
                .isExactlyInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("R5.3 one throws an AssertionError when several components match")
        void oneFailsWithSeveralMatches() {
            assertThatThrownBy(() -> locate(Label.class).withId("customer").one(page))
                .isExactlyInstanceOf(AssertionError.class);
        }

        @Test
        @DisplayName("R5.4 all returns every match in tree order, or an empty list")
        void allReturnsEveryMatchInTreeOrder() {
            assertThat(paths(locate(Component.class).all(page))).startsWith(
                "title", "headline", "note", "closed", "closed:reason",
                "locked", "locked:hint", "locked:unlock", "refresh");
            assertThat(locate(Label.class).withId("missing").all(page)).isEmpty();
        }

        @Test
        @DisplayName("R5.5 existsIn tells whether at least one component matches")
        void existsInTellsWhetherAnythingMatches() {
            assertThat(locate(Label.class).withId("customer").existsIn(page)).isTrue();
            assertThat(locate(Label.class).withId("title").existsIn(page)).isTrue();
            assertThat(locate(Label.class).withId("missing").existsIn(page)).isFalse();
        }
    }

    @Nested
    @DisplayName("R6 describe")
    class Describe {

        @Test
        @DisplayName("R6.1 a locator describes itself with a or an and the class name of its type")
        void describesTheTypeWithAnArticle() {
            var anonymousLabel = new Label("anonymous") {
            };

            assertThat(locate(Label.class)).hasToString("a Label");
            assertThat(locate(AjaxLink.class)).hasToString("an AjaxLink");
            assertThat(locate(anonymousLabel.getClass())).hasToString("a Label");
        }

        @Test
        @DisplayName("R6.2 refinements appear in the table's order, whatever order they were given in")
        void describesRefinementsInTableOrder() {
            var everything = locate(Label.class)
                .inRowOf(Order.GLOBEX_42)
                .inside(locate(UserPanel.class).withId("user"))
                .matching(label -> true, "is bold")
                .withModelObject("ACME")
                .withId("customer")
                .enabled()
                .visible();

            assertThat(everything).hasToString("a visible, enabled Label with id \"customer\""
                + " with model object \"ACME\" that is bold inside an UserPanel with id \"user\""
                + " in the row of Order[id=42, customer=Globex]");
            assertThat(locate(Label.class).visible()).hasToString("a visible Label");
            assertThat(locate(NoOpLink.class).enabled()).hasToString("an enabled NoOpLink");
            assertThat(locate(Label.class).matching(label -> true, "is bold").matching(label -> true, "is short"))
                .hasToString("a Label that is bold and is short");
        }

        @Test
        @DisplayName("R6.3 strings appear in double quotes, other values as String.valueOf shows them")
        void quotesStringsOnly() {
            assertThat(locate(Label.class).withModelObject("42")).hasToString("a Label with model object \"42\"");
            assertThat(locate(Label.class).withModelObject(42)).hasToString("a Label with model object 42");
            assertThat(locate(Label.class).inRowOf("ACME")).hasToString("a Label in the row of \"ACME\"");
        }

        @Test
        @DisplayName("R6.4 toString returns the description")
        void toStringReturnsTheDescription() {
            assertThat(locate(Label.class).withId("title").toString()).isEqualTo("a Label with id \"title\"");
        }
    }
}
