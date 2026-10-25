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
    /// The name in the domain's words, or `null`.
    private final String name;

    private Locator(Class<T> type, List<Criterion> criteria, String name) {
        this.type = type;
        this.criteria = criteria;
        this.name = name;
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
        return new Locator<>(notNull(type, "type"), List.of(), null);
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

    /// Narrows this locator down to components with a label, as form components have.
    ///
    /// ```java
    /// locate(TextField.class).withLabel("Customer")
    /// ```
    ///
    /// The label is the text of the component's label model. Wicket sets it from `setLabel`, or
    /// from the text of a `<wicket:label>` in the markup; a plain `<label wicket:for>` leaves it
    /// unset. Whitespace at either end doesn't count, and every run of whitespace counts as one
    /// space. A label that can't be read doesn't match.
    ///
    /// @param text the label's text
    /// @return a new locator with this criterion added
    public Locator<T> withLabel(String text) {
        return with(Criterion.LabelText.of(notNull(text, "text")));
    }

    /// Narrows this locator down to components whose model object equals the given one.
    ///
    /// ```java
    /// locate(Label.class).withModelObject("ACME")
    /// ```
    ///
    /// The model object is read only once a component met every other criterion; a model object
    /// that can't be read doesn't match.
    ///
    /// @param modelObject the model object, compared with `equals`; `null` matches components
    ///     whose model object is `null`
    /// @return a new locator with this criterion added
    public Locator<T> withModelObject(Object modelObject) {
        return with(new Criterion.ModelObject(modelObject));
    }

    /// Narrows this locator down to components whose model object is of the given type and passes
    /// a predicate. Use it for model objects that don't equal each other across requests, such as
    /// entities a `LoadableDetachableModel` loads again. The description says what the predicate
    /// checks.
    ///
    /// ```java
    /// locate(OrderPanel.class).withModelObject(Order.class, o -> o.getId() == 42, "has id 42")
    /// ```
    ///
    /// The model object is read only once a component met every other criterion; a model object
    /// that can't be read doesn't match.
    ///
    /// @param modelObjectType the type the model object has to be of
    /// @param predicate the check the model object has to pass
    /// @param description what the predicate checks, completing "whose model object is an Order
    ///     that …"
    /// @param <M> the type of the model object
    /// @return a new locator with this criterion added
    /// @throws IllegalArgumentException if the description is blank
    public <M> Locator<T> withModelObject(Class<M> modelObjectType, Predicate<? super M> predicate,
            String description) {
        notNull(modelObjectType, "modelObjectType");
        notNull(predicate, "predicate");
        @SuppressWarnings("unchecked") // ModelObjectWhere applies it to model objects of type M only
        var anyModelObject = (Predicate<Object>) predicate;
        return with(new Criterion.ModelObjectWhere(modelObjectType, anyModelObject, described(description)));
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

    /// Narrows this locator down to components in the row of an element that a predicate accepts:
    /// inside a repeater item whose model object is of the given type and passes the predicate.
    /// Use it for elements that don't equal each other across requests, such as entities a
    /// `LoadableDetachableModel` loads again. The description says what the predicate checks.
    ///
    /// ```java
    /// locate(Label.class).withId("customer").inRowOf(Order.class, o -> o.getId() == 42, "has id 42")
    /// ```
    ///
    /// @param elementType the type of the element the row shows
    /// @param predicate the check the element has to pass
    /// @param description what the predicate checks, completing "in the row of an Order that …"
    /// @param <E> the type of the element
    /// @return a new locator with this criterion added
    /// @throws IllegalArgumentException if the description is blank
    public <E> Locator<T> inRowOf(Class<E> elementType, Predicate<? super E> predicate, String description) {
        notNull(elementType, "elementType");
        notNull(predicate, "predicate");
        @SuppressWarnings("unchecked") // InRowWhere applies it to elements of type E only
        var anyElement = (Predicate<Object>) predicate;
        return with(new Criterion.InRowWhere(elementType, anyElement, described(description)));
    }

    /// Narrows this locator down to components that a predicate accepts. The description says
    /// what the predicate checks, so failure messages stay readable.
    ///
    /// ```java
    /// locate(TextField.class).matching(FormComponent::isRequired, "is required")
    /// ```
    ///
    /// The predicate is applied only once a component met every other criterion.
    ///
    /// @param predicate the check a component has to pass
    /// @param description what the predicate checks, completing "a TextField that …"
    /// @return a new locator with this criterion added
    /// @throws IllegalArgumentException if the description is blank
    public Locator<T> matching(Predicate<? super T> predicate, String description) {
        notNull(predicate, "predicate");
        @SuppressWarnings("unchecked") // matches() applies it to components of type T only
        var anyComponent = (Predicate<Component>) predicate;
        return with(new Criterion.Matching(anyComponent, described(description)));
    }

    /// Gives this locator a name in the domain's words, which its description and failure
    /// messages then start with. A page object can name its locators once, and every test that
    /// uses them fails in the words of the domain.
    ///
    /// ```java
    /// locate(AjaxLink.class).withId("ship").inRowOf(order).named("the Ship button of order 42")
    /// // the Ship button of order 42, an AjaxLink with id "ship" in the row of Order[id=42]
    /// ```
    ///
    /// The name changes nothing about what the locator matches. Refining a named locator returns a
    /// locator without the name, because the name no longer fits; name the refined one instead.
    ///
    /// @param name the name, such as `the Ship button of order 42`
    /// @return a new locator with that name
    /// @throws IllegalArgumentException if the name is blank
    public Locator<T> named(String name) {
        if (notNull(name, "name").isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return new Locator<>(type, criteria, name);
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
            var candidates = matches.isEmpty() && nearMisses.isEmpty() ? candidates(root)
                : List.<FailureMessage.Candidate>of();
            throw new AssertionError(FailureMessage.notExactlyOne(this, root, matches, nearMisses, candidates));
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

    /// A copy with one more criterion and without the name. Criteria that read the model object or
    /// run a predicate go last, so they only see components that met everything else.
    private Locator<T> with(Criterion criterion) {
        var more = new ArrayList<>(criteria);
        more.add(criterion);
        more.sort(Comparator.comparing(Criterion::needsType));
        return new Locator<>(type, List.copyOf(more), null);
    }

    private static <V> V notNull(V value, String parameter) {
        return Objects.requireNonNull(value, () -> parameter + " must not be null");
    }

    /// The description, if it isn't blank.
    private static String described(String description) {
        if (notNull(description, "description").isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        return description;
    }

    /// The type of the components this locator finds.
    Class<T> type() {
        return type;
    }

    /// The name, if this locator has one, as another locator's description shows it; otherwise
    /// the description.
    String nameOrDescription() {
        return name != null ? name : description();
    }

    /// Whether this locator has a criterion besides its type.
    boolean isRefined() {
        return !criteria.isEmpty();
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
        if (isRefined()) {
            root.visitChildren((component, visit) -> nearMiss(component).ifPresent(nearMisses::add));
            nearMisses.sort(Comparator.comparingInt(FailureMessage.NearMiss::order));
        }
        return nearMisses;
    }

    /// The near miss, if the component misses exactly one criterion, its type included.
    private Optional<FailureMessage.NearMiss> nearMiss(Component component) {
        var misses = misses(component);
        if (misses.count() != 1) {
            return Optional.empty();
        }
        if (!misses.ofType()) {
            return Optional.of(new FailureMessage.NearMiss(
                component, "is " + Words.withArticle(Words.className(component.getClass())), 0));
        }
        var missed = misses.missed().get(0);
        return Optional.of(new FailureMessage.NearMiss(component, missed.whatDiffers(component), missed.nearMissOrder()));
    }

    /// What a component misses. Once it missed its type or a criterion, the criteria that need the
    /// type are not tested (R4.4) and count as missed, untested; a criterion that throws counts as
    /// missed too.
    private Misses misses(Component component) {
        var ofType = type.isInstance(component);
        var missed = new ArrayList<Criterion>();
        var untested = 0;
        for (var criterion : criteria) {
            if (criterion.needsType() && (!ofType || !missed.isEmpty())) {
                untested++;
            } else if (!passes(criterion, component)) {
                missed.add(criterion);
            }
        }
        return new Misses(ofType, missed, untested);
    }

    /// Whether a component is of the locator's type, the criteria it failed in the order they were
    /// tested, and how many it wasn't tested for.
    private record Misses(boolean ofType, List<Criterion> missed, int untested) {

        int count() {
            return (ofType ? 0 : 1) + missed.size() + untested;
        }
    }

    /// The root's descendants of this locator's type, with what differs for each criterion they
    /// miss, fewest misses first, then in tree order. A locator with its type as the only
    /// criterion has none.
    private List<FailureMessage.Candidate> candidates(MarkupContainer root) {
        var candidates = new ArrayList<FailureMessage.Candidate>();
        if (isRefined()) {
            root.visitChildren(type, (component, visit) -> candidates.add(candidate(component)));
            candidates.sort(Comparator.comparingInt(candidate -> candidate.whatDiffers().size()));
        }
        return candidates;
    }

    /// The component of this locator's type with what differs for each criterion it was tested
    /// for and missed, in the order of near misses.
    private FailureMessage.Candidate candidate(Component component) {
        return new FailureMessage.Candidate(component, misses(component).missed().stream()
            .sorted(Comparator.comparingInt(Criterion::nearMissOrder))
            .map(criterion -> criterion.whatDiffers(component))
            .toList());
    }

    private static boolean passes(Criterion criterion, Component component) {
        try {
            return criterion.test(component);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /// Describes this locator's criteria in plain words, as failure messages show them, after its
    /// name if it has one.
    ///
    /// ```java
    /// locate(Label.class).withId("customer").inRowOf(order).toString()
    /// // a Label with id "customer" in the row of Order[id=42]
    /// ```
    ///
    /// @return the description
    @Override
    public String toString() {
        return name != null ? name + ", " + description() : description();
    }

    /// The criteria in plain words.
    private String description() {
        var adjectives = Stream.of(new Criterion.Visible(), new Criterion.Enabled())
            .filter(criteria::contains)
            .map(Criterion::phrase)
            .collect(joining(", "));
        var text = new StringBuilder(Words.withArticle(
            (adjectives.isEmpty() ? "" : adjectives + " ") + Words.className(type)));
        appendPhrases(text, Criterion.Id.class);
        appendPhrases(text, Criterion.LabelText.class);
        appendPhrases(text, Criterion.ModelObject.class, Criterion.ModelObjectWhere.class);
        var matching = phrases(Criterion.Matching.class).collect(joining(" and "));
        if (!matching.isEmpty()) {
            text.append(" that ").append(matching);
        }
        appendPhrases(text, Criterion.Inside.class);
        appendPhrases(text, Criterion.InRowOf.class, Criterion.InRowWhere.class);
        return text.toString();
    }

    @SafeVarargs
    private void appendPhrases(StringBuilder text, Class<? extends Criterion>... kinds) {
        phrases(kinds).forEach(phrase -> text.append(' ').append(phrase));
    }

    /// The phrases of the criteria of any of these kinds, in the order they were given.
    @SafeVarargs
    private Stream<String> phrases(Class<? extends Criterion>... kinds) {
        return criteria.stream()
            .filter(criterion -> Stream.of(kinds).anyMatch(kind -> kind.isInstance(criterion)))
            .map(Criterion::phrase);
    }
}
