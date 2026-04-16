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

# Lecture 2.2 - Type Checking

```
 h1 h2 h3 ... hn
------------------
   conclusion
```

Interpreted as `(h1 & h2 & ... & hn) ==> conclusion`

## Expressions

Consider `(3 + 5) + 7`

Bottom-up type checking: 3, 5 primitively typed as `int`.
`3 + 5` derived as `int`.
`7` primitively typed as `int`.
`(3 + 5) + 7` derived as int.

Rule representation:

`t ::= int | boolean`

```
 a : int, b : int
--------------------
    a + b : int

e : t  ~ "e has type t"
```

Note: type checking is Turing-complete in general; we want linear-time type checking ==> some valid programs won't type check.

Side-conditions:

```
a : t      b : s
-----------------  (t = int, s = int)
  a + b : int
```

Expressions of the form `a : t` represent (recursive) calls to the type checker.
Expressions of the form `t = int` are "side-checks."

```java
class Type {
    ...
}

class TypeChecker extends DepthFirstVisitor {
    ...
    Type visit(IntegerConstant n) {
        return Type(int);
    }
    Type visit(Plus n) {
        Type t1 = n.e1.accept(this); // recursive calls
        Type t2 = n.e2.accept(this);
        if (t1 != Type(int) || t2 != Type(int)) {
            // throw Type Exception
        }
        return Type(int);
    }
}
```

```
e ::= new int [e] | e1[e2] | e1.length

  e1 : int[]    e2 : int
---------------------------
      e1[e2] : int[]


      e : int[]
---------------------
  e.length() : int


```

(can generalize over `int`).

## Identifiers

```
id ::= field | parameter | local variable
```

Local declarations maintained in a symbol table/type environment, usually denoted `A`.

`A`:
| id | type |
| --- | ------- |
| x | int |
| y | boolean |
| ... | ... |

## Judgements

A |- e : t ~ in type env A, e has type t

```

  A |- e1 : int[]      A |- e2 : int
---------------------------------------
        A |- e1.length() : int


  A |- id : A(id)


```

## Parameters

Note: cannot redeclare parameters or variables (all parameters and local variables must be distinct).

## Statements

```
A |- s
```

- Note that statements don't return anything (in MiniJava) and so no need to assign type to `s`

e.g.

```
  A(x) = t1    A |- e : t2
---------------------------- (t1 = t2)
       A |- (x = e)
```

```java
class TypeChecker extends DepthFirstVisitor {
    void visit(AssignentStatement n, TypeEnv A) {
        Type t1 = A.lookup(n.x);
        Type t2 = n.e.visit(this);
        if (t1 != t2) {
            // throw type error
        }

    }
}
```

# Lecture 3.1

```java
class X {
    t x
    ...

    u m (t1 a1, ...) {
        u1 b1
        ...
        return e;
    }
}
```

type checking `m`:

```
A = fields(C)
    parameters(m)
    localVars(m)
```

# Lecture 3.2

Compiling factorial in minijava:

```java
class Factorial {
    public static void main(String[] a) {
        System.out.println(new Fac().ComputeFac(6));
    }
}

class Fac {
    public int ComputeFac(int num) {
        int num_aux;
        if (num < 1) {
            num_aux = 1;
        } else {
            num_aux = num * (this.ComputeFac(num-1));
        }
        return num_aux;
    }
}
```

Compiles to Sparrow IR.

## Sparrow

No classes: function names build in class provenance.

Label-and-goto language.

### Sparrow grammar

Very few constants allowed

### Sparrow execution model

Environment: represents parameters and local variables by mapping identifiers to values

Program state: (p, H, b\*, E, b)

- p: program
- H: heap
- b\*: body of currently executing function
- E: environment
- b: current block (b is block-to-go)
