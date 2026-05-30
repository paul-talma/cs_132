# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a MiniJava compiler built incrementally across homework assignments (hw1–hw5). The current homework is set in `gradle.properties` (`homework=hw3`). Each homework builds on the last:
- **hw1**: Parser (`Parse.java`) — done
- **hw2**: Type checker (`Typecheck.java`) — done
- **hw3**: MiniJava → Sparrow code generation (`J2S.java`) — done
- **hw4**: Sparrow → Sparrow-V via register allocation (`S2SV.java`) — in progress
- **hw5**: Sparrow-V → RISC-V (not yet started)

## Build & Test Commands

```bash
# Build for a specific homework (requires JDK 17+)
JAVA_HOME=/opt/homebrew/Cellar/openjdk/25.0.2/libexec/openjdk.jdk/Contents/Home gradle classes -Phomework=hw4

# Run a program via gradle (reads from stdin)
./gradle_run.sh 4 < testcases/hw4/Factorial.sparrow

# Run manually after building
java -cp "build/classes/java/main:lib/cs132.jar" S2SV < testcases/hw4/Factorial.sparrow

# Diff against expected output
java -cp "build/classes/java/main:lib/cs132.jar" S2SV < testcases/hw4/Factorial.sparrow | diff - testcases/hw4/Factorial.sparrow.out
```

## Testcases

`testcases/hw4/` contains pairs of `*.sparrow` (input) and `*.sparrow.out` (expected stdout). Current tests:
- `Factorial.sparrow` / `Factorial.sparrow.out` — computes 6! = 720
- `strech.sparrow` / `strech.sparrow.out` — larger stress test, expected output is a list of integers

Each `.sparrow.out` file contains the expected printed output of the **fully executed** Sparrow-V program — not the Sparrow-V source text. The translation must be semantics-preserving: running the output Sparrow-V program must produce the same printed values as running the input Sparrow program.

To verify correctness, build the output Sparrow-V program and run it (hw5 infrastructure), or use the reference interpreter provided in `lib/cs132.jar`.

## Architecture

### Two Java versions in use
- Gradle build requires JDK 17+ (`/opt/homebrew/Cellar/openjdk/...`)
- The compiled output targets JDK 8 (`/Library/Java/JavaVirtualMachines/zulu-8.jdk/...`)

### Source layout
```
src/main/java/
  S2SV.java              — hw4 entry point
  hw4/
    RegisterAllocator.java  — top-level orchestrator (stub)
    Graph.java              — directed graph (stub)
    Node.java               — graph node (stub)
    notes.md                — design notes
  visitor/               — mix of JTB-generated and hand-written visitors
  syntaxtree/            — MiniJava AST nodes (JTB-generated, do not modify)
src/parse/java/IR/
  SparrowParser.java     — Sparrow parser (JTB-generated, do not modify)
  syntaxtree/            — Sparrow/Sparrow-V AST nodes
  visitor/               — DepthFirstVisitor base class for Sparrow AST
```

### Entry point

`S2SV.java` reads a Sparrow program from stdin, parses it, and calls `RegisterAllocator.allocate()`:

```java
SparrowParser parser = new SparrowParser(System.in);
IR.syntaxtree.Program program = parser.Program();
RegisterAllocator allocator = new RegisterAllocator(program);
allocator.allocate();
```

`allocate()` must translate and print a valid Sparrow-V program to stdout.

### Sparrow vs. Sparrow-V

Both languages share the same program/function/block/instruction structure. The key differences:

| | Sparrow | Sparrow-V |
|---|---|---|
| Operands | Identifiers (unlimited) | Registers (fixed set) + identifiers for spills |
| Register file | None | 23 registers: `a2–a7`, `s1–s11`, `t0–t5` |
| Extra instructions | — | `id = r` (spill reg to var), `r = id` (load var to reg) |

Sparrow identifiers exclude the register names (`a2`–`a7`, `s1`–`s11`, `t0`–`t5`). In Sparrow-V, all computation uses registers; identifiers only appear in the spill/restore instructions `id = r` and `r = id`, which move values between the register file and the per-function local variable environment.

Full syntax and operational semantics are in `../handouts/Sparrow-and-Sparrow-V.pdf`.

### hw4 pipeline: register allocation

The translation from Sparrow to Sparrow-V proceeds in four stages per function:

1. **Control Flow Graph (CFG)** — build a directed graph where each node is an instruction and edges represent possible control flow (sequential flow, `goto`, `if0` branches). Use `Graph` / `Node` in `hw4/`.

2. **Liveness analysis** — compute `in` and `out` live-variable sets at each CFG node using standard iterative dataflow:
   - `use[n]` = variables read by instruction n before any definition
   - `def[n]` = variables defined by instruction n
   - `out[n]` = ∪ `in[s]` for each successor s
   - `in[n]` = `use[n]` ∪ (`out[n]` − `def[n]`)
   Iterate until a fixed point.

3. **Interference graph** — build an undirected graph on variables. Two variables interfere (share an edge) if one is in the `out` set of a node that defines the other. Function parameters are pre-colored to their argument registers.

4. **Linear scan register allocation** — assign registers to variables using the linear scan algorithm on live intervals. Variables that cannot be assigned a register are spilled: they are kept in the local variable environment and loaded/stored around each use/def with `r = id` / `id = r` instructions.

### Sparrow AST visitor pattern

Visitors extend `IR.visitor.DepthFirstVisitor` (in `src/parse/java/IR/visitor/`). AST nodes are in `IR.syntaxtree` with positional fields `f0`, `f1`, ... matching the grammar productions. Inspect the node class to find which field corresponds to which grammar symbol.

### Output format

The output must be valid Sparrow-V source text, printed to stdout, that can be fed to a Sparrow-V interpreter. Study `testcases/hw4/*.sparrow.out` for the expected format — note that the `.out` files contain the *runtime output* (printed integers), not the Sparrow-V source. Use the hw3 output (Sparrow source) and a reference Sparrow-V program as format guides.
