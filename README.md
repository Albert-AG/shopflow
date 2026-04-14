# ShopFlow — Ejercicio Tema 5: Copilot Chat y Prompting Profesional

## Contexto

Estás en la rama `exercise/topic-05`.

El equipo ha heredado `LegacyOrderController.java`, un controlador REST escrito hace años que nadie se ha atrevido a tocar. Funciona (más o menos), pero acumula una deuda técnica considerable. Tu misión es usar el **Chat del IDE** para analizarlo, entenderlo y planificar su refactorización — sin escribir una sola línea de código todavía.

---

## Punto de partida

```bash
git checkout exercise/topic-05
git checkout -b mi-solucion/topic-05
```

El fichero de trabajo es:

```
shopflow-orders/src/main/java/com/shopflow/orders/infrastructure/rest/LegacyOrderController.java
```

Existe también `OrderController.java` en el mismo paquete: **no lo mires todavía**, es la solución de referencia. Úsalo solo al final para contrastar.

---

## Ejercicios

### Ejercicio 1 — El Consultor Arrogante

Pega el contenido de `LegacyOrderController.java` en el Chat del IDE y ejecuta este prompt:

> *"Actúa como un arquitecto senior y consultor. Hazme una lista de 5 críticas arquitectónicas de este controlador antes de proponer CUALQUIER cambio. Clasifica cada crítica por severidad (Crítica / Media / Baja)."*

**Tarea:**
1. Anota las 5 críticas que devuelve la IA.
2. Compáralas con la lista que elaboraste en el Tema 1 al leer `BeforeAiExample.java`. ¿Cuántas coinciden? ¿Qué detectó la IA que tú no viste?
3. Evalúa críticamente: ¿alguna crítica de la IA es incorrecta o está fuera de contexto?

> **Pista:** El controlador tiene al menos 10 problemas intencionados. Si la IA solo encuentra 5, pídele que busque más.

---

### Ejercicio 2 — Análisis de Edge Cases y Trade-offs

#### Parte A: Edge cases

Pega el método `createOrder()` en el Chat y pregunta:

> *"¿Qué ocurre si `items` es null? ¿Si la lista está vacía? ¿Si el `customerId` no existe en la base de datos? ¿Si la base de datos está caída en mitad de la operación? Para cada caso, indica el comportamiento actual del código y el comportamiento correcto."*

#### Parte B: Trade-offs en el cálculo de descuentos

El controlador tiene un `switch` inline sobre `discountCode` para calcular descuentos. Pide al Chat tres alternativas de diseño:

> *"El cálculo de descuentos está hardcodeado en el controlador REST. Dame tres alternativas de diseño para moverlo al lugar correcto:*
> *1. Strategy Pattern en el dominio*
> *2. Tabla de descuentos en base de datos*
> *3. Regla de negocio en el Value Object `DiscountCode`*
>
> *Para cada una, evalúa: dónde vive la lógica, cómo se testea, qué pasa si hay que añadir un nuevo tipo de descuento, y complejidad de implementación."*

**Tarea:** Elige una de las tres opciones y documenta en un comentario en tu rama por qué la elegiste.

---

### Ejercicio 3 — Plan de Refactorización Incremental

Ahora pide al Chat que diseñe el plan de refactorización sin ejecutar ningún cambio:

> *"Diseña un plan en 4-5 pasos para refactorizar `LegacyOrderController` hacia una arquitectura correcta.*
>
> *Restricciones:*
> *- Cada paso debe ser un commit autocontenido e independiente*
> *- Ningún paso puede romper la compilación*
> *- Los tests existentes deben seguir en verde al final de cada paso*
> *- Indica qué ficheros se crean o modifican en cada paso*
>
> *NO ejecutes ningún cambio todavía. Solo el plan."*

**Tarea:**
1. Revisa el plan. ¿Es atómico cada paso? ¿Hay algún paso que debería dividirse?
2. Identifica qué paso tiene mayor riesgo de romper algo y por qué.
3. Si el plan te convence, implementa **solo el primer paso** y verifica que compila.

---

## Criterios de éxito

Al terminar los tres ejercicios deberías ser capaz de:

- [ ] Listar al menos 8 problemas concretos en `LegacyOrderController.java`
- [ ] Haber comparado las críticas de la IA con tu análisis previo del Tema 1
- [ ] Tener documentada tu elección de diseño para el cálculo de descuentos con su justificación
- [ ] Tener un plan de refactorización en pasos atómicos y verificables
- [ ] Haber ejecutado al menos el primer paso del plan (si llegaste hasta aquí)

---

## Ver la solución

Si quieres contrastar con el resultado final del instructor:

```bash
# Ver el controlador refactorizado
git show v05-chat:shopflow-orders/src/main/java/com/shopflow/orders/infrastructure/rest/OrderController.java

# O cambiar a la rama de solución completa
git checkout v05-chat
```

---

## Conexión con el tema

Este ejercicio aplica directamente las técnicas del Tema 5:

| Técnica | Ejercicio |
|---------|-----------|
| Prompt con objetivo + restricciones + criterio de aceptación | Ejercicio 3 (plan incremental) |
| Pedir alternativas comparadas, no "una solución" | Ejercicio 2B (trade-offs descuentos) |
| Pedir "preguntas antes de codificar" | Reflexión sobre edge cases |
| El Consultor Arrogante | Ejercicio 1 |
| Plan de cambio incremental (Safe Refactor) | Ejercicio 3 |
