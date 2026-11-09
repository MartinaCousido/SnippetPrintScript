# SnippetPrintScript

Servicio de PrintScript de [Snippet Searcher](https://github.com/MartinaCousido/SnippetSearcher). Implementa la interfaz `Language` por HTTP: recibe código PrintScript y lo valida (después también lo va a lintear, formatear y ejecutar).

> La arquitectura, las user stories (US) y las decisiones técnicas del sistema completo están en el repo
> [SnippetSearcher](https://github.com/MartinaCousido/SnippetSearcher), que es la fuente de verdad.
> Este README cubre solo lo específico de este servicio.

## Responsabilidad

| Hace | No hace |
|---|---|
| Validar código PrintScript (sintaxis y semántica, sin ejecutarlo) | Guardar snippets ni su código (eso es de **Snippet Service** y del Snippet Storage) |
| Traducir los errores de la librería al formato `Diagnostic` | Saber de usuarios o permisos: recibe el código, nunca el id del snippet |
| Más adelante: `lint`, `format` y `evaluate` | Implementar el lenguaje: eso lo hace la librería |

## La librería

Este repo **no tiene el código del lenguaje**. Usa la librería [PrintScript](https://github.com/MartinaCousido/PrintScript), publicada en GitHub Packages:

```groovy
implementation 'org.printscript:runner:0.3.1'
```

Gradle la baja ya compilada al construir el servicio, igual que Spring Boot. Para usar una versión nueva, primero se publica en el repo PrintScript y después se cambia el número acá, con un PR.

Bajarla pide un **token classic de GitHub con solo el scope `read:packages`**, aunque el repo sea público:

| Dónde | Cómo se pasa el token |
|---|---|
| Tu máquina | `~/.gradle/gradle.properties`, fuera de los repos: `gpr.user=<tu usuario>` y `gpr.key=<token>` |
| Docker local | Variable `PACKAGES_TOKEN` del `.env` |
| CI y CD | Secret `PACKAGES_TOKEN` del repo |

En Docker el token llega como *secret de build*: existe solo mientras se compila y no queda guardado en la imagen.

## Operaciones

Contrato definido en [Decisiones técnicas](https://github.com/MartinaCousido/SnippetSearcher#decisiones-técnicas) de SnippetSearcher.

| Operación | Endpoint | Respuesta | User story |
|---|---|---|---|
| Validar | `POST /validate` con `{ "code": "...", "version": "1.1" }` | **200** con `{ "diagnostics": [...] }`. Lista vacía = válido. **400** si falta un campo o la versión no existe | US-1, US-2 |
| Ejecutar | `POST /evaluate` con `{ "code": "...", "version": "1.1", "inputs": ["Ana", "true"] }` | **200** con `{ "outputs": [...], "diagnostics": [...] }`: lo que imprimió cada `println`, en orden. Cada `readInput` toma el siguiente input, y su texto no cuenta como output. Si la ejecución falla (por ejemplo, se acaban los inputs), el error va en `diagnostics` y `outputs` tiene lo que alcanzó a imprimir. **400** si falta un campo o la versión no existe | US-8, US-16 |
| Reglas | `GET /rules` | **200** con `{ "lint": [...], "format": [...] }`: cada regla con `name`, `type` (`boolean`, `text` o `number`), `options` (solo las de texto), `opposite` (la regla contraria, si tiene) y `default` (`{ isActive, value }`). Los valores por defecto los decide el proyecto: la librería no trae ninguno | US-11, US-14 |
| Lintear | `POST /lint` con `{ "code", "version", "rules": { "identifier_format": "camel case", ... } }`, solo las reglas activas | **200** con `{ "diagnostics": [...] }`: cada regla incumplida. **400** si un valor no sirve o la versión no existe | US-14, US-15 |
| Formatear | `POST /format` con `{ "code", "version", "rules": { ... } }` | **200** con `{ "content": "...", "diagnostics": [] }`. Si el formateador falla (`UNKNOWN_SYMBOL`, `SPACING_TOO_LARGE`), `content` es el código sin cambios y el error va en `diagnostics`. **400** si un valor no sirve o la versión no existe | US-11, US-12 |
| Ejecutar en vivo | WebSocket `/execute` | Ver abajo | US-9, US-10 |

### Ejecutar en vivo (WebSocket `/execute`)

Lo usa solo Snippet Service, por la red interna. Cada conexión hace una sola ejecución. Los mensajes son JSON, con un `type`:

| Dirección | Mensaje | Qué es |
|---|---|---|
| Snippet Service → acá | `{ "type": "start", "code", "version", "inputs"? }` | Empieza. Con `inputs`, es un test (US-9): cada `readInput` toma el siguiente y no se pide nada. Sin `inputs`, es interactiva (US-10) |
| Snippet Service → acá | `{ "type": "input", "value" }` | El input que escribió el usuario |
| acá → Snippet Service | `{ "type": "output", "value" }` | Un `println`, apenas ocurre |
| acá → Snippet Service | `{ "type": "input-request", "prompt" }` | Hace falta un input. `prompt` es el texto de `readInput` |
| acá → Snippet Service | `{ "type": "error", "diagnostic" }` | Un error al ejecutar, con el formato de abajo. Como en `/evaluate`, la librería puede seguir después de un error |
| acá → Snippet Service | `{ "type": "end" }` | Terminó. Después se cierra la conexión (1000) |

- Si pasan **5 minutos** sin un input (`execution.input-timeout`), se manda el error `MISSING_INPUT` de la librería y el `end`.
- Un mensaje que no corresponde (no es JSON, un `input` antes del `start`, un segundo `start`, una versión que no existe) cierra la conexión con **1007** y el motivo.
- Se loguea el inicio y el fin de cada ejecución, con el `requestId` que manda Snippet Service en `X-Request-Id` al abrir la conexión. Nunca el código ni los inputs.

Cada diagnóstico:

```json
{
  "rule": "MISSING_SEMICOLON",
  "message": "Expected ';' at the end of the declaration",
  "range": { "start": { "line": 2, "column": 1 }, "end": { "line": 2, "column": 7 } }
}
```

- `rule` es el código de regla de la librería (`Failure.code()`), no el texto del mensaje.
- `range` es el lugar que marca la librería. Líneas y columnas empiezan en 1. Si falta un `;`, marca el token donde lo esperaba, que puede estar en la línea siguiente.

Versiones soportadas: `1.0` y `1.1`.

## Observabilidad (New Relic)

El agente Java de New Relic (`9.5.0`) va en la imagen, igual que en Snippet Service: manda métricas, traces y los logs del servicio. Cada pedido lleva en el contexto de los logs (MDC) el `requestId` que manda Snippet Service (`X-Request-Id`) y el servicio. Cada operación (validar, lintear, formatear y ejecutar) queda en el log con la versión, el resultado y cuánto tardó; nunca el código del snippet. Ver **Observabilidad (New Relic)** en [Decisiones técnicas](https://github.com/MartinaCousido/SnippetSearcher#decisiones-técnicas).

En local el agente viene apagado. Para mandar datos, se levanta con `NEW_RELIC_AGENT_ENABLED=true` y `NEW_RELIC_LICENSE_KEY` (la de `SnippetSearcher-infra/newrelic/.env`), y aparece como `printscript-service (local)`.

## Stack

**Java 25 + Spring Boot 4.1.1 + Gradle 9.8**, como los otros servicios. No tiene base de datos.

## Imagen Docker

En cada push a `dev` o `main`, GitHub Actions (`.github/workflows/cd.yml`) arma la imagen y la publica en GHCR como `ghcr.io/martinacousido/printscript-service`, con el tag del ambiente (`dev` o `prod`) y el del commit (`sha-…`). Ver [Tags de las imágenes](https://github.com/MartinaCousido/SnippetSearcher-infra#tags-de-las-imágenes) en el repo de infra.

Después, el mismo workflow entra por SSH a la VM del ambiente y corre `docker service update` con el tag `sha-…`. Actualiza **solo** `printscript-service`. Usa los secrets `VM_HOST`, `VM_USER` y `VM_SSH_KEY` de los *Environments* `dev` y `prod`. La VM tiene que estar prendida, y el servicio tiene que estar en el `docker-stack.yml` de infra.

## Cómo levantarlo

Requisitos: Docker Desktop abierto.

```bash
cp .env.example .env          # y poner el token en PACKAGES_TOKEN (el .env no se commitea)
docker compose up --build
```

| Contenedor | URL en tu máquina |
|---|---|
| Servicio de PrintScript | http://localhost:8083 |

Para comprobar que está arriba: http://localhost:8083/actuator/health tiene que responder `"status":"UP"`.

Para probarlo:

```bash
curl -s localhost:8083/validate -H 'Content-Type: application/json' \
  -d '{"code": "let x: number = 1;", "version": "1.1"}'
```

### Correr los tests

No necesitan base de datos. Con el token en `~/.gradle/gradle.properties`:

```bash
./gradlew test
```

En cada PR hacia `dev` o `main`, el CI (`.github/workflows/ci.yml`) corre `./gradlew build`. Su check se llama `compilar-y-testear` y es obligatorio para mergear.

### Linter y cobertura

`./gradlew build` también corre el linter y mide la cobertura. Si algo falla, el build falla, y con él el CI. Ver **Linter y cobertura** en [Decisiones técnicas](https://github.com/MartinaCousido/SnippetSearcher#decisiones-técnicas) de SnippetSearcher.

| Herramienta | Qué revisa | Configuración |
|---|---|---|
| Checkstyle | Nombres y construcciones que suelen ser errores | `config/checkstyle/checkstyle.xml` |
| PMD | Errores probables y malas prácticas | `config/pmd/ruleset.xml` (código) y `config/pmd/ruleset-test.xml` (tests) |
| JaCoCo | Cobertura: mínimo **80 % de líneas**, sin contar `PrintScriptApplication` | `build.gradle` |

Los reportes quedan en `build/reports/` (`checkstyle/`, `pmd/` y `jacoco/test/html/index.html`).

## Flujo de trabajo

Igual que en SnippetSearcher: ramas `feature/*` desde `dev`, PR hacia `dev`, y de `dev` a `main`. Las correcciones de los PRs las hace Martina Cousido.
