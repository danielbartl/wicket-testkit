package dev.jbaby.wicket.testkit;

import static dev.jbaby.wicket.testkit.locator.Locator.locate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.junit.jupiter.api.Assertions.assertAll;

import dev.jbaby.wicket.testkit.fixtures.Order;
import dev.jbaby.wicket.testkit.fixtures.OrderTablePage;
import dev.jbaby.wicket.testkit.locator.Locator;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

/// Each test starts with the id of the requirement it proves.
/// The requirements live in this package's `package-info.java`.
/// The tests compare the rows of a rendered `OrderTablePage`, whose Javadoc shows its component tree.
@SuppressWarnings("rawtypes")
class AssertRowsTests {

    final Locator<ListView> orders = locate(ListView.class).withId("orders");

    WicketTester tester;
    WicketTestkit kit;

    @BeforeEach
    void renderOrderTablePage() {
        tester = new WicketTester();
        kit = WicketTestkit.of(tester);
        tester.startPage(OrderTablePage.class);
    }

    @AfterEach
    void destroyTester() {
        tester.destroy();
    }

    @Test
    @DisplayName("R9.1 assertRows passes when the rows have exactly the table's cell texts, in order")
    void passesWhenTheRowsMatch() {
        kit.assertRows(orders, """
            | customer      | number |
            | ACME          | 41     |
            | Globex        | 42     |
            | Wayne \\| Sons | 44     |
            """);
        kit.assertRows(orders, """
            | number |   remark    |

            | 41     | Call   first |
            | 42     |             |
            | 44     |             |
            """);
        kit.assertRows(locate(ListView.class).withId("tags").inRowOf(Order.GLOBEX_42), """
            | tag  |
            | new  |
            | rush |
            """);
        kit.assertRows(locate(ListView.class).withId("nothing"), "| customer |");
        assertThatThrownBy(() -> kit.assertRows(orders, """
            | number |
            | 42     |
            | 41     |
            | 44     |
            """))
            .as("in order")
            .isInstanceOf(AssertionFailedError.class);
        assertThatThrownBy(() -> kit.assertRows(orders, "| number |"))
            .as("every row")
            .isInstanceOf(AssertionFailedError.class);
    }

    @Test
    @DisplayName("R9.2 different rows fail with both tables in the message and as expected and actual values")
    void differentRowsFailWithBothTables() {
        var failure = catchThrowableOfType(AssertionFailedError.class, () -> kit.assertRows(orders, """
            | customer      | vip |
            | ACME          |     |
            | Globex        |     |
            | Wayne \\| Sons | VIP |
            """));

        assertThat(failure.getMessage()).isEqualTo("""
            a ListView with id "orders" (at orders): the rows differ
            expected:
            | customer      | vip |
            | ACME          |     |
            | Globex        |     |
            | Wayne \\| Sons | VIP |
            but was:
            | customer      | vip |
            | ACME          |     |
            | Globex        | VIP |
            | Wayne \\| Sons |     |""");
        assertThat(failure.getExpected().getEphemeralValue()).isEqualTo("""
            | customer      | vip |
            | ACME          |     |
            | Globex        |     |
            | Wayne \\| Sons | VIP |""");
        assertThat(failure.getActual().getEphemeralValue()).isEqualTo("""
            | customer      | vip |
            | ACME          |     |
            | Globex        | VIP |
            | Wayne \\| Sons |     |""");
    }

    @Test
    @DisplayName("R9.3 both tables are written with every column as wide as its widest cell in either")
    void bothTablesShareTheColumnWidths() {
        var failure = catchThrowableOfType(AssertionFailedError.class, () -> kit.assertRows(orders, """
            |customer|number|
            |ACME|41|
            """));

        assertThat(failure.getExpected().getEphemeralValue()).isEqualTo("""
            | customer      | number |
            | ACME          | 41     |""");
        assertThat(failure.getActual().getEphemeralValue()).isEqualTo("""
            | customer      | number |
            | ACME          | 41     |
            | Globex        | 42     |
            | Wayne \\| Sons | 44     |""");
    }

    @Test
    @DisplayName("R9.4 a component that is not visible in hierarchy has an empty cell text")
    void invisibleComponentsAreEmpty() {
        kit.assertRows(orders, """
            | number | vip |
            | 41     |     |
            | 42     | VIP |
            | 44     |     |
            """);
    }

    @Test
    @DisplayName("R9.5 a row without a component for a column, or with several, fails with a message that says so")
    void rowsNeedExactlyOneComponentPerColumn() {
        assertThatThrownBy(() -> kit.assertRows(orders, "| customer | status |"))
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessage("a ListView with id \"orders\" (at orders): row 1 (at orders:0) has no component with id \"status\"");
        assertThatThrownBy(() -> kit.assertRows(orders, "| tag |"))
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessage("a ListView with id \"orders\" (at orders): row 1 (at orders:0) has 2 components with id \"tag\"");
    }

    @Test
    @DisplayName("R9.6 a malformed table throws an IllegalArgumentException that names the line")
    void malformedTablesAreRejected() {
        assertAll(
            () -> assertThatIllegalArgumentException().isThrownBy(() -> kit.assertRows(orders, " \n  "))
                .withMessage("the table has no header"),
            () -> assertThatIllegalArgumentException().isThrownBy(() -> kit.assertRows(orders, "| customer |\nACME |"))
                .withMessage("line 2 of the table doesn't start and end with |"),
            () -> assertThatIllegalArgumentException().isThrownBy(() -> kit.assertRows(orders, "| customer |\n| ACME \\|"))
                .withMessage("line 2 of the table doesn't start and end with |"),
            () -> assertThatIllegalArgumentException().isThrownBy(() -> kit.assertRows(orders, "| customer |  |"))
                .withMessage("line 1 of the table has a blank id"),
            () -> assertThatIllegalArgumentException()
                .isThrownBy(() -> kit.assertRows(orders, "| customer | number |\n\n| ACME |"))
                .withMessage("line 3 of the table has 1 cell, but the header has 2"));
    }

    @Test
    @DisplayName("R7.3 assertRows fails on an invisible repeater with a message that says so")
    void invisibleRepeatersAreReported() {
        assertThatThrownBy(() -> kit.assertRows(locate(ListView.class).withId("hidden"), "| customer |"))
            .isExactlyInstanceOf(AssertionError.class)
            .hasMessage("a ListView with id \"hidden\" (at hidden): the component is not visible in hierarchy");
    }

    @Test
    @DisplayName("R1.4 a null argument to assertRows throws a NullPointerException that names the parameter")
    void nullArgumentsAreRejected() {
        assertAll(
            () -> assertThatNullPointerException().isThrownBy(() -> kit.assertRows(null, "| customer |"))
                .withMessageContaining("repeater"),
            () -> assertThatNullPointerException().isThrownBy(() -> kit.assertRows(orders, null))
                .withMessageContaining("expectedTable"));
    }
}
