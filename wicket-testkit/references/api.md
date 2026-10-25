# wicket-testkit API (0.2.0)

Every method, what it does, and how it fails. The Javadoc of the library has an example for
each; the user guide is at https://danielbartl.github.io/wicket-testkit/guide.html.

## Contents

- [Locator](#locator)
- [WicketTestkit](#wickettestkit)
- [FormHelper](#formhelper)
- [Tables for assertRows](#tables-for-assertrows)
- [Failure messages](#failure-messages)
- [Versions](#versions)

## Locator

`dev.jbaby.wicket.testkit.locator.Locator<T extends Component>`. Immutable: every method returns
a new locator. Uses wicket-core only, so it works on any component tree.

| Method | Matches / does |
|---|---|
| `static locate(Class<T> type)` | components of that type, subtypes included |
| `withId(String id)` | that `wicket:id` |
| `withLabel(String text)` | components whose label (`ILabelProvider.getLabel()`, set by `setLabel` or `<wicket:label>`) equals the text; whitespace normalized, non-breaking spaces included; matches the label as written or as text without tags and with entities decoded |
| `withModelObject(Object value)` | model object `equals` the value; `null` matches a `null` model object; an unreadable model object doesn't match |
| `withModelObject(Class<M>, Predicate<? super M>, String description)` | model object of that type that passes the predicate; `int.class` means `Integer` |
| `visible()` | visible in hierarchy (the component and every parent) |
| `enabled()` | enabled in hierarchy |
| `inside(Locator<?> ancestor)` | some ancestor matches the other locator, also at or above the root |
| `inRowOf(Object element)` | inside a `ListItem` (also `Item` of `RefreshingView`/`DataView`) whose model object `equals` the element |
| `inRowOf(Class<E>, Predicate<? super E>, String description)` | inside a row whose element is of that type and passes |
| `matching(Predicate<? super T>, String description)` | the predicate accepts the component |
| `named(String name)` | changes no match; the description becomes `<name>, <description>`; refining drops the name; inside another locator only the name shows |
| `one(MarkupContainer root)` | the one match among the root's descendants, or an `AssertionError` |
| `all(MarkupContainer root)` | every match in tree order (depth first), possibly empty |
| `existsIn(MarkupContainer root)` | whether anything matches |
| `toString()` | the description used in failure messages |

- The root itself is never a candidate, only its descendants.
- Model objects, labels and predicates are read only once a component met its type and every
  other criterion, whatever order the refinements were written in. Unrelated models are never
  loaded.
- Blank descriptions or names throw `IllegalArgumentException`; `null` arguments throw
  `NullPointerException` naming the parameter (except `withModelObject(null)`).
- Description order: `visible`/`enabled` before the type, then id, label, model object,
  `matching` descriptions, `inside`, row. Strings are quoted; other values use `String.valueOf`,
  or "an Order whose toString throws".

## WicketTestkit

`dev.jbaby.wicket.testkit.WicketTestkit`, created with `WicketTestkit.of(WicketTester)`. Every
method resolves its locator against `tester.getLastRenderedPage()` at the time of the call.

| Method | Delegates to / does |
|---|---|
| `click(Locator<?>)` | `WicketTester.clickLink`, through Ajax for Ajax links |
| `ajax(Locator<?>, String event)` | `executeAjaxEvent`; fails if the component has no Ajax behavior for the event; sends no form input |
| `form(Locator<? extends Form>)` | a `FormHelper` for the form, starting from its current values |
| `assertLabel(Locator<? extends Label>, String)` | `assertLabel` |
| `assertVisible`, `assertInvisible`, `assertEnabled`, `assertDisabled` (`Locator<?>`) | their WicketTester namesakes |
| `assertModelObject(Locator<?>, Object)` | `assertModelValue`; `null` asserts no model object |
| `assertFeedback(Locator<? extends FeedbackPanel>, Serializable...)` | `assertFeedback`: the panel shows exactly these messages, any order |
| `assertFeedbackOn(Locator<?>, Serializable...)` | `assertFeedbackMessages` with a `ComponentFeedbackMessageFilter`: the component has exactly these messages, any order; none asserts no messages |
| `assertOnAjaxResponse(Locator<?>)` | `assertComponentOnAjaxResponse` |
| `assertCount(Locator<?>, int)` | exactly n matches, zero included |
| `assertRows(Locator<? extends AbstractRepeater>, String table)` | the repeater's visible rows equal the table; see below |
| `one(Locator<T>)` | the located component, typed |
| `pathOf(Locator<?>)` | its page-relative path, for any WicketTester method |

- `click`, `ajax`, `form`, `assertLabel`, `assertModelObject`, `assertEnabled`, `assertDisabled`,
  `assertFeedback` and `assertRows` fail on a component that is not visible in hierarchy,
  because WicketTester looks those up as visible only.
- A WicketTester `AssertionError` is rethrown with the prefix `<description> (at <path>): ` and
  the original as cause. An opentest4j `AssertionFailedError` keeps its expected and actual
  values. Any other exception, such as one from the page's `onClick`, passes through unchanged.

## FormHelper

Returned by `kit.form(…)`. Fields, buttons and other components are located among the form's
descendants only, at any depth. Every method except `submit` and `ajax` returns the helper, so
calls chain.

| Method | Delegates to / does |
|---|---|
| `set(Locator<? extends FormComponent>, String input)` | `FormTester.setValue` |
| `choose(Locator<? extends AbstractChoice>, Object choice)` | `FormTester.select` with the index of the first choice that `equals` it |
| `chooseAll(Locator<? extends ListMultipleChoice>, Object... choices)` | `FormTester.selectMultiple(…, replace)`: exactly these choices; none clears |
| `check(Locator<? extends CheckBox>)`, `uncheck(…)` | `FormTester.setValue` with `true` / `false` |
| `select(Locator<? extends FormComponent>, int index)` | `FormTester.select` at an index |
| `ajax(Locator<?>, String event)` | `executeAjaxEvent` with the values set so far |
| `submit()` | `FormTester.submit()` |
| `submit(Locator<?> button)` | `FormTester.submit(Component)`: Ajax for `AjaxButton`/`AjaxSubmitLink` |
| `formTester()` | the `FormTester`, with the values set so far, e.g. for `setFile` |

Fails with an `AssertionError` (message: prefix plus reason) when:

- the field or button is not enabled in hierarchy (`set`, `select`, `choose`, `chooseAll`,
  `check`, `uncheck`, `submit(button)`): `the component is not enabled in hierarchy`;
- it is hidden: `the component is not visible in hierarchy`;
- `select` or `choose` meets a `FormComponentUpdatingBehavior`, which FormTester would run at
  once, losing the values set so far;
- `choose`/`chooseAll` gets a choice the field doesn't have:
  `the component has no choice "France"; its choices are "Austria", "Germany", …` (at most 10,
  then `and k more`), or `… no choice 12 of type Long; its choices, of type Integer, are …` when
  the classes differ;
- the tester has handled another request since the helper was created.

## Tables for assertRows

```
| customer | status  |     header: wicket:ids, one per column
| ACME     | open    |     one line per visible row, in order
| Globex   | shipped |
```

- Lines are trimmed; blank lines are ignored. Each line must start and end with `|`. A `|` inside
  a cell is `\|` (in a Java text block `\\|`).
- A cell's text: the model object of the row's single component with that id (searched at any
  depth inside the row), converted by its converter as Wicket renders it but without escaping
  markup; whitespace collapsed to single spaces, non-breaking spaces included. A hidden component
  is an empty cell; a hidden row is left out.
- Rows are the direct children of the located repeater (`ListView`, `DataView`,
  `RepeatingView`, …). A `DataTable` doesn't work: all its cells have the id `cell`.
- A malformed table throws `IllegalArgumentException` naming the line (no header, a line not
  delimited by `|`, a blank id, a row with another number of cells).
- A row without exactly one component for a column fails:
  `row 2 (at orders:1) has no component with id "status"` or `has 2 components with id "tag"`.
  A model object that can't be read fails with `… whose model object cannot be read`.
- Different rows throw an opentest4j `AssertionFailedError`: message
  `<prefix>the rows differ`, then `expected:` and the table, then `but was:` and the actual table.
  Both tables are padded to the same column widths and are the error's expected and actual
  values.

## Failure messages

No match, with at least two criteria (the type counts as one):

```
Expected exactly one match for a Label with id "customr" in the row of Order[id=42] in OrdersPage, but found none.
Near misses (all criteria but one):
  - orders:1:customer (has id "customer")
  - orders:1:status (has id "status")
```

Near misses are ordered by the missed criterion (type, visible, enabled, inside, row, model
object, matching, id, label), then tree order; at most 5. What each says:

| Missed | Says |
|---|---|
| type | `is a NoOpLink` / `is an AjaxLink` |
| visible / enabled | `is not visible` / `is not enabled` |
| inside | `is not inside a Panel with id "address"` |
| row | `is not in the row of Order[id=42]` |
| model object | `has model object "ACME"`, `has a model object that cannot be read` |
| matching | `does not match "is required"` |
| id | `has id "status"` |
| label | `has label "Customer"`, `has no label`, `has a label that cannot be read` |

No near miss: `Components of type Label:` and up to 5 components of the type, fewest misses first,
each with every difference, separated by `; `. Nothing of that type at all:
`There is no Form in OrdersPage.`

Several matches: `… but found 3:` then up to 10 paths. `assertCount`:
`Expected 2 matches for … in OrdersPage, but found 3:` and the paths.

## Versions

0.1.0 lacks these; use the alternatives there:

| 0.2.0 | In 0.1.0 |
|---|---|
| `withLabel("Customer")` | `withId(…)`, or `matching(c -> c instanceof FormComponent<?> f && f.getLabel() != null && "Customer".equals(f.getLabel().getObject()), "is labelled Customer")` |
| `withModelObject(Order.class, p, "…")` | `matching(c -> c.getDefaultModelObject() instanceof Order o && p.test(o), "…")` |
| `named(…)` | none; the plain description |
| `choose(field, value)` | `select(field, index)` |
| `chooseAll`, `check`, `uncheck` | `formTester().selectMultiple(…)`, `formTester().setValue(path, true)` with the form-relative path |
| `assertFeedbackOn(field, …)` | `tester.assertFeedbackMessages(new ComponentFeedbackMessageFilter(kit.one(field)), …)` |
| `assertRows(repeater, table)` | one `assertLabel(… .inRowOf(element), …)` per cell |
| "Components of type …" / "There is no …" lines | not shown; only near misses |
