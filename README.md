# Tema 10 — Microservicios con Spring: Patrones Reales

## 🎯 Objetivo

Usar las herramientas del plugin del curso para detectar y corregir los problemas de resiliencia
en `PaymentClient`. El subagente diagnostica; la skill aplica los patrones; los tests verifican.

---

## Punto de partida

```bash
git checkout exercise/topic-10
git checkout -b mi-solucion/topic-10
docker-compose up -d          # levanta postgres + payments-stub (puerto 8081)
mvn test -pl shopflow-orders  # EnvironmentSanityCheck debe estar en verde
```

El `payments-stub` simula un servicio de pagos real con fallos aleatorios:
- 20% de requests → `503 Service Unavailable`
- 30% de requests → delay aleatorio de 3-8 segundos
- 50% de requests → respuesta normal (15% declined, 85% approved)

---

## El problema

`PaymentClient` hace llamadas REST directas sin ninguna protección:

```java
// PaymentClient.java — estado actual (frágil)
public PaymentResult processPayment(UUID orderId, BigDecimal amount) {
    Map<String, Object> request = Map.of(
            "orderId", orderId.toString(),
            "amount", amount.toPlainString());

    Map<?, ?> response = restClient.post()
            .uri("/api/payments/process")
            .body(request)
            .retrieve()
            .body(Map.class);

    return new PaymentResult((String) response.get("paymentId"), orderId,
            "APPROVED".equals(response.get("status")));
    // Sin timeout: si el stub tarda 8s, tu thread queda bloqueado 8s
    // Sin retry: un 503 transitorio falla toda la orden
    // Sin circuit breaker: si el stub cae, cada request sigue bloqueando un thread
    // Sin fallback: el único resultado posible es éxito o excepción no controlada
}
```

También existe `ResilientPaymentClient`, que envuelve a `PaymentClient` y está anotada con
`@Primary`, pero su implementación es un esqueleto vacío que todavía delega directamente
sin aplicar ninguna capa de resiliencia.

Confirma el estado actual antes de empezar:

```bash
mvn test -pl shopflow-orders -Dtest=PaymentClientResilienceTest
```

Verás 5 tests fallando. El de timeout tarda 5 segundos — el thread queda bloqueado
porque no hay `TimeLimiter`. Los demás lanzan `ServiceUnavailableException` porque no
hay `Retry` ni fallback.

---

## Ejercicio

### Paso 1 — Diagnóstico con `spring-resilience-reviewer`

Invoca el subagente sobre `PaymentClient.java`:

```
@spring-resilience-reviewer Revisa el fichero shopflow-orders/src/main/java/com/shopflow/orders/infrastructure/payments/PaymentClient.java
```

El subagente debe detectar como mínimo estos cuatro problemas:

| Problema | Por qué importa |
|---|---|
| Sin timeout | Un stub lento bloquea el thread indefinidamente |
| Sin `CircuitBreaker` | Si el stub cae, cada request sigue intentándolo hasta agotar el pool |
| Sin `Retry` | Un 503 transitorio falla la orden completa en lugar de reintentarse |
| Sin fallback | La excepción se propaga al caller; no hay degradación controlada |

> Si el subagente no detecta alguno de estos problemas, documéntalo — es un hallazgo
> sobre la calidad del subagente, no un error tuyo.

### Paso 2 — Solución con `/add-resilience`

Ejecuta la skill sobre `ResilientPaymentClient`:

```
/add-resilience
```

Cuando la skill pregunte:
- **¿Qué componente fortalecer?** → `ResilientPaymentClient.java`
- **¿Comportamiento de fallback?** → "Si el pago falla por timeout, retry agotado o circuito abierto, devolver `PaymentResult` con `paymentId = "PENDING"` y `approved = false`. El pedido queda en estado pendiente y el pago se reintentará más tarde."

La skill te mostrará su análisis y pedirá confirmación antes de aplicar cambios.
**Revisa el plan antes de decir que sí** — ese es el punto: tú eres el Senior que valida.

La configuración de Resilience4j ya está en `application.properties`. La skill no
necesita añadirla, solo debe implementar la lógica en `ResilientPaymentClient`.

### Paso 3 — Verificación

```bash
mvn test -pl shopflow-orders -Dtest=PaymentClientResilienceTest
# Resultado esperado: 5/5 tests en verde
#   ✅ Timeout: completa en <2.5s con fallback PENDING
#   ✅ Retry: dos 503 transitorios seguidos de éxito → approved=true
#   ✅ Fallback: reintentos agotados → PENDING sin excepción
#   ✅ CircuitBreaker: tras varios fallos, llamadas rechazadas en <200ms
#   ✅ No retry en 422: exactamente una llamada al delegate
```

Suite completa:

```bash
mvn test -pl shopflow-orders
```

---

## Prueba manual con el stub levantado

Con `docker-compose up -d` activo, crea 10 órdenes consecutivas y observa cómo
el circuit breaker gestiona los fallos aleatorios del stub:

```bash
for i in {1..10}; do
  curl -s -X POST http://localhost:8080/api/v1/orders \
    -H "Content-Type: application/json" \
    -d '{"customerId":"11111111-1111-1111-1111-111111111111","items":[{"productId":"22222222-2222-2222-2222-222222222222","quantity":2,"unitPrice":"10.00"}]}' \
    | jq '{status: .status, paymentId: .paymentId}'
done
```

Deberías ver una mezcla de `APPROVED`, `DECLINED` y `PENDING` — este último indica
que el fallback se disparó y el pago se procesará más tarde.

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

```
# Invoca spring-observability-reviewer sobre shopflow-orders
# Debe detectar ausencia de métricas en el fallback del PaymentClient
```

Documenta en el commit:
- ¿Qué hallazgo de observabilidad consideras más urgente y por qué?

---

## Criterios de éxito ✅

- `PaymentClientResilienceTest` 5/5 en verde ✅
- Fallback devuelve `PaymentResult(PENDING)` cuando se agota el retry ✅
- Circuit breaker rechaza llamadas inmediatamente tras varios fallos ✅
- Configuración en `application.properties`, no hardcodeada en el código ✅
- `mvn test -pl shopflow-orders` en verde ✅

---

## Solución de referencia

```bash
git checkout v10-microservices
```

Contiene `ResilientPaymentClient` completo con las cuatro capas. Consúltala solo
después de intentar el ejercicio por tu cuenta.
