# ShopFlow — Instrucciones para GitHub Copilot

## Stack

- Java 21, Spring Boot 3.4, Maven 3.9
- PostgreSQL 16 (H2 en desarrollo inicial)
- JUnit 5, AssertJ, Testcontainers

## Restricciones

- Sin `@Autowired` por campo. Inyección por constructor siempre.
- Sin Lombok en clases de dominio. Usar Java Records.
- Sin `RestTemplate`. Usar `RestClient` (Spring 6+).
- Sin `WebSecurityConfigurerAdapter`. Usar `SecurityFilterChain` (Spring Security 6).
- Sin SQL concatenado. Usar `@Query` con parámetros nombrados.
- Sin `System.out.println`. Usar SLF4J Logger.

## Arquitectura objetivo

Hexagonal (Ports & Adapters). Ver estructura de paquetes en `CLAUDE.md`.

## Tests

- Nombres: `should_[resultado]_when_[condicion]`
- Assertions con AssertJ (`assertThat(...)`)
- Tests de integración con Testcontainers (no mocks de BD)
