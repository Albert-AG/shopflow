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

`OrderService` viola la Regla de Dependencia de Puertos y Adaptadores:

| Violación | Dónde |
|---|---|
| Importa `JpaOrderRepository` directamente desde `infrastructure.persistence` | `OrderService.java` |
| No implementa los puertos `CreateOrderUseCase` / `GetOrderUseCase` | `OrderService.java` |
| `@Service` y `@Transactional` en el paquete `application` pero el servicio tiene lógica de dominio | `OrderService.java` |

Los tests que fallan:

```bash
mvn test -pl shopflow-orders -Dtest=HexagonalArchitectureTest
# domainMustNotDependOnInfrastructure — FAILS
# domainMustNotUseSpringAnnotations — FAILS
# infrastructureMustAccessDomainOnlyThroughPorts — FAILS
```

Los puertos ya existen en la rama (están como referencia):
- `domain/port/in/CreateOrderUseCase.java`
- `domain/port/in/GetOrderUseCase.java`
- `domain/port/out/OrderRepository.java` (desde T04)

Tu misión: conectar todo para que los tests pasen.

---

## Ejercicio — El Desafío del Hexágono

Usa Agent Mode para refactorizar en 3 pasos. **Muestra el plan del agente antes de ejecutarlo**
en cada paso — no apliques cambios sin revisarlos.

### Paso 1 — Crea el adaptador de persistencia

Crea `JpaOrderRepositoryAdapter` en `infrastructure/persistence/` que:
- Implemente `OrderRepository` (el puerto de salida)
- Use `JpaOrderRepository` internamente
- Mapee entre `Order` (dominio) y `OrderEntity` (JPA)

Verifica que `Order` (dominio) no tiene ninguna anotación de Spring o JPA.

```bash
mvn compile -pl shopflow-orders -q   # debe compilar sin errores
```

### Paso 2 — Crea el servicio de aplicación

Crea (o refactoriza) `OrderApplicationService` en `application/` que:
- Implemente `CreateOrderUseCase` y `GetOrderUseCase`
- Reciba `OrderRepository` por constructor (el puerto, no la implementación JPA)
- Tenga `@Service` y `@Transactional` (Spring solo en la capa de aplicación)

Verifica que `OrderApplicationService` no importa ninguna clase de `infrastructure.*`.

```bash
mvn compile -pl shopflow-orders -q
```

### Paso 3 — Conecta los adaptadores

Actualiza la configuración de Spring para que inyecte `JpaOrderRepositoryAdapter`
donde se necesite `OrderRepository`.

```bash
mvn test -pl shopflow-orders -Dtest=HexagonalArchitectureTest
# Los tres tests deben pasar
mvn test -pl shopflow-orders
# Todos los tests deben pasar
```

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

---

## Solución de referencia

```bash
git checkout v11-hexagonal
mvn test   # todos los tests en verde
```

Consúltala solo después de que los tres tests de ArchUnit pasen en tu rama.
