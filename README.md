# wicket-testkit

[![Build](https://github.com/danielbartl/wicket-testkit/actions/workflows/build.yml/badge.svg)](https://github.com/danielbartl/wicket-testkit/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/dev.jbaby/wicket-testkit)](https://central.sonatype.com/artifact/dev.jbaby/wicket-testkit)

**[Website](https://danielbartl.github.io/wicket-testkit/)** &middot; **[User guide](https://danielbartl.github.io/wicket-testkit/guide.html)**

Readable, refactoring-proof [Apache Wicket](https://wicket.apache.org/) tests.

`WicketTester` addresses components by string paths such as `"orders:1:customer"`. Those paths
break as soon as a component gets wrapped in a container or a list changes order, and they tell
the reader nothing about intent. wicket-testkit *locates* a component by what it is: its type,
`wicket:id`, model object, visibility, or position inside another component. It then acts and
asserts on that component through your normal `WicketTester`.

```java
// before
tester.assertLabel("orders:1:customer", "Globex");
tester.clickLink("orders:1:ship");

// after
kit.assertLabel(locate(Label.class).withId("customer").inRowOf(order), "Globex");
kit.click(locate(AjaxLink.class).withId("ship").inRowOf(order));
```

## Installation

Add it in test scope. It needs Wicket 10 and Java 17 or newer, and uses the `wicket-core` and
`wicket-tester` your project already has, so your Wicket version wins.

```xml
<dependency>
    <groupId>dev.jbaby</groupId>
    <artifactId>wicket-testkit</artifactId>
    <version>0.1.0</version>
    <scope>test</scope>
</dependency>
```

Gradle: `testImplementation("dev.jbaby:wicket-testkit:0.1.0")`

## Locating components

A `Locator` starts with a type and is narrowed down step by step. A component matches when it
meets every criterion.

```java
import static dev.jbaby.wicket.testkit.locator.Locator.locate;

locate(Label.class)                                    // every Label, subclasses included
    .withId("customer")                                // with this wicket:id
    .withModelObject("ACME")                           // whose model object equals this
    .visible()                                         // visible in hierarchy
    .enabled()                                         // enabled in hierarchy
    .inside(locate(Panel.class).withId("details"))     // with a matching ancestor
    .inRowOf(order)                                    // in the ListView or DataView row of this element
    .matching(label -> label.getDefaultModelObjectAsString().startsWith("A"), "starts with A");  // anything else
```

Locators are immutable. Every step returns a new locator, so you can keep a base locator and
refine it. A locator finds components among the descendants of a page or any other container:

```java
Label customer = locate(Label.class).withId("customer").inRowOf(order).one(page);  // exactly one, or AssertionError
List<Label> all = locate(Label.class).withId("customer").all(page);                // every match, in tree order
boolean any = locate(Link.class).withId("next").existsIn(page);
```

A locator doesn't filter by visibility unless you call `visible()`. That way the same locator
works for asserting that something is invisible.

The locating layer (`dev.jbaby.wicket.testkit.locator`) uses nothing from wicket-tester, so it
works on any component tree.

## Acting and asserting

`WicketTestkit` wraps the `WicketTester` you already have and leaves it usable as before. Every
method resolves its locator against the last rendered page to exactly one component. It then
hands that component to the matching `WicketTester` method (shown in the comments below), so
Wicket's own behaviour applies unchanged.

```java
WicketTestkit kit = WicketTestkit.of(tester);
tester.startPage(OrdersPage.class);

kit.click(locate(AjaxLink.class).withId("ship").inRowOf(order));                  // clickLink
kit.ajax(locate(WebMarkupContainer.class).withId("row").inRowOf(order), "dblclick");  // executeAjaxEvent

kit.assertLabel(locate(Label.class).withId("status").inRowOf(order), "shipped");  // assertLabel
kit.assertOnAjaxResponse(locate(Label.class).withId("status").inRowOf(order));    // assertComponentOnAjaxResponse
kit.assertVisible(locate(Link.class).withId("details").inRowOf(order));           // assertVisible
kit.assertInvisible(locate(Label.class).withId("secret"));                        // assertInvisible
kit.assertEnabled(locate(Button.class).withId("save"));                           // assertEnabled
kit.assertDisabled(locate(Link.class).withId("archive"));                         // assertDisabled
kit.assertModelObject(locate(TextField.class).withId("customer"), "ACME");        // assertModelValue
kit.assertFeedback(locate(FeedbackPanel.class).visible(), "Shipped order 42");    // assertFeedback
kit.assertCount(locate(Link.class).withId("ship").visible(), 0);
```

For any `WicketTester` method without a counterpart here, `pathOf` gives you the path, and for any
other check, `one` gives you the component:

```java
tester.assertComponent(kit.pathOf(locate(Panel.class).withId("details")), DetailsPanel.class);
assertThat(kit.one(locate(TextField.class).withId("customer")).isRequired()).isTrue();
```

### Forms

`form` wraps a `FormTester` for the located form. Fields and buttons are located inside that
form only.

```java
kit.form(locate(Form.class).withId("order"))
    .set(locate(TextField.class).withId("customer"), "Globex")
    .select(locate(DropDownChoice.class).withId("country"), 1)
    .submit(locate(AjaxButton.class).withId("save"));
```

`submit()` submits without a button. `submit(button)` submits through the button, using Ajax if it
is an `AjaxButton` or `AjaxSubmitLink`, as `FormTester` does.

To fire an Ajax event on a field, such as "change" for an `AjaxFormComponentUpdatingBehavior`, use
the form helper's `ajax`. It sends the values set so far, so the behavior sees the new input.
(`kit.ajax` sends no form input, and would leave the field's model empty.)

```java
kit.form(locate(Form.class).withId("order"))
    .select(locate(DropDownChoice.class).withId("country"), 1)
    .ajax(locate(DropDownChoice.class).withId("country"), "change");
```

A form helper lives for one request. Until the tester handles another one, `kit.form(...)` returns
the same helper, so a page object can fill in a form step by step. After a submit, an Ajax event
or any other request, call `kit.form(...)` again. For anything the helper doesn't offer, such as
files or multiple selections, `formTester()` returns the `FormTester` behind it.

### Failure messages

Every failure is an `AssertionError` that says what was searched for and what was found.

A locator that matches nothing lists its *near misses*: components that meet every criterion but
one. A component of another type or a hidden one comes first.

```
Expected exactly one match for a Label with id "customr" in the row of Order[id=42, customer=Globex] in OrdersPage, but found none.
Near misses (all criteria but one):
  - orders:1:customer (has id "customer")
  - orders:1:status (has id "status")
```

A locator that matches several components lists them:

```
Expected exactly one match for an AjaxLink with id "ship" in OrdersPage, but found 3:
  - orders:0:ship
  - orders:1:ship
  - orders:2:ship
```

When a `WicketTester` check fails, the locator and the component's path go in front of its
message. Expected and actual values are kept, so your IDE still offers its diff:

```
a Label with id "customer" in the row of Order[id=42, customer=Globex] (at orders:1:customer): expected: <ACME> but was: <Globex>
```

## Page objects

Wrap locators in a page object, and your tests read in the words of your domain:

```java
class OrdersPageObject {

    private final WicketTestkit kit;

    OrdersPageObject(WicketTestkit kit) {
        this.kit = kit;
    }

    Row rowOf(Order order) {
        return new Row(order);
    }

    class Row {

        private final Order order;

        Row(Order order) {
            this.order = order;
        }

        void ship() {
            kit.click(locate(AjaxLink.class).withId("ship").inRowOf(order));
        }

        void assertStatus(String status) {
            kit.assertLabel(locate(Label.class).withId("status").inRowOf(order), status);
        }
    }
}
```

```java
@Test
void shipsAnOrder() {
    tester.startPage(OrdersPage.class);
    var ordersPage = new OrdersPageObject(WicketTestkit.of(tester));

    ordersPage.rowOf(order).ship();

    ordersPage.rowOf(order).assertStatus("shipped");
}
```

This exact page object runs as a test in
[`src/test/java/dev/jbaby/wicket/testkit/examples`](src/test/java/dev/jbaby/wicket/testkit/examples).

## Limits

- **Test scope only.** Production code that has to walk the component tree should use Wicket's
  own `visitChildren`.
- **Exactly one.** Every action and assertion needs its locator to match exactly one component.
  Narrow it with `visible()`, `withId`, `inside` or `inRowOf`, and use `assertCount` for "none".
- **Always the last rendered page.** Locators are resolved against the page the tester rendered
  last. To search a part of the page, use `inside`.
- **The component under test itself.** After `tester.startComponentInPage(component)`, you can
  locate everything inside the component, but not the component itself: WicketTester's paths
  can't address it.
- **Rows are `ListItem`s.** `inRowOf` covers `ListView` (including `PropertyListView` and
  `PageableListView`), `RefreshingView` and `DataView`, whose `Item` extends `ListItem`.
  `Loop` items and plain `RepeatingView` children are not rows; use `inside` or `matching`
  for them.
- **Fields that submit on change.** `select` fails on a field with a
  `FormComponentUpdatingBehavior`, because `FormTester` would send a request for it at once and
  lose the values set so far. Select it through `formTester()` if you mean it.
- **Ajax fallback buttons.** `AjaxFallbackButton` extends `Button`, so `FormTester`, and with it
  `submit(button)`, submits through it without Ajax.
- **`WicketTester`, not `BaseWicketTester`.** The assertions exist only on `WicketTester`.
- **Plain articles.** Descriptions put `an` before any vowel letter, so a `UserPanel` comes out as
  `an UserPanel`.
- **Wicket 10 only.** Wicket 9 and older are not supported; Wicket 11 will follow.

## Building

The Javadoc is written as `///` Markdown, which needs JDK 23 or newer to render. The project
compiles with `--release 17` and builds on JDK 25:

```sh
./mvnw verify
```

Each package's spec lives in its `package-info.java`, as EARS requirements with ids like `R2.3`.
Every requirement has a test whose `@DisplayName` starts with its id, and `SpecTraceabilityTests`
fails the build on any gap.

## Credits

wicket-testkit builds on the idea of
[otto-de/enhanced-wickettester](https://github.com/otto-de/enhanced-wickettester) (Apache 2.0),
which selected Wicket components by matchers instead of paths, back in Wicket 7. wicket-testkit is a
from-scratch rewrite for Wicket 10 and shares no code with it. It wraps `WicketTester` instead of
subclassing it, needs no TestNG, Hamcrest or commons-lang, and adds the row shortcut and the
failure messages. Wicket's own `WicketTester` finds components by path, by markup tag, and by
`wicket:id`, the last only after `startComponentInPage`.

The word "locator" is borrowed from [Playwright](https://playwright.dev/).

## License

[Apache License 2.0](LICENSE)
