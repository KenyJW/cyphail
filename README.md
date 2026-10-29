# Cyphail

Prototipo de motor de consultas sobre grafos con un dialecto propio inspirado en Cypher. Proyecto grupal del curso **EIF400-II-2026-CLoria** (“Paradigmas de Programación”), Escuela de Informática, **Universidad Nacional de Costa Rica (UNA)**.

**Grupo 4** — sección 1:00 p. m.

| Integrante | Responsabilidad principal |
|---|---|
| Kenny Jiménez Wang | CLI, REPL, lexer, parser, AST, analizador y `.tree` |
| José Moya Pérez | Engine Broker, comunicación MQI, integración con SWI-Prolog, datos JSON y generación de código Prolog |
| Sebastián Ramírez Calderón | Estructura inicial del proyecto, frontend, router y handlers; se retiró del curso después de P1.1 |

> Sebastián entregó su parte antes de salir del curso. Su código continúa acreditado en los archivos correspondientes. Desde P1.1, el proyecto es desarrollado por Kenny Jiménez Wang y José Moya Pérez.

> **Estado: P2 en desarrollo.** El proyecto cuenta con lexer, parser, AST, análisis semántico, generación de código Prolog y un REPL con soporte para estatutos multilínea y el comando `.load`. También dispone de comunicación real con SWI-Prolog mediante MQI. El REPL todavía utiliza `FakeEngine` mientras se termina de adaptar el generador al AST de P2 y se integra la biblioteca Prolog definitiva.

## Prerrequisitos

- **JDK 26.** El `pom.xml` fija `maven.compiler.release` en `26`.
- **Apache Maven 3.9 o superior.**
- **Git**, para clonar y actualizar el repositorio.
- **SWI-Prolog con la biblioteca MQI.** El ejecutable `swipl` debe estar disponible en el `PATH` o se debe indicar su ruta mediante la propiedad `cyphail.swipl`.

Para verificar las instalaciones:

```bash
java -version
mvn -version
git --version
swipl --version
```

Para comprobar que la biblioteca MQI está disponible:

```bash
swipl -g "use_module(library(mqi)),writeln('MQI OK'),halt"
```

## Obtener el proyecto

```bash
git clone https://github.com/KenyJW/cyphail.git
cd cyphail
```

Si el repositorio ya existe:

```bash
git fetch origin
git pull --ff-only origin main
```

## Compilar el proyecto

Desde la raíz del proyecto:

```bash
mvn clean package
```

Este comando compila el proyecto, ejecuta las pruebas y genera el JAR ejecutable:

```text
target/cyphail.jar
```

## Ejecutar

### Directamente con Java

```bash
java -jar target/cyphail.jar repl
java -jar target/cyphail.jar run examples/movies.cyphail
java -jar target/cyphail.jar compile examples/codegen-demo.cyphail --out target/codegen-demo.pl
java -jar target/cyphail.jar help
```

### Con el wrapper de Windows

```bat
cyphail.bat repl
```

`cyphail.bat` verifica que Java esté disponible y reenvía los argumentos al JAR. No contiene lógica del parser, compilador o motor.

## El REPL

Para iniciar el REPL:

```bash
java -jar target/cyphail.jar repl
```

Salida esperada:

```text
Welcome to Cyphail-04-1pm v.0.1. August 2026. ESCINF/UNA EIF400-II-2026
Visit www.whatiscyphail.com for more information
Type ".help" for more information and commands
Type ".exit" to quit
>>>
```

REPL significa **Read-Eval-Print Loop**:

1. Lee una entrada.
2. La evalúa.
3. Imprime el resultado.
4. Espera la siguiente entrada.

### Comandos del REPL

| Comando | Función |
|---|---|
| `.help` | Muestra la ayuda |
| `.about` | Muestra información del proyecto y sus autores |
| `.use` | Lista los grafos disponibles |
| `.use <nombre>` | Selecciona un grafo |
| `.tree <consulta>` | Parsea una consulta y muestra su AST |
| `.load <archivo>` | Lee y ejecuta un script desde el disco |
| `.exit` | Finaliza el REPL |

Una entrada vacía no ejecuta ninguna operación.

### Estatutos multilínea

El REPL permite escribir un estatuto en varias líneas. El punto y coma indica que la entrada está completa:

```cypher
MATCH (m:Movie)
WHERE m.year > 2000
RETURN m.title AS title;
```

El parser también acepta un punto y coma opcional al final de un programa.

### Cargar un script

```text
>>> .load examples/movies.cyphail
```

El comando `.load` lee el archivo indicado y procesa su contenido mediante el mismo pipeline utilizado por el REPL.

## Compilar Cyphail a Prolog

El comando `compile` procesa un archivo mediante el lexer, parser, analizador semántico y generador de código Prolog:

```bash
java -jar target/cyphail.jar compile examples/codegen-demo.cyphail --out target/codegen-demo.pl
```

Ejemplo de entrada:

```cypher
MATCH (m:Movie)
WHERE m.year > 2000
RETURN m.title AS title,
       m.year AS year
```

Representación Prolog generada durante P1:

```prolog
query([match([node(var('m'), ['Movie'], [])], compare(property_access('m', 'year'), '>', 2000))], [return_item(property_access('m', 'title'), 'title'), return_item(property_access('m', 'year'), 'year')]).
```

Si el programa contiene un error sintáctico o una variable no definida, se informa el error y no se genera el archivo de salida.

El generador se encuentra en proceso de adaptación al AST de P2 para producir metas ejecutables por el motor real de SWI-Prolog.

## El comando `.tree`

`.tree` permite verificar el AST producido por el parser:

```text
>>> .tree MATCH (m:Movie) WHERE m.year > 1990 RETURN m.title AS title
Query{
  Match: {
    Patterns: [
      PatternNode:{
        var: m
        labels: [ Movie ]
        properties: []
      }
    ]
  }
  Where: {
    Expr: (> (. m year) 1990)
  }
  Updates:[]
  Return:{
    Projection:{
      Items:[
        {as (. m title) title}
      ]
      Modifiers:[]
    }
  }
}
```

Las expresiones se muestran en forma prefija:

| Expresión | Impresión |
|---|---|
| Acceso a propiedad `m.year` | `(. m year)` |
| Comparación `m.year > 1990` | `(> (. m year) 1990)` |
| Retorno con alias | `{as (. m title) title}` |

### Error sintáctico

```text
>>> .tree MATCH (p RETURN p
ERROR: Expected RPAREN but found RETURN at token 3
```

### Error semántico

```text
>>> .tree MATCH (p:Person) WHERE q.age > 60 RETURN q AS name
Query{
  ...
}
ERROR: Undefined variable 'q'
```

El parser puede construir un AST aunque posteriormente el analizador detecte una variable no definida.

## Datos fake de P1

Las respuestas simuladas se encuentran en:

```text
data/fake-responses.json
```

`FakeEngine` lee el archivo cada vez que ejecuta una consulta. Esto permite cambiar los datos sin recompilar el JAR.

Gson convierte el JSON en objetos `FakeResponseCatalog` y `FakeResponseData`. `JsonFakeDataSource` busca la respuesta relacionada con la consulta normalizada.

Si no encuentra una respuesta específica, el motor produce una respuesta genérica.

`FakeEngine` se mantiene temporalmente conectado al frontend para conservar el funcionamiento de P1 mientras se completa el motor real de P2.

## Integración con SWI-Prolog mediante MQI

La integración real se divide en cuatro componentes:

| Componente | Responsabilidad |
|---|---|
| `MqiProcess` | Inicia y detiene automáticamente el proceso de SWI-Prolog |
| `MqiClient` | Maneja el socket, handshake, framing y timeout del protocolo MQI |
| `MqiResponseDecoder` | Recorre la respuesta JSON con Gson y extrae bindings |
| `SwiPrologEngineBroker` | Ejecuta código Prolog y convierte el binding `Rows` en un `EngineResult` |

### Flujo de comunicación

```text
Código Prolog
    -> SwiPrologEngineBroker
    -> MqiProcess
    -> MqiClient
    -> socket TCP
    -> SWI-Prolog MQI
    -> respuesta JSON
    -> MqiResponseDecoder
    -> binding Rows
    -> EngineResult
```

### Características implementadas

- Inicio automático de SWI-Prolog.
- Puerto y contraseña generados por MQI.
- Conexión mediante sockets TCP.
- Handshake con contraseña.
- Framing basado en longitud de bytes UTF-8.
- Timeout configurable.
- Compatibilidad con terminaciones de línea `\n` y `\r\n`.
- Decodificación de respuestas con Gson en modo árbol.
- Búsqueda recursiva del binding `Rows`.
- Cierre del cliente y del proceso.
- Manejo de errores de comunicación y respuestas inválidas.

El broker real ya funciona de forma aislada. Su conexión definitiva con `FrontendFactory` se realizará cuando el codegen de P2 produzca las metas que utilizará la biblioteca Prolog.

## Correr las pruebas

Para ejecutar las pruebas normales:

```bash
mvn clean test
```

Las pruebas de integración con SWI-Prolog son opcionales, de modo que una computadora sin SWI-Prolog todavía puede compilar y probar el resto del proyecto.

### Prueba real del proceso MQI

En Windows:

```powershell
mvn test "-Dtest=MqiProcessIntegrationTest" "-Dcyphail.mqi.integration=true" "-Dcyphail.swipl=C:\Program Files\swipl\bin\swipl.exe"
```

### Prueba real del broker

En Windows:

```powershell
mvn test "-Dtest=SwiPrologEngineBrokerIntegrationTest" "-Dcyphail.mqi.integration=true" "-Dcyphail.swipl=C:\Program Files\swipl\bin\swipl.exe"
```

Esta prueba:

1. Inicia SWI-Prolog.
2. Obtiene el puerto y la contraseña de MQI.
3. Abre el socket.
4. Ejecuta una meta que produce `Rows`.
5. Decodifica la respuesta JSON.
6. Comprueba el resultado.
7. Cierra los recursos.

Entre las pruebas automatizadas se encuentran:

- `CasosProfesorTest`
- `AnalyzerTest`
- `PrologCodeGeneratorTest`
- `CompilerEngineBrokerTest`
- `JsonFakeDataSourceTest`
- `CompileCommandTest`
- `MqiClientIntegrationTest`
- `MqiProcessIntegrationTest`
- `MqiResponseDecoderTest`
- `SwiPrologEngineBrokerIntegrationTest`

## Arquitectura

El pipeline de compilación es:

```text
Texto Cyphail
    -> Lexer
    -> Parser
    -> Program (AST)
    -> Analyzer
    -> PrologCodeGenerator
    -> CompilerEngineBroker
    -> EngineBroker
```

Durante la transición de P1 a P2 existen dos implementaciones de `EngineBroker`:

```text
EngineBroker
    -> FakeEngine
    -> SwiPrologEngineBroker
```

Actualmente el REPL utiliza esta ruta:

```text
Texto Cyphail
    -> Lexer
    -> Parser
    -> Analyzer
    -> PrologCodeGenerator
    -> CompilerEngineBroker
    -> FakeEngine
    -> data/fake-responses.json
```

La ruta de P2, ya comprobada mediante una prueba de integración, es:

```text
Meta Prolog
    -> SwiPrologEngineBroker
    -> MqiClient
    -> SWI-Prolog
    -> MqiResponseDecoder
    -> Rows
    -> EngineResult
```

`CompilerEngineBroker` aplica el parser, el análisis semántico y el codegen antes de delegar la ejecución a otra implementación de `EngineBroker`.

Esta separación permite sustituir `FakeEngine` por `SwiPrologEngineBroker` sin modificar el parser ni el frontend.

## Paquetes principales

| Paquete | Contenido |
|---|---|
| `com.cyphail.cli` | Comandos de consola y REPL |
| `com.cyphail.lexer` | Tokens, lexers y combinadores |
| `com.cyphail.parser` | Parsers de expresiones, patrones y estatutos |
| `com.cyphail.ast` | AST con `record` y `sealed interface` |
| `com.cyphail.analyzer` | Análisis semántico y variables |
| `com.cyphail.tree` | Impresión del AST para `.tree` |
| `com.cyphail.codegen` | Generación de código Prolog |
| `com.cyphail.frontend` | Router y handlers |
| `com.cyphail.engine` | Contratos y brokers |
| `com.cyphail.engine.data` | Lectura de respuestas fake en JSON |
| `com.cyphail.engine.mqi` | Proceso, cliente y decodificación de MQI |

## Sobre el lexer y el parser

El lexer y el parser fueron construidos manualmente con combinadores propios, sin generadores externos.

Cada parser devuelve:

- `Ok(resultado, resto)` cuando reconoce la entrada.
- `Fail(razón)` cuando no puede reconocerla.

La entrada es inmutable. Avanzar produce una nueva entrada, por lo que un parser que falla no modifica el valor original.

### Combinadores principales

| Combinador | Función |
|---|---|
| `Or(p, q)` | Prueba `p`; si falla, prueba `q` |
| `Map(p, f)` | Transforma el resultado |
| `And(p, q)` | Ejecuta `p` y después `q` |
| `Opt(p)` | Reconoce cero o una aparición |
| `Star(p)` | Reconoce cero o más apariciones |
| `Some(p)` | Reconoce una o más apariciones |
| `SepBy(p, sep)` | Reconoce elementos separados por un delimitador |

## Generación de código Prolog

`PrologCodeGenerator` recorre el AST mediante pattern matching sobre interfaces selladas y records.

Ejemplos de la representación utilizada en P1:

| AST | Representación Prolog |
|---|---|
| Variable `m` | `var('m')` |
| Propiedad `m.title` | `property_access('m', 'title')` |
| Número `2000` | `2000` |
| String `"adult"` | `'adult'` |
| Comparación | `compare(property_access('m', 'year'), '>', 2000)` |
| Nodo `(m:Movie)` | `node(var('m'), ['Movie'], [])` |

El generador no modifica el AST ni conserva estado. Recibe un `Program` y devuelve un nuevo `String`.

Para P2 será adaptado a:

- `PathPattern`
- relaciones y direcciones
- `SetStatement`
- `RemoveStatement`
- `DecimalLiteral`
- `AndExpression`
- el operador `EQUALS`
- la tabla de variables
- nombres de variables válidos en Prolog

## Principios y decisiones de diseño

- **Responsabilidad única:** lexer, parser, analyzer, codegen, broker y motor tienen funciones separadas.
- **Abierto/cerrado:** `EngineBroker` permite agregar implementaciones sin cambiar a sus consumidores.
- **Inversión de dependencias:** el frontend depende de `EngineBroker`, no de una implementación concreta.
- **Decorator:** `CompilerEngineBroker` agrega compilación y validación antes de delegar.
- **Factory:** `FrontendFactory` construye y conecta los componentes.
- **Tipos algebraicos:** el AST utiliza `sealed interface`, `record` y pattern matching exhaustivo.
- **Inmutabilidad:** las etapas producen valores nuevos.
- **Administración de recursos:** los componentes MQI implementan `AutoCloseable`.
- **Pruebas opcionales de integración:** el proyecto puede probarse sin exigir un proceso externo en cada ejecución.

## Trabajo pendiente de P2

- Integrar el AST definitivo de relaciones con el codegen.
- Generar metas ejecutables de Prolog.
- Implementar la tabla de variables y su mapeo a nombres Prolog.
- Crear la biblioteca `.pl` del motor.
- Agregar soporte para las operaciones nuevas del AST.
- Conectar `SwiPrologEngineBroker` al frontend.
- Mantener las funciones de P1 que todavía correspondan.
- Agregar pruebas del pipeline completo desde parser hasta SWI-Prolog.

## Créditos y fuentes

- Arquitectura, gramática y casos de prueba: documentos SPEC suministrados por el profesor.
- Modelo de combinadores: código y sesiones del curso.
- Procesamiento de argumentos de consola: [Picocli](https://picocli.info/).
- Procesamiento JSON: [Gson](https://github.com/google/gson).
- Pruebas automatizadas: [JUnit 5](https://junit.org/junit5/).
- Motor lógico y protocolo MQI: [SWI-Prolog](https://www.swi-prolog.org/).

## Declaración sobre el uso de IA

Se utilizó IA, incluyendo Claude Code de Anthropic y ChatGPT de OpenAI, como apoyo para estudiar conceptos, proponer implementaciones, detectar errores y revisar código.

En la parte de Kenny Jiménez Wang, se utilizó como apoyo en componentes del lexer, combinadores, parser, AST, analizador, `.tree`, REPL multilínea y `.load`.

En la parte de José Moya Pérez, se utilizó como apoyo en el motor fake, lectura de respuestas JSON, generación de código Prolog, comunicación mediante MQI, framing del protocolo, manejo de sockets, decodificación de respuestas y pruebas de integración con SWI-Prolog.

Los tipos base de los combinadores y algunas implementaciones siguen el modelo presentado por el profesor durante las clases, lo cual se acredita en los encabezados correspondientes.

Cada cambio fue integrado y comprobado mediante pruebas automatizadas o ejecuciones manuales. Los integrantes deben comprender y poder explicar el código que presentan durante la defensa.