# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a MiniJava compiler built incrementally across homework assignments (hw1–hw5). The current homework is set in `gradle.properties` (`homework=hw2`). Each homework builds on the last:
- **hw1**: Parser (`Parse.java`) — done
- **hw2**: Type checker (`Typecheck.java`) — in progress
- **hw3+**: Code generation phases (not yet started)

## Build & Test Commands

```bash
# Build (uses JAVA_HOME for Gradle, which requires JDK 17+)
JAVA_HOME=/opt/homebrew/Cellar/openjdk/25.0.2/libexec/openjdk.jdk/Contents/Home gradle classes -Phomework=hw2

# Run all hw2 tests
./test.sh

# Run a single test (accepts bare name, .java extension optional)
./test.sh BubbleSort
./test.sh BubbleSort-error

# Run manually (requires build first)
java -cp "build/classes/java/main:lib/cs132.jar" Typecheck < testcases/hw2/Basic.java
```

Expected output for each test is in a `.out` file alongside the `.java` file. The type checker prints either `Program type checked successfully` or `Type error`.

**Before submission**: `Typecheck.java` currently has a hardcoded `FileInputStream` for development — switch it back to `System.in`.

## Architecture

### Two Java versions in use
- Gradle build requires JDK 17+ (`/opt/homebrew/Cellar/openjdk/...`)
- The compiled output runs on JDK 8 (`/Library/Java/JavaVirtualMachines/zulu-8.jdk/...`)

### Source layout
- `src/main/java/Typecheck.java` — entry point
- `src/main/java/hw2/` — type checker logic (hand-written)
- `src/main/java/visitor/` — visitors (mix of JTB-generated and hand-written)
- `src/main/java/syntaxtree/` — AST node classes (JTB-generated, do not modify)

### Visitor framework (JTB-generated)
The visitor infrastructure uses the **Generalized Visitor** pattern from JTB 1.3.2. Key base classes in `visitor/`:
- `GJVoidDepthFirst<A>` — visitor that accepts an argument `A`, returns void (used by hw2 visitors)
- `GJDepthFirst<R, A>` — visitor that accepts `A`, returns `R`
- `GJNoArguDepthFirst<R>` — visitor with no argument, returns `R`

AST nodes (in `syntaxtree/`) store children as positional fields `f0`, `f1`, `f2`, ... (e.g., `ClassDeclaration.f1` is the class name `Identifier`). To navigate the tree, look at the syntaxtree node class to find which `fN` field corresponds to which grammar production.

Use `Utils.getIDNameFromIDNode(Identifier n)` to extract a string name from an `Identifier` node.

### Type checker pipeline (`hw2/TypeChecker.java`)

Type checking runs in three phases:

**Phase 1** — `ClassTableBuilderVisitor` (in `visitor/`):
- Walks `MainClass`, `ClassDeclaration`, `ClassExtendsDeclaration`
- Populates `ClassTable` with class names and parent relationships
- Then `ClassTable` validates: all class names unique, no inheritance cycles

**Phase 2** — `SymbolTableBuilderVisitor` (in `visitor/`):
- Walks into each class body to collect fields and methods
- Tracks `currentClass` and `currentMethod` as it descends
- Populates `ClassInfo.fields` (via `VarDeclaration`) and `ClassInfo.methods` (via `MethodDeclaration` + `FormalParameter`)
- Then `ClassTable` validates: unique field names, unique method names, unique param names, no overloads (overriding is allowed only if signatures match exactly)

**Phase 3** — TODO: walk the tree, build per-method symbol tables, type-check statements and expressions.

### Key data model (`hw2/`)
- `ClassTable`: `HashMap<String, ClassInfo>` + ordered `classList`. Has `methodType(className, methodName)` which walks up the inheritance chain.
- `ClassInfo`: name, optional parent, `List<String>` + `Map<String, FieldInfo>` for fields, same pattern for methods.
- `MethodInfo`: name, return type, ordered param list + param map.
- `String`: enum with `INT`, `BOOLEAN`, `ARRAY`, `ID` (where `ID` represents user-defined class types).
- `Utils.getTypeFromTypeString(String)`: maps JTB node class names (e.g. `"IntegerType"`, `"BooleanType"`, `"ArrayType"`) to `String`. Anything else maps to `String.ID`.
- All type errors extend `TypeException` and are caught at the top level in `Typecheck.main`.

### Testcases
`testcases/hw2/` contains pairs of `*.java` and `*.java.out`. Files named `*-error.java` are expected to produce `Type error`; the rest produce `Program type checked successfully`.
