# wicket-testkit

[![Build](https://github.com/danielbartl/wicket-testkit/actions/workflows/build.yml/badge.svg)](https://github.com/danielbartl/wicket-testkit/actions/workflows/build.yml)

Readable, refactoring-proof [Apache Wicket](https://wicket.apache.org/) tests. wicket-testkit
locates components by what they are (type, `wicket:id`, model object, visibility, position inside
another component) instead of by string paths, then acts and asserts on them through the normal
`WicketTester`.

```java
// before
tester.assertLabel("list:orders:0:customer", "ACME");
tester.clickLink("list:orders:0:ship");

// after
kit.assertLabel(locate(Label.class).withId("customer").inRowOf(order), "ACME");
kit.click(locate(AjaxLink.class).withId("ship").inRowOf(order));
```

> **Status:** in development, nothing released yet. The API above is the proposal and can still
> change before 0.1.0.

Requires Wicket 10 and Java 17 or newer. Test scope only.

## Credits

wicket-testkit builds on the idea of
[otto-de/enhanced-wickettester](https://github.com/otto-de/enhanced-wickettester) (Apache 2.0),
which selected Wicket components by matchers instead of paths for Wicket 7. wicket-testkit is a
from-scratch rewrite for Wicket 10 and shares no code with it.

The word "locator" is borrowed from [Playwright](https://playwright.dev/).

## License

[Apache License 2.0](LICENSE)
