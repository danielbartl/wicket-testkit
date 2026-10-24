# wicket-testkit — project brief

| | |
|---|---|
| Coordinates | `dev.jbaby:wicket-testkit` |
| Base package | `dev.jbaby.wicket.testkit` |
| Repository | `wicket-testkit` |
| First version | `0.1.0-SNAPSHOT` |
| Status | new project |

## Purpose

Make WicketTester tests readable and refactoring-proof.

Today Wicket tests address components by string paths such as `"list:orders:0:customer"` or
`"name:input"`. Those paths break when a component is wrapped in a container or a list changes
order, and they tell the reader nothing about intent. wicket-testkit lets a test *locate* a
component by what it is (type, `wicket:id`, model object, visibility, position inside another
component), then act and assert on it through the normal `WicketTester`.

```java
// before
tester.assertLabel("list:orders:0:customer", "ACME");
tester.clickLink("list:orders:0:ship");

// after
kit.assertLabel(locate(Label.class).withId("customer").inRowOf(order), "ACME");
kit.click(locate(AjaxLink.class).withId("ship").inRowOf(order));
```

Application tests then wrap such locators in page objects, so the test itself reads in domain
language:

```java
ordersPage.rowOf(order).ship();
ordersPage.rowOf(order).assertCustomer("ACME");
```

## Audience

- Any Apache Wicket 10 project, with or without Spring.
- fullstack4j projects, which will get it transitively through the test module of the dev.jbaby
  Wicket Spring Boot starter (a follow-up, not part of this project).

## Principles

1. **Test-only.** Consumers add it in test scope. Production code that needs to walk the component
   tree uses Wicket's own `visitChildren`.
2. **Composition, not inheritance.** It wraps any existing `WicketTester`, including one provided
   by `@WicketTest`, instead of subclassing it.
3. **Same semantics as WicketTester.** Every action and assertion resolves the locator to a
   component and then delegates to the existing WicketTester method. Wicket behaviour is never
   re-implemented.
4. **Exactly one by default.** A locator used for an action or an assertion must match exactly one
   component. Anything else fails with a message that says what was searched and what was found.
5. **Framework-agnostic.** Failures are plain `java.lang.AssertionError`. No JUnit, TestNG,
   Hamcrest or AssertJ in main code.
6. **Minimal dependencies.** Only `wicket-core` and `wicket-tester`, both in `provided` scope, so
   the consumer's Wicket version wins.
7. **Small API with familiar words.** "Locator" is borrowed from Playwright, so developers and AI
   agents recognise the concept. When in doubt, leave a method out.

## Scope of 0.1

Two layers in one artifact, kept apart by package.

### Layer 1: locating (`dev.jbaby.wicket.testkit.locator`, wicket-core types only)

An immutable, composable `Locator<T extends Component>`. Proposed API (names to be confirmed in
the spec review, see D4):

| Method | Matches |
|---|---|
| `locate(Class<T>)` | components of that type, subtypes included |
| `.withId(String)` | the given `wicket:id` |
| `.withModelObject(Object)` | components whose model object equals the argument |
| `.visible()` | components visible in hierarchy (the component and all its parents) |
| `.enabled()` | components enabled in hierarchy |
| `.inside(Locator<?>)` | components with an ancestor matching the other locator |
| `.inRowOf(Object)` | components inside a repeater item whose model object equals the argument (`ListView` items and `RefreshingView`/`DataView` items) |
| `.matching(Predicate<? super T>, String description)` | escape hatch; the description is required so failure messages stay readable |

Resolution against a root `MarkupContainer` (a page or any container):

- `one(root)`: exactly one match, otherwise `AssertionError`
- `all(root)`: every match, possibly empty, in tree order
- `existsIn(root)`: whether at least one matches

A locator does **not** filter by visibility unless `.visible()` is called, so the same locator
works for asserting that something is invisible.

Layer 1 must not import anything from `wicket-tester`. A test (ArchUnit, test scope) enforces
this, so the locating layer could become its own artifact later without breaking anyone.

### Layer 2: acting and asserting (`dev.jbaby.wicket.testkit`, wicket-tester)

Entry point `WicketTestkit.of(tester)`, wrapping any `BaseWicketTester`. The root for locating is
the last rendered page unless a root is given.

- **Actions:** `click(locator)`, `ajax(event, locator)`.
- **Forms:** `form(locator)` returns a small helper that computes form-relative paths from
  locators and delegates to `FormTester`: `set(fieldLocator, value)`, `select(fieldLocator,
  index)`, `submit()`, `submit(buttonLocator)`, and an Ajax submit through a button.
- **Assertions:** `assertLabel`, `assertVisible`, `assertInvisible`, `assertEnabled`,
  `assertDisabled`, `assertModelObject`, `assertFeedback`, `assertOnAjaxResponse`, `assertCount`.
- **Escape hatch:** `pathOf(locator)` returns the page-relative path, so any WicketTester method
  that isn't wrapped can still be used with a locator.

### Failure messages

Failure messages are a feature, not an afterthought. When a locator matches zero or several
components, the message contains:

- the criteria in plain words, e.g. `a Label with id "customer" in the row of Order[id=42]`
- how many components were found
- up to a few near misses with their paths, e.g. components of the right type with a different
  id, or the right component but invisible

The format is specified and tested, so it stays stable.

## Out of scope for 0.1

- Any production (non-test) use.
- A base class for page objects. The README shows page objects built on locators; a base class
  comes only after real use shows a pattern.
- Spring and `@WicketTest` integration: belongs in the test module of the dev.jbaby Wicket Spring
  Boot starter.
- Wicket Oat-aware locators (Oat fields are panels with an inner `input`): belong in wicket-oat,
  built on Layer 1.
- Markup or HTML assertions beyond what WicketTester already offers.
- Browser testing (Playwright, Selenium).
- Wicket 9 and older. Wicket 11 comes later.

## Technical baseline

- **Wicket:** latest 10.x. Since Wicket 10, `WicketTester` and the other tester classes live in
  their own artifact, `org.apache.wicket:wicket-tester`.
- **Java:** compile with `--release 17` (Wicket 10's minimum) so every Wicket 10 project can use
  it; build and test on JDK 25. See D1.
- **Build:** Maven with wrapper.
- **The project's own tests:** JUnit Jupiter, AssertJ and ArchUnit, all test scope only. Tests
  render small test pages and panels (with markup in `src/test`) through a real `WicketTester`.
  Wicket itself is never mocked.
- **CI:** GitHub Actions, JDK matrix 17, 21 and 25.
- **Module name:** `Automatic-Module-Name: dev.jbaby.wicket.testkit`.
- **License:** Apache 2.0, see D2.
- **Publishing:** Maven Central under the verified `dev.jbaby` namespace, the same way as the other
  dev.jbaby artifacts.

## Way of working

The project follows the fullstack4j approach.

- **The spec lives in code.** Each package's `package-info.java` holds its spec as `///` Markdown:
  purpose, operations, EARS requirements with stable ids `Rn.m`, out of scope.
- **Every requirement has a test, every test has a requirement.** Each test's `@DisplayName`
  starts with the id it proves. A small traceability test (like fullstack4j's
  `SpecTraceabilityTests`, but without Spring Modulith) fails the build on gaps and drift.
- **Order:** spec → review → failing tests → implementation → `./mvnw verify` green.
- **Javadoc** on every public type and method, each with a short example.

## Milestones

| | Milestone | Done when |
|---|---|---|
| M0 | Skeleton | pom, wrapper, CI, LICENSE, README stub; `./mvnw verify` green |
| M1 | Spec | both `package-info.java` specs and the traceability test written; reviewed by Daniel |
| M2 | Layer 1: locating | all locating requirements tested and green |
| M3 | Failure messages | message format tested for zero matches, several matches and near misses |
| M4 | Layer 2: actions and assertions | all action and assertion requirements green |
| M5 | Forms | form helper requirements green, including Ajax submit |
| M6 | Docs | README: installation, before/after, page-object example, limits, credits |
| M7 | Release 0.1.0 | published to Maven Central (done by Daniel) |

## Decisions

Settled on 2026-10-24.

| | Decision | Outcome |
|---|---|---|
| D1 | Java baseline: `--release 17` for reach, or 25 like fullstack4j | **`--release 17`.** Checked: JDK 25's javadoc renders `///` Markdown comments with `--release 17`. Javadoc must therefore be built on JDK 23 or newer (the release build uses JDK 25). |
| D2 | License | **Apache 2.0**, like Wicket and the prior art |
| D3 | Failure type: plain `AssertionError`, or opentest4j's `AssertionFailedError` (diff view in IDEs, one extra dependency) | **Plain `AssertionError`.** `AssertionFailedError` extends `AssertionError`, so switching later would not break callers. |
| D4 | API names: `WicketTestkit.of`, `locate`, `one` / `all` / `existsIn`, `inRowOf` | **As proposed**, confirmed in the M1 review |
| D5 | GitHub owner of the repository | **`danielbartl`**: `github.com/danielbartl/wicket-testkit` |

## Prior art and credit

- **otto-de/enhanced-wickettester** (Apache 2.0, archived; last commit 2018, Wicket 7). Same core
  idea: selecting components by matchers instead of paths. wicket-testkit is a from-scratch
  rewrite for Wicket 10: composition instead of subclassing `WicketTester`, no TestNG, Hamcrest or
  commons-lang, a `Predicate`-based API, and a row shortcut built on model-object matching. Credit
  it in the README. Don't copy its code; if any is copied, keep its license notice.
- **WicketStuff** has no comparable module. Wicket's own tester only offers path lookup
  (`getComponentFromLastRenderedPage`) and markup-tag lookup (`getTagByWicketId`).
- **Playwright locators** for the vocabulary.

## Follow-ups after 0.1 (separate work)

- **Wicket Spring Boot starter, test module:** depend on wicket-testkit and let `@WicketTest` also
  provide a `WicketTestkit`.
- **wicket-oat:** Oat-aware locators, e.g. a field by name without knowing its inner `input`.
- **fullstack4j skill:** page-test recipes use wicket-testkit and page objects; optionally a
  `Fullstack4jRules` rule that forbids wicket-testkit in main code.
