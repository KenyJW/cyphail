% Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
% Autor: Jose Moya Perez (SWI-Prolog Engine)
%
% Motor en memoria para ejecutar los términos generados por el
% PrologCodeGenerator mediante SWI-Prolog MQI.

:- dynamic cy_node/2.
:- dynamic cy_edge/4.
:- dynamic cy_property/3.


% ============================================================
% Punto de entrada
% ============================================================

cy_execute(query(Clauses, ReturnItems), Rows) :-
    cy_headers(ReturnItems, Headers),
    findall(
        Values,
        (
            cy_run_clauses(Clauses),
            cy_return_values(ReturnItems, Values)
        ),
        ResultRows
    ),
    cy_render_result(Headers, ResultRows, Rows).


% ============================================================
% Ejecución de cláusulas
% ============================================================

cy_run_clauses([]).

cy_run_clauses([Clause | Rest]) :-
    cy_run_clause(Clause),
    cy_run_clauses(Rest).


cy_run_clause(match(Patterns, Where)) :-
    cy_match_patterns(Patterns),
    cy_truth(Where).

cy_run_clause(create(Patterns)) :-
    cy_create_patterns(Patterns).

cy_run_clause(set(Items)) :-
    cy_set_items(Items).

cy_run_clause(remove(Targets)) :-
    cy_remove_targets(Targets).

cy_run_clause(delete(Targets)) :-
    cy_delete_targets(Targets).


% ============================================================
% MATCH
% ============================================================

cy_match_patterns([]).

cy_match_patterns([Pattern | Rest]) :-
    cy_match_pattern(Pattern),
    cy_match_patterns(Rest).


cy_match_pattern(node(Variable, Labels, Properties)) :-
    cy_match_node(
        node(Variable, Labels, Properties),
        _
    ).

cy_match_pattern(path(Start, Steps)) :-
    cy_match_node(Start, StartId),
    cy_match_steps(StartId, Steps).


cy_match_node(
    node(Variable, Labels, Properties),
    NodeId
) :-
    cy_node(NodeId, ExistingLabels),
    cy_bind_reference(Variable, NodeId),
    cy_contains_labels(Labels, ExistingLabels),
    cy_match_properties(NodeId, Properties).


cy_match_steps(_, []).

cy_match_steps(
    CurrentNode,
    [step(Relationship, NextNode) | Rest]
) :-
    cy_match_node(NextNode, NextNodeId),
    cy_match_relationship(
        Relationship,
        CurrentNode,
        NextNodeId
    ),
    cy_match_steps(NextNodeId, Rest).


cy_match_relationship(
    relationship(
        Variable,
        Type,
        Properties,
        out
    ),
    LeftNode,
    RightNode
) :-
    cy_edge(EdgeId, StoredType, LeftNode, RightNode),
    cy_match_relationship_data(
        Variable,
        Type,
        Properties,
        EdgeId,
        StoredType
    ).

cy_match_relationship(
    relationship(
        Variable,
        Type,
        Properties,
        in
    ),
    LeftNode,
    RightNode
) :-
    cy_edge(EdgeId, StoredType, RightNode, LeftNode),
    cy_match_relationship_data(
        Variable,
        Type,
        Properties,
        EdgeId,
        StoredType
    ).

cy_match_relationship(
    relationship(
        Variable,
        Type,
        Properties,
        none
    ),
    LeftNode,
    RightNode
) :-
    (
        cy_edge(EdgeId, StoredType, LeftNode, RightNode)
    ;
        cy_edge(EdgeId, StoredType, RightNode, LeftNode)
    ),
    cy_match_relationship_data(
        Variable,
        Type,
        Properties,
        EdgeId,
        StoredType
    ).


cy_match_relationship_data(
    Variable,
    Type,
    Properties,
    EdgeId,
    StoredType
) :-
    cy_bind_reference(Variable, EdgeId),
    cy_match_type(Type, StoredType),
    cy_match_properties(EdgeId, Properties).


cy_match_type(none, _).

cy_match_type(Expected, Stored) :-
    Expected \== none,
    Expected == Stored.


cy_match_properties(_, []).

cy_match_properties(
    EntityId,
    [property(Name, ExpectedExpression) | Rest]
) :-
    cy_eval_expression(ExpectedExpression, ExpectedValue),
    cy_property(EntityId, Name, ExpectedValue),
    cy_match_properties(EntityId, Rest).


cy_contains_labels([], _).

cy_contains_labels(
    [Label | Rest],
    ExistingLabels
) :-
    memberchk(Label, ExistingLabels),
    cy_contains_labels(Rest, ExistingLabels).


% ============================================================
% CREATE
% ============================================================

cy_create_patterns([]).

cy_create_patterns([Pattern | Rest]) :-
    cy_create_pattern(Pattern),
    cy_create_patterns(Rest).


cy_create_pattern(node(Variable, Labels, Properties)) :-
    cy_create_node(
        node(Variable, Labels, Properties),
        _
    ).

cy_create_pattern(path(Start, Steps)) :-
    cy_create_node(Start, StartId),
    cy_create_steps(StartId, Steps).


cy_create_node(
    node(Variable, Labels, Properties),
    NodeId
) :-
    cy_resolve_or_create_node_id(
        Variable,
        NodeId
    ),
    cy_ensure_node(NodeId, Labels),
    cy_write_properties(NodeId, Properties).


cy_resolve_or_create_node_id(
    Variable,
    NodeId
) :-
    var(Variable),
    !,
    cy_new_id(node, NodeId),
    Variable = NodeId.

cy_resolve_or_create_node_id(
    anonymous,
    NodeId
) :-
    !,
    cy_new_id(node, NodeId).

cy_resolve_or_create_node_id(
    NodeId,
    NodeId
).


cy_ensure_node(NodeId, NewLabels) :-
    cy_node(NodeId, ExistingLabels),
    !,
    append(ExistingLabels, NewLabels, AllLabels),
    sort(AllLabels, MergedLabels),
    retractall(cy_node(NodeId, _)),
    assertz(cy_node(NodeId, MergedLabels)).

cy_ensure_node(NodeId, Labels) :-
    sort(Labels, UniqueLabels),
    assertz(cy_node(NodeId, UniqueLabels)).


cy_create_steps(_, []).

cy_create_steps(
    CurrentNode,
    [step(Relationship, NextNode) | Rest]
) :-
    cy_create_node(NextNode, NextNodeId),
    cy_create_relationship(
        Relationship,
        CurrentNode,
        NextNodeId
    ),
    cy_create_steps(NextNodeId, Rest).


cy_create_relationship(
    relationship(
        Variable,
        Type,
        Properties,
        out
    ),
    LeftNode,
    RightNode
) :-
    cy_create_edge(
        Variable,
        Type,
        Properties,
        LeftNode,
        RightNode
    ).

cy_create_relationship(
    relationship(
        Variable,
        Type,
        Properties,
        in
    ),
    LeftNode,
    RightNode
) :-
    cy_create_edge(
        Variable,
        Type,
        Properties,
        RightNode,
        LeftNode
    ).

cy_create_relationship(
    relationship(
        Variable,
        Type,
        Properties,
        none
    ),
    LeftNode,
    RightNode
) :-
    cy_create_edge(
        Variable,
        Type,
        Properties,
        LeftNode,
        RightNode
    ).


cy_create_edge(
    Variable,
    Type,
    Properties,
    Source,
    Target
) :-
    cy_resolve_or_create_edge_id(
        Variable,
        EdgeId
    ),
    cy_normalize_type(Type, StoredType),
    (
        cy_edge(EdgeId, _, _, _)
    ->
        true
    ;
        assertz(
            cy_edge(
                EdgeId,
                StoredType,
                Source,
                Target
            )
        )
    ),
    cy_write_properties(EdgeId, Properties).


cy_resolve_or_create_edge_id(
    Variable,
    EdgeId
) :-
    var(Variable),
    !,
    cy_new_id(edge, EdgeId),
    Variable = EdgeId.

cy_resolve_or_create_edge_id(
    anonymous,
    EdgeId
) :-
    !,
    cy_new_id(edge, EdgeId).

cy_resolve_or_create_edge_id(
    EdgeId,
    EdgeId
).


cy_normalize_type(none, relationship) :-
    !.

cy_normalize_type(Type, Type).


% ============================================================
% SET, REMOVE y DELETE
% ============================================================

cy_set_items([]).

cy_set_items(
    [set_item(Target, ValueExpression) | Rest]
) :-
    cy_set_item(Target, ValueExpression),
    cy_set_items(Rest).


cy_set_item(
    property_access(EntityId, PropertyName, Output),
    ValueExpression
) :-
    cy_eval_expression(ValueExpression, Value),
    retractall(
        cy_property(
            EntityId,
            PropertyName,
            _
        )
    ),
    assertz(
        cy_property(
            EntityId,
            PropertyName,
            Value
        )
    ),
    Output = Value.


cy_remove_targets([]).

cy_remove_targets([Target | Rest]) :-
    cy_remove_target(Target),
    cy_remove_targets(Rest).


cy_remove_target(
    property_access(EntityId, PropertyName, _)
) :-
    retractall(
        cy_property(
            EntityId,
            PropertyName,
            _
        )
    ).


cy_delete_targets([]).

cy_delete_targets([Target | Rest]) :-
    cy_delete_target(Target),
    cy_delete_targets(Rest).


cy_delete_target(EntityId) :-
    cy_edge(EntityId, _, _, _),
    !,
    retractall(cy_edge(EntityId, _, _, _)),
    retractall(cy_property(EntityId, _, _)).

cy_delete_target(EntityId) :-
    retractall(cy_edge(_, _, EntityId, _)),
    retractall(cy_edge(_, _, _, EntityId)),
    retractall(cy_property(EntityId, _, _)),
    retractall(cy_node(EntityId, _)).


% ============================================================
% Propiedades
% ============================================================

cy_write_properties(_, []).

cy_write_properties(
    EntityId,
    [property(Name, Expression) | Rest]
) :-
    cy_eval_expression(Expression, Value),
    retractall(cy_property(EntityId, Name, _)),
    assertz(cy_property(EntityId, Name, Value)),
    cy_write_properties(EntityId, Rest).


% ============================================================
% Expresiones y WHERE
% ============================================================

cy_truth(none).

cy_truth(and(Left, Right)) :-
    cy_truth(Left),
    cy_truth(Right).

cy_truth(or(Left, Right)) :-
    (
        cy_truth(Left)
    ->
        true
    ;
        cy_truth(Right)
    ).

cy_truth(not(Expression)) :-
    \+ cy_truth(Expression).

cy_truth(compare(LeftExpression, Operator, RightExpression)) :-
    cy_eval_expression(LeftExpression, Left),
    cy_eval_expression(RightExpression, Right),
    cy_compare(Operator, Left, Right).

cy_truth(true).


cy_eval_expression(
    property_access(EntityId, PropertyName, Output),
    Value
) :-
    !,
    cy_property(EntityId, PropertyName, Value),
    Output = Value.

cy_eval_expression(
    Expression,
    Value
) :-
    Value = Expression.


cy_compare('<', Left, Right) :-
    Left < Right.

cy_compare('>', Left, Right) :-
    Left > Right.

cy_compare('<=', Left, Right) :-
    Left =< Right.

cy_compare('>=', Left, Right) :-
    Left >= Right.

cy_compare('=', Left, Right) :-
    cy_equal(Left, Right).

cy_compare('<>', Left, Right) :-
    \+ cy_equal(Left, Right).


cy_equal(Left, Right) :-
    number(Left),
    number(Right),
    !,
    Left =:= Right.

cy_equal(Left, Right) :-
    Left == Right.


% ============================================================
% RETURN
% ============================================================
cy_return_values([], []).

cy_return_values(
    [return_item(Expression, _) | Rest],
    [Value | Values]
) :-
    cy_eval_return_expression(Expression, Value),
    cy_return_values(Rest, Values).


cy_eval_return_expression(
    property_access(EntityId, PropertyName, Output),
    Value
) :-
    !,
    (
        cy_property(EntityId, PropertyName, StoredValue)
    ->
        Output = StoredValue,
        Value = StoredValue
    ;
        Output = null,
        Value = null
    ).

cy_eval_return_expression(Expression, Value) :-
    cy_eval_expression(Expression, Value).

cy_headers([], []).

cy_headers(
    [return_item(Expression, Alias) | Rest],
    [Header | Headers]
) :-
    cy_header(Expression, Alias, Header),
    cy_headers(Rest, Headers).


cy_header(_, Alias, Header) :-
    Alias \== none,
    !,
    cy_cell_text(Alias, Header).

    cy_header(
        Expression,
        none,
        "id"
    ) :-
        var(Expression),
        !.

cy_header(
    property_access(_, PropertyName, _),
    none,
    Header
) :-
    !,
    cy_cell_text(PropertyName, Header).

cy_header(_, none, "id").


% ============================================================
% Formato de salida
% ============================================================

cy_render_result([], _, "OK. Statement executed.") :-
    !.

cy_render_result(Headers, ResultRows, Output) :-
    cy_row_text(Headers, HeaderLine),
    cy_separator(HeaderLine, Separator),
    maplist(
        cy_row_text,
        ResultRows,
        RowLines
    ),
    append(
        [
            HeaderLine,
            Separator
        ],
        RowLines,
        TableLines
    ),
    append(
        TableLines,
        ["OK. Query resolved."],
        OutputLines
    ),
    atomics_to_string(
        OutputLines,
        "\n",
        Output
    ).


cy_row_text(Cells, Text) :-
    maplist(
        cy_cell_text,
        Cells,
        CellTexts
    ),
    atomics_to_string(
        CellTexts,
        "\t",
        Text
    ).


cy_cell_text(Value, Text) :-
    string(Value),
    !,
    Text = Value.

cy_cell_text(Value, Text) :-
    atom(Value),
    !,
    atom_string(Value, Text).

cy_cell_text(Value, Text) :-
    number(Value),
    !,
    number_string(Value, Text).

cy_cell_text(Value, Text) :-
    term_string(Value, Text).


cy_separator(HeaderLine, Separator) :-
    string_length(HeaderLine, Length),
    SeparatorLength is max(Length, 10),
    length(Codes, SeparatorLength),
    maplist(=(0'-), Codes),
    string_codes(Separator, Codes).


% ============================================================
% Utilidades
% ============================================================

cy_bind_reference(Variable, _) :-
    Variable == anonymous,
    !.

cy_bind_reference(Variable, Value) :-
    Variable = Value.


cy_new_id(node, Id) :-
    flag(
        cy_node_sequence,
        Current,
        Current + 1
    ),
    Next is Current + 1,
    atom_concat(n, Next, Id).

cy_new_id(edge, Id) :-
    flag(
        cy_edge_sequence,
        Current,
        Current + 1
    ),
    Next is Current + 1,
    atom_concat(e, Next, Id).


% Permite limpiar el grafo durante las pruebas.
cy_reset :-
    retractall(cy_node(_, _)),
    retractall(cy_edge(_, _, _, _)),
    retractall(cy_property(_, _, _)),
    flag(cy_node_sequence, _, 0),
    flag(cy_edge_sequence, _, 0).