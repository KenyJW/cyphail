# Cyphail

Prototipo de motor de consultas sobre grafos con un dialecto propio inspirado en Cypher. Proyecto grupal del curso **EIF400-II-2026-CLoria** (“Paradigmas de Programación”), Escuela de Informática, **Universidad Nacional de Costa Rica (UNA)**.

**Grupo 4** — sección 1:00 p. m.

| Integrante | Responsabilidad principal |
|---|---|
| Kenny Jiménez Wang | CLI, REPL, lexer, parser, AST, analizador y `.tree` |
| José Moya Pérez | Codegen, tabla de variables Prolog, Engine Broker, comunicación MQI, integración con SWI-Prolog y biblioteca del motor `.pl` |
| Sebastián Ramírez Calderón | Estructura inicial del proyecto, frontend, router y handlers; se retiró del curso después de P1.1 |

> Sebastián entregó su parte antes de salir del curso. Su código continúa acreditado en los archivos correspondientes. Desde P1.1, el proyecto es desarrollado por Kenny Jiménez Wang y José Moya Pérez.

> **Estado: integración funcional de P2.** El proyecto cuenta con lexer, parser, AST, análisis semántico, generación de código Prolog, tabla de variables y un REPL con soporte para estatutos multilínea y `.load`. El frontend utiliza el motor real de SWI-Prolog mediante MQI; el proceso se inicia automáticamente y se cierra al terminar la sesión.

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

## Casos de prueba de P2

Los 16 casos de prueba están en [`casos-p2/`](casos-p2/README.md), uno por archivo,
junto con el grafo contra el que corren. Trece vienen del documento de casos de
referencia del profesor; los otros tres cubren `SET` y `REMOVE`, que ese documento no
incluye y el SPEC sí exige.

El REPL carga `casos-p2/grafo.cyphail` al abrir la sesión, así que los casos corren
de una vez:

```text
java -jar target/cyphail.jar repl
Base graph loaded: 22 statements.
>>> .load "casos-p2/caso-01-relacion-sin-tipo.cyphail"
```

Volver a cargar el grafo a mano duplica los datos. Para reiniciar, se cierra el REPL.

El índice de la carpeta trae la tabla de los 16 casos, las filas que devuelve cada uno
y el orden recomendado, porque seis de ellos modifican el grafo.

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

Ejemplo simplificado de una meta Prolog ejecutable generada durante P2:

```prolog
ensure_loaded('prolog/cyphail_engine.pl'),
cy_execute(
    query(
        [
            match(
                [node(__M, ['Movie'], [])],
                compare(property_access(__M, 'year', __Myear), '>', 2000)
            )
        ],
        [
            return_item(property_access(__M, 'title', __Mtitle), 'title'),
            return_item(property_access(__M, 'year', __Myear), 'year')
        ]
    ),
    Rows
).
```

Si el programa contiene un error sintáctico o una variable no definida, se informa el error y no se genera el archivo de salida.

El generador produce metas ejecutables, transforma las variables de Cyphail en variables Prolog con prefijo `__` y utiliza `Rows` para recibir la tabla producida por el motor.

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

### Expresiones del `WHERE`

La condición es una expresión booleana completa. La precedencia no se declara en
ninguna tabla: está en qué nivel del parser llama a cuál.

| Nivel | Construye | Ejemplo |
|---|---|---|
| 1 (más flojo) | `OrExpression` | `a OR b` |
| 2 | `AndExpression` | `a AND b` |
| 3 | `NotExpression` | `NOT a` |
| 4 | `ComparisonExpression` | `a.x >= 1` |
| 5 | operando o grupo | `a.x`, `( ... )` |

Cada nivel llama al siguiente, así que `a OR b AND c` da `(or a (and b c))` sin que
nadie escriba una regla de precedencia. Los paréntesis vuelven al nivel 1, y no son
un nodo del AST: solo agrupan.

```text
>>> .tree MATCH (p) WHERE (p.a > 1 OR p.b > 2) AND NOT p.c >= 3 RETURN p
Expr: (and (or (> (. p a) 1) (> (. p b) 2)) (not (>= (. p c) 3)))
```

Los niveles 3 y 5 se referencian a sí mismos, para admitir `NOT NOT x` y
`((x))`. Un parser es una función `entrada -> resultado`, así que construirlo
como `source -> nivel().parse(source)` retrasa la construcción hasta que alguien
de verdad parsea; sin eso la recursión no terminaría.

### Reglas del analizador

| Regla | SPEC | Ejemplo rechazado |
|---|---|---|
| Toda variable usada debe estar definida antes | P1 | `MATCH (p) RETURN q` |
| Una variable de relación nombra una sola arista | II.6 | `MATCH (a)-[r]->(b), (c)-[r]->(d)` |
| Un nombre no es nodo y relación a la vez | II.6 | `MATCH (a:N), (b)-[a]->(c)` |
| Una variable no empieza con `__` | II.7 | `MATCH (__person:Persona)` |

Una variable de nodo sí puede repetirse: `MATCH (a)-[r]->(b) MATCH (b)-[s]->(c)` significa
que es el mismo nodo, y es la forma de encadenar dos patrones.

Una variable reutilizada con otra etiqueta **no es un error: las etiquetas se suman**.
La guía deja esa decisión abierta y esta es la que tomamos, porque es la semántica de
Cypher: `MATCH (e:Empresa), (e:Ciudad)` pide un nodo que sea las dos cosas, así que
devuelve vacío salvo que exista uno con ambas etiquetas. No se fusiona quedándose con
la última ni se descarta la segunda.

Las dos últimas existen por el mismo motivo: el codegen convierte cada variable del query
en una de Prolog con el prefijo `__` (`a` pasa a `__A`). Un nombre usado como nodo y como
relación daría la misma variable de Prolog ligada a un nodo y a una arista, y un nombre que
ya empieza con `__` chocaría con los que el generador inventa. La restricción no alcanza a
propiedades ni etiquetas, que se transpilan a átomos: `p.__interno` es válido.

### Tabla de variables

`VariableTable.from(Program)` recorre los patrones del estatuto y devuelve cada variable
con su tipo (`NODE` o `RELATIONSHIP`) en orden de primera aparicion. Solo `MATCH` y `CREATE`
definen variables; `SET`, `REMOVE`, `DELETE` y el `RETURN` unicamente las usan.

```text
MATCH (p:Persona)-[r:TRABAJA_EN]->(e:Empresa) RETURN p
-> { p: NODE, r: RELATIONSHIP, e: NODE }
```

Es una funcion pura del AST: el codegen la llama cuando necesita el mapeo, sin que el
parser tenga que cargarla de etapa en etapa.

## Motor de grafos en SWI-Prolog

El motor anterior de P1 fue reemplazado por la biblioteca:

```text
prolog/cyphail_engine.pl
```

La biblioteca mantiene nodos, relaciones y propiedades durante la sesión mediante los predicados dinámicos `cy_node/2`, `cy_edge/4` y `cy_property/3`. Su punto de entrada es `cy_execute/2`, que ejecuta la representación generada por el codegen y devuelve la tabla textual en `Rows`.

El motor implementa `MATCH`, `CREATE`, `SET`, `REMOVE` y `DELETE`, además de comparaciones, expresiones `AND`, `OR` y `NOT`, caminos con relaciones y la construcción de los resultados de `RETURN`.

## Integración con SWI-Prolog mediante MQI

La integración real se divide en los siguientes componentes:

| Componente | Responsabilidad |
|---|---|
| `MqiProcess` | Inicia y detiene automáticamente el proceso de SWI-Prolog |
| `MqiClient` | Maneja el socket, handshake, framing y timeout del protocolo MQI |
| `MqiResponseDecoder` | Recorre la respuesta JSON con Gson y extrae bindings |
| `SwiPrologEngineBroker` | Ejecuta código Prolog y convierte el binding `Rows` en un `EngineResult` |
| `FrontendSession` | Mantiene el handler y cierra el cliente y el proceso al terminar |
| `FrontendFactory` | Construye el pipeline real conectado con SWI-Prolog |

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
- Biblioteca `prolog/cyphail_engine.pl` para ejecutar las metas generadas.
- Almacenamiento de nodos, relaciones y propiedades durante la sesión.
- Ejecución de `MATCH`, `CREATE`, `SET`, `REMOVE` y `DELETE`.
- Evaluación de comparaciones y expresiones `AND`, `OR` y `NOT`.
- Generación de tablas para los elementos de `RETURN`.

`FrontendFactory` construye una `FrontendSession` con el `SwiPrologEngineBroker`. El REPL y el comando `run` inician SWI-Prolog mediante MQI y cierran el socket y el proceso al terminar.

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
- `PrologVariableTableTest`
- `CompilerEngineBrokerTest`
- `CompileCommandTest`
- `MqiClientIntegrationTest`
- `MqiProcessIntegrationTest`
- `MqiResponseDecoderTest`
- `SwiPrologEngineBrokerIntegrationTest`
- `FrontendFactoryTest`

## Arquitectura

El pipeline de compilación es:

```text
Texto Cyphail
    -> Lexer
    -> Parser
    -> Program (AST)
    -> Analyzer
    -> VariableTable
    -> PrologCodeGenerator
    -> CompilerEngineBroker
    -> SwiPrologEngineBroker
    -> MqiProcess
    -> MqiClient
    -> SWI-Prolog
    -> MqiResponseDecoder
    -> Rows
    -> EngineResult
```

`CompilerEngineBroker` aplica el parser, el análisis semántico y el codegen antes de delegar la ejecución a otra implementación de `EngineBroker`.

Esta separación permitió sustituir el motor simulado de P1 por `SwiPrologEngineBroker` sin modificar el lexer, el parser ni el AST.

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

Ejemplos de la representación utilizada en P2:

| AST | Representación Prolog |
|---|---|
| Variable `m` | `__M` |
| Propiedad `m.title` | `property_access(__M, 'title', __Mtitle)` |
| Número `2000` | `2000` |
| String `"adult"` | `'adult'` |
| Comparación | `compare(property_access(__M, 'year', __Myear), '>', 2000)` |
| Nodo `(m:Movie)` | `node(__M, ['Movie'], [])` |

El generador no modifica el AST ni conserva estado. Recibe un `Program` y devuelve un nuevo `String`.

Para P2 el generador soporta:

- `PathPattern`
- relaciones y direcciones
- `SetStatement`
- `RemoveStatement`
- `DecimalLiteral`
- `AndExpression`, `OrExpression` y `NotExpression`
- los operadores `EQUALS`, `>=` y `<=`
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

## Integración completada en P2

- AST de relaciones integrado con el codegen.
- Metas ejecutables de Prolog.
- Mapeo de `VariableTable` a nombres válidos en Prolog.
- Biblioteca `prolog/cyphail_engine.pl`.
- Soporte de codegen para caminos, relaciones, `SET`, `REMOVE`, decimales y expresiones lógicas.
- `SwiPrologEngineBroker` conectado al frontend.
- Prueba del pipeline completo desde el parser hasta SWI-Prolog.

## Créditos y fuentes

- Arquitectura, gramática y casos de prueba: documentos SPEC suministrados por el profesor.
- Modelo de combinadores: código y sesiones del curso.
- Procesamiento de argumentos de consola: [Picocli](https://picocli.info/).
- Procesamiento JSON: [Gson](https://github.com/google/gson).
- Pruebas automatizadas: [JUnit 5](https://junit.org/junit5/).
- Motor lógico y protocolo MQI: [SWI-Prolog](https://www.swi-prolog.org/).

## Declaración sobre el uso de IA

Se utilizó IA, incluyendo Claude Code de Anthropic y ChatGPT de OpenAI, como apoyo para estudiar conceptos, proponer implementaciones, detectar errores y revisar código.

En la parte de Kenny Jiménez Wang, se utilizó como apoyo en componentes del lexer, combinadores, parser, AST, analizador, `.tree`, REPL multilínea y `.load`. En P2 se sumaron los patrones de relaciones y su dirección, `SET`, `REMOVE`, los operadores `AND`, `OR`, `NOT`, `>=` y `<=`, los paréntesis en el `WHERE`, la tabla de variables, las validaciones de semántica estática nuevas y los archivos de `casos-p2/`.

En la parte de José Moya Pérez, se utilizó como apoyo en la generación de código Prolog, la biblioteca `.pl` del motor, la comunicación mediante MQI, el framing del protocolo, el manejo de sockets, la decodificación de respuestas JSON y las pruebas de integración con SWI-Prolog. El motor fake de P1 fue eliminado en P2.

Los comentarios del código fueron redactados con apoyo de IA para mantener una documentación uniforme en todo el proyecto.

En `casos-p2/`, los casos 1 a 10, 8b y 8c son el texto literal del documento de casos de referencia del profesor. Los casos 11, 12 y 13 fueron redactados con apoyo de IA a partir del SPEC de P2, sección II.4, porque ese documento no cubre `SET` ni `REMOVE`. El grafo `grafo.cyphail` y el índice de la carpeta también fueron elaborados con apoyo de IA.

Los tipos base de los combinadores y algunas implementaciones siguen el modelo presentado por el profesor durante las clases, lo cual se acredita en los encabezados correspondientes.

Cada cambio fue integrado y comprobado mediante pruebas automatizadas o ejecuciones manuales. Los integrantes deben comprender y poder explicar el código que presentan durante la defensa.
