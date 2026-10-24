package dev.jbaby.wicket.testkit.locator;

import org.apache.wicket.Component;

/// One refinement of a locator: a condition a component has to meet besides the locator's type.
interface Criterion {

    /// Whether the component meets this criterion.
    boolean test(Component component);
}
