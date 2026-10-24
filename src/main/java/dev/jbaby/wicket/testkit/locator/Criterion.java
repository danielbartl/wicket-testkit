package dev.jbaby.wicket.testkit.locator;

import java.util.function.Predicate;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;

/// One refinement of a locator: a condition a component has to meet besides the locator's type.
sealed interface Criterion {

    /// Whether the component meets this criterion.
    boolean test(Component component);

    /// What this criterion adds to the locator's description.
    String phrase();

    record Id(String id) implements Criterion {

        @Override
        public boolean test(Component component) {
            return id.equals(component.getId());
        }

        @Override
        public String phrase() {
            return "with id " + Words.value(id);
        }
    }

    record ModelObject(Object modelObject) implements Criterion {

        @Override
        public boolean test(Component component) {
            return modelObject.equals(component.getDefaultModelObject());
        }

        @Override
        public String phrase() {
            return "with model object " + Words.value(modelObject);
        }
    }

    record Visible() implements Criterion {

        @Override
        public boolean test(Component component) {
            return component.isVisibleInHierarchy();
        }

        @Override
        public String phrase() {
            return "visible";
        }
    }

    record Enabled() implements Criterion {

        @Override
        public boolean test(Component component) {
            return component.isEnabledInHierarchy();
        }

        @Override
        public String phrase() {
            return "enabled";
        }
    }

    /// The predicate is only ever applied to components of the locator's type.
    record Matching(Predicate<Component> predicate, String description) implements Criterion {

        @Override
        public boolean test(Component component) {
            return predicate.test(component);
        }

        @Override
        public String phrase() {
            return description;
        }
    }

    record Inside(Locator<?> ancestor) implements Criterion {

        @Override
        public boolean test(Component component) {
            for (MarkupContainer parent = component.getParent(); parent != null; parent = parent.getParent()) {
                if (ancestor.matches(parent)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String phrase() {
            return "inside " + ancestor;
        }
    }

    /// A row is a `ListItem`; `Item`, which `RefreshingView` renders, extends it.
    record InRowOf(Object element) implements Criterion {

        @Override
        public boolean test(Component component) {
            for (MarkupContainer parent = component.getParent(); parent != null; parent = parent.getParent()) {
                if (parent instanceof ListItem && element.equals(parent.getDefaultModelObject())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String phrase() {
            return "in the row of " + Words.value(element);
        }
    }
}
