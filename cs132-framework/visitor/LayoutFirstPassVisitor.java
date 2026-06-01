package visitor;

import hw3.*;
import minijava.syntaxtree.*;
import minijava.visitor.*;

/**
 * Pass 1: builds the class table skeleton.
 *
 * Walks every class declaration in the program and registers a ClassInfo
 * containing only the class name and (if present) parent name. Fields and
 * methods are left empty — they are filled in by LayoutSecondPassVisitor.
 *
 * After this pass, the class table can be used to check for duplicate class
 * names and to resolve inheritance relationships.
 */
public class LayoutFirstPassVisitor extends GJVoidDepthFirst<ClassTable> {

    public void visit(MainClass n, ClassTable c) {
        String className = getNameFromIdentifierNode(n.f1);
        ClassInfo classInfo = new ClassInfo(className);
        classInfo.setMain();
        c.addClassInfo(className, classInfo);
    }

    public void visit(ClassDeclaration n, ClassTable c) {
        String className = getNameFromIdentifierNode(n.f1);
        c.addClassInfo(className, new ClassInfo(className));
    }

    public void visit(ClassExtendsDeclaration n, ClassTable c) {
        String className = getNameFromIdentifierNode(n.f1);
        String parentClassName = getNameFromIdentifierNode(n.f3);
        c.addClassInfo(className, new ClassInfo(className, parentClassName));
    }

    String getNameFromIdentifierNode(Identifier id) {
        return id.f0.toString();
    }
}
