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
import dev.jbaby.wicket.testkit.fixtures.OrderFormPage;
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
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
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

        @Test
        @DisplayName("R2.9 withLabel matches only components whose label equals the text")
        void withLabelMatchesTheLabel() {
            tester.startPage(OrderFormPage.class);
            var formPage = tester.getLastRenderedPage();

            assertThat(paths(locate(TextField.class).withLabel("Customer").all(formPage)))
                .containsExactly("order:customer");
            assertThat(locate(DropDownChoice.class).withLabel("Country").all(formPage))
                .as("a plain <label wicket:for> sets no label")
                .isEmpty();
            assertThat(locate(Label.class).withLabel("Shop").all(page))
                .as("a Label has no label")
                .isEmpty();
        }

        @Test
        @DisplayName("R2.10 labels are compared without whitespace at either end and with every run of whitespace as one space")
        void labelsAreComparedWithWhitespaceNormalized() {
            tester.startPage(OrderFormPage.class);
            var formPage = tester.getLastRenderedPage();

            assertThat(paths(locate(TextField.class).withLabel("Search term").all(formPage)))
                .as("<wicket:label> spread over lines")
                .containsExactly("search:query");
            assertThat(paths(locate(TextField.class).withLabel(" Search\t term\n").all(formPage)))
                .containsExactly("search:query");
            assertThat(locate(TextField.class).withLabel("Searchterm").all(formPage)).isEmpty();
        }

        @Test
        @DisplayName("R2.13 a label is read as text, without markup tags and with character entities decoded")
        void labelsAreReadAsText() {
            tester.startPage(OrderFormPage.class);
            var formPage = tester.getLastRenderedPage();

            assertThat(paths(locate(CheckBox.class).withLabel("Terms & conditions").all(formPage)))
                .as("<wicket:label>Terms &amp; <b>conditions</b></wicket:label>")
                .containsExactly("search:terms");
        }

        @Test
        @DisplayName("R2.11 a component whose label cannot be read does not match withLabel")
        void unreadableLabelsDoNotMatch() {
            var root = new WebMarkupContainer("root");
            root.add(new TextField<String>("broken").setLabel(CombineRefinements.failingModel()));
            root.add(new TextField<String>("customer").setLabel(Model.of("Customer")));

            assertThat(paths(locate(TextField.class).withLabel("Customer").all(root))).containsExactly("customer");
        }

        @Test
        @DisplayName("R2.12 withModelObject with a type and a predicate matches only components whose model object passes")
        void withModelObjectAndAPredicateMatchesModelObjectsThatPass() {
            assertThat(paths(locate(Component.class)
                .withModelObject(Order.class, order -> order.id() == 42, "has id 42").all(page)))
                .as("the rows themselves")
                .containsExactly("orders:1", "table:2");
            assertThat(paths(locate(Label.class)
                .withModelObject(Integer.class, number -> number > 41, "is above 41").all(page)))
                .containsExactly("orders:1:number", "orders:2:number");
        }
    }

    @Nested
    @DisplayName("R2 refine by the component itself, null")
    class RefineByANullModelObject {

        @Test
        @DisplayName("R2.8 withModelObject(null) matches only components whose model object is null")
        void withNullModelObjectMatchesNullModelObjects() {
            assertThat(paths(locate(WebMarkupContainer.class).withId("closed").withModelObject(null).all(page)))
                .containsExactly("closed");
            assertThat(locate(Label.class).withModelObject(null).all(page)).isEmpty();
            assertThat(locate(Label.class).withModelObject(null)).hasToString("a Label with model object null");
        }
    }

    /// A value whose `toString` throws, as an entity's may outside a database session.
    record Exploding() implements Serializable {

        @Override
        public String toString() {
            throw new IllegalStateException("no session");
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

            var root = new WebMarkupContainer("root", new CompoundPropertyModel<>(new Customer("ACME")));
            var view = new RepeatingView("view");
            view.add(new WebMarkupContainer("name").add(new Label("label", "text")));
            root.add(view);
            assertThat(locate(Label.class).inRowOf("ACME").all(root))
                .as("a RepeatingView child is no row, though a CompoundPropertyModel would give it the model object")
                .isEmpty();
        }

        @Test
        @DisplayName("R3.3 inRowOf matches only components inside a RefreshingView row whose model object equals the argument")
        void inRowOfMatchesRefreshingViewRows() {
            assertThat(paths(locate(Label.class).inRowOf(Order.GLOBEX_42).all(page)))
                .filteredOn(path -> path.startsWith("table:"))
                .containsExactly("table:2:customer");
        }

        @Test
        @DisplayName("R3.4 inRowOf with a type and a predicate matches only components in a row whose element passes")
        void inRowOfWithAPredicateMatchesRowsWhoseElementPasses() {
            assertThat(paths(locate(Label.class).withId("customer")
                .inRowOf(Order.class, order -> order.id() == 42, "has id 42").all(page)))
                .containsExactly("orders:1:customer", "table:2:customer");
            assertThat(locate(Label.class).inRowOf(String.class, element -> true, "is anything").all(page))
                .isEmpty();
        }

        @Test
        @DisplayName("R3.6 a row whose model object cannot be read does not match inRowOf")
        void unreadableRowsDoNotMatch() {
            var root = new WebMarkupContainer("root");
            root.add(new ListItem<>(0, CombineRefinements.failingModel()).add(new Label("customer", "ACME")));
            root.add(new ListItem<>(1, Model.of(Order.ACME_41)).add(new Label("customer", "ACME")));

            assertThat(paths(locate(Label.class).inRowOf(Order.ACME_41).all(root))).containsExactly("1:customer");
            assertThat(paths(locate(Label.class).inRowOf(Order.class, order -> true, "is any order").all(root)))
                .containsExactly("1:customer");
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
            assertThat(title.named("the title")).isNotSameAs(title);
            assertThat(title).hasToString("a Label with id \"title\"");
        }

        @Test
        @DisplayName("R4.3 a null argument throws a NullPointerException that names the parameter")
        void nullArgumentsAreRejected() {
            var labels = locate(Label.class);

            assertAll(
                () -> assertThatNullPointerException().isThrownBy(() -> locate(null)).withMessageContaining("type"),
                () -> assertThatNullPointerException().isThrownBy(() -> labels.withId(null)).withMessageContaining("id"),
                () -> assertThatNullPointerException().isThrownBy(() -> labels.withLabel(null))
                    .withMessageContaining("text"),
                () -> assertThatNullPointerException().isThrownBy(() -> labels.named(null))
                    .withMessageContaining("name"),
                () -> assertThatNullPointerException()
                    .isThrownBy(() -> labels.withModelObject(null, object -> true, "is anything"))
                    .withMessageContaining("modelObjectType"),
                () -> assertThatNullPointerException()
                    .isThrownBy(() -> labels.withModelObject(Order.class, null, "is anything"))
                    .withMessageContaining("predicate"),
                () -> assertThatNullPointerException()
                    .isThrownBy(() -> labels.withModelObject(Order.class, order -> true, null))
                    .withMessageContaining("description"),
                () -> assertThatNullPointerException().isThrownBy(() -> labels.inside(null))
                    .withMessageContaining("ancestor"),
                () -> assertThatNullPointerException().isThrownBy(() -> labels.inRowOf(null))
                    .withMessageContaining("element"),
                () -> assertThatNullPointerException()
                    .isThrownBy(() -> labels.inRowOf(null, element -> true, "is anything"))
                    .withMessageContaining("elementType"),
                () -> assertThatNullPointerException()
                    .isThrownBy(() -> labels.inRowOf(Order.class, null, "is anything"))
                    .withMessageContaining("predicate"),
                () -> assertThatNullPointerException()
                    .isThrownBy(() -> labels.inRowOf(Order.class, order -> true, null))
                    .withMessageContaining("description"),
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
            assertThat(paths(locate(Label.class).withModelObject(String.class, "ACME"::equals, "is ACME")
                .withId("customer").all(root)))
                .containsExactly("customer");
            locate(Label.class).withId("customer").matching(seen::add, "is recorded").all(root);
            locate(Label.class).matching(seenWhenGivenFirst::add, "is recorded").withId("customer").all(root);
            assertThat(paths(seen)).containsExactly("customer");
            assertThat(paths(seenWhenGivenFirst)).containsExactly("customer");

            var seenAfterAMiss = new ArrayList<Component>();
            assertThatThrownBy(() -> locate(Label.class).withId("customer").withModelObject("Globex")
                .matching(seenAfterAMiss::add, "is recorded").one(root))
                .isInstanceOf(AssertionError.class);
            assertThat(seenAfterAMiss)
                .as("not after the model object missed, also not for the failure message")
                .isEmpty();

            var labelsRead = new ArrayList<String>();
            IModel<String> recordedLabel = () -> {
                labelsRead.add("read");
                return "Customer";
            };
            root.add(new TextField<String>("field").setLabel(recordedLabel));
            assertThatThrownBy(() -> locate(Label.class).withLabel("Customer").one(root))
                .isInstanceOf(AssertionError.class);
            assertThat(labelsRead).as("the label of a component of another type").isEmpty();
        }

        static IModel<String> failingModel() {
            return () -> {
                throw new IllegalStateException("this model must not be read");
            };
        }

        @Test
        @DisplayName("R4.5 matching, inRowOf or withModelObject with a blank description throws an IllegalArgumentException")
        void blankDescriptionsAreRejected() {
            var labels = locate(Label.class);

            assertThatIllegalArgumentException().isThrownBy(() -> labels.matching(label -> true, ""));
            assertThatIllegalArgumentException().isThrownBy(() -> labels.matching(label -> true, "  "));
            assertThatIllegalArgumentException().isThrownBy(() -> labels.inRowOf(Order.class, order -> true, " "));
            assertThatIllegalArgumentException()
                .isThrownBy(() -> labels.withModelObject(Order.class, order -> true, " "));
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
            assertThat(locate(Label.class).inRowOf(Order.class, order -> order.id() == 42, "has id 42"))
                .hasToString("a Label in the row of an Order that has id 42");
            assertThat(locate(TextField.class)
                .withModelObject(Order.class, order -> order.id() == 42, "has id 42")
                .withLabel("Customer")
                .withId("customer"))
                .hasToString("a TextField with id \"customer\" with label \"Customer\""
                    + " whose model object is an Order that has id 42");
        }

        @Test
        @DisplayName("R6.3 strings appear in double quotes, other values as String.valueOf shows them")
        void quotesStringsOnly() {
            assertThat(locate(Label.class).withModelObject("42")).hasToString("a Label with model object \"42\"");
            assertThat(locate(Label.class).withModelObject(42)).hasToString("a Label with model object 42");
            assertThat(locate(Label.class).inRowOf("ACME")).hasToString("a Label in the row of \"ACME\"");
        }

        @Test
        @DisplayName("R6.5 a value whose toString throws appears as a or an and its class name, whose toString throws")
        void valuesWhoseToStringThrowsAreNamed() {
            var root = new WebMarkupContainer("root");
            root.add(new Label("exploding", Model.of(new Exploding())));

            assertThat(locate(Label.class).withModelObject(new Exploding()))
                .hasToString("a Label with model object an Exploding whose toString throws");
            assertThat(locate(Label.class).inRowOf(new Exploding()))
                .hasToString("a Label in the row of an Exploding whose toString throws");
            assertThat(FailureMessageTests.messageOf(locate(Label.class).withId("exploding").withModelObject("ACME"), root))
                .endsWith("\n  - exploding (has model object an Exploding whose toString throws)");
        }

        @Test
        @DisplayName("R6.4 toString returns the description")
        void toStringReturnsTheDescription() {
            assertThat(locate(Label.class).withId("title").toString()).isEqualTo("a Label with id \"title\"");
        }
    }

    @Nested
    @DisplayName("R8 name")
    class Name {

        @Test
        @DisplayName("R8.1 a named locator describes itself by its name, then its description")
        void describesItselfByItsNameFirst() {
            var refresh = locate(NoOpLink.class).withId("refresh").named("the Refresh link");

            assertThat(refresh).hasToString("the Refresh link, a NoOpLink with id \"refresh\"");
            assertThat(FailureMessageTests.messageOf(locate(Label.class).withId("missing").named("the missing label"),
                page))
                .startsWith("Expected exactly one match for the missing label, a Label with id \"missing\" in ShopPage,"
                    + " but found none.");
        }

        @Test
        @DisplayName("R8.2 inside a named locator, the description and what differs show only its name")
        void insideANamedLocatorShowsOnlyItsName() {
            var title = locate(Label.class).withId("title").inside(locate(UserPanel.class).named("the user panel"));

            assertThat(title).hasToString("a Label with id \"title\" inside the user panel");
            assertThat(FailureMessageTests.messageOf(title, page))
                .endsWith("\n  - title (is not inside the user panel)\n  - user:name (has id \"name\")");
        }

        @Test
        @DisplayName("R8.3 refining a named locator returns a locator without the name")
        void refiningDropsTheName() {
            var refresh = locate(NoOpLink.class).withId("refresh").named("the Refresh link");

            assertThat(refresh.enabled()).hasToString("an enabled NoOpLink with id \"refresh\"");
        }

        @Test
        @DisplayName("R8.4 a named locator matches the same components as without the name")
        void namingChangesNoMatch() {
            var customers = locate(Label.class).withId("customer");

            assertThat(customers.named("the customers").all(page)).containsExactlyElementsOf(customers.all(page));
            assertThat(customers.named("the customers").existsIn(page)).isTrue();
        }

        @Test
        @DisplayName("R8.5 named with a blank name throws an IllegalArgumentException")
        void blankNamesAreRejected() {
            var labels = locate(Label.class);

            assertThatIllegalArgumentException().isThrownBy(() -> labels.named(""));
            assertThatIllegalArgumentException().isThrownBy(() -> labels.named("  "));
        }
    }
}
