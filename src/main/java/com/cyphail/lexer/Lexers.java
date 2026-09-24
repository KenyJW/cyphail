/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Lexer)
 */
package com.cyphail.lexer;

import java.util.regex.Pattern;
import java.util.List;
import java.util.stream.Stream;

public final class Lexers {

    private Lexers() {
    }

    public static Lexer Number() {
        var re_num = Pattern.compile("\\s*(?<token>\\d+)");

        Lexer lexer = (InputString source) -> {
            var matcher = re_num.matcher(source.input());
            matcher.region(source.index(), source.input().length());

            if (!matcher.lookingAt()) {
                return new Fail<>("No number could be matched");
            }

            var token = matcher.group("token");
            return new Ok<>(new TokenString(TToken.NUM, token),
                            new InputString(source.input(), matcher.end()));
        };

        return lexer;
    }

    public static Lexer Id() {
        var re_id = Pattern.compile("\\s*(?<token>[a-zA-Z_]\\w*)");

        Lexer lexer = (InputString source) -> {
            var matcher = re_id.matcher(source.input());
            matcher.region(source.index(), source.input().length());

            if (!matcher.lookingAt()) {
                return new Fail<>("No identifier could be matched");
            }

            var token = matcher.group("token");
            return new Ok<>(new TokenString(TToken.ID, token),
                            new InputString(source.input(), matcher.end()));
        };

        return lexer;
    }

    public static Lexer Keyword() {
        var re_kw = Pattern.compile(
            "\\s*(?<token>MATCH|WHERE|RETURN|AS|CREATE|SET|DELETE|DETACH"
                + "|NOT|AND|OR|COUNT|SAVE|LOAD|TRUE|FALSE)\\b",
            Pattern.CASE_INSENSITIVE);

        Lexer lexer = (InputString source) -> {
            var matcher = re_kw.matcher(source.input());
            matcher.region(source.index(), source.input().length());

            if (!matcher.lookingAt()) {
                return new Fail<>("No keyword could be matched");
            }
            var token = matcher.group("token");
            var type = TToken.valueOf(token.toUpperCase());
            return new Ok<>(new TokenString(type, token),
                            new InputString(source.input(), matcher.end()));
        };

        return lexer;
    }

	public static Lexer Symbol() {
		var re_sym = Pattern.compile("\\s*(?<token>->|<-|<>|<=|>=|\\.\\.|\\(|\\)|\\[|\\]|\\{|\\}|:|,|;|\\.|-|\\*|=|<|>)");

    Lexer lexer = (InputString source) -> {
        var matcher = re_sym.matcher(source.input());
        matcher.region(source.index(), source.input().length());
        if (!matcher.lookingAt()) {
            return new Fail<>("No symbol could be matched");
        }

        var token = matcher.group("token");
        var type = switch (token) {
            case "->" -> TToken.ARROW_RIGHT;
			case "<-" -> TToken.ARROW_LEFT;
			case "<>" -> TToken.NEQ;
			case "<=" -> TToken.LE;
			case ">=" -> TToken.GE;
			case ".." -> TToken.DOTDOT;
			case "("  -> TToken.LPAREN;
			case ")"  -> TToken.RPAREN;
			case "["  -> TToken.LBRACKET;
			case "]"  -> TToken.RBRACKET;
			case "{"  -> TToken.LBRACE;
			case "}"  -> TToken.RBRACE;
			case ":"  -> TToken.COLON;
			case ","  -> TToken.COMMA;
			case ";"  -> TToken.SEMICOLON;
			case "."  -> TToken.DOT;
			case "-"  -> TToken.DASH;
			case "*"  -> TToken.STAR;
			case "="  -> TToken.EQ;
			case "<"  -> TToken.LT;
			case ">"  -> TToken.GT;
            default   -> throw new IllegalStateException(token);
        };
		return new Ok<>(new TokenString(type, token),new InputString(source.input(), matcher.end()));
    };

    return lexer;
}

public static Lexer StringLiteral() {
                var re_str = Pattern.compile("\\s*\"(?<token>[^\"]*)\"");

        Lexer lexer = (InputString source) -> {
            var matcher = re_str.matcher(source.input());
            matcher.region(source.index(), source.input().length());

            if (!matcher.lookingAt()) {
                return new Fail<>("No string could be matched");
            }

            var token = matcher.group("token");
            return new Ok<>(new TokenString(TToken.STRING, token),
                            new InputString(source.input(), matcher.end()));
        };

        return lexer;
    }
		

    // Un token cualquiera: prueba los cinco lexers en orden.
    // Keyword() va ANTES que Id(), o MATCH se leeria como identificador.
    // Los otros tres no compiten: empiezan con comilla, digito o simbolo.
    public static Lexer anyToken() {
        Parser<InputString, TokenString, String> combinado =
                Parsers.Or(Keyword(),
                        Parsers.Or(StringLiteral(),
                                Parsers.Or(Number(),
                                        Parsers.Or(Id(), Symbol()))));

        return combinado::parse;
    }

    // Convierte el texto completo en la lista de tokens, terminada en EOF.
    // Devuelve Fail si sobra texto que ningun lexer reconocio: el SPEC pide
    // que los errores se reporten como error, no como un resultado incompleto.
    public static Result<InputString, List<TokenString>, String> tokenize(String source) {
        var inicio = new InputString(source, 0);

        return switch (Parsers.Star(anyToken()).parse(inicio)) {

            // Star nunca falla; esta rama existe solo porque el tipo la exige
            case Fail<InputString, List<TokenString>, String> f ->
                    new Fail<>(f.reason());

            case Ok<InputString, List<TokenString>, String> ok -> {
                var resto = ok.rest();
                var sobra = resto.input().substring(resto.index());

                if (!sobra.isBlank()) {
                    yield new Fail<>("Unrecognized character '%s' at position %d"
                            .formatted(sobra.strip().charAt(0), resto.index()));
                }

                var conEof = Stream.concat(ok.token().stream(),
                                           Stream.of(new TokenString(TToken.EOF, "")))
                                   .toList();
                yield new Ok<>(conEof, resto);
            }
        };
    }
}
