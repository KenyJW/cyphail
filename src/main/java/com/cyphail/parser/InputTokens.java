/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Parser)
 */
package com.cyphail.parser;

import com.cyphail.lexer.Input;
import com.cyphail.lexer.TokenString;

import java.util.List;

public record InputTokens(List<TokenString> input, int index) implements Input<List<TokenString>> {

    public InputTokens(List<TokenString> input) {
        this(input, 0);
    }

    public boolean isEmpty() {
        return index >= input.size();
    }

    public TokenString first() {
        return input.get(index);
    }

    public InputTokens next() {
        return new InputTokens(input, index + 1);
    }
}
