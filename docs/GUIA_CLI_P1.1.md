# Guía de uso y arquitectura interna — Cyphail CLI/REPL (P1.1)

Documento de referencia para entender y defender el avance de P1.1.
Cubre cómo se usa la aplicación desde consola y cómo está armado el
código por dentro, clase por clase.

---

## 1. Cómo iniciar la aplicación desde CMD

### Prerrequisitos

- JDK (el `pom.xml` está en Java 26 — verificar con el profesor si el
  Lab exige 24, ver nota en el README).
- Apache Maven 3.9+.

### Compilar

```bat
mvn clean package
```

Esto hace tres cosas en orden: compila el código (`src/main`), corre
todos los tests (`src/test`) y, si todo pasa, empaqueta un jar ejecutable
en `target\cyphail.jar` (el plugin `maven-shade-plugin` mete adentro las
dependencias — picocli — para que sea un solo archivo autocontenido).

### Ejecutar

```bat
java -jar target\cyphail.jar repl
java -jar target\cyphail.jar run examples\movies.cyphail
java -jar target\cyphail.jar compile examples\movies.cyphail --out target\movies.pl
java -jar target\cyphail.jar help
```

O con el wrapper, que hace exactamente lo mismo pero con menos que
teclear:

```bat
cyphail.bat repl
```

`cyphail.bat` no tiene lógica propia: solo revisa que `java` exista en el
PATH y le pasa todos los argumentos (`%*`) al jar.

---

## 2. Comandos del CLI (nivel `cyphail <comando>`)

Estos son los comandos que entiende picocli **antes** de entrar al REPL —
se escriben una sola vez al invocar `cyphail` desde CMD.

| Comando | Qué hace |
|---|---|
| `cyphail repl` | Abre el REPL interactivo (sección 3). |
| `cyphail run <archivo>` | Lee un script `.cyphail` línea por línea y ejecuta cada línea como si se hubiera tecleado en el REPL (sin el banner ni el prompt). Ignora líneas vacías y comentarios `//`. |
| `cyphail compile <archivo> [--out <ruta>]` | **No implementado en P1.1.** Solo imprime un aviso. Se implementa en P1 cuando exista el parser real. |
| `cyphail help [comando]` | Ayuda general, o de un comando específico (por ejemplo `cyphail help run`). |
| `cyphail --version` | Muestra la versión. |

---

## 3. Comandos dentro del REPL

Al correr `cyphail repl` aparece primero el banner:

```
Welcome to Cyphail-04-1pm v.0.1. August 2026. ESCINF/UNA EIF400-II-2026
Visit www.whatiscyphail.com for more information
Type ".help" for more information and commands
Type ".exit" to quit
>>>
```

En el prompt `>>>` hay dos tipos de entrada posibles:

### a) Comandos al REPL (empiezan con `.`)

| Comando | Qué hace |
|---|---|
| `.help` | Lista estos mismos comandos. |
| `.about` | Autores, curso y universidad. |
| `.use` | Lista los grafos fake disponibles (tabla). |
| `.use <nombre>` | Finge conectarse a ese grafo. Error en inglés si no existe. |
| `.exit` | Sale del REPL (imprime `Bye!`). |
| cualquier otro `.algo` | `ERROR: unknown REPL command "..."`. |

### b) Statements fake de Cyphail (todo lo que no empieza con `.`)

Ejemplo:

```
>>> MATCH (p:Persona) RETURN p.nombre, p.edad
p.nombre     p.edad
---------------------
"Ana"        28
...
OK. Query available after 42 ms.
```

No hay parser real: el motor (`FakeEngine`) reconoce un par de consultas
de ejemplo tal cual (las del correo del profesor) y devuelve su tabla
fija; cualquier otro `MATCH ... RETURN` cae en una tabla genérica de una
fila; `CREATE`/`SET`/`DELETE`/`DETACH` devuelven un mensaje de
confirmación; cualquier otra cosa se acepta igual (es fake, no valida
sintaxis).

**Enter en blanco**: no hace nada, solo vuelve a mostrar el prompt.

---

## 4. Arquitectura interna: las capas y por qué existen

```
Usuario
  |
  v
CyphailCommand (picocli, parsea "repl"/"run"/"compile"/"help")
  |
  v
ReplCommand / RunCommand           <- Kenny: CLI/REPL
  |  (String linea de texto)
  v
RequestHandler.handle(String)      <- contrato estable, no cambia
  |
  v
FrontendRouter                     <- Sebastian: valida vacio, limpia espacios
  |
  v
StatementHandler                   <- Sebastian: adapta EngineResult -> CyphailResponse
  |
  v
EngineBroker.execute(String)       <- contrato estable, no cambia
  |
  v
FakeEngine                         <- Moya: statement -> EngineResult (fake)
```

**Por qué esta separación en capas (y no todo en un solo método):**

- El CLI (`ReplCommand`) nunca sabe *cómo* se resuelve un statement —
  solo conoce la interfaz `RequestHandler`. Eso significa que cuando en
  P2.1 aparezca el broker real hacia SWI-Prolog (MQI), **no hay que tocar
  ni una línea del REPL**: solo se cambia qué implementación se conecta
  en `FrontendFactory`.
- `EngineResult` (motor) y `CyphailResponse` (CLI) son dos records
  separados aunque tengan la misma forma (`boolean success, String
  output/message`) porque son conceptos de capas distintas: uno es "lo
  que respondió el motor", el otro es "lo que se le muestra al usuario".
  Hoy son casi idénticos, pero mantenerlos separados evita que un cambio
  en el motor (por ejemplo, agregar metadata de Prolog) obligue a tocar
  el CLI.
- Esto imita el diagrama de arquitectura del SPEC del curso (Frontend →
  Engine Broker → motor), que es justo lo que el profesor pidió ver en el
  correo ("que la arquitectura del proyecto se asemeje algo al diagrama
  que acompaña al SPEC").

---

## 5. Recorrido clase por clase

### `com.cyphail.Main`

Punto de entrada del jar (el `Main-Class` del manifest). Una sola línea
de lógica: crea `CyphailCommand`, se lo pasa a picocli (`new
CommandLine(...).execute(args)`), y usa el código de salida que devuelve
como código de salida del proceso (`System.exit`).

### `com.cyphail.cli.CyphailCommand`

El comando raíz `cyphail`. Es una clase anotada con `@Command` de
picocli: la anotación declara el nombre, la descripción, y la lista de
subcomandos (`ReplCommand`, `RunCommand`, `CompileCommand`, y el
`HelpCommand` que trae picocli de fábrica). Si se corre `cyphail` sin
ningún subcomando, `run()` simplemente imprime el uso.

### `com.cyphail.cli.ReplCommand`

El corazón del REPL. Implementa `Callable<Integer>` (picocli llama a
`call()` y usa lo que devuelve como código de salida).

Tiene dos constructores:
- El público, sin argumentos, que picocli usa en producción: arma un
  `FrontendFactory.createP11Handler()` de verdad y usa `System.in`/
  `System.out`.
- Uno de paquete (sin `public`) que recibe el `RequestHandler`, el
  `InputStream` y el `PrintStream` como parámetros — existe **solo para
  los tests**, así se le puede inyectar una entrada/salida falsa (un
  `ByteArrayInputStream` con un guion armado a mano) sin tener que
  simular teclado real ni tocar `System.in`.

`call()` hace un loop clásico de REPL:
1. Imprime el banner.
2. Imprime el prompt.
3. Lee una línea (`BufferedReader.readLine()`, bloquea hasta que llega
   una línea o se acaba el stream).
4. Si está vacía: vuelve a imprimir el prompt y sigue.
5. Si es exactamente `.exit` (sin importar mayúsculas): rompe el loop.
6. Si empieza con `.`: la manda a `dispatchReplCommand` (switch sobre el
   nombre del comando, ver abajo) e imprime lo que devuelve.
7. Si no: la manda a `handler.handle(linea)` (cruza a la capa de
   Frontend) e imprime la `CyphailResponse` — con `ERROR: ` de prefijo
   si `success()` es falso.
8. Vuelve al paso 2, hasta que el stream se acaba o se rompe el loop.
9. Al salir del loop, imprime `Bye!`.

`dispatchReplCommand` separa el comando del argumento buscando el primer
espacio (`.use amigos` → comando `.use`, argumento `amigos`), y usa un
`switch` de Java (permitido porque el profesor autorizó paradigma
OOP-imperativo para este sprint) para mandarlo a `ReplHelp.text()`,
`AboutInfo.text()`, o `GraphCatalog.use(argumento)`. Cualquier comando
`.algo` no reconocido cae en el `default` con un mensaje de error.

### `com.cyphail.cli.RunCommand`

Modo no interactivo: en vez de leer de `System.in` línea por línea, lee
todas las líneas de un archivo (`Files.readAllLines`), descarta las
vacías y las que empiezan con `//`, y manda cada una a
`handler.handle(...)` — el mismo `RequestHandler` que usa el REPL. No
entiende comandos `.` (no tendría sentido en un script).

### `com.cyphail.cli.CompileCommand`

Placeholder deliberado: recibe los argumentos (`source`, `--out`) para
que la forma del CLI ya sea la definitiva, pero `call()` solo imprime que
no está implementado todavía (se implementa en P1, cuando exista
lexer/parser real).

### `com.cyphail.cli.Banner`, `ReplHelp`, `AboutInfo`, `GraphCatalog`

Clases pequeñas, todas `final` con constructor privado y solo métodos
`static` (no tiene sentido instanciarlas — son texto fijo o una lista
fija con un método de búsqueda). `GraphCatalog` es la única con algo de
lógica: guarda una lista de grafos fake (`record Graph(String name,
String description)`) y arma la tabla de `.use` a mano con
`String.repeat`/padding, sin librerías externas.

### `com.cyphail.frontend.RequestHandler` / `CyphailResponse`

El contrato entre el CLI y todo lo demás. `RequestHandler` es una
interfaz funcional de un solo método (`handle(String) ->
CyphailResponse`), así que cualquier implementación — incluso un lambda —
sirve para los tests. `CyphailResponse` es un `record` inmutable de dos
campos con dos factory methods (`ok`/`error`) para no tener que escribir
`new CyphailResponse(true, ...)` en todos lados.

### `com.cyphail.frontend.FrontendRouter`

Implementa `RequestHandler`. Es la primera parada después del CLI:
rechaza entradas vacías/en blanco sin llegar a tocar el motor
(`"Empty command."`), y si hay contenido, lo limpia (`strip()`) y se lo
pasa a `StatementHandler`.

### `com.cyphail.frontend.StatementHandler`

Traductor entre capas: le pide al `EngineBroker` que ejecute el
statement, y convierte el `EngineResult` (vocabulario del motor) en un
`CyphailResponse` (vocabulario del CLI) — mismo booleano, mismo texto,
pero es un paso explícito para que el CLI nunca dependa directamente del
paquete `engine`.

### `com.cyphail.frontend.FrontendFactory`

Una sola función estática, `createP11Handler()`, que arma el árbol de
objetos completo: `new FrontendRouter(new FakeEngine())`. Es el **único**
lugar del código donde se decide qué motor se está usando. Cuando llegue
el broker real de Prolog, este método (o uno nuevo al lado) es lo único
que cambia.

### `com.cyphail.engine.EngineBroker` / `EngineResult`

Mismo patrón que `RequestHandler`/`CyphailResponse`, pero un nivel más
abajo: `EngineBroker` es el contrato que en P2.1 va a cumplir el broker
real hacia SWI-Prolog vía MQI (socket + JSON). `EngineResult` es la
respuesta cruda del motor, antes de que `StatementHandler` la traduzca.

### `com.cyphail.engine.FakeEngine`

Implementa `EngineBroker`. Toda la "inteligencia" fingida vive acá:

1. Si el statement es exactamente una de las dos consultas de ejemplo
   del correo del profesor (comparación de texto literal, guardadas en
   un `Map<String, String>`), devuelve la tabla armada a mano con esos
   valores y el tiempo fijo (`42 ms` / `666 ms`) tal cual el ejemplo.
2. Si no, pero empieza con `MATCH` y contiene `RETURN`, arma una tabla
   genérica de una fila con un tiempo aleatorio (`ThreadLocalRandom`).
3. Si empieza con `CREATE`/`SET`/`DELETE`/`DETACH`, devuelve un mensaje
   de confirmación con tiempo aleatorio.
4. Cualquier otra cosa: mensaje genérico de "recibido".

El método privado `table(headers, rows)` es un formateador de tablas
casero (calcula el ancho de cada columna según el contenido más largo,
rellena con espacios) — no usa ninguna librería de terceros.

---

## 6. Trazado completo de un ejemplo (para poder explicarlo en la defensa)

Usuario teclea `MATCH (p:Persona) RETURN p.nombre, p.edad` en el REPL:

1. `ReplCommand.call()` ya está en el loop, `reader.readLine()` devuelve
   ese texto.
2. `trimmed` no está vacío, no es `.exit`, no empieza con `.` → va al
   `else`: `handler.handle(trimmed)`.
3. `handler` es el objeto que armó `FrontendFactory.createP11Handler()`
   al construir `ReplCommand()` — un `FrontendRouter`.
4. `FrontendRouter.handle(...)`: el input no está en blanco, hace
   `strip()` (no cambia nada acá) y llama a
   `statementHandler.handle(...)`.
5. `StatementHandler.handle(...)`: llama a
   `engineBroker.execute(...)` — el `engineBroker` es un `FakeEngine`.
6. `FakeEngine.execute(...)`: el texto coincide exactamente con una de
   las claves de `CANNED_QUERIES` → devuelve `EngineResult.ok(tabla +
   "\nOK. Query available after 42 ms.")`.
7. `StatementHandler` ve `result.success() == true` → devuelve
   `CyphailResponse.ok(result.output())`.
8. `FrontendRouter` devuelve esa misma respuesta tal cual (no la toca).
9. De vuelta en `ReplCommand.call()`: `printResponse(response)` ve
   `success() == true` → `out.println(response.message())` — se imprime
   la tabla completa.
10. Se vuelve a imprimir el prompt `>>> ` y el loop sigue.

---

## 7. Qué es temporal/fake y qué no

- **Temporal (se reemplaza en sprints futuros):** todo `com.cyphail.engine`
  (el broker real de Prolog llega en P2.1), y el hecho de que `FakeEngine`
  no valide sintaxis real (el parser llega en P1).
- **No temporal (es la forma definitiva del CLI):** la estructura de
  comandos (`repl`/`run`/`compile`/`help`), el contrato `RequestHandler`/
  `CyphailResponse`, y la separación en capas Frontend/Engine — eso se
  mantiene igual aunque cambie lo que hay detrás de cada capa.
