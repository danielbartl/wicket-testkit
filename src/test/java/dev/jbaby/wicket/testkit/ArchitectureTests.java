package dev.jbaby.wicket.testkit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.apache.wicket.util.tester.BaseWicketTester;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;

/// Keeps the main code within the brief's principles, and every test switched on.
class ArchitectureTests {

    static final String BASE = "dev.jbaby.wicket.testkit";

    static final JavaClasses MAIN = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(BASE);

    static final JavaClasses TESTS = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
        .importPackages(BASE);

    @Test
    @DisplayName("the locator package uses wicket-core only, nothing from wicket-tester")
    void locatorPackageDoesNotUseWicketTester() {
        noClasses().that().resideInAPackage(BASE + ".locator..")
            .should().dependOnClassesThat().resideInAPackage("org.apache.wicket.util.tester..")
            .because("the locating layer could become an artifact of its own")
            .check(MAIN);
    }

    @Test
    @DisplayName("main code depends on the JDK, Wicket and opentest4j only")
    void mainCodeDependsOnTheJdkWicketAndOpentest4jOnly() {
        classes().should().onlyDependOnClassesThat()
            .resideInAnyPackage("java..", "org.apache.wicket..", "org.opentest4j..", BASE + "..")
            .because("main code must not use JUnit, TestNG, Hamcrest, AssertJ or anything else;"
                + " opentest4j comes with wicket-tester, whose assertions throw its AssertionFailedError")
            .check(MAIN);
    }

    @Test
    @DisplayName("no class extends a WicketTester")
    void noClassExtendsAWicketTester() {
        noClasses().should().beAssignableTo(BaseWicketTester.class)
            .because("the testkit wraps a WicketTester instead of subclassing it")
            .check(MAIN);
    }

    @Test
    @DisplayName("every method with a display name is a test that JUnit runs")
    void displayNamesSitOnTests() {
        methods().that().areAnnotatedWith(DisplayName.class)
            .should().beAnnotatedWith(Test.class)
            .orShould().beAnnotatedWith(TestFactory.class)
            .orShould().beAnnotatedWith(ParameterizedTest.class)
            .because("SpecTraceabilityTests counts an id in a display name as proven, run or not")
            .check(TESTS);
    }

    @Test
    @DisplayName("tests sit in classes that Maven runs: top-level *Tests classes and their @Nested classes")
    void testsSitInClassesMavenRuns() {
        classes().that(CONTAIN_TESTS).and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("Tests")
            .because("surefire runs only classes named like that")
            .check(TESTS);
        classes().that(CONTAIN_TESTS).and().areMemberClasses()
            .should().beAnnotatedWith(Nested.class)
            .andShould().notHaveModifier(JavaModifier.STATIC)
            .because("JUnit runs an inner class's tests only if it is a non-static @Nested class")
            .check(TESTS);
    }

    static final DescribedPredicate<JavaClass> CONTAIN_TESTS = DescribedPredicate.describe("contain tests",
        javaClass -> javaClass.getMethods().stream().anyMatch(method -> method.isAnnotatedWith(Test.class)
            || method.isAnnotatedWith(TestFactory.class) || method.isAnnotatedWith(ParameterizedTest.class)
            || method.isAnnotatedWith(DisplayName.class)));

    @Test
    @DisplayName("no test is disabled")
    void noTestIsDisabled() {
        noClasses().should().beAnnotatedWith(Disabled.class)
            .because("a disabled test would still count as proving its requirement")
            .check(TESTS);
        noMethods().should().beAnnotatedWith(Disabled.class)
            .because("a disabled test would still count as proving its requirement")
            .check(TESTS);
    }
}
