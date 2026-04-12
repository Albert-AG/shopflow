# ShopFlow — Instrucciones para el Agente

Este archivo es leído automáticamente por Claude Code en cada sesión.
Define las reglas del proyecto que el agente debe respetar siempre.

---

## Stack Tecnológico

- **Java 21** — Usar records, sealed classes y pattern matching cuando sea apropiado.
- **Spring Boot 3.4** — Usar `RestClient` (no `RestTemplate`), `ProblemDetail` (RFC 7807), `@ConfigurationProperties`.
- **Maven 3.9** — Proyecto multi-módulo. El módulo principal es `shopflow-orders`.
- **PostgreSQL 16** — Base de datos de producción (H2 solo en arranque inicial).
- **JUnit 5 + AssertJ** — Para tests. No usar `assertTrue`/`assertEquals` de JUnit directamente.

## Arquitectura (evoluciona a lo largo del curso)

La arquitectura objetivo es **Hexagonal (Ports & Adapters)**:

```
src/main/java/com/shopflow/orders/
├── domain/
│   ├── model/          # Entidades, Value Objects, Aggregates (sin dependencias de framework)
│   ├── port/
│   │   ├── in/         # Casos de uso (interfaces)
│   │   └── out/        # Puertos de salida (interfaces: repositorios, servicios externos)
│   └── service/        # Servicios de dominio (implementan puertos in)
└── infrastructure/
    ├── rest/            # Adaptador REST (Controllers, DTOs de request/response)
    ├── persistence/     # Adaptador JPA/JDBC (implementa puertos out)
    └── messaging/       # Adaptador Kafka (desde T13)
```

> Durante los primeros temas, la estructura aún no es hexagonal.
> No reorganices la arquitectura antes de que el curso llegue al Tema 11.

## Restricciones Permanentes

- **Sin `@Autowired` por campo**. Usar siempre inyección por constructor.
- **Sin Lombok en clases de dominio** (`domain/model/`). Usar Java Records.
- **Sin `var` en firmas de métodos públicos** ni en campos. Solo en variables locales donde mejore la legibilidad.
- **Sin `System.out.println`**. Usar `Logger` de SLF4J.
- **Sin SQL concatenado**. Usar `@Query` con parámetros nombrados o Spring Data method names.
- **Sin `Optional.get()` sin `isPresent()`**. Usar `orElseThrow()` con excepción descriptiva.

## Convenciones de Código

- Nombres en inglés.
- Métodos de dominio con nombres semánticos: `confirm()`, `ship()`, `cancel()` en lugar de `setStatus()`.
- Value Objects inmutables: si representa un concepto del dominio (dinero, ID), usar record.
- Tests: nombre del método en formato `should_[resultado]_when_[condicion]`.

## Lo que el Agente NO debe hacer sin confirmación explícita

- Cambiar la estructura de paquetes (`domain/` → otra estructura).
- Añadir dependencias al `pom.xml`.
- Borrar o renombrar clases existentes.
- Modificar `BeforeAiExample.java` (es material didáctico, no tocar).
- Añadir migraciones de base de datos (hasta que se use Flyway).
