package dev.jbaby.wicket.testkit.locator;

import static dev.jbaby.wicket.testkit.locator.Locator.locate;
import static org.assertj.core.api.Assertions.assertThat;

import dev.jbaby.wicket.testkit.fixtures.Headline;
import dev.jbaby.wicket.testkit.fixtures.ShopPage;
import dev.jbaby.wicket.testkit.fixtures.UserPanel;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
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
}
