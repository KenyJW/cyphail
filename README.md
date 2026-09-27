# Cyphail

Prototipo de motor de consultas sobre grafos, con un dialecto propio
inspirado en Cypher. Proyecto grupal del curso **EIF400-II-2026-CLoria**
("Paradigmas de Programación"), Escuela de Informática, **Universidad
Nacional de Costa Rica (UNA)**.

**Grupo 4** (sección 1:00 p.m.)

| Integrante | Responsabilidad principal |
|---|---|
| Kenny | CLI, REPL, lexer, parser, analizador, `.tree` |
| Moya | Engine Broker, Fake Engine, JSON fake y generación de código Prolog |
| Sebastián | Estructura del proyecto Java, Frontend/Router, Handlers — salió del curso después de P1.1 |

> Sebastián alcanzó a entregar su parte (`com.cyphail.frontend`) antes de
> salir del curso, por lo que su código forma parte de este entregable y
> se mantiene acreditado a su nombre en los fuentes correspondientes.
> A partir de P1 el grupo continúa con dos integrantes.

> **Estado: P1 (Lexer/Parser).** El compilador convierte texto de Cyphail
> en un AST, detecta errores de sintaxis y variables no definidas, y genera
> una representación textual de Prolog. La ejecución continúa simulada
> mediante `FakeEngine`; la conexión real con SWI-Prolog corresponde a un
> sprint posterior.

## Prerrequisitos

- **JDK 26**, tal como lo exige el SPEC del curso. El `pom.xml` fija
  `maven.compiler.release` en `26`.
- **Apache Maven 3.9+.**

## Compilar el proyecto

Desde la raíz del proyecto, en una consola CMD, PowerShell o bash:

```bash
mvn clean package
```

Esto compila el proyecto, ejecuta las pruebas y genera un JAR ejecutable:

```text
target/cyphail.jar
```

## Ejecutar

### Opción 1: directamente con Java

```bash
java -jar target/cyphail.jar repl
java -jar target/cyphail.jar run ./examples/movies.cyphail
java -jar target/cyphail.jar compile ./examples/codegen-demo.cyphail --out ./target/codegen-demo.pl
java -jar target/cyphail.jar help
```

### Opción 2: con el wrapper `cyphail.bat` en Windows

```bat
cyphail.bat repl
```

`cyphail.bat` solamente verifica que `java` esté disponible y reenvía los
argumentos al JAR. No contiene lógica del motor ni del compilador.

## Compilar Cyphail a Prolog

El comando `compile` procesa un archivo de Cyphail mediante el lexer,
parser, analizador semántico y generador de código Prolog.

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

Ejemplo del código generado:

```prolog
query([match([node(var('m'), ['Movie'], [])], compare(property_access('m', 'year'), '>', 2000))], [return_item(property_access('m', 'title'), 'title'), return_item(property_access('m', 'year'), 'year')]).
```

Si la consulta es válida, se genera el archivo `.pl`. Si existe un error
sintáctico o una variable no definida, se muestra el error y no se genera
el archivo de salida.

## El REPL

```text
> cyphail repl
Welcome to Cyphail-04-1pm v.0.1. August 2026. ESCINF/UNA EIF400-II-2026
Visit www.whatiscyphail.com for more information
Type ".help" for more information and commands
Type ".exit" to quit
>>>
```

### Comandos del REPL

| Comando | Qué hace |
|---|---|
| `.help` | Muestra la lista de comandos disponibles |
| `.about` | Muestra los autores, el curso y la universidad |
| `.use` | Lista los grafos fingidos disponibles |
| `.use <nombre>` | Finge conectarse a un grafo |
| `.tree <query>` | Parsea la consulta y muestra su AST |
| `.exit` | Sale del REPL |

Cualquier otra línea se procesa como un estatuto de Cyphail. Enter en
blanco no hace nada.

Antes de llegar al motor, cada consulta pasa por el parser, el análisis
semántico y el generador de código. Una consulta con errores sintácticos o
variables no definidas se rechaza antes de llegar al `FakeEngine`.

## Datos fake externos

Las respuestas simuladas se encuentran en:

```text
data/fake-responses.json
```

El `FakeEngine` lee este archivo cada vez que ejecuta una consulta. Por
eso los datos pueden modificarse mientras el programa está funcionando y
los cambios se reflejan en el REPL sin recompilar el JAR.

Gson convierte el contenido del archivo en `FakeResponseCatalog` y
`FakeResponseData`. La clase `JsonFakeDataSource` busca la respuesta
asociada con la consulta normalizada.

Si no existe una respuesta específica en el JSON, el motor produce una
respuesta fake genérica.

El JSON contiene respuestas para las consultas anteriores de P1.1 y para
los casos válidos de referencia del profesor. Los casos con variables no
definidas son rechazados por el analizador antes de consultar los datos
fake.

## El comando `.tree`

Es la forma de verificar que el compilador construye correctamente el
AST. Parsea la consulta y recorre el árbol para imprimirlo en el formato
del SPEC de P1: bloques `Query`, `Match`, `Where`, `Updates` y `Return`,
con las expresiones en preorden.

```text
>>> .tree MATCH (m:Movie) WHERE m.year > 1990 RETURN m.title AS title, m.year AS year
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
        {as (. m year) year}
      ]
      Modifiers:[]
    }
  }
}
```

Las expresiones se escriben en prefijo. Un acceso a propiedad se muestra
como `(. m year)` y una comparación como
`(> izquierda derecha)`.

El bloque `Where` se omite cuando no existe `WHERE`. `CREATE` y `DELETE`
aparecen dentro de `Updates`. Una proyección sin `AS` se imprime como
`{(. m title)}`.

### Error sintáctico

Un error de sintaxis no produce un árbol:

```text
>>> .tree MATCH (p RETURN p
ERROR: Expected RPAREN but found RETURN at token 3
```

### Error semántico

Un error semántico sí permite mostrar el árbol porque el parser logró
construirlo. Después se agrega el error encontrado por el analizador:

```text
>>> .tree MATCH (p:Person) WHERE q.age > 60 RETURN q AS name
Query{
  ...
}
ERROR: Undefined variable 'q'
```

En `m.title`, `m` es la variable y `title` es una propiedad. Por eso el
analizador solamente exige que `m` haya sido definida.

## Correr las pruebas

```bash
mvn test
```

El proyecto contiene **177 pruebas automatizadas**.

Entre ellas:

- `CasosProfesorTest` parsea los once casos de referencia del sprint.
- `AnalyzerTest` comprueba la detección de variables no definidas.
- `PrologCodeGeneratorTest` verifica la generación de términos Prolog.
- `CompilerEngineBrokerTest` comprueba el pipeline completo.
- `JsonFakeDataSourceTest` verifica que los cambios del JSON se leen sin
  recompilar.
- `CompileCommandTest` verifica la creación del archivo `.pl` y el manejo
  de errores semánticos.

## Arquitectura

El camino completo de una consulta es:

```text
Texto Cyphail
  -> Lexer
  -> Parser
  -> Program (AST)
  -> Analyzer
  -> PrologCodeGenerator
  -> CompilerEngineBroker
  -> FakeEngine
  -> data/fake-responses.json
  -> respuesta del REPL
```

`CyphailParser.parse(String)` conecta el lexer y el parser. Devuelve un
`Program` cuando la consulta es sintácticamente válida o un `Fail` con el
mensaje del error.

`CompilerEngineBroker` funciona como decorador del broker. Recibe la
consulta, ejecuta el parser y el analizador, genera el código Prolog y
finalmente delega la ejecución al motor fake.

El `FakeEngine` utiliza la consulta original para encontrar la respuesta
en el archivo JSON. El código Prolog generado queda disponible para que
un broker real pueda enviarlo a SWI-Prolog en un sprint posterior.

| Paquete | Contenido |
|---|---|
| `com.cyphail.cli` | CLI, REPL y comandos construidos con Picocli |
| `com.cyphail.lexer` | Tipos base, combinadores y tokenización |
| `com.cyphail.parser` | Parsers de expresiones, patrones y cláusulas |
| `com.cyphail.ast` | AST implementado con `record` y `sealed interface` |
| `com.cyphail.analyzer` | Análisis semántico de variables |
| `com.cyphail.tree` | Recorrido e impresión del AST para `.tree` |
| `com.cyphail.codegen` | Conversión del AST a texto Prolog |
| `com.cyphail.frontend` | Contrato entre el CLI y el motor |
| `com.cyphail.engine` | Contratos del broker, pipeline compilador y motor fake |
| `com.cyphail.engine.data` | Modelos y lectura dinámica de respuestas JSON |

## Sobre el lexer y el parser

El lexer y el parser se construyeron manualmente con combinadores propios,
sin generadores de parsers ni librerías externas de parsing, siguiendo el
modelo visto en clase.

Un lexer es una función. Por ejemplo, `Lexers.Number()` fabrica la función
que sabe reconocer un número. Cada parser devuelve:

- `Ok(resultado, resto)` cuando reconoce la entrada.
- `Fail(razón)` cuando no puede reconocerla.

La entrada, representada por `InputString` o `InputTokens`, es inmutable.
Avanzar significa construir una nueva entrada. Por eso un parser que falla
no altera el valor original y otro parser puede intentar desde la misma
posición.

Los combinadores genéricos sirven tanto para texto como para tokens:

| Combinador | Qué hace |
|---|---|
| `Or(p, q)` | Prueba `p`; si falla, prueba `q` |
| `Map(p, f)` | Transforma el resultado de `p` |
| `And(p, q)` | Ejecuta `p` y después `q` |
| `Opt(p)` | Reconoce cero o una aparición |
| `Star(p)` | Reconoce cero o más apariciones |
| `Some(p)` | Reconoce una o más apariciones |
| `SepBy(p, sep)` | Reconoce elementos separados por un separador |

## Generación de código Prolog

`PrologCodeGenerator` recorre el AST mediante pattern matching sobre las
interfaces selladas y los records.

Ejemplos de traducción:

| AST | Representación Prolog |
|---|---|
| Variable `m` | `var('m')` |
| Propiedad `m.title` | `property_access('m', 'title')` |
| Número `2000` | `2000` |
| String `"adult"` | `'adult'` |
| Comparación `m.year > 2000` | `compare(property_access('m', 'year'), '>', 2000)` |
| Nodo `(m:Movie)` | `node(var('m'), ['Movie'], [])` |

El generador no guarda estado ni modifica el AST. Recibe un `Program` y
devuelve un nuevo `String`, por lo que funciona como una transformación
pura.

## Alcance del lexer frente al parser

El lexer implementa la tabla de tokens completa de la gramática publicada,
incluidos tokens de relaciones, rangos y palabras reservadas.

El parser de P1 consume el subconjunto requerido para:

- `MATCH`
- `WHERE`
- `CREATE`
- `DELETE`
- `RETURN`

Los demás tokens se reconocen, pero todavía no existe una regla del parser
que los consuma.

Esto permite que una relación no soportada falle en el parser con un
mensaje relacionado con la sintaxis esperada, en lugar de fallar en el
lexer como un carácter desconocido.

## Limitaciones conocidas

- Los patrones de relación como `-[:FOLLOWS]->` todavía no forman parte
  del AST de P1.
- `DETACH` se reconoce, pero `DeleteStatement` no conserva esa información.
- El mapa de propiedades vacío `{}` todavía es rechazado.
- Los operadores `=`, `<=` y `>=` son reconocidos por el lexer, pero
  `ComparisonOperator` solamente incluye `<`, `>` y `<>`.
- `RETURN` es obligatorio en el parser actual.
- El motor real de SWI-Prolog todavía no está conectado; durante P1 se usa
  un motor fake con respuestas JSON.

## Principios y decisiones de diseño

- **Responsabilidad única:** lexer, parser, analyzer, Codegen, broker y
  motor fake tienen responsabilidades separadas.
- **Abierto/cerrado:** `EngineBroker` permite agregar posteriormente un
  broker real sin modificar el frontend.
- **Inversión de dependencias:** el frontend depende de `EngineBroker`, no
  directamente de una implementación de SWI-Prolog.
- **Decorator:** `CompilerEngineBroker` agrega compilación y validación
  antes de delegar al motor.
- **Factory:** `FrontendFactory` construye y conecta los componentes.
- **Tipos algebraicos:** el AST utiliza `sealed interface`, `record` y
  pattern matching exhaustivo.
- **Inmutabilidad:** las etapas producen nuevos valores y evitan modificar
  los objetos de entrada.

## Créditos y fuentes

- Arquitectura, casos de uso y gramática: documentos SPEC suministrados
  por el profesor.
- Modelo de combinadores `Result`, `Ok`, `Fail`, `Parser`, `Lexer`,
  `InputString` y `Or`: código y sesiones del curso.
- Casos de prueba de referencia del sprint P1 publicados por el profesor.
- Procesamiento de argumentos de consola:
  [Picocli](https://picocli.info/).
- Manejo de respuestas fake en JSON:
  [Gson](https://github.com/google/gson).
- Pruebas automatizadas:
  [JUnit 5](https://junit.org/junit5/).

## Declaración sobre el uso de IA

Se usó IA, incluyendo Claude Code de Anthropic y ChatGPT de OpenAI, como
apoyo durante el desarrollo para estudiar conceptos, proponer
implementaciones y revisar código.

Los comentarios del código fuente se redactaron con ayuda de IA, con el
fin de mantener una documentación interna uniforme y estructurada en todo
el proyecto.

Cada cambio fue integrado, revisado y comprobado mediante pruebas
automatizadas y ejecución manual. El uso de IA se realizó como apoyo al
proceso de aprendizaje y no sustituye la comprensión ni la defensa del
código por parte de los integrantes.
