package hw2;

import minijava.syntaxtree.*;
import visitor.*;

/**
 * Orchestrates type-checking of a MiniJava program in three phases:
 *
 * Phase 1 — build the class table: collect all class names and parent
 * relationships, then verify that class names are unique and the inheritance
 * graph is acyclic.
 *
 * Phase 2 — populate the class table: collect fields and methods for each
 * class, then verify that field/method/param names are unique within their
 * scope and that subclass method overrides match the parent signature.
 *
 * Phase 3 — type-check expressions and statements: walk the AST with a
 * symbol table, verify that every expression has a well-formed type, and
 * that assignments and calls are type-compatible.
 */
public class TypeChecker {
    Goal goal;
    ClassTable classTable = new ClassTable();
    SymbolTable symbolTable = new SymbolTable();

    public TypeChecker(Goal g) {
        this.goal = g;
    }

    public void typeCheck() throws TypeException {
        phaseOne();
        phaseTwo();
        phaseThree();
    }

    /** Phase 1: build class table, check uniqueness and acyclicity. */
    void phaseOne() throws TypeException {
        buildClassTable();
        classTable.allClassesUnique();
        classTable.acyclicTypes();
    }

    void buildClassTable() {
        ClassTableBuilderVisitor classVisitor = new ClassTableBuilderVisitor();
        goal.accept(classVisitor, classTable);
    }

    /** Phase 2: populate fields/methods, check name uniqueness and no overloads. */
    void phaseTwo() throws TypeException {
        augmentClassTable();
        classTable.allMethodParamsAndLocalsUnique();
        classTable.noOverloads();
    }

    void augmentClassTable() {
        ClassTableUpdaterVisitor stVisitor = new ClassTableUpdaterVisitor();
        goal.accept(stVisitor, classTable);
    }

    /** Phase 3: type-check all statements and expressions. */
    void phaseThree() throws TypeException {
        SymbolTableVisitor tcVisitor = new SymbolTableVisitor(classTable);
        goal.accept(tcVisitor, symbolTable);
    }
}
