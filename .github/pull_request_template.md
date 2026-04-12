## Descripción

<!-- Explica qué hace este PR y por qué. Sé conciso pero completo. -->

## Cambios realizados

- [ ] <!-- Cambio 1 -->
- [ ] <!-- Cambio 2 -->

## Uso de IA en este PR

<!-- Documenta cómo usaste la IA durante el desarrollo. Ser transparente es una práctica de equipo. -->

| Tarea | Herramienta | Revisión humana |
|-------|-------------|-----------------|
| <!-- ej: Scaffolding del endpoint --> | <!-- ej: Claude Code Agent Mode --> | <!-- ej: Revisé el diff, ajusté validaciones --> |

## Checklist de calidad

### Código
- [ ] `mvn verify` pasa en verde localmente
- [ ] No hay `@Autowired` por campo — solo inyección por constructor
- [ ] No hay `System.out.println` — se usa SLF4J Logger
- [ ] No hay SQL concatenado con strings
- [ ] No hay `Optional.get()` sin `isPresent()`/`orElseThrow()`

### Tests
- [ ] Los nuevos tests verifican valores concretos (no solo `assertDoesNotThrow` / `isNotNull`)
- [ ] Los tests fallan si se borra la lógica que prueban (verificado manualmente)
- [ ] Tests de integración usan Testcontainers, no mocks de base de datos

### Seguridad (OWASP checklist)
- [ ] No hay secretos hardcodeados (API keys, passwords, tokens)
- [ ] Endpoints nuevos tienen `@PreAuthorize` explícito
- [ ] Excepciones internas no se exponen al cliente (`e.getMessage()` en respuesta)
- [ ] Inputs de usuario validados con Bean Validation antes de usarse

### Arquitectura
- [ ] Las clases de dominio no dependen de Spring ni de infraestructura
- [ ] Los controladores REST solo hacen mapeo — sin lógica de negocio
- [ ] `HexagonalArchitectureTest` sigue en verde después de los cambios

## Contexto para el reviewer

<!-- ¿Hay algo en este PR que requiera atención especial? ¿Decisiones técnicas no obvias? -->

---

*Este PR fue creado con asistencia de Claude Code. La revisión humana es obligatoria antes de merge.*
