# HW3 Design: MiniJava → Sparrow

## Pipeline

1. **Build class table** (reuse hw2 visitors) — class hierarchy, fields, methods.
2. **Compute class layouts** — assign byte offsets to fields and vtable slot indices to methods.
3. **Emit Sparrow** — walk the AST and produce a list of `FunctionDecl`s.

---

## Memory Model

**Objects**: `alloc` block where slot 0 is a vtable pointer and slots 1..N are fields (4 bytes each). Parent fields come before subclass fields, so the layout is compatible across the hierarchy.

**Arrays**: `alloc` block where slot 0 is the length and slots 1..N are the elements. `new int[n]` allocates `(n+1)*4` bytes.

**Vtables**: `alloc` block where each slot holds a function pointer. Slot indices are fixed per method name across the hierarchy; a subclass override fills the same slot with its own function pointer. Since Sparrow has no globals, vtables are allocated fresh inside every `new C()`.

---

## Naming

- `main` body → Sparrow function `Main`
- Method `m` in class `C` → Sparrow function `C_m`
- `this` is always the first formal parameter of every method function.

---

## Key Translation Rules

**Expressions** produce a fresh Sparrow identifier holding the result.

| MiniJava                        | Sparrow                                                                                                        |
| ------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| integer literal                 | `Move_Id_Integer`                                                                                              |
| `true`/`false`                  | `Move_Id_Integer` 1 / 0                                                                                        |
| local/param `x`                 | already-bound identifier                                                                                       |
| `e1 + e2`, `e1 - e2`, `e1 * e2` | `Add`, `Subtract`, `Multiply`                                                                                  |
| `e1 < e2`                       | `LessThan`                                                                                                     |
| `!e`                            | `1 - e` via `Subtract`                                                                                         |
| `e1 && e2`                      | short-circuit: branch past `e2` if `e1` is 0                                                                   |
| `a.length`                      | null-check `a`, then `Load a[0]`                                                                               |
| `a[i]`                          | null-check, bounds-check, `Load a[(i+1)*4]`                                                                    |
| `new int[n]`                    | `Alloc (n+1)*4`, store `n` at offset 0                                                                         |
| `new C()`                       | alloc vtable, fill function pointers, alloc object, store vtable ptr at offset 0                               |
| `obj.m(args)`                   | null-check obj, load vtable from `obj[0]`, load `fptr` from vtable at method's slot, `Call fptr(obj, args...)` |

**Statements** emit instructions with no return value.

| MiniJava            | Sparrow                                                                     |
| ------------------- | --------------------------------------------------------------------------- |
| `x = e`             | compile `e`, `Move_Id_Id` into x's identifier                               |
| `a[i] = e`          | null-check, bounds-check, `Store a[(i+1)*4]`                                |
| `println(e)`        | compile `e`, `Print`                                                        |
| `if (e) s1 else s2` | `IfGoto e then_label`, emit s2, `Goto end`, `then_label:`, emit s1, `end:`  |
| `while (e) s`       | `loop:`, compile e, invert, `IfGoto not_e end`, emit s, `Goto loop`, `end:` |

**Null / bounds errors**: before any dereference, emit `IfGoto ptr ok_label` + `ErrorMessage` + `ok_label:`. For bounds, also check `index < 0` and `index >= length`.

---

## Class Layout Details

Walk the inheritance chain root-first to assign offsets. This ensures a subclass object is a valid superset of a parent object:

- Field offsets: parent fields first (offset 4, 8, ...), then own fields.
- Vtable slots: parent methods first (slot 0, 1, ...), then new methods added by the subclass.

When filling a vtable during `new C()`, each slot's function pointer must name the _most-derived_ class that implements that method (i.e. walk down from C, not up).
