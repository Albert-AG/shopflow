# Tema 10 — Microservicios con Spring: Patrones Reales

## 🎯 Objetivo

Transformar un `PaymentClient` frágil en un cliente indestructible añadiendo las cuatro
capas de resiliencia con Resilience4j: timeout, retry, circuit breaker y fallback.
El stub de pagos falla aleatoriamente — igual que en producción.

---

## Punto de partida

```bash
git checkout exercise/topic-10
git checkout -b mi-solucion/topic-10
docker-compose up -d   # levanta postgres + payments-stub
mvn test -pl shopflow-orders   # debe pasar el EnvironmentSanityCheck
```

El módulo `shopflow-payments-stub` ya está levantado. Simula un servicio de pagos real:
- 20% de requests → `503 Service Unavailable`
- 30% de requests → delay aleatorio de 3-8 segundos
- 50% de requests → respuesta normal (15% declined, 85% approved)

---

## El problema

`PaymentClient` hace llamadas REST directas sin ninguna protección:

```java
// PaymentClient.java — estado actual (frágil)
public PaymentResult processPayment(PaymentRequest request) {
    return restClient.post()
        .uri("/payments")
        .body(request)
        .retrieve()
        .body(PaymentResult.class);
    // Sin timeout: si el stub tarda 8s, tu thread queda bloqueado 8s
    // Sin retry: un 503 transitorio falla toda la orden
    // Sin circuit breaker: si el stub cae, cada request sigue bloqueando un thread
    // Sin fallback: el único resultado posible es éxito o excepción no controlada
}
```

Observa el problema antes de modificar nada:

```bash
mvn test -pl shopflow-orders -Dtest=PaymentClientResilienceTest
# Algunos threads quedan colgados. El test tarda mucho más de lo esperado.
```

---

## Ejercicio — El Cliente Indestructible

Crea `ResilientPaymentClient` que envuelva al `PaymentClient` original con las cuatro capas.
Usa Resilience4j directamente (no Spring Cloud Circuit Breaker).

### Capa 1 — Timeout

Añade un `TimeLimiter` de 2 segundos. Si el stub no responde en 2 segundos,
la llamada debe lanzar una excepción, no bloquear el thread.

### Capa 2 — Retry con exponential backoff

Añade un `Retry` con 3 intentos máximos y backoff exponencial (500ms base).
Solo reintenta en errores de red y 503. No reintenta en 400 ni 422.

### Capa 3 — Circuit Breaker

Añade un `CircuitBreaker` que abra tras 3 fallos consecutivos. Cuando esté abierto,
las llamadas deben fallar inmediatamente (sin llegar al stub) durante 30 segundos.

### Capa 4 — Fallback

Cuando falle todo lo anterior (timeout, reintentos agotados, circuito abierto),
el método debe devolver un `PaymentResult` con estado `PENDING` en lugar de lanzar excepción.
El pedido se procesa igualmente — el pago se reintentará en un job posterior.

**Configuración Resilience4j en `application.properties`:**
```properties
resilience4j.circuitbreaker.instances.payment.slidingWindowSize=10
resilience4j.circuitbreaker.instances.payment.failureRateThreshold=50
resilience4j.circuitbreaker.instances.payment.waitDurationInOpenState=30s
resilience4j.retry.instances.payment.maxAttempts=3
resilience4j.retry.instances.payment.waitDuration=500ms
resilience4j.timelimiter.instances.payment.timeoutDuration=2s
```

> ⚠️ Usa `@Primary` en `ResilientPaymentClient` para que Spring lo inyecte
> en lugar del `PaymentClient` original. El `PaymentClient` original no se modifica.

---

## Verificación

```bash
mvn test -pl shopflow-orders -Dtest=PaymentClientResilienceTest
# Resultado esperado:
# - Ningún thread queda bloqueado más de 2 segundos
# - Los 503 transitorios se reintentan automáticamente
# - El fallback devuelve PaymentResult con estado PENDING
```

Prueba manual con el stub levantado:
```bash
# Crea 10 órdenes y observa cómo el circuit breaker las gestiona
for i in {1..10}; do
  curl -s -X POST http://localhost:8080/orders \
    -H "Content-Type: application/json" \
    -d '{"customerId":"cust-1","items":[{"productId":"prod-1","quantity":2}]}' | jq .status
done
```

---

## Ejercicio Plugin — Construye `spring-observability-reviewer` y `/audit-contracts`

> Este ejercicio es adicional y complementa el ejemplo guiado del tema.

### Parte A — Subagente `spring-observability-reviewer`

Crea `.claude/agents/spring-observability-reviewer.md`. Audita el microservicio en busca de
anti-patrones de observabilidad en cuatro categorías:

- **Logs**: ausencia de `traceId`/`correlationId` en operaciones críticas, datos sensibles
  en logs, `System.out.println` o `e.printStackTrace()`, logs en bucles de alta frecuencia.
- **Métricas**: ausencia de contadores para operaciones de negocio (`orders.created`,
  `payments.authorized`), ausencia de timer en endpoints con SLO, ausencia de métrica
  para el fallback del circuit breaker.
- **Trazas**: `traceId` no propagado en llamadas HTTP salientes ni en consumers Kafka.
- **SLOs**: ausencia de `@Timed` en endpoints con SLO, métricas no publicadas en actuator/prometheus.

El subagente devuelve tabla de hallazgos por categoría con severidad y corrección concreta.

### Parte B — Skill `/audit-contracts`

Crea `.claude/commands/audit-contracts.md`. Recibe dos versiones de un contrato OpenAPI
(o un diff de DTO) y clasifica cada cambio como `compatible`, `potencialmente incompatible`
o `breaking change`. Si hay cambios breaking, propone un plan de migración con dos
versiones conviviendo.

**Verificación:**
```bash
# Invoca spring-observability-reviewer sobre shopflow-orders
# Debe detectar ausencia de métricas en el fallback del PaymentClient

# Crea un contrato v2 de orders con un campo requerido añadido
# y verifica que la skill lo clasifica como breaking
/audit-contracts shopflow-orders/src/main/resources/openapi.yaml docs/openapi-v2.yaml
```

Documenta en el commit:
- ¿Qué hallazgo de observabilidad consideras más urgente y por qué?
- ¿Cuál es el cambio breaking más difícil de detectar sin herramienta automatizada?

---

## Criterios de éxito ✅

- `PaymentClientResilienceTest` en verde sin threads bloqueados ✅
- Fallback devuelve `PaymentResult(PENDING)` cuando se agota el retry ✅
- Circuit breaker se abre tras 3 fallos y rechaza inmediatamente durante 30s ✅
- Configuración en `application.properties`, no hardcodeada en el código ✅
- `mvn test -pl shopflow-orders` en verde ✅

---

## Solución de referencia

```bash
git checkout v10-microservices
```

Contiene `ResilientPaymentClient` completo con las cuatro capas. Consúltala solo
después de intentar el ejercicio por tu cuenta.
