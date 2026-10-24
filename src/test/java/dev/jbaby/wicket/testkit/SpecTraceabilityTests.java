package dev.jbaby.wicket.testkit;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toCollection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/// Keeps every package's spec and its tests in step.
///
/// A requirement id in a package's `package-info.java` with no test in the same package naming
/// it at the start of its `@DisplayName` is a gap. A test naming an id that its package's spec
/// doesn't have is drift, also in a package without a spec. An id declared twice in one spec is
/// a duplicate. Each of them fails the build.
///
/// Tests belong to the spec of their own package only, not to a parent package's: the tests in
/// `locator` prove the `locator` spec, and both specs number their requirements from R1.
class SpecTraceabilityTests {

    static final Path MAIN = Path.of("src/main/java");
    static final Path TEST = Path.of("src/test/java");

    static final Pattern REQUIREMENT = Pattern.compile(
        "^\\s*///\\s*-\\s*(R\\d+\\.\\d+)\\b", Pattern.MULTILINE);
    static final Pattern TESTED = Pattern.compile(
        "@(?:[\\w.]+\\.)?DisplayName\\(\\s*\"(R\\d+\\.\\d+)\\b");

    @TestFactory
    Stream<DynamicTest> everyPackageMatchesItsSpec() {
        var packages = new TreeSet<Path>();
        packages.addAll(packagesWithIds(MAIN, REQUIREMENT));
        packages.addAll(packagesWithIds(TEST, TESTED));
        return packages.stream().map(pkg -> {
            var name = String.join(".", StreamSupport.stream(pkg.spliterator(), false)
                .map(Path::toString)
                .toList());
            return dynamicTest(name, () -> {
                var declared = declaredIds(MAIN.resolve(pkg));
                var required = new TreeSet<>(declared);
                var tested = testedIds(TEST.resolve(pkg));
                assertAll(
                    () -> assertThat(without(required, tested))
                        .as(name + ": requirements without a test")
                        .isEmpty(),
                    () -> assertThat(without(tested, required))
                        .as(name + ": tests without a requirement")
                        .isEmpty(),
                    () -> assertThat(duplicates(declared))
                        .as(name + ": requirements declared twice")
                        .isEmpty());
            });
        });
    }

    /// The packages, relative to `root`, with a file directly in them that contains an id.
    static Set<Path> packagesWithIds(Path root, Pattern pattern) {
        try (var files = Files.walk(root)) {
            return files
                .filter(file -> file.toString().endsWith(".java"))
                .filter(file -> !matches(pattern, file).isEmpty())
                .map(file -> root.relativize(file.getParent()))
                .collect(toCollection(TreeSet::new));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /// Every id in the package's spec, in order and with duplicates.
    static List<String> declaredIds(Path dir) {
        var spec = dir.resolve("package-info.java");
        return Files.isRegularFile(spec) ? matches(REQUIREMENT, spec) : List.of();
    }

    /// The ids that tests directly in the package name, not those of tests in subpackages.
    static Set<String> testedIds(Path dir) {
        if (!Files.isDirectory(dir)) {
            return Set.of();
        }
        try (var files = Files.list(dir)) {
            return files
                .filter(file -> file.toString().endsWith(".java"))
                .flatMap(file -> matches(TESTED, file).stream())
                .collect(toCollection(TreeSet::new));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static List<String> matches(Pattern pattern, Path file) {
        try {
            return pattern.matcher(Files.readString(file)).results()
                .map(match -> match.group(1))
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Set<String> without(Set<String> these, Set<String> those) {
        var rest = new TreeSet<>(these);
        rest.removeAll(those);
        return rest;
    }

    static Set<String> duplicates(List<String> ids) {
        return ids.stream()
            .collect(groupingBy(id -> id, counting()))
            .entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .map(Map.Entry::getKey)
            .collect(toCollection(TreeSet::new));
    }
}
