---
description: Genera una feature completa de Spring Boot siguiendo el flujo spec→scaffold→harden→verify. Recibe una descripción funcional o ruta a contrato OpenAPI y produce código hexagonal listo para revisión humana.
---

Actúa como orquestador de la generación de una feature Spring Boot siguiendo el flujo profesional de cuatro fases.

Si el usuario no ha indicado qué feature construir, pregunta antes de continuar:
- ¿Cuál es el nombre del recurso/feature? (ej: `Inventory`, `Payment`)
- ¿Hay un contrato OpenAPI disponible? Si es así, ¿cuál es la ruta del fichero?
- ¿Hay reglas de negocio específicas que el dominio deba garantizar?

## Fase 1 — Spec

Lee el contrato OpenAPI indicado (o la descripción funcional si no hay contrato).

Antes de generar código, produce:
1. **Lista de endpoints** a implementar con método HTTP, ruta, request y response.
2. **Reglas de negocio identificadas** (validaciones, invariantes, restricciones).
3. **Gaps de especificación**: qué comportamiento no está definido en el contrato y requiere decisión.
4. **Lista de ficheros a crear** con su capa (domain/application/infrastructure).

Muestra este análisis al usuario y espera confirmación:
**"¿Procedo con el scaffold? (sí/no)"**

No avances sin "sí" explícito.

## Fase 2 — Scaffold

Genera la estructura hexagonal completa para la feature:

Restricciones obligatorias:
- Clases en `domain/model/` sin ninguna anotación de framework.
- DTOs de request/response como `record` de Java 21.
- Inyección por constructor en todas las clases Spring.
- Ningún import de `jakarta.persistence` en `domain/`.
- El servicio de aplicación implementa el puerto de entrada; el adaptador implementa el puerto de salida.

Estructura esperada (adaptar al módulo del proyecto):

domain/model/[Resource].java
domain/port/in/[Create|Get|Update]ResourceUseCase.java
domain/port/out/[Resource]Repository.java
domain/service/[Create|Get|Update]ResourceService.java
infrastructure/rest/[Resource]Controller.java
infrastructure/rest/[Resource]Request.java
infrastructure/rest/[Resource]Response.java
infrastructure/persistence/[Resource]PersistenceAdapter.java
infrastructure/persistence/[Resource]JpaEntity.java
infrastructure/persistence/[Resource]JpaMapper.java


Después del scaffold, ejecuta `mvn -q -DskipTests compile`. Si hay errores de compilación, corrígelos antes de continuar.

## Fase 3 — Harden

Aplica las siguientes mejoras sobre el scaffold:

1. **Manejo de errores**: asegura que las excepciones de dominio están mapeadas en `@ControllerAdvice` con `ProblemDetail`.
2. **Validaciones**: añade `@Valid` en el controlador y `jakarta.validation` en los records de request.
3. **Persistencia**: verifica que no hay `findAll()` sin `Pageable` en endpoints de lista. Añade índices en columnas de filtro si existen queries de filtrado.
4. **Observabilidad**: añade logging estructurado con contexto de negocio (ID de entidad, operación) en el servicio de aplicación. Sin datos sensibles.
5. **Configuración**: si hay propiedades externas (URLs, timeouts, claves), usa `@ConfigurationProperties`, nunca `@Value` disperso.

## Fase 4 — Verify

1. Ejecuta `mvn test -pl <módulo>` y reporta resultado.
2. Usa el subagente `spring-boot-reviewer` para auditar el código generado. Pásale todos los ficheros creados.
3. Muestra el informe de revisión al usuario.
4. Si hay hallazgos 🔴 Bloqueantes, corrígelos y vuelve a ejecutar los tests.
5. Si solo hay 🟡 o 🟢, muéstralos al usuario para que decida.

## Informe final

Al terminar, genera un resumen con:
- Ficheros creados y su propósito
- Tests en verde: sí/no
- Hallazgos del revisor resueltos y pendientes
- Próximos pasos sugeridos (tests de integración, contrato AsyncAPI si aplica)