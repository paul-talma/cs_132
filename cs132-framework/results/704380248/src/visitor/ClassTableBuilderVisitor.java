package visitor;

import hw2.*;
import minijava.syntaxtree.*;
import minijava.visitor.*;

// walks class declarations and records names and parents in class table
public class ClassTableBuilderVisitor extends GJVoidDepthFirst<ClassTable> {
    public void visit(MainClass n, ClassTable classTable) {
        String className = getClassName(n);
        ClassInfo mainInfo = new ClassInfo(className);
        classTable.addClassInfo(className, mainInfo);
    }

    public void visit(ClassDeclaration n, ClassTable classTable) {
        String className = getClassName(n);
        ClassInfo classInfo = new ClassInfo(className);
        classTable.addClassInfo(className, classInfo);
    }

    public void visit(ClassExtendsDeclaration n, ClassTable classTable) {
        String className = getClassName(n);
        String parentName = getParentName(n);
        ClassInfo classInfo = new ClassInfo(className, parentName);
        classTable.addClassInfo(className, classInfo);
    }

    String getClassName(MainClass n) {
        return n.f1.f0.toString();
    }

    String getClassName(ClassDeclaration n) {
        return n.f1.f0.toString();
    }

    String getClassName(ClassExtendsDeclaration n) {
        return n.f1.f0.toString();
    }

    String getParentName(ClassExtendsDeclaration n) {
        return n.f3.f0.toString();
    }

}
