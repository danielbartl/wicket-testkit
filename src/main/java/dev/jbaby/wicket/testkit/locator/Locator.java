package dev.jbaby.wicket.testkit.locator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;

/// Finds Wicket components by what they are instead of by their path: their type, `wicket:id`,
/// model object, state and position.
///
/// A locator starts with a type and is narrowed down by refinements. A component matches when
/// it meets all of them. Locators are immutable: every refinement returns a new locator, so a
/// page object can keep base locators and refine them per test.
///
/// ```java
/// Locator<Label> customer = locate(Label.class).withId("customer");
///
/// Label acme = customer.inRowOf(order).one(page);
/// List<Label> everyCustomer = customer.all(page);
/// boolean anyHidden = customer.matching(label -> !label.isVisible(), "is hidden").existsIn(page);
/// ```
///
/// A locator finds components among the descendants of a root, a page or any other container,
/// in tree order. It doesn't filter by visibility unless [#visible()] is called, so the same
/// locator works for asserting that something is invisible.
///
/// @param <T> the type of the components it finds
public final class Locator<T extends Component> {

    private final Class<T> type;
    private final List<Criterion> criteria;

    private Locator(Class<T> type, List<Criterion> criteria) {
        this.type = type;
        this.criteria = criteria;
    }

    /// Starts a locator for the components of a type, subtypes included.
    ///
    /// ```java
    /// locate(Label.class)       // every label
    /// locate(Component.class)   // every component
    /// ```
    ///
    /// @param type the type of the components to find
    /// @param <T> the type of the components to find
    /// @return a locator that matches every component of that type
    public static <T extends Component> Locator<T> locate(Class<T> type) {
        return new Locator<>(Objects.requireNonNull(type, "type must not be null"), List.of());
    }

    /// Narrows this locator down to components with a `wicket:id`.
    ///
    /// ```java
    /// locate(Label.class).withId("customer")
    /// ```
    ///
    /// @param id the `wicket:id`
    /// @return a new locator with this criterion added
    public Locator<T> withId(String id) {
        return with(new Criterion.Id(id));
    }

    /// Narrows this locator down to components whose model object equals the given one.
    ///
    /// ```java
    /// locate(Label.class).withModelObject("ACME")
    /// ```
    ///
    /// Only the model objects of components of this locator's type are read.
    ///
    /// @param modelObject the model object, compared with `equals`
    /// @return a new locator with this criterion added
    public Locator<T> withModelObject(Object modelObject) {
        return with(new Criterion.ModelObject(modelObject));
    }

    /// Narrows this locator down to components that are visible in hierarchy: they and all
    /// their parents are visible.
    ///
    /// ```java
    /// locate(Button.class).withId("save").visible()
    /// ```
    ///
    /// @return a new locator with this criterion added
    public Locator<T> visible() {
        return with(new Criterion.Visible());
    }

    /// Narrows this locator down to components that are enabled in hierarchy: they and all
    /// their parents are enabled.
    ///
    /// ```java
    /// locate(Button.class).withId("save").enabled()
    /// ```
    ///
    /// @return a new locator with this criterion added
    public Locator<T> enabled() {
        return with(new Criterion.Enabled());
    }

    /// Narrows this locator down to components with an ancestor that the other locator matches.
    /// The ancestor may be anywhere above the component, also at or above the root.
    ///
    /// ```java
    /// locate(Label.class).withId("name").inside(locate(UserPanel.class))
    /// ```
    ///
    /// @param ancestor the locator an ancestor has to match
    /// @return a new locator with this criterion added
    public Locator<T> inside(Locator<?> ancestor) {
        return with(new Criterion.Inside(ancestor));
    }

    /// Narrows this locator down to components in the row of an element: inside a repeater item
    /// (a `ListItem`, as rendered by `ListView`, `RefreshingView` and `DataView`) whose model
    /// object equals the element.
    ///
    /// ```java
    /// locate(Label.class).withId("customer").inRowOf(order)
    /// ```
    ///
    /// @param element the element the row shows, compared with `equals`
    /// @return a new locator with this criterion added
    public Locator<T> inRowOf(Object element) {
        return with(new Criterion.InRowOf(element));
    }

    /// Narrows this locator down to components that a predicate accepts. The description says
    /// what the predicate checks, so failure messages stay readable.
    ///
    /// ```java
    /// locate(TextField.class).matching(FormComponent::isRequired, "is required")
    /// ```
    ///
    /// The predicate is applied only to components of this locator's type.
    ///
    /// @param predicate the check a component has to pass
    /// @param description what the predicate checks, completing "a TextField that …"
    /// @return a new locator with this criterion added
    /// @throws IllegalArgumentException if the description is blank
    public Locator<T> matching(Predicate<? super T> predicate, String description) {
        @SuppressWarnings("unchecked") // matches() applies it to components of type T only
        var anyComponent = (Predicate<Component>) predicate;
        return with(new Criterion.Matching(anyComponent, description));
    }

    /// Finds the one component this locator matches.
    ///
    /// ```java
    /// Label customer = locate(Label.class).withId("customer").inRowOf(order).one(page);
    /// ```
    ///
    /// @param root the page or container to search, not a candidate itself
    /// @return the matching component
    /// @throws AssertionError if no component or several match; the message names the criteria,
    ///     what was found and the near misses
    public T one(MarkupContainer root) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    /// Finds every component this locator matches.
    ///
    /// ```java
    /// List<Label> customers = locate(Label.class).withId("customer").all(page);
    /// ```
    ///
    /// @param root the page or container to search, not a candidate itself
    /// @return the matching components in tree order, empty if none matches
    public List<T> all(MarkupContainer root) {
        var matches = new ArrayList<T>();
        root.visitChildren((component, visit) -> {
            if (matches(component)) {
                matches.add(type.cast(component));
            }
        });
        return matches;
    }

    /// Tells whether this locator matches at least one component.
    ///
    /// ```java
    /// if (locate(Link.class).withId("next").existsIn(page)) { ... }
    /// ```
    ///
    /// @param root the page or container to search, not a candidate itself
    /// @return whether any component matches
    public boolean existsIn(MarkupContainer root) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    private Locator<T> with(Criterion criterion) {
        var more = new ArrayList<>(criteria);
        more.add(criterion);
        return new Locator<>(type, List.copyOf(more));
    }

    /// Whether the component meets every criterion of this locator.
    boolean matches(Component component) {
        return type.isInstance(component)
            && criteria.stream().allMatch(criterion -> criterion.test(component));
    }

    /// Describes this locator's criteria in plain words, as failure messages show them.
    ///
    /// ```java
    /// locate(Label.class).withId("customer").inRowOf(order).toString()
    /// // a Label with id "customer" in the row of Order[id=42]
    /// ```
    ///
    /// @return the description
    @Override
    public String toString() {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
