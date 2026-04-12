# ShopFlow

Proyecto demo del curso **Spring Boot Senior: Agentic & Spec-Driven Development**.

ShopFlow es un sistema backend de gestión de pedidos para un marketplace B2B. A lo largo del curso el proyecto **nace como un monolito mal diseñado y evoluciona** hasta convertirse en una arquitectura hexagonal con eventos, DDD y seguridad de producción.

---

## El dominio

Un **Customer** realiza un **Order** con uno o más **OrderItem**. El sistema verifica el inventario, procesa el pago a través de un servicio externo, notifica al comprador y genera una factura.

```
Customer ──► Order ──► OrderItem ──► Product
                │
                ├──► Payment (servicio externo)
                ├──► Inventory (reserva de stock)
                └──► Notification (email/SMS)
```

---

## Stack tecnológico

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.4 |
| Persistencia | Spring Data JPA + PostgreSQL 16 |
| Mensajería | Apache Kafka 3.7 |
| Resiliencia | Resilience4j |
| Tests | JUnit 5 + AssertJ + Testcontainers |
| Arquitectura | Hexagonal (Ports & Adapters) |
| Seguridad | Spring Security 6 |
| Build | Maven 3.9 (multi-módulo) |

---

## Estructura del repositorio

```
shopflow/
├── shopflow-orders/          # Módulo principal — ciclo de vida del pedido
├── shopflow-payments-stub/   # Servicio de pagos simulado (falla aleatoriamente)
├── shopflow-notifications/   # Consumidor de eventos Kafka
├── db/
│   └── schema.sql            # DDL completo de la base de datos
├── docker-compose.yml        # PostgreSQL 16 + Kafka 3.7 + Redis 7
└── CLAUDE.md                 # Instrucciones para el agente de IA
```

---

## Cómo funciona este repositorio

El repositorio tiene **tres tipos de referencias git** con propósitos distintos:

### `main` — la historia limpia

La rama `main` contiene el código correcto y siempre compilable. Representa la solución del instructor al final de cada tema. **No trabajes directamente aquí.**

### Tags `vXX-nombre` — el estado al final de cada tema

Cada tag marca exactamente el estado del proyecto tras completar un tema del curso. Úsalos para navegar a un punto concreto de la historia o para ver la solución del instructor.

```bash
# Ver el código al final del Tema 9
git checkout v09-spring-boot

# Volver a la última versión
git checkout main
```

### Ramas `exercise/topic-XX` — el punto de partida de cada ejercicio

Cada rama de ejercicio contiene el código **intencionalmente incompleto o con bugs** desde el que debes trabajar. Es tu punto de partida para cada ejercicio.

```bash
# Empezar el ejercicio del Tema 11
git checkout exercise/topic-11
git checkout -b mi-solucion/topic-11

# Si te bloqueas, consulta la solución del instructor
git checkout v11-hexagonal
```

---

## Referencia de tags y ramas de ejercicio

### Tags

| Tag | Tema | Qué contiene |
|-----|------|-------------|
| `v01-bootstrap` | T01 — IA: el antes y el después | Esqueleto Maven, `BeforeAiExample.java` (código pre-IA intencionalmente malo), `EnvironmentSanityCheck` |
| `v02-tooling` | T02 — Copilot, Claude y Alternativas | `CLAUDE.md`, `.github/copilot-instructions.md`, `.claude/settings.json` |
| `v04-domain` | T04 — Completions Pro | Modelo de dominio completo: `Order`, `Money`, `OrderItem`, `CustomerId`. `CompletionAntipatterns.java` con 5 bugs |
| `v05-chat` | T05 — Chat: Prompting Profesional | `LegacyOrderController.java` (200 líneas con lógica mezclada) para análisis con Chat |
| `v06-agent` | T06 — Agent Mode | `OrderMapper` con bugs corregidos tras la demo de revisión de diff |
| `v07-models` | T07 — Modelos de IA | `InventoryService` (race condition + HashMap) y `InventoryServiceFixed` |
| `v08-mcp` | T08 — MCP | `docker-compose.yml`, `db/schema.sql`, entidades JPA generadas via MCP |
| `v09-spring-boot` | T09 — Spring Boot Aplicado | `@Valid`, `GlobalExceptionHandler` RFC 7807, `@PageableDefault`, `@ConfigurationProperties` |
| `v10-microservices` | T10 — Microservicios | `shopflow-payments-stub` + `ResilientPaymentClient` (Circuit Breaker + Retry + Fallback) |
| `v11-hexagonal` | T11 — Arquitectura Hexagonal | Puertos `domain/port/in` y `domain/port/out` + tests ArchUnit en verde |
| `v12-ddd` | T12 — DDD con IA | `Order` rico con `confirm()`, `ship()`, `deliver()`, `cancel()` e invariantes |
| `v13-eda` | T13 — Event-Driven Architecture | `OutboxEventPublisher`, `NaiveEventPublisher`, módulo `shopflow-notifications` |
| `v14-testing` | T14 — Testing con IA | `FakeOrderTests` (antipatrón) y `OrderTest` con assertions precisas |
| `v15-refactoring` | T15 — Refactorización | `LegacyBillingService` (God Object) + `TaxCalculationService` extraído |
| `v16-security` | T16 — Seguridad con IA | `SecurityConfig` Spring Security 6 + `InsecureOrderController` con 4 vulnerabilidades OWASP |
| `v17-github` | T17 — Copilot en GitHub | PR template con checklist de seguridad y documentación de uso de IA |

### Ramas de ejercicio

| Rama | Tema | Tu misión |
|------|------|-----------|
| `exercise/topic-04` | T04 — Completions Pro | Completa `OrderItem.java` y `CustomerId.java` usando solo autocompletado con neighboring files |
| `exercise/topic-06` | T06 — Agent Mode | Propaga el campo `notes` en cascada usando el flujo brief→plan→execute→review |
| `exercise/topic-08` | T08 — MCP | Genera las entidades JPA leyendo `db/schema.sql` con el MCP de PostgreSQL |
| `exercise/topic-09` | T09 — Spring Boot | Añade `@Valid`, `GlobalExceptionHandler`, `Pageable` y `@ConfigurationProperties` |
| `exercise/topic-10` | T10 — Microservicios | Añade Circuit Breaker + Retry + Timeout + Fallback al `PaymentClient` |
| `exercise/topic-11` | T11 — Hexagonal | Refactoriza hasta que los 3 tests de `HexagonalArchitectureTest` pasen |
| `exercise/topic-12` | T12 — DDD | Enriquece `Order` con métodos semánticos e invariantes de dominio |
| `exercise/topic-13` | T13 — EDA | Implementa el Outbox Pattern y transforma `NaiveOrderEventConsumer` |
| `exercise/topic-14` | T14 — Testing | Reescribe `FakeOrderTests` con assertions que realmente protegen la lógica |
| `exercise/topic-15` | T15 — Refactorización | Extrae responsabilidades de `LegacyBillingService` (primero los tests de caracterización) |
| `exercise/topic-16` | T16 — Seguridad | Audita y corrige las 4 vulnerabilidades de `InsecureOrderController` |
| `exercise/topic-17` | T17 — GitHub | Encuentra los 3 bugs en `CancellationPolicy`, completa la feature y abre una PR |

---

## Flujo de trabajo recomendado para cada ejercicio

```bash
# 1. Descarga todas las ramas y tags
git fetch --all --tags

# 2. Ve al punto de partida del ejercicio
git checkout exercise/topic-XX

# 3. Crea tu propia rama de trabajo
git checkout -b mi-solucion/topic-XX

# 4. Haz el ejercicio...

# 5. Verifica que los tests pasan
mvn test -pl shopflow-orders

# 6. Si necesitas ver la solución del instructor
git checkout vXX-nombre
```

---

## Arrancar el entorno local

```bash
# Levantar PostgreSQL, Kafka y Redis
docker compose up -d

# Verificar que el entorno está bien configurado
mvn test -pl shopflow-orders -Dtest=EnvironmentSanityCheck

# Compilar todos los módulos
mvn compile
```

Requisitos previos: **Java 21**, **Maven 3.9+**, **Docker Desktop**.

---

## Material didáctico en el código

Algunos archivos son intencionalmente incorrectos — forman parte del material del curso:

| Archivo | Tema | Propósito |
|---------|------|-----------|
| `demo/BeforeAiExample.java` | T01 | Controlador pre-IA con SQL injection, lógica mezclada, antipatrones |
| `demo/CompletionAntipatterns.java` | T04 | 5 bugs generados por IA para identificar |
| `demo/inventory/InventoryService.java` | T07 | Race condition y HashMap en singleton |
| `infrastructure/rest/LegacyOrderController.java` | T05 | Controlador legacy para análisis con Chat |
| `infrastructure/messaging/NaiveEventPublisher.java` | T13 | Publicador sin Outbox (pierde eventos) |
| `notifications/NaiveOrderEventConsumer.java` | T13 | Consumidor sin DLQ ni ACK manual |
| `demo/billing/LegacyBillingService.java` | T15 | God Object para el ejercicio de refactorización |
| `infrastructure/rest/InsecureOrderController.java` | T16 | 4 vulnerabilidades OWASP para el ejercicio de auditoría |

> Estos archivos existen para que los analices, no para que los uses como referencia.
