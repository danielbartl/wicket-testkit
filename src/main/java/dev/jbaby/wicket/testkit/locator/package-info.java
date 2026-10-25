/// # Locating
/// > Find Wicket components by what they are instead of by their path.
///
/// A `Locator` describes components by their type, `wicket:id`, model object, state and
/// position, and finds them in any component tree. This package uses wicket-core only, never
/// wicket-tester, so it works on any component tree and could become an artifact of its own.
///
/// ```java
/// Label customer = locate(Label.class).withId("customer").inRowOf(order).one(page);
/// ```
///
/// ## Operations
/// - `locate`: start a locator for the components of a type (`Locator.locate`)
/// - `refine`: narrow a locator down (`Locator.withId`, `withLabel`, `withModelObject`,
///   `visible`, `enabled`, `inside`, `inRowOf`, `matching`)
/// - `resolve-one`: the one matching component, or a failure (`Locator.one`)
/// - `resolve-all`: every matching component (`Locator.all`)
/// - `check-exists`: whether anything matches (`Locator.existsIn`)
/// - `describe`: the criteria in plain words (`Locator.toString`)
/// - `name`: give a locator a name in the domain's words (`Locator.named`)
///
/// ## Terms
/// - **criterion**: one condition of a locator. Its type is one, and every refinement adds one.
/// - **root**: the container a locator is resolved against. Its descendants are the
///   candidates, the root itself is not.
/// - **tree order**: the order in which `MarkupContainer.visitChildren` visits the
///   descendants: depth first, a parent before its children, children in the order they were
///   added.
/// - **row**: a `ListItem`, which `ListView` renders for each element of its list. A
///   `RefreshingView` or `DataView` renders an `Item`, which extends `ListItem`. The row's
///   model object is its element.
/// - **label**: the text of a component's label model (`ILabelProvider.getLabel()`), which form
///   components have. Wicket sets it from `setLabel`, or from the text of a `<wicket:label>` in
///   the markup when the field has no label yet; a plain `<label wicket:for>` leaves it unset.
/// - **path**: a component's path relative to the root, its ids joined by `:`. With a page as
///   the root it is the page-relative path that WicketTester takes.
/// - **class name**: a class's simple name, or for an anonymous class the simple name of its
///   nearest named superclass.
/// - **near miss**: a component that meets every criterion of a locator but one. A criterion
///   the locator did not evaluate for the component (see R4.4) counts as missed.
///
/// ## Requirements
/// ### R1: Locate by type
/// - R1.1 — When a locator for a type is resolved, the locator shall match every component of
///   that type, subtypes included.
/// - R1.2 — The locator shall consider only the root's descendants, never the root itself.
///
/// ### R2: Refine by the component itself
/// - R2.1 — Where `withId` is given, the locator shall match only components with that
///   `wicket:id`.
/// - R2.2 — Where `withModelObject` is given, the locator shall match only components whose
///   model object equals the argument.
/// - R2.3 — Where `visible` is given, the locator shall match only components that are
///   visible in hierarchy.
/// - R2.4 — While `visible` is not given, the locator shall match visible and invisible
///   components alike.
///   _(why: the same locator has to work for asserting that something is invisible)_
/// - R2.5 — Where `enabled` is given, the locator shall match only components that are
///   enabled in hierarchy.
/// - R2.6 — Where `matching` is given, the locator shall match only components that the
///   predicate accepts.
/// - R2.7 — If reading a component's model object throws, then the locator shall treat the
///   component as not matching `withModelObject`.
///   _(why: under a `CompoundPropertyModel`, Wicket creates a model for any component without
///   one, and that model throws when the object has no property named like the component)_
/// - R2.8 — Where `withModelObject` is given `null`, the locator shall match only components
///   whose model object is `null`.
///   _(why: a test can then find a field that was cleared)_
/// - R2.9 — Where `withLabel` is given, the locator shall match only components whose label
///   equals the given text.
/// - R2.10 — The locator shall compare a label with the given text ignoring leading and
///   trailing whitespace in both, and treating every run of whitespace as one space.
///   _(why: Wicket takes a label from the markup as it is written, line breaks and indentation
///   included)_
/// - R2.11 — If reading a component's label throws, then the locator shall treat the component
///   as not matching `withLabel`.
/// - R2.12 — Where `withModelObject` is given a type and a predicate, the locator shall match
///   only components whose model object is of that type and accepted by the predicate.
///   _(why: entities that are loaded again for every request often equal only themselves, as
///   for rows in R3.4)_
/// - R2.13 — The locator shall read a label as text: without its markup tags, and with its
///   character entities decoded.
///   _(why: Wicket takes a `<wicket:label>`'s body as markup, such as
///   `Terms &amp; <b>conditions</b>` for "Terms & conditions")_
///
/// ### R3: Refine by position
/// - R3.1 — Where `inside` is given, the locator shall match only components with an ancestor
///   that meets every criterion of the other locator, whether that ancestor is below, at or
///   above the root.
/// - R3.2 — Where `inRowOf` is given, the locator shall match only components inside a
///   `ListView` row whose model object equals the argument.
/// - R3.3 — Where `inRowOf` is given, the locator shall match only components inside a
///   `RefreshingView` row whose model object equals the argument, `DataView` rows included.
/// - R3.4 — Where `inRowOf` is given an element type and a predicate, the locator shall match
///   only components inside a row whose model object is of that type and accepted by the
///   predicate.
///   _(why: entities that are loaded again for every request often equal only themselves)_
/// - R3.6 — If reading a row's model object throws, then the locator shall treat that row as not
///   matching `inRowOf`.
///   _(why: a row's model may load its element and fail, as in R2.7)_
///
/// ### R4: Combine refinements
/// - R4.1 — The locator shall match only components that meet all its criteria.
/// - R4.2 — When a locator is refined or named, the locator shall return a new locator and leave
///   the original unchanged.
///   _(why: a page object can then keep base locators and refine them per test)_
/// - R4.3 — If any method of a locator is given `null`, other than the model object of
///   `withModelObject`, then the locator shall throw a `NullPointerException` that names the
///   parameter.
/// - R4.4 — The locator shall read a component's model object or label, and apply a `matching`
///   predicate to it, only once the component has met the locator's type and all its other
///   criteria, whatever order the refinements were given in.
///   _(why: models of unrelated components may load data or fail outside their own page, and a
///   predicate may only make sense for the components it was written for)_
/// - R4.5 — If `matching`, `inRowOf` or `withModelObject` is given a blank description, then
///   the locator shall throw an `IllegalArgumentException`.
///   _(why: the description is what failure messages show)_
///
/// ### R5: Resolve
/// - R5.1 — When `one` is called and exactly one component matches, the locator shall return
///   it.
/// - R5.2 — If `one` finds no match, then the locator shall throw an `AssertionError`.
/// - R5.3 — If `one` finds more than one match, then the locator shall throw an
///   `AssertionError`.
/// - R5.4 — When `all` is called, the locator shall return every match in tree order, or an
///   empty list if nothing matches.
/// - R5.5 — When `existsIn` is called, the locator shall return whether at least one component
///   matches.
///
/// ### R6: Describe
/// - R6.1 — The locator shall describe itself as `a` or `an` and the class name of its type:
///   `an` before a vowel letter (`an AjaxLink`), `a` otherwise (`a Label`).
/// - R6.2 — The locator shall add each refinement to its description as the table below
///   shows, in the table's order, whatever order the refinements were given in.
/// - R6.3 — The locator shall put a string value in double quotes and show any other value as
///   `String.valueOf` does.
/// - R6.4 — When `toString` is called, the locator shall return its description.
/// - R6.5 — If a value's `toString` throws, then the locator shall show the value as
///   `<a or an> <class name> whose toString throws`.
///   _(why: a description, and so a failure message, must not fail itself; an entity's
///   `toString` may need a database session)_
///
/// | Refinement | Adds | Example |
/// |---|---|---|
/// | `visible` | `visible` before the type | `a visible Label` |
/// | `enabled` | `enabled` before the type | `an enabled Button`, `a visible, enabled Button` |
/// | `withId` | ` with id "<id>"` | `a Label with id "customer"` |
/// | `withLabel` | ` with label "<text>"` | `a TextField with label "Customer"` |
/// | `withModelObject` | ` with model object <value>` | `a Label with model object "ACME"` |
/// | `withModelObject` with a predicate | ` whose model object is <a or an> <class name> that <description>` | `a Panel whose model object is an Order that has id 42` |
/// | `matching` | ` that <description>`, several joined by ` and ` | `a TextField that is required` |
/// | `inside` | ` inside <the other locator's description>` | `a Label inside a Panel with id "user"` |
/// | `inRowOf` | ` in the row of <value>` | `a Label in the row of Order[id=42]` |
/// | `inRowOf` with a predicate | ` in the row of <a or an> <class name> that <description>` | `a Label in the row of an Order that has id 42` |
///
/// ### R7: Failure messages
/// - R7.1 — If `one` finds no match, then the locator shall start the message with
///   `Expected exactly one match for <description> in <root>, but found none.`, where
///   `<root>` is the root's class name, followed by ` "<path>"` unless the root is a page.
/// - R7.2 — If `one` finds several matches, then the locator shall start the message with
///   `Expected exactly one match for <description> in <root>, but found <n>:` and list the
///   matches in tree order, one line `  - <path>` each.
/// - R7.3 — If the list of matches would have more than 10 lines, then the locator shall show
///   the first 10 and end the list with `  - and <k> more`.
/// - R7.4 — Where a locator has at least two criteria and nothing matches, the locator shall
///   add the line `Near misses (all criteria but one):` and list the near misses, one line
///   `  - <path> (<what differs>)` each, at most 5, then `  - and <k> more`. It shall order
///   them by the criterion they miss, as in the table below, and in tree order within each.
///   _(why: a component of another type or hidden is the likelier culprit than one more
///   component of the right type with another id)_
/// - R7.5 — The locator shall say what differs for each criterion as the table below shows.
/// - R7.6 — If evaluating a criterion throws while the locator looks for near misses or for the
///   components of R7.7, then the locator shall count that criterion as missed.
///   _(why: a failure message must not fail itself)_
/// - R7.7 — Where nothing matches and no component is a near miss, the locator shall add the
///   line `Components of type <class name>:` and list the root's descendants of the locator's
///   type, one line `  - <path> (<what differs>; <what differs>)` each, at most 5, then
///   `  - and <k> more`. Each line shall say what differs for every criterion the component
///   misses, in the order of the table below, leaving out the criteria that R4.4 keeps the
///   locator from evaluating. It shall order the components by how many criteria they miss,
///   fewest first, and in tree order within each.
///   _(why: after a refactoring a component often misses two criteria at once, such as a
///   renamed id in a row that moved, and a message that only says "found none" leaves the
///   reader to search the page)_
/// - R7.8 — Where a locator has at least two criteria, nothing matches, no component is a near
///   miss and the root has no descendant of the locator's type, the locator shall add the line
///   `There is no <class name> in <root>.`, with `<root>` as in R7.1.
///
/// | Missed criterion | What differs |
/// |---|---|
/// | type | `is a <class name>`, or `is an <class name>` before a vowel letter, as in R6.1 |
/// | `visible` | `is not visible` |
/// | `enabled` | `is not enabled` |
/// | `inside` | `is not inside <the other locator's description>` |
/// | `inRowOf` | `is not in the row of <value>`, or `is not in the row of <a or an> <class name> that <description>` |
/// | `withModelObject` | `has model object <value>`, or `has a model object that cannot be read` |
/// | `matching` | `does not match "<description>"` |
/// | `withId` | `has id "<id>"` |
/// | `withLabel` | `has label "<text>"`, `has no label`, or `has a label that cannot be read` |
///
/// A complete message, for a page with two orders whose rows hold `customerName` labels:
///
/// ```
/// Expected exactly one match for a Label with id "customer" in the row of Order[id=42] in OrdersPage, but found none.
/// Near misses (all criteria but one):
///   - orders:0:customerName (has id "customerName")
/// ```
///
/// And for the same page, asked for the row of an order it doesn't show:
///
/// ```
/// Expected exactly one match for a Label with id "customer" in the row of Order[id=99] in OrdersPage, but found none.
/// Components of type Label:
///   - orders:0:customerName (is not in the row of Order[id=99]; has id "customerName")
///   - orders:1:customerName (is not in the row of Order[id=99]; has id "customerName")
/// ```
///
/// ### R8: Name
/// - R8.1 — Where `named` is given, the locator shall describe itself as `<name>, <description>`,
///   where `<description>` is what R6 describes.
///   _(why: failure messages then speak the domain's words and still say what was searched)_
/// - R8.2 — Where `inside` is given a named locator, the locator shall show only that locator's
///   name in its description and in what differs.
///   _(why: a second description in the middle of the first one would be hard to read)_
/// - R8.3 — When a named locator is refined, the locator shall return a locator without the
///   name.
///   _(why: the name says what the named locator matches, and a refinement changes that)_
/// - R8.4 — The locator shall match the same components whether it is named or not.
/// - R8.5 — If `named` is given a blank name, then the locator shall throw an
///   `IllegalArgumentException`.
///
/// A message for a named locator:
///
/// ```
/// Expected exactly one match for the Ship button of order 42, an AjaxLink with id "ship" in the row of Order[id=42] in OrdersPage, but found none.
/// ```
///
/// ## Out of scope
/// - acting on or asserting about components: `dev.jbaby.wicket.testkit` does that through
///   WicketTester
/// - finding components by markup, CSS classes or rendered text: use `matching`
/// - waiting or retrying; a Wicket component tree doesn't change on its own
/// - locators aware of particular component libraries, such as Wicket Oat fields (wicket-oat)
/// - rows of other repeaters, such as the children of a `RepeatingView` or a `Loop`: one
///   without a model of its own takes one from a `CompoundPropertyModel` above it as soon as its
///   model is read, which would change the page and match the wrong row; use `inside` instead
package dev.jbaby.wicket.testkit.locator;
