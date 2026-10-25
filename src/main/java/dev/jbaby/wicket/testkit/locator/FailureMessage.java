package dev.jbaby.wicket.testkit.locator;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;

/// The message of the `AssertionError` that [Locator#one(MarkupContainer)] throws.
final class FailureMessage {

    static final int MAX_MATCHES = 10;
    static final int MAX_NEAR_MISSES = 5;
    static final int MAX_CANDIDATES = 5;

    private FailureMessage() {
    }

    /// A component that meets every criterion of a locator but one, and what differs.
    record NearMiss(Component component, String whatDiffers, int order) {
    }

    /// A component of a locator's type that misses several criteria, what differs for each it was
    /// tested for, and how many it misses, untested ones included.
    record Candidate(Component component, List<String> whatDiffers, int misses) {
    }

    /// The candidates are shown only if nothing matches and there is no near miss.
    static String notExactlyOne(Locator<?> locator, MarkupContainer root,
            List<? extends Component> matches, List<NearMiss> nearMisses, List<Candidate> candidates) {
        var message = new StringBuilder("Expected exactly one match for ")
            .append(locator).append(" in ").append(describe(root)).append(", but found ");
        if (!matches.isEmpty()) {
            message.append(matches.size()).append(':');
            appendList(message, matches.stream().map(match -> path(match, root)).toList(), MAX_MATCHES);
        } else {
            message.append("none.");
            if (!nearMisses.isEmpty()) {
                message.append("\nNear misses (all criteria but one):");
                appendList(message, nearMisses.stream()
                    .map(nearMiss -> path(nearMiss.component(), root) + " (" + nearMiss.whatDiffers() + ")")
                    .toList(), MAX_NEAR_MISSES);
            } else if (!candidates.isEmpty()) {
                message.append("\nComponents of type ").append(Words.className(locator.type())).append(':');
                appendList(message, candidates.stream()
                    .map(candidate -> path(candidate.component(), root)
                        + " (" + String.join("; ", candidate.whatDiffers()) + ")")
                    .toList(), MAX_CANDIDATES);
            } else if (locator.isRefined()) {
                message.append("\nThere is no ").append(Words.className(locator.type()))
                    .append(" in ").append(describe(root)).append('.');
            }
        }
        return message.toString();
    }

    private static void appendList(StringBuilder message, List<String> lines, int max) {
        lines.stream().limit(max).forEach(line -> message.append("\n  - ").append(line));
        if (lines.size() > max) {
            message.append("\n  - and ").append(lines.size() - max).append(" more");
        }
    }

    /// A page by its class name, any other container also by its path.
    private static String describe(MarkupContainer root) {
        var name = Words.className(root.getClass());
        if (root instanceof Page) {
            return name;
        }
        MarkupContainer top = root;
        while (top.getParent() != null) {
            top = top.getParent();
        }
        var path = top instanceof Page ? root.getPageRelativePath() : root.getPath();
        return name + " " + Words.value(path);
    }

    /// The component's ids below the root, joined by `:`.
    private static String path(Component component, MarkupContainer root) {
        var path = new StringBuilder(component.getId());
        for (var parent = component.getParent(); parent != root; parent = parent.getParent()) {
            path.insert(0, ':').insert(0, parent.getId());
        }
        return path.toString();
    }
}
