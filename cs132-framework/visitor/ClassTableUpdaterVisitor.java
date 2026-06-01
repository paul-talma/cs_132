package visitor;

import hw2.*;
import minijava.syntaxtree.*;
import minijava.visitor.*;

// walks class declarations
// visits field and method declarations
// adds field info and method info to each class in class table
// for main, only visit fields
public class ClassTableUpdaterVisitor extends GJVoidDepthFirst<ClassTable> {
    ClassInfo currentClass;
    MethodInfo currentMethod;

    // visit Main class declarations
    public void visit(MainClass n, ClassTable classTable) {
        // set current class to main
        String className = n.f1.f0.toString();
        currentClass = classTable.getClass(className);

        // visit locals
        n.f14.accept(this, classTable);

        // add arg
        String argName = n.f11.f0.toString();
        currentClass.addField(argName, "String[]");

        // reset current class
        currentClass = null;
    }

    // visit class declarations
    public void visit(ClassDeclaration n, ClassTable classTable) {
        String className = n.f1.f0.toString();
        currentClass = classTable.getClass(className); // set currClassInfo to current class
        n.f3.accept(this, classTable); // visit fields
        n.f4.accept(this, classTable); // visit method declarations
        currentClass = null; // reset current class
    }

    public void visit(ClassExtendsDeclaration n, ClassTable classTable) {
        String className = n.f1.f0.toString();
        currentClass = classTable.getClass(className); // set currClassInfo to current class
        n.f5.accept(this, classTable); // visit fields
        n.f6.accept(this, classTable); // visit method declarations
        currentClass = null; // reset current class
    }

    // visit declarations
    public void visit(VarDeclaration n, ClassTable classTable) {
        String name = n.f1.f0.toString();
        String type = getType(n.f0);
        if (currentMethod == null) {// if a class field
            currentClass.addField(name, type);
        } else {// if a method local var
            currentMethod.addLocal(name, type);
        }
    }

    // process method declarations
    public void visit(MethodDeclaration n, ClassTable classTable) {
        // set current method, record name and type
        String methodName = n.f2.f0.toString();
        String returnType = getType(n.f1);
        currentMethod = new MethodInfo(methodName, returnType);

        // add formal parameters to current method
        n.f4.accept(this, classTable);

        // add local vars to current method
        n.f7.accept(this, classTable);

        // update current class with method
        currentClass.addMethod(methodName, currentMethod);

        // reset current method
        currentMethod = null;
    }

    String getMethodName(MethodDeclaration n) {
        return n.f2.f0.toString();
    }

    // visit method parameter
    public void visit(FormalParameter n, ClassTable classTable) {
        String type = getType(n.f0);
        String name = n.f1.f0.toString();
        // add param to current method declaration parameter list
        currentMethod.addParam(name, type);
    }

    String getType(Type t) {
        switch (t.f0.which) {
            case 0:
                return "int[]";
            case 1:
                return "boolean";
            case 2:
                return "int";
        }
        return ((Identifier) t.f0.choice).f0.toString();
    }
}
