package hw2;

import minijava.syntaxtree.*;
import visitor.*;

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

    /**
     * Builds initial class table
     * Verifies that all class names are unique
     * Verifies that subtype is acyclic
     * 
     * @throws TypeException
     */
    void phaseOne() throws TypeException {
        buildClassTable();
        classTable.allClassesUnique();
        classTable.acyclicTypes();
    }

    void buildClassTable() {
        ClassTableBuilderVisitor classVisitor = new ClassTableBuilderVisitor();
        goal.accept(classVisitor, classTable);
    }

    void augmentClassTable() {
        ClassTableUpdaterVisitor stVisitor = new ClassTableUpdaterVisitor();
        goal.accept(stVisitor, classTable);
    }

    /**
     * Augments class table with symbol information
     * classTable[class] now contains (fieldName, Type) map
     * and list of MethodInfo objects
     * Verifies that field names are unique
     * 
     * @throws TypeException
     */
    void phaseTwo() throws TypeException {
        augmentClassTable();
        classTable.allMethodParamsAndLocalsUnique();
        classTable.noOverloads();
    }

    void phaseThree() throws TypeException {
        SymbolTableVisitor tcVisitor = new SymbolTableVisitor(classTable);
        goal.accept(tcVisitor, symbolTable);
    }

}
