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
/// - `fill-in-form`: set field values and submit a form (`WicketTestkit.form`,
///   `FormHelper.set`, `FormHelper.select`, `FormHelper.submit`)
/// - `assert`: assert about one component (`WicketTestkit.assertLabel`, `assertVisible`,
///   `assertInvisible`, `assertEnabled`, `assertDisabled`, `assertModelObject`,
///   `assertFeedback`, `assertOnAjaxResponse`)
/// - `count`: assert how many components match (`WicketTestkit.assertCount`)
/// - `get-path`: the page-relative path, for any WicketTester method the testkit doesn't wrap
///   (`WicketTestkit.pathOf`)
///
/// ## Terms
/// - **located component**: the one component that a locator matches in the last rendered
///   page, or for the form helper's methods, inside the form.
/// - **prefix**: `<description> (at <path>): `, made of the locator's description and the
///   located component's page-relative path.
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
/// - R1.4 — If any method of the testkit or of the form helper is given `null`, then it shall
///   throw a `NullPointerException` that names the parameter.
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
/// - R3.4 — If the located component has no Ajax behavior for the event, then `ajax` shall
///   throw an `AssertionError` whose message is the prefix followed by
///   `the component has no Ajax behavior for the event "<event>"`.
///   _(why: `WicketTester.executeAjaxEvent` silently does nothing then)_
///
/// ### R4: Fill in a form
/// - R4.1 — When `form` is called, the testkit shall return a form helper for the located form
///   that starts from the form's current values, as `WicketTester.newFormTester` does.
/// - R4.2 — The form helper shall locate fields and buttons among the form's descendants only,
///   at any depth.
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
/// - R4.8 — If the located field is not enabled in hierarchy, then `set` and `select` shall
///   throw an `AssertionError` whose message is the prefix followed by
///   `the component is not enabled in hierarchy`.
///   _(why: Wicket ignores the input of a disabled field, so the value would be lost without
///   a word)_
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
///   `set`, `select`, `submit` with a button, `assertLabel`, `assertModelObject`,
///   `assertEnabled`, `assertDisabled` and `assertFeedback` shall throw an `AssertionError`
///   whose message is the prefix followed by `the component is not visible in hierarchy`.
///   _(why: WicketTester looks these components up as visible only, and fails with a
///   `NullPointerException` or "does not exist" otherwise)_
///
/// A complete message, for a label whose text differs:
///
/// ```
/// a Label with id "customer" in the row of Order[id=42] (at orders:0:customer): expected: <ACME> but was: <Acme Inc.>
/// ```
///
/// ## Out of scope
/// - a root other than the last rendered page: narrow a locator with `inside` instead
/// - the component started with `startComponentInPage` itself as the located component:
///   WicketTester's paths can't address it, only its descendants
/// - a method for every WicketTester method: pass `pathOf` to the tester instead
/// - feedback messages of a single form component: give
///   `locator.one(tester.getLastRenderedPage())` to WicketTester's
///   `assertFeedbackMessages` with a `ComponentFeedbackMessageFilter`
/// - a base class for page objects; the README shows page objects built on locators
/// - Spring and `@WicketTest` integration (the dev.jbaby Wicket Spring Boot starter's test
///   module)
/// - markup or HTML assertions beyond WicketTester's own
/// - browser testing
package dev.jbaby.wicket.testkit;
