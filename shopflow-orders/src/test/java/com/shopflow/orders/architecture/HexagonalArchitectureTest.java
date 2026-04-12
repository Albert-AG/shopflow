package com.shopflow.orders.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit tests enforcing Hexagonal Architecture rules.
 *
 * EXERCISE T11: These tests FAIL on exercise/topic-11.
 * They PASS on v11-hexagonal.
 *
 * The three rules enforced:
 * 1. Domain layer has no dependencies on infrastructure layer
 * 2. Domain layer has no dependencies on Spring annotations
 * 3. Infrastructure layer accesses domain only via ports (interfaces)
 *
 * When all three tests are green, the hexagonal refactoring is complete.
 */
class HexagonalArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .importPackages("com.shopflow.orders");
    }

    @Test
    @DisplayName("Domain classes must not depend on infrastructure classes")
    void domainMustNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Domain classes must not use Spring annotations (@Autowired, @Service, @Repository)")
    void domainMustNotUseSpringAnnotations() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework..");

        rule.check(classes);
    }

    @Test
    @DisplayName("Infrastructure classes must not directly use domain model classes (only ports)")
    void infrastructureMustAccessDomainOnlyThroughPorts() {
        // Infrastructure adapters should depend on domain.port interfaces, not concrete services
        // This rule checks that infrastructure.rest does not import domain.service directly
        ArchRule rule = noClasses()
                .that().resideInAPackage("..infrastructure.rest..")
                .should().dependOnClassesThat()
                .resideInAPackage("..domain.service..");

        rule.check(classes);
    }
}
