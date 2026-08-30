# Cyphail

Prototipo de motor de consultas sobre grafos, con un dialecto propio
inspirado en Cypher. Proyecto grupal del curso **EIF400-II-2026-CLoria**
("Paradigmas de Programación"), Escuela de Informática, **Universidad
Nacional de Costa Rica (UNA)**.

**Grupo 4** (sección 1:00 p.m.)

| Integrante | Responsabilidad principal |
|---|---|
| Kenny | CLI, REPL, interacción con el usuario |
| Moya | Engine Broker, Fake Engine |
| Sebastián | Estructura del proyecto Java, Frontend/Router, Handlers — salió del curso después de P1.1 |

> Sebastián alcanzó a entregar su parte (`com.cyphail.frontend`) antes de
> salir del curso, por lo que su código forma parte de este entregable y
> se mantiene acreditado a su nombre en los fuentes correspondientes.
> A partir de P1 el grupo continúa con dos integrantes.

> **Estado: P1.1 (setup inicial formal).** La conexión con el motor
> todavía es simulada/fake, tal como lo pide el SPEC para este entregable.
> No se parsea Cypher real, no hay AST, no hay comunicación con Prolog.

## Prerrequisitos

- **JDK.** El `pom.xml` está configurado para **Java 26**. El correo del
  profesor sobre P1.1 indica que el Lab normalmente tiene **JDK 24**
  instalado — **esto está pendiente de confirmar con el profesor** antes
  de la defensa. Si el Lab solo tiene JDK 24, hay que bajar
  `maven.compiler.release` a `24` en `pom.xml` (las features usadas —
  sealed interfaces, records, pattern matching for switch — están
  finalizadas desde Java 21, así que no se pierde nada al bajar la
  versión).
- **Apache Maven 3.9+.**

## Compilar

Desde la raíz del proyecto, en una consola (CMD, PowerShell o bash):

```bash
mvn clean package
```

Esto compila, corre las pruebas y genera un jar ejecutable en
`target/cyphail.jar`.

## Ejecutar

### Opción 1: directamente con `java`

```bash
java -jar target/cyphail.jar repl
java -jar target/cyphail.jar run ./examples/movies.cyphail
java -jar target/cyphail.jar compile ./examples/movies.cyphail --out ./target/movies.pl
java -jar target/cyphail.jar help
```

### Opción 2: con el wrapper `cyphail.bat` (Windows)

```bat
cyphail.bat repl
```

`cyphail.bat` solo verifica que `java` esté disponible y reenvía los
argumentos al jar — no tiene lógica adicional.

## El REPL

```
> cyphail repl
Welcome to Cyphail-04-1pm v.0.1. August 2026. ESCINF/UNA EIF400-II-2026
Visit www.whatiscyphail.com for more information
Type ".help" for more information and commands
Type ".exit" to quit
>>>
```

En el prompt `>>>` se pueden escribir dos tipos de cosas:

**a) Comandos al REPL** (empiezan con `.`):

| Comando | Qué hace |
|---|---|
| `.help` | Muestra la lista de comandos disponibles |
| `.about` | Muestra los autores, el curso y la universidad |
| `.use` | Lista los grafos fingidos disponibles |
| `.use <nombre>` | Finge conectarse a un grafo |
| `.exit` | Sale del REPL |

**b) Supuestos (fake) statements de Cyphail**, por ejemplo:

```
>>> MATCH (p:Persona) RETURN p.nombre, p.edad
```

No hay parser real todavía: el motor fake (`FakeEngine`) reconoce un par
de consultas de ejemplo tal cual y devuelve una tabla fingida; cualquier
otro `MATCH ... RETURN` cae en una tabla genérica, y `CREATE`/`SET`/
`DELETE`/`DETACH` devuelven un mensaje de confirmación. Todo esto vive en
un solo lugar del código (`com.cyphail.engine.FakeEngine`), fácil de
ubicar y modificar.

Enter en blanco: no hace nada, solo vuelve a mostrar el prompt.

## Correr las pruebas

```bash
mvn test
```

## Arquitectura

```
Usuario -> CLI/REPL (Kenny) -> FrontendRouter -> StatementHandler
        -> EngineBroker -> FakeEngine -> respuesta -> REPL
```

- `com.cyphail.cli` — CLI y REPL (Kenny). Parsea comandos con
  [picocli](https://picocli.info/), maneja el prompt, los comandos `.` y
  muestra las respuestas.
- `com.cyphail.frontend` — `RequestHandler`/`CyphailResponse` (contrato
  estable que usa el CLI) y `FrontendRouter`/`StatementHandler`/
  `FrontendFactory` (Sebastián).
- `com.cyphail.engine` — `EngineBroker`/`EngineResult`/`FakeEngine`
  (Moya). En P2.1 `FakeEngine` se reemplaza por el broker real hacia
  SWI-Prolog (MQI) sin tocar las capas de arriba.

> Nota de integración: `com.cyphail.frontend` y `com.cyphail.engine` son
> los archivos entregados por Sebastián y Moya respectivamente, integrados
> sin modificaciones. Cada fuente indica su autor en el encabezado.

## Créditos y fuentes

- Arquitectura general y casos de uso: SPEC del curso
  (`docs/EIF400-II-2026-SPEC_Inicial_Cyphail-CLoria.pdf` y
  `docs/EIF400-II-2026-Arquitectura General Cyphail-CLoria.pdf`).
- Formato del REPL, comandos `.help`/`.about`/`.use`/`.exit` y ejemplos de
  salida: correo del profesor sobre el alcance de P1.1 (agosto 2026).
- Librería de parsing de argumentos de línea de comandos:
  [picocli](https://picocli.info/) (recomendada por el SPEC).
- Pruebas: [JUnit 5](https://junit.org/junit5/).

## Declaración sobre el uso de IA

Se usó IA (Claude Code, modelo Claude Sonnet 5, Anthropic) como parte del
desarrollo de este avance. El SPEC del curso autoriza el uso de IA para
**entender y estudiar**, pero prohíbe que una IA agéntica genere el
proyecto de forma "zero coding". El equipo está aclarando con el profesor
el alcance exacto de uso permitido para este entregable; los prompts
usados están disponibles si se requieren para la revisión.
