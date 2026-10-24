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

    /// What differs for a component that misses this criterion.
    String whatDiffers(Component component);

    /// Where a near miss of this criterion goes in a failure message; the type comes first, at 0.
    int nearMissOrder();

    /// Whether this criterion may be tested only for components of the locator's type, because
    /// it reads the model object or runs the caller's predicate.
    default boolean needsType() {
        return false;
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

        @Override
        public String whatDiffers(Component component) {
            return "is not visible";
        }

        @Override
        public int nearMissOrder() {
            return 1;
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

        @Override
        public String whatDiffers(Component component) {
            return "is not enabled";
        }

        @Override
        public int nearMissOrder() {
            return 2;
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

        @Override
        public String whatDiffers(Component component) {
            return "is not " + phrase();
        }

        @Override
        public int nearMissOrder() {
            return 3;
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

        @Override
        public String whatDiffers(Component component) {
            return "is not " + phrase();
        }

        @Override
        public int nearMissOrder() {
            return 4;
        }
    }

    record ModelObject(Object modelObject) implements Criterion {

        /// Stands for a model object that could not be read.
        private static final Object UNREADABLE = new Object();

        @Override
        public boolean test(Component component) {
            var actual = read(component);
            return actual != UNREADABLE && modelObject.equals(actual);
        }

        @Override
        public String phrase() {
            return "with model object " + Words.value(modelObject);
        }

        @Override
        public String whatDiffers(Component component) {
            var actual = read(component);
            return actual == UNREADABLE ? "has a model object that cannot be read"
                : "has model object " + Words.value(actual);
        }

        /// The component's model object, or `UNREADABLE` if reading it throws.
        private static Object read(Component component) {
            try {
                return component.getDefaultModelObject();
            } catch (RuntimeException e) {
                return UNREADABLE;
            }
        }

        @Override
        public int nearMissOrder() {
            return 5;
        }

        @Override
        public boolean needsType() {
            return true;
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

        @Override
        public String whatDiffers(Component component) {
            return "does not match " + Words.value(description);
        }

        @Override
        public int nearMissOrder() {
            return 6;
        }

        @Override
        public boolean needsType() {
            return true;
        }
    }

    record Id(String id) implements Criterion {

        @Override
        public boolean test(Component component) {
            return id.equals(component.getId());
        }

        @Override
        public String phrase() {
            return "with id " + Words.value(id);
        }

        @Override
        public String whatDiffers(Component component) {
            return "has id " + Words.value(component.getId());
        }

        @Override
        public int nearMissOrder() {
            return 7;
        }
    }
}
