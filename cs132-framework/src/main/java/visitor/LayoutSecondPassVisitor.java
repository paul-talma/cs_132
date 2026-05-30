package visitor;

import hw3.*;
import minijava.syntaxtree.*;
import minijava.visitor.*;

/**
 * Pass 2: populates each ClassInfo with its declared fields and methods.
 *
 * For each class, visits VarDeclarations (fields) and MethodDeclarations.
 * For each method, visits FormalParameters and local VarDeclarations.
 * Results are stored directly on the ClassInfo/MethodInfo objects already
 * registered in the class table by pass 1.
 *
 * At this point each ClassInfo contains only its *own* declared members.
 * Inherited members are merged in during pass 3
 * (ClassTable.closeFieldsUnderInheritance / closeMethodsUnderInheritance).
 *
 * Context tracks the current class and method so that VarDeclaration nodes
 * (which appear both as class fields and as method locals) know where to
 * register themselves.
 *
 * Note on AST field indices:
 *   ClassDeclaration:      f3 = fields, f4 = methods
 *   ClassExtendsDeclaration: f5 = fields, f6 = methods  (f3 = parent name, f4 = "{")
 *   MethodDeclaration:     f4 = formal params, f7 = local vars
 *   MainClass:             f14 = local vars of the main method
 *   Type.f0.which:         0 = int[], 1 = boolean, 2 = int, other = class name
 */
public class LayoutSecondPassVisitor extends GJVoidDepthFirst<ClassTable> {
    Context context = new Context();

    public void visit(MainClass n, ClassTable c) {
        String className = getNameFromIdentifierNode(n.f1);
        ClassInfo currentClass = c.getClassInfo(className);
        context.setCurrentClass(currentClass);

        MethodInfo mainMethodInfo = new MethodInfo("main", className, "void");
        currentClass.addMethod("main", mainMethodInfo);
        context.setCurrentMethod(mainMethodInfo);
        n.f14.accept(this, c); // visit locals of the main method
        context.clearCurrentMethod();
    }

    public void visit(ClassDeclaration n, ClassTable c) {
        context.setCurrentClass(c.getClassInfo(getNameFromIdentifierNode(n.f1)));
        n.f3.accept(this, c); // fields
        n.f4.accept(this, c); // methods
    }

    public void visit(ClassExtendsDeclaration n, ClassTable c) {
        context.setCurrentClass(c.getClassInfo(getNameFromIdentifierNode(n.f1)));
        n.f5.accept(this, c); // fields  (f3=parent name, f4="{", so fields are at f5)
        n.f6.accept(this, c); // methods
    }

    /** Routes a variable declaration to either the current class's fields or the current method's locals. */
    public void visit(VarDeclaration n, ClassTable c) {
        String id   = getNameFromIdentifierNode(n.f1);
        String type = getTypeFromTypeNode(n.f0);
        if (context.getCurrentMethod() != null)
            context.getCurrentMethod().addLocal(id, type);
        else
            context.getCurrentClass().addField(id, type);
    }

    public void visit(MethodDeclaration n, ClassTable c) {
        String returnType  = getTypeFromTypeNode(n.f1);
        String methodName  = getNameFromIdentifierNode(n.f2);
        MethodInfo method  = new MethodInfo(methodName, context.getCurrentClass().getClassName(), returnType);
        context.setCurrentMethod(method);
        n.f4.accept(this, c); // formal parameters
        n.f7.accept(this, c); // local variable declarations
        context.getCurrentClass().addMethod(methodName, method);
        context.clearCurrentMethod();
    }

    public void visit(FormalParameter n, ClassTable c) {
        String id   = getNameFromIdentifierNode(n.f1);
        String type = getTypeFromTypeNode(n.f0);
        context.getCurrentMethod().addParam(id, type);
    }

    // --- helpers ---

    String getNameFromIdentifierNode(Identifier id) {
        return id.f0.toString();
    }

    /** Maps a Type AST node to a type string. 0=int[], 1=boolean, 2=int, else=class name. */
    String getTypeFromTypeNode(Type t) {
        switch (t.f0.which) {
            case 0: return "int[]";
            case 1: return "boolean";
            case 2: return "int";
        }
        return getNameFromIdentifierNode((Identifier) t.f0.choice);
    }
}
