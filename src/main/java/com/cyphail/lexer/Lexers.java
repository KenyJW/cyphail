/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Lexer)
 Basado en el modelo de combinadores visto en clase (Work.java, 15/09/2026).
 */
package com.cyphail.lexer;

import java.util.regex.Pattern;

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
		

    public static <I, T, R> Parser<I, T, R> Or(Parser<I, T, R> p, Parser<I, T, R> q) {
        Parser<I, T, R> parser = (I source) -> {
            var result = p.parse(source);
            if (!(result instanceof Fail<I, T, R>)) {
                return result;
            }
            return q.parse(source);
        };
        return parser;
    }
	
	
	
}
