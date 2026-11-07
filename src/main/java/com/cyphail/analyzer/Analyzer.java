/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Analyzer)
 */
package com.cyphail.analyzer;

import com.cyphail.ast.AndExpression;
import com.cyphail.ast.ComparisonExpression;
import com.cyphail.ast.CreateStatement;
import com.cyphail.ast.DeleteStatement;
import com.cyphail.ast.Expression;
import com.cyphail.ast.LiteralExpression;
import com.cyphail.ast.MatchStatement;
import com.cyphail.ast.NodePattern;
import com.cyphail.ast.NotExpression;
import com.cyphail.ast.OrExpression;
import com.cyphail.ast.PathPattern;
import com.cyphail.ast.Program;
import com.cyphail.ast.PropertyAccessExpression;
import com.cyphail.ast.PropertyEntry;
import com.cyphail.ast.RelationshipPattern;
import com.cyphail.ast.RemoveStatement;
import com.cyphail.ast.ReturnItem;
import com.cyphail.ast.SetStatement;
import com.cyphail.ast.Statement;
import com.cyphail.ast.VariableExpression;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Revisa que toda variable usada haya sido definida antes.
// El parser solo mira la forma; esto mira el significado.
public final class Analyzer {

    private Analyzer() {
    }

    // Lo que se sabe en un punto del recorrido: las variables definidas hasta
    // aqui, y el primer error encontrado, si lo hubo.
    private record Scope(Set<String> defined, Optional<String> error) {

        boolean failed() {
            return error.isPresent();
        }

        // Devuelve un Scope nuevo: nada se modifica en el sitio.
        Scope define(Optional<String> variable) {
            return variable
                    .map(name -> new Scope(
                            Stream.concat(defined.stream(), Stream.of(name))
                                  .collect(Collectors.toUnmodifiableSet()),
                            error))
                    .orElse(this);
        }

        Scope fail(String message) {
            return new Scope(defined, Optional.of(message));
        }
    }

    // Retorna el mensaje de error, o vacio si el programa es correcto.
    public static Optional<String> analyze(Program program) {
        // Los chequeos de estructura no dependen del recorrido de ambitos:
        // miran el programa entero de una vez. Van antes porque si el patron
        // es contradictorio no tiene sentido ver que variables estan definidas.
        var structural = reservedName(program)
                .or(() -> repeatedRelationship(program))
                .or(() -> nodeUsedAsRelationship(program));

        if (structural.isPresent()) {
            return structural;
        }

        var afterClauses = clauses(program.clauses(), new Scope(Set.of(), Optional.empty()));

        if (afterClauses.failed()) {
            return afterClauses.error();
        }

        // el RETURN se revisa al final, con todo lo que quedo definido
        return firstError(program.returnItems().stream().map(ReturnItem::expression).toList(),
                          afterClauses.defined());
    }

    // Una variable de relacion nombra UNA arista. Nombrarla dos veces pide que
    // la misma arista este en dos lugares a la vez, asi que es un error. Las de
    // nodo si se repiten: ahi significa "el mismo nodo", y es como se encadenan
    // dos patrones.
    //
    // Se revisa en todo el programa y no clausula por clausula porque las dos
    // clausulas de un mismo estatuto hablan del mismo grafo.
    private static Optional<String> repeatedRelationship(Program program) {
        var names = relationshipVariables(program);

        // frequency recorre la lista por cada nombre, pero un estatuto trae
        // un punado de variables: la claridad vale mas que el ahorro.
        return names.stream()
                .filter(name -> Collections.frequency(names, name) > 1)
                .findFirst()
                .map("Relationship variable '%s' is used more than once"::formatted);
    }

    // El codegen convierte cada variable del query en una de Prolog poniendole
    // este prefijo: "a" pasa a ser "__A". Una variable que ya empieza asi
    // chocaria con los nombres que el generador inventa, y con los del propio
    // engine (__cy...). Por eso el SPEC las prohibe en el fuente.
    private static final String RESERVED_PREFIX = "__";

    // Se revisan los nombres que DEFINE el programa -- los de los patrones y
    // los alias del RETURN -- porque son los unicos que llegan al codegen. Un
    // "__x" que solo se usa y nunca se define ya falla por no estar definido.
    private static Optional<String> reservedName(Program program) {
        var defined = Stream.concat(nodeVariables(program).stream(),
                                    relationshipVariables(program).stream());

        var aliases = program.returnItems().stream()
                .flatMap(item -> item.alias().stream());

        return Stream.concat(defined, aliases)
                .filter(name -> name.startsWith(RESERVED_PREFIX))
                .findFirst()
                .map("Variable '%s' cannot start with '__'"::formatted);
    }

    // Los nombres de relacion del programa, en orden y CON repeticiones: el
    // chequeo de arriba necesita verlas dos veces para detectarlas.
    private static List<String> relationshipVariables(Program program) {
        return program.clauses().stream()
                .flatMap(clause -> clause.paths().stream())
                .flatMap(path -> path.steps().stream())
                .flatMap(step -> step.relationship().variable().stream())
                .toList();
    }

    // Los nombres de nodo: el inicial de cada camino y el de llegada de cada paso.
    private static List<String> nodeVariables(Program program) {
        return program.clauses().stream()
                .flatMap(clause -> clause.paths().stream())
                .flatMap(path -> Stream.concat(
                        path.start().variable().stream(),
                        path.steps().stream().flatMap(step -> step.node().variable().stream())))
                .toList();
    }

    // Un nombre no puede ser nodo en un lado y relacion en otro. El codegen
    // convierte cada variable del query en una de Prolog, asi que las dos
    // apariciones darian la MISMA variable de Prolog ligada a un nodo y a una
    // arista: el query no unificaria nunca.
    private static Optional<String> nodeUsedAsRelationship(Program program) {
        var nodes = nodeVariables(program);

        return relationshipVariables(program).stream()
                .filter(nodes::contains)
                .findFirst()
                .map("Variable '%s' is used as a node and as a relationship"::formatted);
    }

    private static Scope clauses(List<Statement> pending, Scope scope) {
        if (pending.isEmpty() || scope.failed()) {
            return scope;
        }
        return clauses(pending.subList(1, pending.size()), clause(pending.get(0), scope));
    }

    private static Scope clause(Statement statement, Scope scope) {
        return switch (statement) {

            // primero los patrones (que definen variables), despues el WHERE
            case MatchStatement match -> {
                var afterPatterns = patterns(elementsOf(match.patterns()), scope);
                yield match.where()
                        .map(where -> check(where, afterPatterns))
                        .orElse(afterPatterns);
            }

            // CREATE tambien define su variable
            case CreateStatement create -> patterns(elementsOf(create.patterns()), scope);

            // DELETE solo usa: todo objetivo debe estar definido
            case DeleteStatement delete -> scope.error()
                    .or(() -> firstError(delete.targets(), scope.defined()))
                    .map(scope::fail)
                    .orElse(scope);

            // SET usa la variable del objetivo y las del valor; no define ninguna
            case SetStatement set -> scope.error()
                    .or(() -> firstError(set.items().stream()
                            .flatMap(item -> Stream.of(item.target(), item.value()))
                            .toList(), scope.defined()))
                    .map(scope::fail)
                    .orElse(scope);

            // REMOVE solo usa, como DELETE
            case RemoveStatement remove -> scope.error()
                    .or(() -> firstError(remove.targets(), scope.defined()))
                    .map(scope::fail)
                    .orElse(scope);
        };
    }

    // Un elemento del patron: nodo o relacion. Los dos aportan lo mismo al
    // analisis -- una variable opcional y unas propiedades -- asi que el
    // recorrido no tiene que distinguirlos.
    private record Element(Optional<String> variable, List<PropertyEntry> properties) {
    }

    private static Element elementOf(NodePattern node) {
        return new Element(node.variable(), node.properties());
    }

    private static Element elementOf(RelationshipPattern relationship) {
        return new Element(relationship.variable(), relationship.properties());
    }

    // Los elementos de una lista de caminos, en el orden en que se escribieron:
    // el nodo inicial y, por cada paso, su relacion y despues su nodo.
    // La relacion va en el medio para que "(a)-[r]->(b) RETURN r" defina r.
    private static List<Element> elementsOf(List<PathPattern> paths) {
        return paths.stream()
                .flatMap(path -> Stream.concat(
                        Stream.of(elementOf(path.start())),
                        path.steps().stream().flatMap(step -> Stream.of(
                                elementOf(step.relationship()),
                                elementOf(step.node())))))
                .toList();
    }

    // El orden importa: las propiedades del patron se revisan ANTES de definir
    // su variable. Por eso el caso 8 es valido y el 11 no.
    private static Scope patterns(List<Element> pending, Scope scope) {
        if (pending.isEmpty() || scope.failed()) {
            return scope;
        }

        var pattern = pending.get(0);

        var values = pattern.properties().stream().map(entry -> entry.value()).toList();
        var afterValues = firstError(values, scope.defined())
                .map(scope::fail)
                .orElse(scope);

        var afterDefine = afterValues.failed() ? afterValues : afterValues.define(pattern.variable());

        return patterns(pending.subList(1, pending.size()), afterDefine);
    }

    private static Scope check(Expression expression, Scope scope) {
        if (scope.failed()) {
            return scope;
        }
        return usage(expression, scope.defined()).map(scope::fail).orElse(scope);
    }

    private static Optional<String> firstError(List<? extends Expression> expressions, Set<String> defined) {
        return expressions.stream()
                .map(expression -> usage(expression, defined))
                .flatMap(Optional::stream)
                .findFirst();
    }

    // Que variables usa una expresion, y si estan definidas.
    private static Optional<String> usage(Expression expression, Set<String> defined) {
        return switch (expression) {

            case LiteralExpression literal -> Optional.empty();

            case VariableExpression variable -> require(variable.name(), defined);

            case PropertyAccessExpression access -> require(access.variable(), defined);

            case ComparisonExpression comparison ->
                    usage(comparison.left(), defined)
                            .or(() -> usage(comparison.right(), defined));

            // AND revisa los dos lados, igual que una comparacion
            case AndExpression and ->
                    usage(and.left(), defined)
                            .or(() -> usage(and.right(), defined));

            // NOT solo envuelve: lo que importa es lo que trae dentro
            case NotExpression not -> usage(not.operand(), defined);

            // OR revisa los dos lados, igual que el AND
            case OrExpression or ->
                    usage(or.left(), defined)
                            .or(() -> usage(or.right(), defined));
        };
    }

    private static Optional<String> require(String name, Set<String> defined) {
        return defined.contains(name)
                ? Optional.empty()
                : Optional.of("Undefined variable '%s'".formatted(name));
    }
}
