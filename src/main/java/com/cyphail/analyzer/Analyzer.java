/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Analyzer)
 */
package com.cyphail.analyzer;

import com.cyphail.ast.ComparisonExpression;
import com.cyphail.ast.CreateStatement;
import com.cyphail.ast.DeleteStatement;
import com.cyphail.ast.Expression;
import com.cyphail.ast.LiteralExpression;
import com.cyphail.ast.MatchStatement;
import com.cyphail.ast.NodePattern;
import com.cyphail.ast.Program;
import com.cyphail.ast.PropertyAccessExpression;
import com.cyphail.ast.ReturnItem;
import com.cyphail.ast.Statement;
import com.cyphail.ast.VariableExpression;

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
        var afterClauses = clauses(program.clauses(), new Scope(Set.of(), Optional.empty()));

        if (afterClauses.failed()) {
            return afterClauses.error();
        }

        // el RETURN se revisa al final, con todo lo que quedo definido
        return firstError(program.returnItems().stream().map(ReturnItem::expression).toList(),
                          afterClauses.defined());
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
                var afterPatterns = patterns(match.patterns(), scope);
                yield match.where()
                        .map(where -> check(where, afterPatterns))
                        .orElse(afterPatterns);
            }

            // CREATE tambien define su variable
            case CreateStatement create -> patterns(create.patterns(), scope);

            // DELETE solo usa: todo objetivo debe estar definido
            case DeleteStatement delete -> scope.error()
                    .or(() -> firstError(delete.targets(), scope.defined()))
                    .map(scope::fail)
                    .orElse(scope);
        };
    }

    // El orden importa: las propiedades del patron se revisan ANTES de definir
    // su variable. Por eso el caso 8 es valido y el 11 no.
    private static Scope patterns(List<NodePattern> pending, Scope scope) {
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

    private static Optional<String> firstError(List<Expression> expressions, Set<String> defined) {
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
        };
    }

    private static Optional<String> require(String name, Set<String> defined) {
        return defined.contains(name)
                ? Optional.empty()
                : Optional.of("Undefined variable '%s'".formatted(name));
    }
}
