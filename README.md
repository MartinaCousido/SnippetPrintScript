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
implementation 'org.printscript:runner:0.3.0'
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

## Flujo de trabajo

Igual que en SnippetSearcher: ramas `feature/*` desde `dev`, PR hacia `dev`, y de `dev` a `main`. Las correcciones de los PRs las hace Martina Cousido.
