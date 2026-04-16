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
    @DisplayName("REST adapter must depend on ports, not on concrete application services")
    void restAdapterMustDependOnPorts() {
        // OrderController should inject CreateOrderUseCase / GetOrderUseCase (domain ports),
        // not OrderService (concrete application class).
        // This enforces the Dependency Inversion Principle at the REST adapter boundary.
        ArchRule rule = noClasses()
                .that().resideInAPackage("..infrastructure.rest..")
                .should().dependOnClassesThat()
                .resideInAPackage("..application..");

        rule.check(classes);
    }
}
