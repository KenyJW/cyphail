# Casos de prueba de P2

Los casos de prueba de P2, uno por archivo, con el grafo contra el que corren en
`grafo.cyphail`. Vienen de dos fuentes distintas:

- **Casos 1 a 10, 8b y 8c:** el documento de referencia
  `EIF400-II-2026-Cypahil-P2-10-Casos_Prueba_Unitarios.pdf`, con el texto literal del
  profesor. Ese documento cubre `MATCH`, `CREATE`, `DELETE`, `WHERE` y `RETURN`.
- **Casos 11 a 13:** escritos por nosotros a partir del SPEC de P2, sección II.4,
  porque el documento de referencia **no incluye `SET` ni `REMOVE`** y son features
  obligatorias del sprint.

## Cómo correrlos

Desde la raíz del proyecto:

```text
java -jar target/cyphail.jar repl
>>> .load "casos-p2/grafo.cyphail"
>>> .load "casos-p2/caso-01-relacion-sin-tipo.cyphail"
```

El grafo **no** se carga solo al arrancar: el primer `.load` es el paso uno de la demo.

**Para volver al estado inicial hay que reiniciar el REPL, no recargar el grafo.**
Un segundo `.load "casos-p2/grafo.cyphail"` duplica los datos: las 5 personas pasan
a ser 10, y los conteos de abajo dejan de cuadrar.

## Los casos

El orden de la tabla es el recomendado: primero los que solo leen, al final los tres
que modifican el grafo. Las filas son las que devuelve cada caso partiendo del grafo
recién cargado.

| # | Archivo | Qué ejercita | Filas |
|---|---|---|---|
| 1 | `caso-01-relacion-sin-tipo.cyphail` | `-[r]->` sin tipo, `RETURN` de una relación completa | 1 |
| 2 | `caso-02-tipo-de-relacion.cyphail` | `-[:TIPO]->` sin variable | 6 |
| 3 | `caso-03-direccion-inversa.cyphail` | dirección `<-` (IN) | 4 |
| 3b | `caso-03b-sin-direccion.cyphail` | dirección `-` (NONE) | 12 |
| 4 | `caso-04-propiedades-inline.cyphail` | propiedades en la relación, `r.prop` | 1 |
| 5 | `caso-05-where-nodo-y-relacion.cyphail` | `WHERE` sobre nodo y relación a la vez | 4 |
| 6 | `caso-06-or-y-not.cyphail` | `OR`, `NOT`, `>=` | 2 |
| 8b | `caso-08b-variable-compartida.cyphail` | dos patrones que comparten `e` | 4 |
| 8c | `caso-08c-patrones-desconectados.cyphail` | patrones sin conexión, producto cartesiano | 169 |
| 10 | `caso-10-integrador.cyphail` | tres patrones, paréntesis, `AND`/`OR`/`NOT`, decimales | 2 |
| 7 | `caso-07-create-con-match.cyphail` | **modifica:** `CREATE` que reusa variables del `MATCH` | 0 |
| 8 | `caso-08-create-sin-match.cyphail` | **modifica:** `CREATE` sin `MATCH`, `RETURN a, r, e` | 1 |
| 9 | `caso-09-delete-relacion.cyphail` | **modifica:** `DELETE` de una relación con `WHERE` | 0 |
| 11 | `caso-11-set-multiple.cyphail` | **modifica:** `SET` con varias asignaciones | 1 |
| 12 | `caso-12-remove-multiple.cyphail` | **modifica:** `REMOVE` de varias propiedades | 1 |
| 13 | `caso-13-set-en-relacion.cyphail` | **modifica:** `SET` sobre una relación, con decimal | 1 |

Los seis últimos van al final a propósito. El 9 borra las relaciones `CONOCE`
anteriores al 2000, así que si se corre antes, los casos 2, 3b y 8c devuelven menos
filas de las que dice la tabla.

Dos conteos que conviene poder explicar:

- **8c da 169** porque es 13², el producto cartesiano de las 13 relaciones del grafo.
  Si diera otra cosa, el join estaría asumiendo una conexión que el patrón no pide.
- **10 da 2 y no 1.** Las dos filas son Ana–Marta–Acme, pero con distinto `k.desde`
  (2012 y 2015): `(p)-[k:CONOCE]-(q)` va sin dirección y engancha las dos relaciones
  que existen entre Ana y Marta.
- **12 devuelve `Ana null null`.** Retornar una propiedad recién quitada da `null` y la
  fila sobrevive; no desaparece. Es el comportamiento correcto y conviene mostrarlo.

## Sobre el grafo

Los valores de `grafo.cyphail` no son decorativos: cada uno está puesto para que algún
caso distinga. Si se cambian, los conteos de la tabla dejan de valer.

| Dato | Existe por |
|---|---|
| `desde` en 1999, 2005, 2012, 2015, 2016, 2019, 2021 | los umbrales 2000 (caso 9), 2010 (caso 5), 2015 (caso 4) y 2018 (casos 6 y 10) |
| Sofía, Dev en Globex (`pais: 'MX'`) | la segunda rama del `OR` del caso 6, `NOT e.pais = 'CR'` |
| Jane y John, etiqueta `Person` en inglés | el caso 1, que busca `(p:Person)` y no `(p:Persona)` |
| `porcentaje: 30.5` y `12.75` | los decimales del caso 10 |
| `activo` verdadero y falso | el filtro `{activo: true}` del caso 10 |

## Las dos validaciones que la guía deja abiertas

- **Etiqueta repetida en una variable reutilizada.** No es error: las etiquetas se
  suman. `MATCH (e:Empresa), (e:Ciudad)` pide un nodo que sea las dos cosas y devuelve
  vacío si no existe. Es la semántica de Cypher.
- **Una relación no se usa dos veces en un query.** Sí es error, y el analizador lo
  rechaza: `Relationship variable 'r' is used more than once`. Lo pide el SPEC II.6.

## La prueba automática

`src/test/java/com/cyphail/casos/CasosP2Test.java` recorre todos los `.cyphail` de esta
carpeta y, por cada estatuto, verifica tres cosas: que parsea, que el analizador lo
acepta y que el codegen no falla.

No levanta SWI-Prolog, así que corre en cualquier máquina. Las dos primeras son
necesarias por separado: `MATCH (a:N), (b)-[a]->(c)` parsea perfecto y es inválido, y
un caso así pasaría una prueba que solo mirara la sintaxis.

Un archivo nuevo en esta carpeta entra solo en la prueba: se descubren, no se listan.
