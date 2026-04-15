# Tema 9 — Copilot Aplicado a Spring Boot: Productividad sin Romper Estándares

## 🎯 Objetivo

Añadir cinco capas de calidad a un `OrderController` funcional pero básico usando Chat y completions.
El controller ya funciona y pasa el test de sanidad — tu misión es llevarlo a estándar de producción
sin romper nada.

---

## Punto de partida

```bash
git checkout exercise/topic-09
git checkout -b mi-solucion/topic-09
docker-compose up -d postgres
mvn test -pl shopflow-orders   # debe pasar el EnvironmentSanityCheck
```

El `OrderController` hace CRUD básico. Compila. El test de sanidad pasa.
Pero tiene cinco problemas concretos que debes resolver.

---

## El estado actual

| Archivo | Problema |
|---|---|
| `OrderController.java` | Sin `@Valid` — input inválido devuelve `500` genérico |
| `OrderController.java` | `findAll()` sin `Pageable` — devuelve todos los registros |
| `OrderController.java` | Sin MDC — logs sin `traceId` ni `orderId` |
| `GlobalExceptionHandler.java` | **No existe** — debes crearlo desde cero |
| `ShopflowProperties.java` | **No existe** — configuración hardcodeada en el controller |

Prueba el estado inicial antes de cambiar nada:

```bash
# Muestra que input inválido devuelve 500 sin cuerpo estructurado
curl -s -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{}' | jq .

# Muestra que findAll devuelve todo sin paginación
curl -s http://localhost:8080/orders | jq 'length'
```

---

## Ejercicio — Las 5 capas de calidad

Usa Chat y completions para añadir cada capa. Compila y ejecuta los tests después de cada paso.

### Capa 1 — Validación de entrada

Añade `@NotNull`, `@Size` y `@Min` donde corresponda en `CreateOrderRequest`.
Fuerza que `OrderController.createOrder()` use `@Valid @RequestBody`.

**Verificación:**
```bash
curl -s -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
# Resultado esperado: 400 con mensaje de validación, no 500
```

### Capa 2 — Manejo de errores con RFC 7807

Crea `GlobalExceptionHandler` en `infrastructure/rest/` con `@RestControllerAdvice`.
Debe manejar al menos:
- `OrderNotFoundException` → `404` con `ProblemDetail`
- `MethodArgumentNotValidException` → `400` con `ProblemDetail` listando los campos inválidos
- `IllegalStateException` → `409` con `ProblemDetail`

**Verificación:**
```bash
curl -s http://localhost:8080/orders/non-existent-id | jq .
# Resultado esperado: {"type":"...", "status":404, "title":"Not Found", "detail":"..."}
```

### Capa 3 — Paginación en GET /orders

Reemplaza `findAll()` por `findAll(Pageable pageable)` en el controller.
El servicio y el repositorio deben aceptar también `Pageable`.

**Verificación:**
```bash
curl -s "http://localhost:8080/orders?page=0&size=5&sort=createdAt,desc" | jq .
# Resultado esperado: objeto Page con content, totalElements, totalPages
```

### Capa 4 — Logging estructurado con MDC

Añade al controller un filtro o interceptor que incluya `traceId` (UUID generado por request)
y `orderId` (cuando esté disponible) en el MDC de cada operación.

**Verificación:**
```bash
# Crea una orden y observa el log en la consola
# Debe aparecer: traceId=xxx orderId=yyy en la misma línea del log
```

### Capa 5 — Configuración externalizada

Crea `ShopflowProperties` con `@ConfigurationProperties(prefix = "shopflow.orders")` para:
- `maxItemsPerOrder` (default: 50)
- `defaultPageSize` (default: 20)
- `pendingOrderTtlHours` (default: 48)

Añade las propiedades al `application.properties`. Usa `ShopflowProperties` en el controller
en lugar de los valores hardcodeados.

---

## Ejercicio Plugin — Construye `spring-persistence-reviewer` y `/harden-service`

> Este ejercicio es adicional y complementa el ejemplo guiado del tema.

### Parte A — Subagente `spring-persistence-reviewer`

Crea `.claude/agents/spring-persistence-reviewer.md`. Este subagente es especialista en
problemas de persistencia y concurrencia. Al pasarle un módulo Spring Boot analiza:

- `findAll()` sin `Pageable` en repositorios Spring Data
- Ausencia de `@EntityGraph` en relaciones `@OneToMany`/`@ManyToMany` (N+1)
- `@Transactional` mal colocado (métodos privados, controladores, llamadas HTTP dentro de transacción)
- Entidades con campos susceptibles de contención sin `@Version`
- Queries que devuelven entidades completas cuando una proyección sería suficiente

El subagente devuelve: tabla de hallazgos por repositorio, consulta SQL estimada para cada
problema, y recomendación concreta.

### Parte B — Skill `/harden-service`

Crea `.claude/commands/harden-service.md`. Esta skill recibe la ruta de un servicio existente
y le aplica las mejoras del tema sin tocar la lógica de negocio:
validaciones, errores RFC 7807, paginación, `@ConfigurationProperties`, y MDC.

Al terminar ejecuta `mvn test -pl <módulo>` e invoca `spring-boot-reviewer` sobre
los ficheros modificados.

**Verificación:**
```bash
# Invoca el subagente sobre el estado inicial del ejercicio
# Debe detectar: findAll() sin Pageable y ausencia de @Version en OrderJpaEntity

# Prueba la skill
/harden-service shopflow-orders/src/main/java/com/shopflow/orders/application/OrderService.java
```

Documenta en el commit:
- ¿Qué halló el subagente que no habías detectado manualmente?
- ¿Qué capa de `/harden-service` produjo más cambios en el código?

---

## Criterios de éxito ✅

- `POST /orders` con body vacío devuelve `400` con `ProblemDetail` ✅
- `GET /orders` acepta `?page=0&size=5` y devuelve un objeto `Page` ✅
- Los logs incluyen `traceId` en todas las operaciones ✅
- `ShopflowProperties` inyectada en el controller, sin valores hardcodeados ✅
- `mvn test -pl shopflow-orders` en verde ✅

---

## Solución de referencia

```bash
git checkout v09-spring-boot
```

Contiene las cinco capas implementadas. Consúltala solo después de completar el ejercicio.
