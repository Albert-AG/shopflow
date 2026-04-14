# Tema 8 — MCP (Model Context Protocol)

## 🎯 Objetivo

Entender qué aporta MCP cuando la IA tiene acceso directo al contexto real del proyecto:
el esquema de base de datos, el repositorio de GitHub, las herramientas externas.

En este tema verás la diferencia entre pedirle a la IA que genere código "a ciegas"
y pedírselo cuando tiene acceso al esquema real. Después aplicarás el mismo principio
con el servidor MCP de GitHub.

---

## Punto de partida

```bash
git checkout exercise/topic-08
git checkout -b mi-solucion/topic-08
docker-compose up -d postgres
```

Las entidades JPA **no existen aún**. La única fuente de verdad es `db/schema.sql`.

---

## Ejercicio 1 — PostgreSQL MCP: IA sin contexto vs. IA con contexto

### Paso 1 — Sin MCP (línea base)

Sin conectar ningún servidor MCP, pide a la IA que genere la entidad JPA para `order_items`:

> "Genera la entidad JPA `OrderItem` para Spring Boot 3.4 con Java 21."

Guarda el resultado. Fíjate en los tipos que elige: probablemente `Long` para los IDs,
`double` o `float` para precios, sin constraints, sin relaciones.

### Paso 2 — Configura el servidor MCP de PostgreSQL

El archivo `.claude/settings.json` ya incluye la configuración del servidor MCP
para la base de datos de desarrollo. Arranca Claude Code en este directorio y
verifica que el servidor aparece activo:

```
/mcp
```

> **IntelliJ + JetBrains AI Assistant:** Configura el servidor MCP desde
> `Settings > Tools > AI Assistant > MCP Servers`.

### Paso 3 — Con MCP (mismo prompt, resultado diferente)

Con el servidor MCP activo, lanza exactamente el mismo prompt:

> "Genera la entidad JPA `OrderItem` para Spring Boot 3.4 con Java 21."

Ahora la IA tiene acceso al esquema real. Compara el resultado con el del Paso 1.

### Criterios de éxito ✅

Verifica que la entidad generada con MCP cumple todo esto:

| Campo DDL | Tipo Java esperado |
|---|---|
| `id UUID` | `UUID` con `@GeneratedValue` y estrategia UUID |
| `order_id UUID REFERENCES orders(id)` | `@ManyToOne` con `@JoinColumn` |
| `product_id UUID REFERENCES products(id)` | `@ManyToOne` con `@JoinColumn` |
| `quantity INTEGER CHECK (quantity >= 1)` | `int` o `Integer` |
| `unit_price DECIMAL(19,4)` | `BigDecimal` con precisión correcta |
| `subtotal GENERATED ALWAYS AS ... STORED` | `@Column(insertable = false, updatable = false)` |

### Entrega

Documenta en un comentario o en un fichero `notes-topic-08.md` las diferencias
encontradas entre la versión sin MCP y con MCP. ¿Qué bugs habría introducido
en producción la versión sin contexto?

---

## Ejercicio 2 — GitHub MCP: el agente como colaborador del repositorio

### Prerequisitos

- Servidor MCP de GitHub instalado:
  ```bash
  npx -y @modelcontextprotocol/server-github
  ```
- Un `GITHUB_PERSONAL_ACCESS_TOKEN` con permisos `repo` e `issues`.
  Exporta la variable antes de arrancar Claude Code:
  ```bash
  export GITHUB_TOKEN=ghp_xxxxxxxxxxxx
  ```

### Configuración

Añade el servidor GitHub a `.claude/settings.json` (no lo versiones con el token):

```json
{
  "mcpServers": {
    "github": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "env": {
        "GITHUB_PERSONAL_ACCESS_TOKEN": "${GITHUB_TOKEN}"
      }
    }
  }
}
```

> ⚠️ Nunca commits el token. Usa la variable de entorno del sistema.

Verifica que el servidor aparece activo:

```
/mcp
```

### Paso 1 — Explora tus repositorios

Pide al agente que liste tus repositorios:

> "Lista todos mis repositorios de GitHub. Para cada uno muéstrame el nombre,
> la descripción y si tiene issues abiertas."

Identifica el repositorio del curso en la lista.

### Paso 2 — Crea una issue desde el agente

Usa lenguaje natural para crear una issue. Deja que el agente formatee el título
y la descripción a partir de tu intención:

> "Crea una issue en el repositorio `[nombre-de-tu-repo]` describiendo algo que
> hayas aprendido hoy o una duda que tengas sobre MCP o sobre las entidades JPA
> que acabas de generar."

### Paso 3 — Pide al agente que lea la issue

Inmediatamente después, sin cerrar la sesión:

> "Lee la descripción completa de la issue que acabas de crear y dime si
> la información es suficiente para que otro desarrollador la resuelva
> sin necesidad de preguntar nada."

Observa que el agente recupera la issue del repositorio real, no de su memoria.

### Paso 4 — (Opcional avanzado) Cierra la issue

> "Añade un comentario a esa issue indicando que la duda fue resuelta durante
> la sesión de hoy y ciérrala."

### Reflexión

Responde en `notes-topic-08.md`:

- ¿Qué operaciones del flujo diario de equipo (issues, PRs, revisiones)
  podrías delegar al agente sin riesgo?
- ¿Cuáles deben pasar siempre por revisión humana y por qué?

---

## Solución de referencia

```bash
git checkout v08-mcp
```

Contiene las entidades JPA completas generadas con el contexto MCP.
Consúltala solo después de completar los ejercicios por tu cuenta.
