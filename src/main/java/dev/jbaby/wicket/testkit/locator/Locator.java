package dev.jbaby.wicket.testkit.locator;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.util.visit.IVisit;

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
        return new Locator<>(notNull(type, "type"), List.of());
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
        return with(new Criterion.Id(notNull(id, "id")));
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
        return with(new Criterion.ModelObject(notNull(modelObject, "modelObject")));
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
        return with(new Criterion.Inside(notNull(ancestor, "ancestor")));
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
        return with(new Criterion.InRowOf(notNull(element, "element")));
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
        notNull(predicate, "predicate");
        if (notNull(description, "description").isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
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
        var matches = all(root);
        if (matches.size() != 1) {
            var nearMisses = matches.isEmpty() ? nearMisses(root) : List.<FailureMessage.NearMiss>of();
            throw new AssertionError(FailureMessage.notExactlyOne(this, root, matches, nearMisses));
        }
        return matches.get(0);
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
        notNull(root, "root").visitChildren((component, visit) -> {
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
        Boolean found = notNull(root, "root").visitChildren((Component component, IVisit<Boolean> visit) -> {
            if (matches(component)) {
                visit.stop(true);
            }
        });
        return found != null;
    }

    private Locator<T> with(Criterion criterion) {
        var more = new ArrayList<>(criteria);
        more.add(criterion);
        return new Locator<>(type, List.copyOf(more));
    }

    private static <V> V notNull(V value, String parameter) {
        return Objects.requireNonNull(value, () -> parameter + " must not be null");
    }

    /// Whether the component meets every criterion of this locator.
    boolean matches(Component component) {
        return type.isInstance(component)
            && criteria.stream().allMatch(criterion -> criterion.test(component));
    }

    /// The components that meet every criterion but one, ordered by the missed criterion, then in
    /// tree order. A locator with its type as the only criterion has none.
    private List<FailureMessage.NearMiss> nearMisses(MarkupContainer root) {
        var nearMisses = new ArrayList<FailureMessage.NearMiss>();
        if (!criteria.isEmpty()) {
            root.visitChildren((component, visit) -> nearMiss(component).ifPresent(nearMisses::add));
            nearMisses.sort(Comparator.comparingInt(FailureMessage.NearMiss::order));
        }
        return nearMisses;
    }

    /// The near miss, if the component misses exactly one criterion. A criterion that needs the
    /// type is not tested for a component of another type, and counts as missed.
    private Optional<FailureMessage.NearMiss> nearMiss(Component component) {
        var rightType = type.isInstance(component);
        var miss = rightType ? null
            : new FailureMessage.NearMiss(component, "is a " + Words.className(component.getClass()), 0);
        for (var criterion : criteria) {
            if ((!rightType && criterion.needsType()) || !criterion.test(component)) {
                if (miss != null) {
                    return Optional.empty();
                }
                miss = new FailureMessage.NearMiss(
                    component, criterion.whatDiffers(component), criterion.nearMissOrder());
            }
        }
        return Optional.ofNullable(miss);
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
        var adjectives = Stream.of(new Criterion.Visible(), new Criterion.Enabled())
            .filter(criteria::contains)
            .map(Criterion::phrase)
            .collect(joining(", "));
        var text = new StringBuilder(Words.withArticle(
            (adjectives.isEmpty() ? "" : adjectives + " ") + Words.className(type)));
        appendPhrases(text, Criterion.Id.class);
        appendPhrases(text, Criterion.ModelObject.class);
        var matching = phrases(Criterion.Matching.class).collect(joining(" and "));
        if (!matching.isEmpty()) {
            text.append(" that ").append(matching);
        }
        appendPhrases(text, Criterion.Inside.class);
        appendPhrases(text, Criterion.InRowOf.class);
        return text.toString();
    }

    private void appendPhrases(StringBuilder text, Class<? extends Criterion> kind) {
        phrases(kind).forEach(phrase -> text.append(' ').append(phrase));
    }

    private Stream<String> phrases(Class<? extends Criterion> kind) {
        return criteria.stream().filter(kind::isInstance).map(Criterion::phrase);
    }
}
