---
description: Añade patrones de resiliencia (circuit breaker, retry, timeout, fallback) a un cliente HTTP o consumidor Kafka existente. Sigue el estándar Resilience4j + Spring Boot 3.4. Incluye configuración en application.yml y tests de resiliencia básicos.
---

Actúa como especialista en resiliencia para microservicios Spring Boot. Tu misión es añadir la capa de resiliencia necesaria a un componente existente sin alterar su lógica de negocio.

Si el usuario no ha indicado qué componente fortalecer, pregunta antes de continuar:
- ¿Es un cliente HTTP o un consumidor Kafka?
- ¿Cuál es la ruta del fichero?
- ¿Hay un comportamiento de fallback esperado definido? (ej: "si el servicio de pagos falla, devolver estado PENDING")

## Fase 1 — Análisis del componente

Lee el componente indicado y produce:
1. **Tipo de componente**: cliente HTTP / productor Kafka / consumidor Kafka.
2. **Operaciones expuestas a fallos externos**: lista de métodos que llaman a sistemas externos.
3. **Comportamiento actual ante fallo**: excepción propagada / sin manejo / fallback existente.
4. **Dependencias de resiliencia presentes en pom.xml**: Resilience4j, spring-kafka, etc. Si faltan, indícalo.

Muestra el análisis y espera confirmación:
**"¿Procedo con los cambios? (sí/no)"**

## Fase 2A — Resiliencia para clientes HTTP

Si el componente es un cliente HTTP, aplica en este orden:

1. **TimeLimiter**: añade configuración en `application.yml` bajo `resilience4j.timelimiter.instances.<nombre>` con `timeoutDuration`.
2. **Retry**: añade configuración con `maxAttempts`, `waitDuration` y `enableExponentialBackoff`.
3. **CircuitBreaker**: añade configuración con `slidingWindowSize`, `failureRateThreshold`, `waitDurationInOpenState`.
4. **Anotaciones**: decora los métodos que llaman al exterior con `@CircuitBreaker`, `@Retry` y `@TimeLimiter` de Resilience4j. El orden de las anotaciones importa: CircuitBreaker > Retry > TimeLimiter.
5. **Fallback**: añade el método `fallback[NombreOriginal](Throwable)` con el comportamiento de degradación indicado por el usuario. Si no hay comportamiento definido, pregunta antes de inventar uno.

Restricción: no uses `spring-cloud-starter-circuitbreaker-resilience4j`. Usa `resilience4j-spring-boot3` directamente.

## Fase 2B — Resiliencia para consumidores Kafka

Si el componente es un consumidor Kafka, aplica en este orden:

1. **AckMode manual**: configura `spring.kafka.listener.ack-mode: manual` o `MANUAL_IMMEDIATE` en `application.yml`.
2. **ACK explícito**: añade el parámetro `Acknowledgment ack` al método listener y llama a `ack.acknowledge()` solo después de procesar con éxito.
3. **DLQ**: configura `DeadLetterPublishingRecoverer` con el topic `<topic-original>-dlq` en la configuración de Kafka.
4. **Idempotencia**: si el componente no tiene verificación de `eventId` ya procesado, añade la lógica usando una tabla `processed_event` (o un `Set` en memoria si es suficiente para el contexto del ejercicio).
5. **Error handler**: configura `DefaultErrorHandler` con `FixedBackOff(200L, 3)` para reintentos limitados antes de enviar a DLQ.

## Fase 3 — Tests de resiliencia

Genera al menos dos tests:

Para clientes HTTP:
- Test que simula fallo del servidor externo (WireMock o `@MockBean`) y verifica que se invoca el fallback.
- Test que simula timeout y verifica que el circuit breaker registra el fallo.

Para consumidores Kafka:
- Test que envía un mensaje válido y verifica ACK + idempotencia (segundo envío del mismo `eventId` no produce side effect).
- Test que envía un mensaje que provoca excepción y verifica que acaba en DLQ tras los reintentos.

Ejecuta `mvn test -pl <módulo>` y reporta resultado.

## Fase 4 — Auditoría final

Lanza el subagente `spring-resilience-reviewer` sobre los ficheros modificados.
Muestra el informe al usuario. Corrige los hallazgos 🔴 Bloqueantes antes de terminar.

## Informe final

- Ficheros modificados y cambios aplicados.
- Configuración añadida a `application.yml`.
- Tests generados y resultado.
- Hallazgos del revisor: resueltos y pendientes.