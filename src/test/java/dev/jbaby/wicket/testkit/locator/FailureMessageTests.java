package dev.jbaby.wicket.testkit.locator;

import static dev.jbaby.wicket.testkit.locator.Locator.locate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import dev.jbaby.wicket.testkit.fixtures.NoOpLink;
import dev.jbaby.wicket.testkit.fixtures.Order;
import dev.jbaby.wicket.testkit.fixtures.OrderFormPage;
import dev.jbaby.wicket.testkit.fixtures.ShopPage;
import dev.jbaby.wicket.testkit.fixtures.UserPanel;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// Each test starts with the id of the requirement it proves.
/// The requirements live in this package's `package-info.java`.
/// The tests search a rendered `ShopPage`, whose Javadoc shows its component tree.
class FailureMessageTests {

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

    static String messageOf(Locator<?> locator, MarkupContainer root) {
        return catchThrowableOfType(AssertionError.class, () -> locator.one(root)).getMessage();
    }

    @Test
    @DisplayName("R7.1 no match: the message names the criteria and the root")
    void noMatchNamesTheCriteriaAndTheRoot() {
        var closed = (MarkupContainer) page.get("closed");
        var orders = (MarkupContainer) page.get("orders");

        assertThat(messageOf(locate(Form.class), page))
            .isEqualTo("Expected exactly one match for a Form in ShopPage, but found none.");
        assertThat(messageOf(locate(Link.class), closed))
            .isEqualTo("Expected exactly one match for a Link in WebMarkupContainer \"closed\", but found none.");
        assertThat(messageOf(locate(Link.class), orders))
            .as("the root is an anonymous ListView")
            .isEqualTo("Expected exactly one match for a Link in ListView \"orders\", but found none.");
    }

    @Test
    @DisplayName("R7.2 several matches: the message lists their paths relative to the root")
    void severalMatchesAreListed() {
        var orders = (MarkupContainer) page.get("orders");

        assertThat(messageOf(locate(Label.class).withId("customer"), page)).isEqualTo("""
            Expected exactly one match for a Label with id "customer" in ShopPage, but found 6:
              - orders:0:customer
              - orders:1:customer
              - orders:2:customer
              - table:1:customer
              - table:2:customer
              - table:3:customer""");
        assertThat(messageOf(locate(Label.class).withId("customer"), orders)).isEqualTo("""
            Expected exactly one match for a Label with id "customer" in ListView "orders", but found 3:
              - 0:customer
              - 1:customer
              - 2:customer""");
    }

    @Test
    @DisplayName("R7.3 more than 10 matches: the message lists the first 10 and how many more")
    void longListsAreCut() {
        var every = locate(Component.class);
        var paths = every.all(page).stream().map(Component::getPageRelativePath).toList();

        var lines = messageOf(every, page).lines().toList();

        assertThat(paths).hasSizeGreaterThan(10);
        assertThat(lines.get(0))
            .isEqualTo("Expected exactly one match for a Component in ShopPage, but found " + paths.size() + ":");
        assertThat(lines.subList(1, 11))
            .containsExactlyElementsOf(paths.subList(0, 10).stream().map(path -> "  - " + path).toList());
        assertThat(lines.subList(11, lines.size())).containsExactly("  - and " + (paths.size() - 10) + " more");
    }

    @Test
    @DisplayName("R7.4 no match with two or more criteria: the message lists the near misses")
    void nearMissesAreListed() {
        assertThat(messageOf(locate(Label.class).withId("client").inRowOf(Order.GLOBEX_42), page)).isEqualTo("""
            Expected exactly one match for a Label with id "client" in the row of Order[id=42, customer=Globex] \
            in ShopPage, but found none.
            Near misses (all criteria but one):
              - orders:1:customer (has id "customer")
              - orders:1:number (has id "number")
              - table:2:customer (has id "customer")""");
        assertThat(messageOf(locate(Label.class).withId("refresh"), page))
            .as("ordered by the missed criterion, at most 5")
            .isEqualTo("""
                Expected exactly one match for a Label with id "refresh" in ShopPage, but found none.
                Near misses (all criteria but one):
                  - refresh (is a NoOpLink)
                  - title (has id "title")
                  - headline (has id "headline")
                  - note (has id "note")
                  - closed:reason (has id "reason")
                  - and 11 more""");
    }

    @Test
    @DisplayName("R7.6 a criterion that throws while near misses or components of the type are looked for counts as missed")
    void throwingCriteriaCountAsMissed() {
        var exploding = locate(UserPanel.class).matching(panel -> {
            throw new IllegalStateException("must not escape");
        }, "explodes");

        var user = (MarkupContainer) page.get("user");

        assertThat(messageOf(locate(Label.class).withId("missing").inside(exploding), page))
            .startsWith("Expected exactly one match for a Label with id \"missing\" inside an UserPanel that explodes");
        assertThat(messageOf(locate(Label.class).withId("missing").inside(exploding), user))
            .endsWith("\nComponents of type Label:\n  - name (is not inside an UserPanel that explodes; has id \"name\")");
    }

    @Test
    @DisplayName("R7.5 each near miss says what differs, per missed criterion")
    void nearMissesSayWhatDiffers() {
        assertThat(messageOf(locate(Label.class).withId("user"), page))
            .contains("\n  - user (is an UserPanel)\n");
        assertThat(messageOf(locate(Label.class).withId("reason").visible(), page))
            .contains("\n  - closed:reason (is not visible)\n");
        assertThat(messageOf(locate(NoOpLink.class).withId("archive").enabled(), page))
            .endsWith("\n  - archive (is not enabled)\n  - refresh (has id \"refresh\")");
        assertThat(messageOf(locate(Label.class).withId("hint").inside(locate(UserPanel.class)), page))
            .endsWith("\n  - locked:hint (is not inside an UserPanel)\n  - user:name (has id \"name\")");
        assertThat(messageOf(locate(Label.class).withId("title").inRowOf(Order.GLOBEX_42), page))
            .contains("\n  - title (is not in the row of Order[id=42, customer=Globex])\n");
        assertThat(messageOf(locate(Label.class).withId("title")
            .inRowOf(Order.class, order -> order.id() == 42, "has id 42"), page))
            .contains("\n  - title (is not in the row of an Order that has id 42)\n");
        assertThat(messageOf(locate(Label.class).withId("title").withModelObject("Bazaar"), page))
            .endsWith("\n  - title (has model object \"Shop\")");
        assertThat(messageOf(locate(Label.class).withId("title").matching(label -> false, "is bold"), page))
            .endsWith("\n  - title (does not match \"is bold\")");
        assertThat(messageOf(locate(Label.class).withId("number")
            .withModelObject(Integer.class, number -> number > 50, "is above 50"), page))
            .contains("\n  - orders:0:number (has model object 41)\n");
    }

    @Test
    @DisplayName("R7.5 a near miss of withLabel says which label the component has, if any")
    void nearMissesOfWithLabelSayWhatDiffers() {
        var root = new WebMarkupContainer("root");
        IModel<String> failing = () -> {
            throw new IllegalStateException("this label must not be read");
        };
        root.add(new TextField<String>("broken").setLabel(failing));
        tester.startPage(OrderFormPage.class);
        var formPage = tester.getLastRenderedPage();

        assertThat(messageOf(locate(TextField.class).withId("customer").withLabel("Client"), formPage))
            .endsWith("\n  - order:customer (has label \"Customer\")");
        assertThat(messageOf(locate(TextField.class).withId("query").withLabel("Query"), formPage))
            .endsWith("\n  - search:query (has label \"Search term\")");
        assertThat(messageOf(locate(TextField.class).withId("note").withLabel("Note"), formPage))
            .endsWith("\n  - order:archived:note (has no label)");
        assertThat(messageOf(locate(TextField.class).withId("broken").withLabel("Broken"), root))
            .endsWith("\n  - broken (has a label that cannot be read)");
    }

    @Test
    @DisplayName("R7.7 no near misses: the message lists the components of the locator's type, fewest misses first")
    void componentsOfTheTypeAreListedWithoutNearMisses() {
        assertThat(messageOf(locate(Label.class).withId("client").visible().inside(locate(Form.class)), page))
            .as("note and closed:reason are invisible too, so they come last")
            .isEqualTo("""
                Expected exactly one match for a visible Label with id "client" inside a Form in ShopPage, \
                but found none.
                Components of type Label:
                  - title (is not inside a Form; has id "title")
                  - headline (is not inside a Form; has id "headline")
                  - locked:hint (is not inside a Form; has id "hint")
                  - user:name (is not inside a Form; has id "name")
                  - orders:0:customer (is not inside a Form; has id "customer")
                  - and 10 more""");
        assertThat(messageOf(locate(Label.class).withId("client").inside(locate(Form.class)).withModelObject("ACME"),
            page))
            .as("the model object is not read for a component that misses other criteria")
            .contains("\n  - title (is not inside a Form; has id \"title\")\n");
    }

    @Test
    @DisplayName("R7.8 no near misses and nothing of the locator's type: the message says so")
    void aMissingTypeIsNamed() {
        var closed = (MarkupContainer) page.get("closed");

        assertThat(messageOf(locate(Form.class).withId("order"), page)).isEqualTo("""
            Expected exactly one match for a Form with id "order" in ShopPage, but found none.
            There is no Form in ShopPage.""");
        assertThat(messageOf(locate(Link.class).withId("open"), closed)).endsWith(
            "\nThere is no Link in WebMarkupContainer \"closed\".");
        assertThat(messageOf(locate(Form.class), page))
            .as("not for a locator with its type as the only criterion")
            .isEqualTo("Expected exactly one match for a Form in ShopPage, but found none.");
    }
}
