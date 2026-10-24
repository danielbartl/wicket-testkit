package dev.jbaby.wicket.testkit.locator;

import java.util.function.Predicate;
import org.apache.wicket.Component;

/// One refinement of a locator: a condition a component has to meet besides the locator's type.
sealed interface Criterion {

    /// Whether the component meets this criterion.
    boolean test(Component component);

    record Id(String id) implements Criterion {

        @Override
        public boolean test(Component component) {
            return id.equals(component.getId());
        }
    }

    record ModelObject(Object modelObject) implements Criterion {

        @Override
        public boolean test(Component component) {
            return modelObject.equals(component.getDefaultModelObject());
        }
    }

    record Visible() implements Criterion {

        @Override
        public boolean test(Component component) {
            return component.isVisibleInHierarchy();
        }
    }

    record Enabled() implements Criterion {

        @Override
        public boolean test(Component component) {
            return component.isEnabledInHierarchy();
        }
    }

    /// The predicate is only ever applied to components of the locator's type.
    record Matching(Predicate<Component> predicate, String description) implements Criterion {

        @Override
        public boolean test(Component component) {
            return predicate.test(component);
        }
    }
}
