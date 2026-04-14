# Tema 6 — Agent Mode: Ejercicios

Rama de trabajo: `exercise/topic-06`

---

## Ejercicio 1 — Propagación en cascada con Agent Mode

### Contexto

El modelo de dominio tiene el record `OrderItem` con tres campos: `productId`, `quantity` y `unitPrice`. El equipo ha decidido añadir un campo `notes` (String, nullable) para que quien crea el pedido pueda adjuntar una observación a cada línea de pedido (por ejemplo: *"envolver para regalo"*, *"sin gluten"*).

El campo debe propagarse en cascada por toda la capa REST: desde el DTO de entrada hasta el DTO de respuesta, pasando por el mapper y el controlador si fuera necesario.

**Archivos relevantes:**

| Archivo | Rol |
|---------|-----|
| `shopflow-orders/src/main/java/com/shopflow/orders/domain/model/OrderItem.java` | Record de dominio — aquí añades el campo |
| `shopflow-orders/src/main/java/com/shopflow/orders/infrastructure/rest/dto/CreateOrderRequest.java` | DTO de entrada — `OrderItemRequest` anidado |
| `shopflow-orders/src/main/java/com/shopflow/orders/infrastructure/rest/dto/OrderResponse.java` | DTO de salida — `OrderItemResponse` anidado |
| `shopflow-orders/src/main/java/com/shopflow/orders/infrastructure/rest/OrderMapper.java` | Mapper entre dominio y DTOs |

### Protocolo obligatorio

Sigue los cuatro pasos en este orden. No saltes ninguno.

**Paso 1 — Brief**

Escribe el brief *antes* de abrir el agente. El brief tiene tres partes obligatorias:

```
## Objetivo
[Qué quieres que haga el agente]

## Restricciones
[Qué NO puede hacer]

## Criterio de éxito
[Cómo vas a validar que está correcto]
```

Ejemplo de brief para este ejercicio:

```
## Objetivo
Añade el campo `notes` (String, nullable) a `OrderItem` y propágalo
en cascada: CreateOrderRequest.OrderItemRequest → OrderMapper → OrderResponse.OrderItemResponse.

## Restricciones
- No toques Order.java ni OrderController.java
- Si OrderItem necesita cambio en el compact constructor, valida que notes puede ser null
- No añadas dependencias nuevas al pom.xml
- Respeta el estilo del proyecto: records para DTOs, sin Lombok en dominio

## Criterio de éxito
- mvn test pasa sin errores tras los cambios
- El campo notes aparece en la respuesta del endpoint POST /api/v1/orders
- El campo es opcional en el request (acepta null sin error)
```

**Paso 2 — Plan**

Antes de ejecutar ningún cambio, pide el plan al agente:

```
Antes de hacer ningún cambio, muéstrame:
1. Qué archivos vas a crear
2. Qué archivos vas a modificar y qué cambio exacto vas a hacer en cada uno
3. Qué archivos vas a leer pero NO modificar

No ejecutes hasta que yo te diga "adelante".
```

Valida el plan: ¿ha listado solo los archivos del scope? ¿ha incluido alguno que no debería tocar?

**Paso 3 — Ejecutar**

Da el "adelante". No modifiques archivos manualmente mientras el agente trabaja.

**Paso 4 — Revisar el diff**

Abre `Git > Commit` en IntelliJ y revisa el diff archivo por archivo usando este checklist:

| Pilar | Qué mirar | Señal de alerta |
|-------|-----------|-----------------|
| **Invariantes de dominio** | ¿El compact constructor de `OrderItem` sigue validando los campos obligatorios? | El agente eliminó o relajó las validaciones existentes |
| **Seguridad** | ¿Se ha añadido algún log con el contenido de `notes`? | `log.info(... notes ...)` — podría contener datos sensibles |
| **Rendimiento** | ¿El mapper sigue usando `.toList()` (inmutable) para las listas? | El agente cambió a `Collectors.toList()` |
| **Observabilidad** | ¿Se han eliminado comentarios Javadoc existentes? | El agente "limpió" comentarios que documentaban comportamiento |

**Verificación final:**

```bash
mvn test
```

Todos los tests deben estar en verde. Documenta en el mensaje de commit qué encontraste en la revisión del diff.

---

## Ejercicio 2 — Skills y Agentes: `/audit-controller`

### Contexto

Ya tienes `OrderController.java` funcionando con lógica real. En este ejercicio vas a construir una **skill** `/audit-controller` que, antes de que un Controller llegue a una PR, ejecuta dos análisis automáticos: una auditoría de seguridad y una revisión de código estructurada.

La skill orquesta dos **agentes especializados** que tú mismo vas a crear. Los prompts de la **Biblioteca del Tema 5** son el punto de partida para los system prompts de cada agente.

### Lo que vas a construir

```
.claude/
├── agents/
│   ├── java-security-auditor.md    ← Agente 1
│   └── java-code-reviewer.md       ← Agente 2
└── commands/
    └── audit-controller.md         ← Skill que los orquesta
```

---

### Paso 1 — Crear el agente `java-security-auditor`

Crea el fichero `.claude/agents/java-security-auditor.md`.

**Base:** toma los prompts de la sección 🔐 del Tema 5 (*"Auditoría de endpoints"* y *"Análisis de vulnerabilidades OWASP"*) y conviértelos en el system prompt del agente.

El system prompt debe definir:

- **Rol:** arquitecto de seguridad especializado en Spring Boot 3.4 + Spring Security 6.x + JWT. Solo reporta hallazgos, no propone correcciones.
- **Categorías a revisar:** endpoints sin autenticación, roles mal asignados o ausentes (`@PreAuthorize`), `@CrossOrigin` sin restricción de orígenes, exposición de datos sensibles en respuestas, OWASP Top 10 adaptado a Spring.
- **Formato de respuesta obligatorio:** tabla de hallazgos con columnas `Severidad | Línea | Hallazgo | Riesgo`. Severidades: 🔴 Crítica / 🟡 Media / 🟢 Baja.
- **Restricción explícita:** no modifica ningún archivo. Solo produce el informe.

Estructura del fichero:

```markdown
---
name: java-security-auditor
description: [Una línea describiendo cuándo usarlo y cuándo NO]
model: claude-sonnet-4-6
---

[System prompt aquí]
```

---

### Paso 2 — Crear el agente `java-code-reviewer`

Crea el fichero `.claude/agents/java-code-reviewer.md`.

**Base:** toma los prompts de la sección 🔍 del Tema 5 (*"Code review estructurado"* y *"Análisis de deuda técnica"*) y conviértelos en el system prompt del agente.

El system prompt debe definir:

- **Rol:** arquitecto senior revisando código para PR. Solo lista observaciones, no genera correcciones hasta que el desarrollador las pida explícitamente.
- **Clasificación de observaciones:** 🔴 Bloqueante / 🟡 Importante / 🟢 Sugerencia.
- **Aspectos a revisar específicamente en Controllers Spring Boot:** lógica de negocio dentro del Controller (señal: más de una decisión condicional), más de 4 dependencias inyectadas, métodos de más de 20 líneas, ausencia de `@Valid` en parámetros de entrada, manejo de errores con excepciones genéricas.
- **Formato de respuesta:** una observación por punto con cita exacta del fragmento afectado, clasificación de severidad, riesgo si no se corrige, y si existe una corrección concreta.
- **Restricción explícita:** no modifica ningún archivo.

---

### Paso 3 — Crear la skill `/audit-controller`

Crea el fichero `.claude/commands/audit-controller.md`.

La skill debe implementar este flujo:

1. Si el usuario no indica qué Controller auditar, preguntárselo antes de continuar.
2. Leer el fichero del Controller indicado.
3. Lanzar el agente `java-security-auditor` pasándole el contenido del Controller.
4. Lanzar el agente `java-code-reviewer` pasándole el mismo contenido.
5. Consolidar los dos informes en un reporte único con dos secciones separadas: `## Auditoría de Seguridad` y `## Revisión de Código`.
6. Cerrar con un **Resumen ejecutivo**: número de hallazgos por severidad y una recomendación explícita: *"Listo para PR"* o *"Requiere correcciones antes de PR"*.

---

### Paso 4 — Probar la skill sobre `OrderController`

```bash
/audit-controller shopflow-orders/src/main/java/com/shopflow/orders/infrastructure/rest/OrderController.java
```

Revisa el informe consolidado y responde estas preguntas:

- ¿El agente de seguridad señaló algún riesgo de autorización en los endpoints `GET /api/v1/orders` o `POST /api/v1/orders`?
- ¿El revisor encontró alguna responsabilidad que no debería estar en el Controller?
- ¿Algún hallazgo del informe coincide con algo que ya detectaste manualmente en el Ejercicio 1?

---

### Reflexión (5 min)

Compara el resultado de `/audit-controller` con haber copiado directamente los prompts de la Biblioteca del Tema 5 en el chat del IDE y responde:

1. **Skill vs. prompt manual:** ¿Qué ventaja concreta da la skill frente a copiar el prompt cada vez? ¿Qué se pierde?
2. **Agentes vs. instrucciones inline:** Si las fases de análisis estuvieran escritas directamente en la skill en lugar de delegar en agentes, ¿qué cambiaría en la calidad del resultado? ¿Y en el mantenimiento?
3. **Extensibilidad:** ¿Qué prompt de la Biblioteca del Tema 5 añadirías como tercer agente en esta skill? Justifica por qué encaja con los otros dos.
