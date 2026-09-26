/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (.tree)
 */
package com.cyphail.tree;

import java.util.List;

// Arbol generico de presentacion: solo un nombre y sus hijos.
// Separa QUE se muestra (TreeBuilder) de COMO se escribe (render).
public record TreeNode(String label, List<TreeNode> children) {

    // Hoja: un valor, sin hijos. Por ejemplo el nombre "m".
    public static TreeNode leaf(String label) {
        return new TreeNode(label, List.of());
    }

    public static TreeNode node(String label, List<TreeNode> children) {
        return new TreeNode(label, children);
    }

    public static TreeNode node(String label, TreeNode... children) {
        return new TreeNode(label, List.of(children));
    }

    // Un nombre con un unico hijo hoja: variable -> m
    public static TreeNode field(String label, String value) {
        return new TreeNode(label, List.of(leaf(value)));
    }

    // FASE B: el arbol como texto, con dos espacios por nivel.
    public String render() {
        return render(0);
    }

    private String render(int level) {
        var here = "  ".repeat(level) + label + System.lineSeparator();

        return children.stream()
                .map(child -> child.render(level + 1))
                .reduce(here, String::concat);
    }
}
