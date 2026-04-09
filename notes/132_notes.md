# Lecture 1.2

REs limited to lexing; recognizing real-world grammars is usually beyond their scope.
CFGs limited to non-contextual information - type checker can verify non-local dependencies

## Leftmost v. rightmost derivations

- leftmost/rightmost node in derivation is expanded

## Top-down v bottom-up parsing

Top-down parsers correspond to leftmost derivations
Bottom-up parsers correspond to rightmost derivations

- top down
    - start at root symbol
    - pick a production rule; try to match input
    - may require backtracking if wrong production is chosen
        - some grammars don't require backtracking (predictive grammars)

- bottom up
    - start at leaves and fill in
    - start in state valid for legal first tokens
    - consume input and encode possibilities for following tokens
    - store state and sentential forms in a stack

### Desiderata

- no backtracking
    - backtracking may require nonlinear time

- termination!
    - left-recursive rules in a leftmost derivation may prevent termination
    - can convert left-recursive rules to right-recursive rules, solving the problem
        - transform
          <expr> ::= <expr> <op> <expr>
          into
          <expr> ::= <term> <expr'>
          <expr'> ::= + <term> <expr'> | eps

- left-factorization
    - want all productions for a nonterminal to start with a distinct symbol, so we know which production to apply using 1-step lookahead
    - can factor out longest common prefixes
    - this almost yields LL(1), except for epsilons

## Predictive parsing

- FIRST(a) is set of tokens(terminals) that appear first in a string derived from a
- Condition: for any two rules A -> a and A -> b, want FIRST(a) \cap FIRST(b) = \empty
    - this allows for single-step lookahead

Nullable:

- Nullable(a) iff a ->\* eps
- i.e. a can go to eps

First:

- recursively add firsts for rhs if nullable

Follow:

- for nonterminal B, Follow(B) is set of terminals that can appear immediately to the right of B

- EOF always follows Goal G
- if A -> aBb, then First(b) \subset Follow(B)
    - if b is Nullable, then Follow(A) \subset Follow(B)
        - if b null, then what follows B is whatever follows A.

## LL(1) Grammar

Intuition:

- G is LL(1) iff for all nonterminal A, each pair of productions
  A -> b
  A -> g
  satisfy First(b) \cap First(g) = \empty

Formally:

- G is LL(1) iff for each productions A -> a1 | a2 | ... | a_n

1. First(a_i) are pairwise disjoint
2. If Nullable(a_i), then for all other a_j, First(a_j) \cap Follow(A) = \empty

Note: condition 1 implies that at most one a_i is nullable.

Note:

- LL(1) -> not-left recursive
- LL(1) -> not ambiguous
- Some languages are not LL(1)
- An eps-free grammar where each productions for an A start with distinct symbols is a _simple_ LL(1) grammar
