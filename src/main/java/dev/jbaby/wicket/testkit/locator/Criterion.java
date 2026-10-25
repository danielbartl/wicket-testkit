package dev.jbaby.wicket.testkit.locator;

import java.util.Objects;
import java.util.function.Predicate;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.ILabelProvider;
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

    /// Whether this criterion may be tested only for components of the locator's type, and only
    /// once they met every other criterion, because it reads the model object or label, or runs
    /// the caller's predicate.
    default boolean needsType() {
        return false;
    }

    /// Whether the component is inside a row whose model object can be read and is accepted.
    private static boolean inRow(Component component, Predicate<Object> element) {
        for (MarkupContainer parent = component.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof ListItem) {
                var modelObject = ModelObject.read(parent);
                if (modelObject != ModelObject.UNREADABLE && element.test(modelObject)) {
                    return true;
                }
            }
        }
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
            return "inside " + ancestor.nameOrDescription();
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

    /// A row is a `ListItem`; `Item`, which `RefreshingView` renders, extends it. Other children of
    /// repeaters are not rows: reading the model of one without a model of its own would give it
    /// one from a `CompoundPropertyModel` above it.
    record InRowOf(Object element) implements Criterion {

        @Override
        public boolean test(Component component) {
            return inRow(component, element::equals);
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

    /// The predicate is only ever applied to elements of the element type.
    record InRowWhere(Class<?> elementType, Predicate<Object> predicate, String description) implements Criterion {

        @Override
        public boolean test(Component component) {
            return inRow(component, element -> elementType.isInstance(element) && predicate.test(element));
        }

        @Override
        public String phrase() {
            return "in the row of " + Words.withArticle(Words.className(elementType)) + " that " + description;
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
            return actual != UNREADABLE && Objects.equals(modelObject, actual);
        }

        @Override
        public String phrase() {
            return "with model object " + Words.value(modelObject);
        }

        @Override
        public String whatDiffers(Component component) {
            return modelObjectOf(component);
        }

        /// What a near miss says about the component's model object.
        private static String modelObjectOf(Component component) {
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

    /// The predicate is only ever applied to model objects of the given type.
    record ModelObjectWhere(Class<?> modelObjectType, Predicate<Object> predicate, String description)
            implements Criterion {

        @Override
        public boolean test(Component component) {
            var actual = ModelObject.read(component);
            return actual != ModelObject.UNREADABLE && modelObjectType.isInstance(actual) && predicate.test(actual);
        }

        @Override
        public String phrase() {
            return "whose model object is " + Words.withArticle(Words.className(modelObjectType)) + " that "
                + description;
        }

        @Override
        public String whatDiffers(Component component) {
            return ModelObject.modelObjectOf(component);
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

    /// A component that provides no label, or a label model whose object is `null`, has no label.
    /// The searched text is the given one, normalized once.
    record LabelText(String text, String searched) implements Criterion {

        /// Stands for a label that could not be read.
        private static final Label UNREADABLE = new Label("", "");

        /// A label as it is written and as text, both with their whitespace normalized.
        private record Label(String written, String asText) {
        }

        static LabelText of(String text) {
            return new LabelText(text, Words.normalized(text));
        }

        @Override
        public boolean test(Component component) {
            var actual = read(component);
            return actual != null && actual != UNREADABLE
                && (searched.equals(actual.written()) || searched.equals(actual.asText()));
        }

        @Override
        public String phrase() {
            return "with label " + Words.value(text);
        }

        @Override
        public String whatDiffers(Component component) {
            var actual = read(component);
            return actual == UNREADABLE ? "has a label that cannot be read"
                : actual == null ? "has no label" : "has label " + Words.value(actual.written());
        }

        /// The component's label, `null` if it has none, or `UNREADABLE` if reading it throws.
        private static Label read(Component component) {
            if (!(component instanceof ILabelProvider<?> provider)) {
                return null;
            }
            try {
                var label = provider.getLabel();
                var object = label == null ? null : label.getObject();
                if (object == null) {
                    return null;
                }
                var written = String.valueOf(object);
                return new Label(Words.normalized(written), Words.normalized(Words.textOf(written)));
            } catch (RuntimeException e) {
                return UNREADABLE;
            }
        }

        @Override
        public int nearMissOrder() {
            return 8;
        }

        @Override
        public boolean needsType() {
            return true;
        }
    }
}
