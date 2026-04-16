# Tema 11 — Arquitectura Hexagonal con Copilot

## 🎯 Objetivo

Refactorizar `shopflow-orders` hacia arquitectura hexagonal en tres pasos con Agent Mode.
El criterio de éxito es objetivo: los tests de ArchUnit deben pasar.

---

## Punto de partida

```bash
git checkout exercise/topic-11
git checkout -b mi-solucion/topic-11
docker-compose up -d postgres
mvn test -pl shopflow-orders -Dtest=HexagonalArchitectureTest
```

Los tests de ArchUnit **fallan**. Eso es correcto — es el punto de partida del ejercicio.

---

## El problema

Hay tres violaciones de la Regla de Dependencia que rompen los tests de ArchUnit:

| Test que falla | Violación | Clase responsable |
|---|---|---|
| `domainMustNotDependOnInfrastructure` | `OrderDomainService` (capa `domain`) importa `JpaOrderRepository` y `OrderEntity` de la capa `infrastructure` | `domain/service/OrderDomainService.java` |
| `domainMustNotUseSpringAnnotations` | `OrderDomainService` tiene `@Service` y `@Transactional` — anotaciones de framework en la capa de dominio | `domain/service/OrderDomainService.java` |
| `restAdapterMustDependOnPorts` | `OrderController` importa `OrderService` (clase concreta) y `CreateOrderCommand` (ambos en `application.*`) en lugar de depender solo de los puertos | `infrastructure/rest/OrderController.java` |

> **Nota**: `CreateOrderCommand` está en `application/command/`, pero es parte del contrato del puerto de entrada — pertenece a `domain/port/in/`. Mientras permanezca en `application.*`, `OrderController` seguirá violando la regla aunque elimines la dependencia de `OrderService`.

```bash
mvn test -pl shopflow-orders -Dtest=HexagonalArchitectureTest
# domainMustNotDependOnInfrastructure — FAILS
# domainMustNotUseSpringAnnotations — FAILS
# restAdapterMustDependOnPorts — FAILS
```

Los puertos ya existen en la rama como referencia:
- `domain/port/in/CreateOrderUseCase.java`
- `domain/port/in/GetOrderUseCase.java`
- `domain/port/out/OrderRepository.java`

Tu misión: conectar todo para que los tres tests pasen.

---

## Ejercicio — El Desafío del Hexágono

Usa Agent Mode para refactorizar paso a paso. **Muestra el plan del agente antes de ejecutarlo**
en cada paso — no apliques cambios sin revisarlos.

### Paso 1 — Elimina la clase con violaciones de dominio

Elimina `OrderDomainService` de `domain/service/`. Esta clase tiene `@Service`, `@Transactional`
e importa `JpaOrderRepository` y `OrderEntity` — todo ello viola las dos primeras reglas.
Su lógica ya existe en `OrderService` (capa `application`), que es donde corresponde.

```bash
mvn test -pl shopflow-orders -Dtest=HexagonalArchitectureTest
# domainMustNotDependOnInfrastructure — debe pasar ahora
# domainMustNotUseSpringAnnotations — debe pasar ahora
# restAdapterMustDependOnPorts — todavía falla
```

### Paso 2 — Crea el adaptador de persistencia y el servicio de aplicación

**2a.** Crea `JpaOrderRepositoryAdapter` en `infrastructure/persistence/` que:
- Implemente el puerto `OrderRepository` (en `domain/port/out/`)
- Use `JpaOrderRepository` internamente
- Mapee entre `Order` (dominio) y `OrderEntity` (JPA)

**2b.** Crea `OrderApplicationService` en `application/` que:
- Implemente `CreateOrderUseCase` y `GetOrderUseCase`
- Reciba `OrderRepository` (el puerto, no `JpaOrderRepository`) por constructor
- Tenga `@Service` y `@Transactional` en la capa de aplicación — correcto

Verifica que `OrderApplicationService` no importa ninguna clase de `infrastructure.*`.

```bash
mvn compile -pl shopflow-orders -q   # debe compilar sin errores
```

### Paso 3 — Mueve `CreateOrderCommand` al paquete de puertos

`CreateOrderCommand` pertenece al contrato del puerto de entrada, no a la capa de
aplicación. Muévelo de `application/command/` a `domain/port/in/` y actualiza todos
los imports (`CreateOrderUseCase`, `OrderService`, `OrderController`).

```bash
mvn compile -pl shopflow-orders -q   # debe compilar sin errores
```

### Paso 4 — Crea `CancelOrderUseCase` y completa el servicio de aplicación

`OrderController` también tiene un endpoint `cancelOrder` que usa `OrderService`.
Sin un puerto para cancelar, el controlador nunca podrá dejar de depender de `application.*`.

**4a.** Crea `CancelOrderUseCase` en `domain/port/in/`:

```java
public interface CancelOrderUseCase {
    Order cancel(UUID id);
}
```

**4b.** Haz que `OrderApplicationService` implemente también `CancelOrderUseCase`.

```bash
mvn compile -pl shopflow-orders -q   # debe compilar sin errores
```

### Paso 5 — Conecta el REST adapter con los puertos

Refactoriza `OrderController` para que inyecte `CreateOrderUseCase`, `GetOrderUseCase`
y `CancelOrderUseCase` en lugar de `OrderService`. Con este cambio, `infrastructure.rest`
ya no depende de `application.*` y el tercer test pasa.

Usa `OrderMapper` para convertir los objetos `Order` (dominio) a `OrderResponse` (DTO).

```bash
mvn test -pl shopflow-orders -Dtest=HexagonalArchitectureTest
# Los tres tests deben pasar ✅
mvn test -pl shopflow-orders
# Todos los tests en verde ✅
```

---

## Compara: solución manual vs. skill

Ya tienes los tres tests en verde en `mi-solucion/topic-11`. Ahora repite el diagnóstico
y la refactorización usando las herramientas del plugin del curso — sin conversación libre con
el agente — y compara si obtienes el mismo resultado.

```bash
git checkout exercise/topic-11
git checkout -b mi-solucion/topic-11-skill
```

1. Invoca el subagente `@spring-architecture-guard` sobre el módulo `shopflow-orders`.
   Anota qué violaciones detecta y en qué clases.

2. Ejecuta `/audit-architecture` para que el agente aplique las correcciones.
   Sigue el plan que proponga antes de aprobar cada paso.

3. Verifica que los tres tests pasan:

```bash
mvn test -pl shopflow-orders -Dtest=HexagonalArchitectureTest
mvn test -pl shopflow-orders
```

4. Compara ambas ramas:

```bash
git diff mi-solucion/topic-11 mi-solucion/topic-11-skill
```

¿El skill llega a la misma solución? ¿Detecta las mismas violaciones? ¿En qué difiere
el camino (pasos, orden, código generado)?

---

## Ejercicio Plugin — Construye `spring-domain-purity-checker` y `/enforce-ports`

> Este ejercicio es adicional y complementa el ejemplo guiado del tema.

### Parte A — Subagente `spring-domain-purity-checker`

Crea `.claude/agents/spring-domain-purity-checker.md`. Especialista en verificar que
las clases de `domain/model/` son Java puro sin dependencias de framework.

Verifica para cada clase de dominio:
- Inmutabilidad: campos `final` o mutación solo mediante métodos con nombre de intención de negocio
- Factory methods: creación vía `create()`, reconstrucción vía `reconstitute()`
- Value Objects: tipos sin identidad propia implementados como `record` con validación
- Excepciones de dominio propias (no `RuntimeException` genérico)
- Ausencia de tipos de infraestructura en firmas de métodos (ej: `Page<Order>` de Spring Data)

Responde con tabla de hallazgos por clase y puntuación de pureza (0–10).

### Parte B — Skill `/enforce-ports`

Crea `.claude/commands/enforce-ports.md`. Recibe el nombre de un recurso de dominio
y genera la estructura completa de puertos y adaptadores en el orden correcto:
puertos de entrada → puerto de salida → servicio de aplicación → adaptador de persistencia.
Verifica la Regla de Dependencia en cada fichero antes de escribirlo e invoca
`spring-architecture-guard` al terminar.

**Verificación:**
```bash
# Compara la puntuación de spring-domain-purity-checker entre este estado
# (exercise/topic-11) y v11-hexagonal
# Resultado esperado: puntuación más baja aquí por setters y RuntimeException genérico

# Genera la estructura para el recurso Customer
/enforce-ports Customer
# spring-architecture-guard debe dar veredicto limpio
```

Documenta en el commit:
- ¿En qué se diferencia el informe de `spring-domain-purity-checker` entre los dos estados?
- ¿Qué paso de `/enforce-ports` tiene mayor riesgo de violación de la Regla de Dependencia?

---

## Criterios de éxito ✅

```bash
mvn test -pl shopflow-orders -Dtest=HexagonalArchitectureTest
# Los tres tests de ArchUnit pasan ✅

mvn test -pl shopflow-orders
# Todos los tests en verde ✅
```

- `OrderApplicationService` no importa nada de `infrastructure.*` ✅
- `Order` (dominio) no tiene anotaciones de Spring ni JPA ✅
- `JpaOrderRepositoryAdapter` implementa `OrderRepository` y usa `JpaOrderRepository` internamente ✅
- `CreateOrderCommand` reside en `domain/port/in/`, no en `application/command/` ✅
- `OrderController` solo importa de `domain.*` e `infrastructure.*` — cero imports de `application.*` ✅

---

## Solución de referencia

```bash
git checkout v11-hexagonal
mvn test   # todos los tests en verde
```

Consúltala solo después de que los tres tests de ArchUnit pasen en tu rama.
