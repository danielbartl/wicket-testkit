---
name: wicket-testkit
description: Write, fix and refactor tests for Apache Wicket pages, panels and forms with wicket-testkit, which locates components by type, wicket:id, label, model object and row instead of WicketTester string paths such as "orders:1:customer". Use it whenever you write or change a WicketTester test, a page test, an @WicketTest test in Spring Boot or a page object for a Wicket application, whenever a test breaks because a component path changed, and whenever you convert path-based tests, even if the user never mentions wicket-testkit. Also use it to read wicket-testkit failure messages (near misses, "Components of type …") and to turn examples from a specification into assertRows tables.
---

# wicket-testkit

wicket-testkit (`dev.jbaby:wicket-testkit`, test scope, Wicket 10, Java 17+) wraps the
`WicketTester` a test already has. A `Locator` describes a component by what it is; every action
and assertion resolves it against the last rendered page to exactly one component and then calls
the WicketTester or FormTester method that does the job. Wicket's behaviour stays as it is, and
the test stops depending on where the component sits in the tree.

Prefer it over string paths for every new Wicket test. A path like `"form:items:1:name"`
encodes the whole tree on the way to the component: wrap a container around the list or sort the
rows, and the test breaks or, worse, checks the wrong row.

This skill describes version **0.2.0**. Check the version in the project's build file first. In
0.1.0, `withLabel`, `withModelObject(type, predicate, description)`, `named`, `choose`,
`chooseAll`, `check`, `uncheck`, `assertFeedbackOn` and `assertRows` don't exist yet; use the
0.1.0 alternatives listed in [references/api.md](references/api.md#versions).

## Setup

```xml
<dependency>
    <groupId>dev.jbaby</groupId>
    <artifactId>wicket-testkit</artifactId>
    <version>0.2.0</version>
    <scope>test</scope>
</dependency>
```

It brings no dependencies of its own and uses the project's `wicket-core` and
`org.apache.wicket:wicket-tester`, which must be on the test classpath.

```java
import static dev.jbaby.wicket.testkit.locator.Locator.locate;

import dev.jbaby.wicket.testkit.WicketTestkit;

WicketTester tester = new WicketTester(new MyApplication());   // or the one @WicketTest hands you
WicketTestkit kit = WicketTestkit.of(tester);

tester.startPage(OrdersPage.class);                            // start pages with the tester, as always
kit.click(locate(AjaxLink.class).withId("ship").inRowOf(order));
kit.assertLabel(locate(Label.class).withId("status").inRowOf(order), "shipped");
```

## Choosing a locator

Start with the most specific type you know, then add the fewest criteria that make the match
unique and say what the test means:

| To find | Write |
|---|---|
| a component by `wicket:id` | `locate(Label.class).withId("total")` |
| a form field by its label | `locate(TextField.class).withLabel("Customer")` |
| the component in a row of a `ListView`/`DataView` | `.withId("status").inRowOf(order)` |
| a row whose element has no value-based `equals` (entities) | `.inRowOf(Order.class, o -> o.getId() == 42, "has id 42")` |
| a component inside a panel, dialog or form | `.inside(locate(Panel.class).withId("address"))` |
| the shown one of two (one hidden) | `.visible()` |
| by its model object | `.withModelObject("ACME")`, or `.withModelObject(Order.class, o -> …, "…")` |
| anything else | `.matching(FormComponent::isRequired, "is required")` |

- **Rows, not indexes.** Never encode a row index (`orders:1:…`); use `inRowOf(element)`. It
  compares the row's model object with `equals`, so records and value objects work directly.
- **Labels need Wicket to know them.** `withLabel` sees labels set with `setLabel` or a
  `<wicket:label>` in the markup. A plain `<label wicket:for="x">Name</label>` sets none; use
  `withId` there.
- **Visibility is not filtered** unless you call `visible()`, so the same locator also serves
  `assertInvisible`.
- **Descriptions are required** for `matching` and the predicate forms. They appear in failure
  messages and complete sentences like "a TextField that …" or "in the row of an Order that …".
- **Locators are immutable values.** Keep base locators in fields of a page object and refine
  them per use. Give them domain names with `named("the Ship button of order 42")`, and failure
  messages start with that name. Refining a named locator drops the name.

## Acting and asserting

| Instead of WicketTester's | Use |
|---|---|
| `tester.clickLink("…")` | `kit.click(locator)` |
| `tester.executeAjaxEvent("…", "dblclick")` | `kit.ajax(locator, "dblclick")` |
| `tester.assertLabel("…", "text")` | `kit.assertLabel(labelLocator, "text")` |
| `assertVisible` / `assertInvisible` / `assertEnabled` / `assertDisabled` | the same names on `kit` |
| `tester.assertModelValue("…", value)` | `kit.assertModelObject(locator, value)` |
| `tester.assertFeedback("panel", "msg")` | `kit.assertFeedback(panelLocator, "msg")` |
| feedback messages of one field | `kit.assertFeedbackOn(fieldLocator, "'Customer' is required.")` |
| `tester.assertComponentOnAjaxResponse("…")` | `kit.assertOnAjaxResponse(locator)` |
| counting matches, or "none" | `kit.assertCount(locator, n)` |
| any other `WicketTester` method | `tester.someMethod(kit.pathOf(locator), …)` |
| your own check on the component | `kit.one(locator)` returns it, typed |

Every one of these needs its locator to match **exactly one** component. To assert that
something doesn't exist, use `kit.assertCount(locator, 0)`; `assertInvisible` needs the component
to exist, hidden.

## Forms

```java
kit.form(locate(Form.class).withId("order"))
    .set(locate(TextField.class).withLabel("Customer"), "Globex")
    .choose(locate(DropDownChoice.class).withId("country"), "Germany")
    .chooseAll(locate(CheckBoxMultipleChoice.class).withId("extras"), "insurance", "tracking")
    .check(locate(CheckBox.class).withId("giftWrap"))
    .submit(locate(AjaxButton.class).withId("save"));
```

- Fields and buttons are located **inside that form**, at any depth; no form-relative paths.
- `choose(field, choice)` selects by value and survives reordered choices. Prefer it over
  `select(field, index)`.
- `submit(button)` goes through Ajax for an `AjaxButton` or `AjaxSubmitLink`; `submit()` submits
  without a button. `AjaxFallbackButton` extends `Button`, so it submits without Ajax.
- **For an `AjaxFormComponentUpdatingBehavior`**, set or choose the value and fire the event
  through the form helper: `kit.form(f).choose(country, "Germany").ajax(country, "change")`.
  `kit.ajax` sends no form input and would leave the field's model empty.
- **One helper per request.** Until the tester handles another request, `kit.form(f)` returns the
  same helper, so page-object methods can fill a form step by step. After a submit or any Ajax
  event, call `kit.form(f)` again; an old helper fails.
- Disabled or hidden fields fail instead of silently dropping the value. `select` and `choose`
  fail on a field with a `FormComponentUpdatingBehavior`, which FormTester would submit at once;
  go through `formTester()` if that is really what you want. Files also go through
  `formTester()`.

## Example tables

When a requirement or the user gives examples as a table, assert them as a table. The header
names the `wicket:id`s to read in every row of a repeater; each further line is one row, in order:

```java
kit.assertRows(locate(ListView.class).withId("orders"), """
    | customer | status  |
    | ACME     | open    |
    | Globex   | shipped |
    """);
```

Cells hold the component's model object as Wicket renders it but unescaped (`O'Brien & Sons`),
with whitespace as a browser shows it. Hidden rows are left out and hidden components are empty
cells. A `|` inside a cell is `\\|` in a Java text block. A `DataTable` can't be compared this
way: all its cells have the id `cell`. On a mismatch, the IDE shows both tables side by side.

## Page objects

Wrap locators in a page object so tests read in the domain's words:

```java
class OrdersPageObject {

    private final WicketTestkit kit;

    OrdersPageObject(WicketTestkit kit) {
        this.kit = kit;
    }

    void ship(Order order) {
        kit.click(locate(AjaxLink.class).withId("ship").inRowOf(order).named("the Ship button of " + order));
    }

    void assertStatus(Order order, String status) {
        kit.assertLabel(locate(Label.class).withId("status").inRowOf(order), status);
    }
}
```

`locate(TextField.class)` and other generic components give raw types (`Locator<TextField>`). In
a field, add `@SuppressWarnings("rawtypes")`, or declare it `Locator<?>` where the method takes
any locator.

## When a test fails

The messages say what was searched and what was found; read them before changing anything.

| Message | What to do |
|---|---|
| `… but found none.` then `Near misses (all criteria but one):` | Each line names one differing criterion (`has id "customerName"`, `is not visible`, `is an AjaxLink`). Fix that criterion in the locator, or the page if the page is wrong. |
| `Components of type X:` | No component came close; the lines list everything that differs, closest first. Usually a renamed id plus a moved component. |
| `There is no X in Page.` | Wrong type, or the component isn't rendered on this page: check that the right page is rendered and the type is right. |
| `… but found 3:` with paths | Add a refinement: `inRowOf`, `inside` or `visible()`. |
| `(at path): the component is not visible in hierarchy` | It exists but is hidden. Check the page state, or assert it with `assertInvisible`. |
| `the component is not enabled in hierarchy` | The field or button is disabled at that moment. |
| `has no Ajax behavior for the event "…"` | Wrong event name, or the component has no such behavior; for an `AjaxLink` use `click`. |
| `the tester has handled a request since this form helper was created` | Call `kit.form(…)` again after a submit or Ajax event. |
| `has no choice X; its choices are …` | The value isn't among the choices; with `of type Long … of type Integer` the types differ. |
| `the rows differ` | Compare the expected and actual tables; only the differing rows differ. |
| `row 2 (at orders:1) has no component with id "x"` | A header id is wrong or the component sits elsewhere. |
| `The tester has not rendered a page yet.` | Start a page or component first. |

When WicketTester's own assertion fails, its message follows the locator and the path:
`a Label with id "total" (at summary:total): expected: <42> but was: <41>`. Expected and actual
values are kept, so the IDE's diff works.

## Limits worth knowing

- Locators always search the **last rendered page**. After `startComponentInPage(panel)`,
  everything inside the panel can be located, but not the panel itself.
- **Rows are `ListItem`s**: `ListView`, `PropertyListView`, `PageableListView`, `RefreshingView`,
  `DataView` and `GridView`. Children of a `RepeatingView` and `Loop` items are not rows; use
  `inside`.
- `WicketTestkit.of` takes a `WicketTester`, not a `BaseWicketTester`.
- Test scope only; production code walks the tree with Wicket's `visitChildren`.

For every method and its exact behaviour, read [references/api.md](references/api.md).
