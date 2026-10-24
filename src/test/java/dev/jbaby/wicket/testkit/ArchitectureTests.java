package dev.jbaby.wicket.testkit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.apache.wicket.util.tester.BaseWicketTester;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    @DisplayName("main code depends on the JDK and Wicket only")
    void mainCodeDependsOnTheJdkAndWicketOnly() {
        classes().should().onlyDependOnClassesThat()
            .resideInAnyPackage("java..", "org.apache.wicket..", BASE + "..")
            .because("main code must not use JUnit, TestNG, Hamcrest, AssertJ or anything else")
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
