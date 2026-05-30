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

`testcases/hw4/` contains pairs of `*.sparrow` (input) and `*.sparrow.out` (expected **runtime** output). The test runner is `test_runners/test_hw4.sh`.

Each `.sparrow.out` file contains the expected printed output of the **fully executed** Sparrow-V program — not the Sparrow-V source text. The translation must be semantics-preserving: running the output Sparrow-V program must produce the same printed values as running the input Sparrow program.

### Running the Sparrow-V interpreter

```bash
# Translate Sparrow → Sparrow-V and run:
java -cp "build/classes/java/main:lib/cs132.jar" S2SV < file.sparrow \
  | java -jar sparrow_interpreter/sparrow-1.jar sv

# Run Sparrow directly (reference interpreter):
java -jar sparrow_interpreter/sparrow-1.jar s file.sparrow
```

### Writing Sparrow test cases

Sparrow syntax rules to remember:
- **Store/Load use `[base + literal_offset]`** — offset must be a compile-time integer literal and **must be a multiple of 4**. For variable-indexed arrays, compute the address first: `ptr = arr + i; [ptr + 0] = val`
- **All arithmetic operands must be identifiers** — integer literals cannot appear in arithmetic; assign to a variable first: `zero = 0; neg = zero - i`
- **Identifiers cannot be register names** (`a2`–`a7`, `s1`–`s11`, `t0`–`t5`)
- **Call args are identifiers** — the call instruction `r = call f(id...)` looks up each arg in the caller's identifier environment `E`; materialize register values to identifiers before calling

### Getting expected output for a new test

Run the Sparrow reference interpreter to get the correct output, then save it as the `.sparrow.out` file:
```bash
java -jar sparrow_interpreter/sparrow-1.jar s testcases/hw4/mytest.sparrow > testcases/hw4/mytest.sparrow.out
```

## Architecture

### Two Java versions in use
- Gradle build requires JDK 17+ (`/opt/homebrew/Cellar/openjdk/...`)
- The compiled output targets JDK 8 (`/Library/Java/JavaVirtualMachines/zulu-8.jdk/...`)

### Source layout
```
src/main/java/
  S2SV.java                    — hw4 entry point
  hw4/
    Translator.java            — Sparrow → Sparrow-V instruction emitter
    LinearScanVisitor.java     — computes live intervals per function
    LinearScanAllocator.java   — linear scan register allocator (two-pool)
    IntervalList.java          — sorted live-interval container
    Interval.java              — single variable interval [start, end, crossesCall]
    Active.java                — active-set for linear scan (sorted by end)
    Home.java                  — register or identifier home for a variable
    FunctionAllocation.java    — maps varName → Home for one function
    Allocator.java             — interface for allocators
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

Per-function pipeline in `Translator.visit(FunctionDeclaration)`:

1. **Live-interval computation** (`LinearScanVisitor`) — single sequential pass over instructions. Each variable gets interval `[start, end]` where start = first def position and end = last use position. Backward branches (`goto`/`if0` to an earlier label) extend intervals for variables that straddle the branch target. A second pass marks `interval.crossesCall = true` for any variable whose interval spans a call site.

2. **Register allocation** (`LinearScanAllocator`) — standard linear-scan algorithm over intervals sorted by start. Two register pools:
   - `freeCalleePool` = s1–s11: preferred for cross-call variables (preserved by callee, no per-call save needed)
   - `freeCallerPool` = t0–t3: preferred for non-cross-call variables (avoids unnecessary callee-save overhead)
   Falls back to the other pool when the preferred is exhausted. Spills the farthest-reaching active interval when both pools are empty.

3. **Translation** (`Translator`) — emits Sparrow-V instructions:
   - **Function prologue**: save only the callee-saved registers actually used (`usedCalleeSavedRegs`), then load parameters from identifier environment `E` into their allocated registers.
   - **Function epilogue**: spill return value to `E`, restore callee-saved registers.
   - **Call sites**: before each call, materialize args from registers to `E`; save only the caller-saved registers (t0–t3) that hold a variable live past this call site; after the call, restore them (skipping the LHS register).
   - **Scratch registers**: `t4` and `t5` are never allocated to variables — used as temporaries within a single instruction's translation.

### Callee-save convention

Our generated functions follow: save every callee-saved register in `usedCalleeSavedRegs` at function entry (before any instruction), restore at function exit (after all instructions, before `return`). Identifiers are `callee_saved_<regname>` in the function's local `E`. Since each call gets its own `E`, these don't conflict across call frames.

### Sparrow AST visitor pattern

Visitors extend `IR.visitor.DepthFirstVisitor` (in `src/parse/java/IR/visitor/`). AST nodes are in `IR.syntaxtree` with positional fields `f0`, `f1`, ... matching the grammar productions. Inspect the node class to find which field corresponds to which grammar symbol.

### Output format

The output must be valid Sparrow-V source text, printed to stdout, that can be fed to a Sparrow-V interpreter. Study `testcases/hw4/*.sparrow.out` for the expected format — note that the `.out` files contain the *runtime output* (printed integers), not the Sparrow-V source. Use the hw3 output (Sparrow source) and a reference Sparrow-V program as format guides.
