/// # Testing with locators
/// > Act on and assert about the components that locators find, through the test's own
/// > WicketTester.
///
/// `WicketTestkit` wraps a `WicketTester` that the test already has, for example one from
/// `@WicketTest`, and leaves it usable as before. Each action and assertion resolves its locator
/// against the last rendered page to exactly one component, then passes that component, or its
/// path, to the WicketTester or FormTester method its requirement names. The testkit never
/// re-implements what those methods do. Locators come from `dev.jbaby.wicket.testkit.locator`.
///
/// ```java
/// WicketTestkit kit = WicketTestkit.of(tester);
/// kit.click(locate(AjaxLink.class).withId("ship").inRowOf(order));
/// kit.assertLabel(locate(Label.class).withId("status").inRowOf(order), "shipped");
/// ```
///
/// ## Operations
/// - `wrap`: wrap a WicketTester (`WicketTestkit.of`)
/// - `click`: click a link (`WicketTestkit.click`)
/// - `fire-ajax-event`: fire an Ajax event on a component (`WicketTestkit.ajax`)
/// - `fill-in-form`: set field values, choose choices, check check boxes, fire Ajax events with
///   them and submit a form (`WicketTestkit.form`, `FormHelper.set`, `FormHelper.select`,
///   `FormHelper.choose`, `FormHelper.chooseAll`, `FormHelper.check`, `FormHelper.uncheck`,
///   `FormHelper.ajax`, `FormHelper.submit`, and `FormHelper.formTester` for anything else)
/// - `assert`: assert about one component (`WicketTestkit.assertLabel`, `assertVisible`,
///   `assertInvisible`, `assertEnabled`, `assertDisabled`, `assertModelObject`,
///   `assertFeedback`, `assertFeedbackOn`, `assertOnAjaxResponse`)
/// - `count`: assert how many components match (`WicketTestkit.assertCount`)
/// - `assert-rows`: compare a repeater's rows with an example table (`WicketTestkit.assertRows`)
/// - `get-path`: the page-relative path, for any WicketTester method the testkit doesn't wrap
///   (`WicketTestkit.pathOf`)
/// - `get-component`: the located component, for any check the testkit doesn't wrap
///   (`WicketTestkit.one`)
///
/// ## Terms
/// - **located component**: the one component that a locator matches in the last rendered
///   page, or for the form helper's methods, inside the form.
/// - **prefix**: `<description> (at <path>): `, made of the locator's description and the
///   located component's page-relative path.
/// - **choice**: an element of a choice field's list of choices (`AbstractChoice.getChoices()`),
///   compared with `equals`.
/// - **table**: text whose lines, blank ones aside, are rows of cells between `|` characters,
///   `| ACME | open |`. The first row, the header, holds `wicket:id`s; every other row the cell
///   texts of one row of a repeater. `\|` stands for a `|` inside a cell.
/// - **cell text**: for a table, a cell's content; for a repeater's row, the model object of the
///   row's component with the column's `wicket:id`, converted to text by the component's
///   converter, as Wicket renders it but without escaping markup. Either way without whitespace
///   at either end, and with every run of whitespace, non-breaking spaces included, as one space,
///   as a browser shows it.
/// - **row**, for `assertRows`: a direct child of the located repeater, such as a `ListItem` of
///   a `ListView` or a child of a `RepeatingView`.
///
/// Description, path, root and the failure messages of a locator are defined in
/// `dev.jbaby.wicket.testkit.locator`.
///
/// ## Requirements
/// ### R1: Wrap a tester
/// - R1.1 — The testkit shall resolve every locator against the page that the tester rendered
///   last at the time of the call.
///   _(why: a click or a submit can render another page)_
/// - R1.2 — While the tester renders a single component (`startComponentInPage`), the testkit
///   shall locate and act on that component's descendants as on those of any page.
/// - R1.3 — If no page has been rendered yet, then the testkit shall throw an `AssertionError`
///   that says so.
/// - R1.4 — If any method of the testkit or of the form helper is given `null`, other than the
///   expected model object of `assertModelObject`, then it shall throw a `NullPointerException`
///   that names the parameter.
///
/// ### R2: Exactly one
/// - R2.1 — If the locator of an action, a form helper method or an assertion other than
///   `assertCount` matches no component or several, then the testkit shall throw the
///   locator's `AssertionError`.
///
/// ### R3: Act
/// - R3.1 — When `click` is called for a link, the testkit shall click it as
///   `WicketTester.clickLink` does.
/// - R3.2 — When `click` is called for an Ajax link, the testkit shall click it through Ajax as
///   `WicketTester.clickLink` does.
/// - R3.3 — When `ajax` is called with an event, the testkit shall fire that event on the
///   located component as `WicketTester.executeAjaxEvent` does.
/// - R3.4 — If the located component has no Ajax behavior for the event, then `ajax`, of the
///   testkit or of the form helper, shall throw an `AssertionError` whose message is the prefix
///   followed by
///   `the component has no Ajax behavior for the event "<event>"`.
///   _(why: `WicketTester.executeAjaxEvent` silently does nothing then)_
///
/// ### R4: Fill in a form
/// - R4.1 — When `form` is called, the testkit shall return a form helper for the located form
///   that starts from the form's current values, as `WicketTester.newFormTester` does.
/// - R4.2 — The form helper shall locate fields, buttons and other components among the form's
///   descendants only, at any depth.
/// - R4.3 — When `set` is called, the form helper shall set the located field's input as
///   `FormTester.setValue` does.
/// - R4.4 — When `select` is called, the form helper shall select the choice at the given index
///   of the located field as `FormTester.select` does.
/// - R4.5 — When `submit` is called without a button, the form helper shall submit the form
///   with the values set so far as `FormTester.submit` does.
/// - R4.6 — When `submit` is called with a button, the form helper shall submit the form through
///   the located button as `FormTester.submit` does.
/// - R4.7 — Where the located button is an `AjaxButton` or an `AjaxSubmitLink`, the form helper
///   shall submit the form through Ajax with the values set so far.
/// - R4.8 — If the located field or button is not enabled in hierarchy, then `set`, `select`,
///   `choose`, `chooseAll`, `check`, `uncheck` and `submit` with a button shall throw an
///   `AssertionError` whose message is the prefix followed by
///   `the component is not enabled in hierarchy`.
///   _(why: Wicket ignores the input of a disabled field, so the value would be lost without
///   a word, and nobody can click a disabled button, though FormTester submits through it)_
/// - R4.9 — When `form` is called again for the same form before the tester has handled
///   another request, the testkit shall return the same form helper, with the values set so far.
///   _(why: a second FormTester would add the form's current values to the request again, and
///   which value wins would be left to chance)_
/// - R4.10 — If the tester has handled a request since the form helper was created, then the
///   form helper's methods shall throw an `AssertionError` whose message is the form's prefix
///   followed by
///   `the tester has handled a request since this form helper was created; call form again`.
///   _(why: the values a FormTester set are gone after a request, also after an Ajax submit)_
/// - R4.11 — If the located field has a `FormComponentUpdatingBehavior`, then `select` and
///   `choose` shall throw an `AssertionError` whose message is the prefix followed by
///   `the component has a FormComponentUpdatingBehavior, which FormTester would run at once,
///   losing the values set so far; use formTester() to select it anyway`.
///   _(why: `FormTester.select` sends a request of its own for that behavior)_
/// - R4.12 — When `ajax` is called on the form helper with an event, the form helper shall fire
///   that event on the located component with the values set so far, as
///   `WicketTester.executeAjaxEvent` does.
///   _(why: an `AjaxFormComponentUpdatingBehavior` reads the field's input from the request)_
/// - R4.13 — When `formTester` is called, the form helper shall return its `FormTester`, with
///   the values set so far.
/// - R4.14 — When `choose` is called with a choice, the form helper shall select the first of
///   the located field's choices that equals it, as `FormTester.select` does with that
///   choice's index.
///   _(why: an index silently selects another choice once the choices are reordered)_
/// - R4.15 — If a choice given to `choose` or `chooseAll` equals none of the located field's
///   choices, then the form helper shall throw an `AssertionError` whose message is the prefix
///   followed by `the component has no choice <value>; its choices are <choices>`. `<choices>`
///   is the field's choices separated by `, `, at most 10 followed by `, and <k> more`, or
///   `none` if it has none. Values appear as a locator's description shows them.
/// - R4.16 — When `chooseAll` is called with choices, the form helper shall replace the located
///   field's selection with exactly those choices, as `FormTester.selectMultiple` does with
///   their indexes and `replace` set.
/// - R4.17 — When `chooseAll` is called without choices, the form helper shall clear the
///   located field's selection.
/// - R4.18 — When `check` is called, the form helper shall check the located check box, as
///   `FormTester.setValue` does with `true`.
/// - R4.19 — When `uncheck` is called, the form helper shall uncheck the located check box, as
///   `FormTester.setValue` does with `false`.
/// - R4.20 — If no choice of the located field is of the given choice's class, then the message
///   of R4.15 shall name the classes:
///   `the component has no choice <value> of type <class name>; its choices, of type <class names>, are <choices>`.
///   _(why: `12L` and `12` look alike, though a `Long` never equals an `Integer`)_
/// - R4.21 — Where classes in the message of R4.20 have the same simple name, the form helper
///   shall name them by their full names.
///
/// ### R5: Assert about one component
/// - R5.1 — When `assertLabel` is called, the testkit shall check the located label's text as
///   `WicketTester.assertLabel` does.
/// - R5.2 — When `assertVisible` is called, the testkit shall check that the located component
///   is visible in hierarchy, as `WicketTester.assertVisible` does.
/// - R5.3 — When `assertInvisible` is called, the testkit shall check that the located
///   component is not visible in hierarchy, as `WicketTester.assertInvisible` does.
/// - R5.4 — When `assertEnabled` is called, the testkit shall check that the located component
///   is enabled in hierarchy, as `WicketTester.assertEnabled` does.
/// - R5.5 — When `assertDisabled` is called, the testkit shall check that the located component
///   is not enabled in hierarchy, as `WicketTester.assertDisabled` does.
/// - R5.6 — When `assertModelObject` is called, the testkit shall check the located component's
///   model object as `WicketTester.assertModelValue` does.
/// - R5.7 — When `assertFeedback` is called, the testkit shall check that the located feedback
///   panel shows the given messages, as `WicketTester.assertFeedback` does.
/// - R5.8 — When `assertOnAjaxResponse` is called, the testkit shall check that the last Ajax
///   response rendered the located component, as `WicketTester.assertComponentOnAjaxResponse`
///   does.
/// - R5.9 — When `assertModelObject` is given `null`, the testkit shall check that the located
///   component's model object is `null`.
///   _(why: a test can then assert that a field was cleared)_
/// - R5.10 — When `assertFeedbackOn` is called with messages, the testkit shall check that the
///   located component has exactly those feedback messages, in any order, as
///   `WicketTester.assertFeedbackMessages` does with a `ComponentFeedbackMessageFilter` for the
///   component.
/// - R5.11 — When `assertFeedbackOn` is called without messages, the testkit shall check that
///   the located component has no feedback messages.
///
/// ### R6: Count
/// - R6.1 — When `assertCount` is called and exactly the given number of components match, the
///   testkit shall pass, for zero as well.
/// - R6.2 — If a different number of components match, then `assertCount` shall throw an
///   `AssertionError` whose message starts with
///   `Expected <n> matches for <description> in <root>, but found <m>:` (`match` for one,
///   `but found none.` for zero), followed by the list of matches as the locator lists them.
///
/// ### R7: Failure messages
/// - R7.1 — If a WicketTester or FormTester method throws an `AssertionError` for the located
///   component, then the testkit shall throw an `AssertionError` whose message is the prefix
///   followed by that error's message, with that error as its cause.
/// - R7.2 — If a WicketTester or FormTester method throws any other exception, then the testkit
///   shall let it through unchanged.
///   _(why: an exception from the application under test is not a failed assertion)_
/// - R7.3 — If the located component is not visible in hierarchy, then `click`, `ajax`, `form`,
///   `set`, `select`, `choose`, `chooseAll`, `check`, `uncheck`, the form helper's `ajax`,
///   `submit` with a button, `assertRows`, `assertLabel`,
///   `assertModelObject`, `assertEnabled`, `assertDisabled` and `assertFeedback` shall throw an
///   `AssertionError` whose message is the prefix followed by
///   `the component is not visible in hierarchy`.
///   _(why: WicketTester looks these components up as visible only, and fails with a
///   `NullPointerException` or "does not exist" otherwise)_
/// - R7.4 — Where the WicketTester or FormTester error is an opentest4j `AssertionFailedError`,
///   the testkit shall throw an `AssertionFailedError` with the message of R7.1 and the same
///   expected and actual values.
///   _(why: IDEs then offer their expected/actual diff; opentest4j comes with wicket-tester)_
///
/// ### R8: Hand over to code the testkit doesn't wrap
/// - R8.1 — When `pathOf` is called, the testkit shall return the located component's
///   page-relative path, which the tester's own methods accept.
/// - R8.2 — When `one` is called, the testkit shall return the located component, typed as the
///   locator's type.
///
/// A complete message, for a label whose text differs:
///
/// ```
/// a Label with id "customer" in the row of Order[id=42] (at orders:0:customer): expected: <ACME> but was: <Acme Inc.>
/// ```
///
/// ### R9: Compare rows with a table
/// - R9.1 — When `assertRows` is called with a table, the testkit shall pass if the located
///   repeater's rows, in tree order, have exactly the table's rows of cell texts in the columns
///   of the header's ids.
/// - R9.2 — If the rows differ from the table, then `assertRows` shall throw an opentest4j
///   `AssertionFailedError` whose message is the prefix followed by `the rows differ`, then the
///   line `expected:` and the expected table, then the line `but was:` and the rows as a table,
///   and whose expected and actual values are these two tables.
///   _(why: IDEs then show the two tables side by side, row by row)_
/// - R9.3 — The testkit shall write both tables as one line per row, `| <cell> | <cell> |`, the
///   header first, with every column padded with spaces to its widest cell in either table and a
///   `|` inside a cell written as `\|`.
///   _(why: with equal widths, the rows that match are equal lines in the diff)_
/// - R9.4 — Where a row's component for a column is not visible in hierarchy, the testkit shall
///   take its cell text as empty.
///   _(why: the table shows what a user sees)_
/// - R9.5 — If a row has no component with a column's id, or several, then `assertRows` shall
///   throw an `AssertionError` whose message is the prefix followed by
///   `row <n> (at <path>) has no component with id "<id>"` or
///   `row <n> (at <path>) has <k> components with id "<id>"`, counting the compared rows from 1.
/// - R9.6 — If the table has no header, a line that doesn't start and end with `|`, a blank id,
///   or a row with another number of cells than the header, then `assertRows` shall throw an
///   `IllegalArgumentException` whose message names the line, counting the table's lines from 1.
///   _(why: a malformed table is a mistake in the test, not a failed assertion)_
/// - R9.7 — The testkit shall leave out the rows that are not visible in hierarchy.
///   _(why: the table shows what a user sees)_
/// - R9.8 — The testkit shall compare cell texts without escaping markup.
///   _(why: a table then says `O'Brien & Sons`, as a user reads it, not `O&#039;Brien &amp; Sons`)_
/// - R9.9 — If reading the model object of a row's component for a column throws, or converting
///   it to text, then `assertRows` shall throw an `AssertionError` whose message is the prefix
///   followed by `row <n> (at <path>) has a component with id "<id>" whose model object cannot be read`,
///   with that exception as its cause.
///   _(why: a component without a model of its own in a `PropertyListView` row gets one for a
///   property that may not exist)_
///
/// A table and the message when one row differs:
///
/// ```java
/// kit.assertRows(locate(ListView.class).withId("orders"), """
///     | customer | status  |
///     | ACME     | open    |
///     | Globex   | shipped |
///     """);
/// ```
///
/// ```
/// a ListView with id "orders" (at orders): the rows differ
/// expected:
/// | customer | status  |
/// | ACME     | open    |
/// | Globex   | shipped |
/// but was:
/// | customer | status  |
/// | ACME     | open    |
/// | Globex   | open    |
/// ```
///
/// ## Out of scope
/// - a root other than the last rendered page: narrow a locator with `inside` instead
/// - the component started with `startComponentInPage` itself as the located component:
///   WicketTester's paths can't address it, only its descendants
/// - a method for every WicketTester method: pass `pathOf` to the tester instead
/// - file uploads: use `formTester()`
/// - choosing a choice by the text it shows: `choose` takes the choice itself
/// - comparing a `DataTable` with a table: its cells all have the id `cell`, so a header can't
///   name them
/// - a base class for page objects; the README shows page objects built on locators
/// - Spring and `@WicketTest` integration (the dev.jbaby Wicket Spring Boot starter's test
///   module)
/// - markup or HTML assertions beyond WicketTester's own
/// - browser testing
package dev.jbaby.wicket.testkit;
