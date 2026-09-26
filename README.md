# Cyphail

Prototipo de motor de consultas sobre grafos, con un dialecto propio
inspirado en Cypher. Proyecto grupal del curso **EIF400-II-2026-CLoria**
("Paradigmas de Programación"), Escuela de Informática, **Universidad
Nacional de Costa Rica (UNA)**.

**Grupo 4** (sección 1:00 p.m.)

| Integrante | Responsabilidad principal |
|---|---|
| Kenny | CLI, REPL, lexer, parser, analizador, `.tree` |
| Moya | Engine Broker, Fake Engine |
| Sebastián | Estructura del proyecto Java, Frontend/Router, Handlers — salió del curso después de P1.1 |

> Sebastián alcanzó a entregar su parte (`com.cyphail.frontend`) antes de
> salir del curso, por lo que su código forma parte de este entregable y
> se mantiene acreditado a su nombre en los fuentes correspondientes.
> A partir de P1 el grupo continúa con dos integrantes.

> **Estado: P1 (Lexer/Parser).** El compilador convierte texto de Cyphail
> en un AST y detecta errores de sintaxis y de variables no definidas. La
> ejecución de consultas sigue siendo simulada (`FakeEngine`): la conexión
> real con SWI-Prolog corresponde a un sprint posterior.

## Prerrequisitos

- **JDK 26**, tal como lo exige el SPEC del curso. El `pom.xml` fija
  `maven.compiler.release` en `26`.
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

**Comandos del REPL** (empiezan con `.`):

| Comando | Qué hace |
|---|---|
| `.help` | Muestra la lista de comandos disponibles |
| `.about` | Muestra los autores, el curso y la universidad |
| `.use` | Lista los grafos fingidos disponibles |
| `.use <nombre>` | Finge conectarse a un grafo |
| `.tree <query>` | Parsea la consulta y muestra su AST |
| `.exit` | Sale del REPL |

Cualquier otra línea se envía al motor fake, que devuelve una tabla o un
mensaje de confirmación. Enter en blanco no hace nada.

## El comando `.tree`

Es la forma de verificar que el compilador funciona. Parsea la consulta y,
si puede, recorre el AST mostrándolo con sangría:

```
>>> .tree MATCH (m:Movie) RETURN m.title, m.year AS year
Query
  matchPart
    nodePattern
      variable
        m
      labels
        Movie
  returnPart
    returnItem
      propertyAccess
        variable
          m
        property
          title
    returnItem
      propertyAccess
        variable
          m
        property
          year
      alias
        year
```

Lo que no está presente se omite: un patrón sin etiquetas no muestra el
nodo `labels`, y una proyección sin `AS` no muestra `alias`.

**Un error de sintaxis no muestra árbol**, solo el mensaje:

```
>>> .tree MATCH (p RETURN p
ERROR: Expected RPAREN but found RETURN at token 3
```

**Un error semántico sí muestra el árbol**, y agrega el error al final: el
árbol demuestra que el parser funcionó, y el mensaje que el analizador
también.

```
>>> .tree MATCH (p:Person) WHERE q.age > 60 RETURN q AS name
Query
  ...
ERROR: Undefined variable 'q'
```

## Correr las pruebas

```bash
mvn test
```

Son **165 pruebas**. Entre ellas, `CasosProfesorTest` parsea los once
casos de referencia del sprint con su texto exacto, y `AnalyzerTest`
comprueba que los casos 10 y 11 —los de variables no definidas— sean
rechazados.

## Arquitectura

El camino de una consulta, de texto a árbol:

```
texto  ->  Lexers.tokenize            ->  List<TokenString>
       ->  StatementParsers.program() ->  Program (AST)
       ->  Analyzer.analyze()         ->  errores de variables
       ->  TreeBuilder + render()     ->  salida de .tree
```

Las dos primeras etapas las encadena `CyphailParser.parse(String)`, que
devuelve el `Program` o un `Fail` con el mensaje de error.

| Paquete | Contenido |
|---|---|
| `com.cyphail.cli` | CLI y REPL, con [picocli](https://picocli.info/) |
| `com.cyphail.lexer` | Tipos base (`Result`, `Ok`, `Fail`, `Parser`), combinadores genéricos (`Parsers`) y los lexers (`Lexers`) |
| `com.cyphail.parser` | Parsers sobre tokens: expresiones, patrones, cláusulas y el punto de entrada |
| `com.cyphail.ast` | El AST: `record` y `sealed interface` |
| `com.cyphail.analyzer` | Análisis semántico de variables no definidas |
| `com.cyphail.tree` | Recorrido del AST para `.tree` |
| `com.cyphail.frontend` | Contrato entre el CLI y el motor (Sebastián) |
| `com.cyphail.engine` | `EngineBroker`/`FakeEngine` (Moya) |

### Sobre el lexer y el parser

Se construyeron a mano con **combinadores propios**, sin generadores de
parsers ni librerías de parsing, siguiendo el modelo visto en clase
(`Work.java`, sesiones del 15 y 22 de septiembre).

Un lexer es una función, no un objeto: `Lexers.Number()` no reconoce un
número, sino que **fabrica la lambda** que sabe reconocerlo. Cada parser
devuelve `Ok(resultado, resto)` o `Fail(razón)`, y la entrada
(`InputString`, `InputTokens`) es inmutable: avanzar significa construir
una entrada nueva. Por eso un parser que falla no deja nada consumido y el
siguiente puede intentar desde la misma posición.

Los combinadores genéricos, en `com.cyphail.lexer.Parsers`, sirven tanto
sobre texto como sobre tokens:

| Combinador | Qué hace |
|---|---|
| `Or(p, q)` | prueba `p`; si falla, prueba `q` |
| `Map(p, f)` | transforma el resultado de `p` |
| `And(p, q)` | `p` y después `q`, desde donde `p` quedó |
| `Opt(p)` | cero o una vez; nunca falla |
| `Star(p)` | cero o más veces; nunca falla |
| `Some(p)` | una o más veces |
| `SepBy(p, sep)` | uno o más `p` separados por `sep` |

## Limitaciones conocidas

Están documentadas con pruebas donde corresponde:

- **Patrones de relación** (`-[:FOLLOWS]->`) no se soportan. El lexer los
  tokeniza, pero el AST no tiene una clase para representarlos.
- **`DETACH`** se reconoce pero se descarta: `DeleteStatement` no guarda
  si venía o no.
- **`{}`** (mapa de propiedades vacío) es válido en la gramática y hoy es
  rechazado.
- **Operadores `=`, `<=`, `>=`**: el lexer los reconoce, pero
  `ComparisonOperator` solo tiene `<`, `>` y `<>`.
- **El `RETURN` es obligatorio**; la gramática lo declara opcional.

## Créditos y fuentes

- Arquitectura general, casos de uso y gramática: SPEC del curso
  (`docs/EIF400-II-2026-SPEC_Inicial_Cyphail-CLoria.pdf`,
  `docs/EIF400-II-2026-Arquitectura General Cyphail-CLoria.pdf` y
  `EIF400-II-2026-GrammarCypherSprint1.g4`).
- Modelo de combinadores (`Result`/`Ok`/`Fail`, `Parser`, `Lexer`,
  `InputString`, `Or`): código de clase del profesor, `Work.java`,
  sesiones 18 y 19.
- Casos de prueba de referencia del sprint P1, publicados por el profesor.
- Librería de parsing de argumentos de línea de comandos:
  [picocli](https://picocli.info/) (recomendada por el SPEC).
- Pruebas: [JUnit 5](https://junit.org/junit5/).

## Declaración sobre el uso de IA

Se usó IA (Claude Code, Anthropic) como apoyo durante el desarrollo de
este avance, tanto para estudiar los conceptos como para escribir y
revisar código. El SPEC del curso autoriza el uso de IA para **entender y
estudiar**, pero prohíbe que una IA agéntica genere el proyecto de forma
"zero coding". El tema se conversó con el profesor. Los prompts usados
están disponibles si se requieren para la revisión.
