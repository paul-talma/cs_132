package visitor;

import minijava.syntaxtree.*;
import minijava.visitor.*;

import java.util.ArrayList;
import java.util.List;

import hw2.*;

public class SymbolTableVisitor extends GJDepthFirst<String, SymbolTable> {
    ClassTable classTable;
    ClassInfo currentClass;
    MethodInfo currentMethod;

    public SymbolTableVisitor(ClassTable classTable) {
        this.classTable = classTable;
    }

    public String visit(MainClass n, SymbolTable symbolTable) {
        // get current class
        String className = n.f1.f0.accept(this, symbolTable);
        currentClass = classTable.getClass(className);

        // copy symbol table and update with class fields
        SymbolTable newSymbolTable = new SymbolTable(symbolTable);
        newSymbolTable.update(currentClass.getFields());

        // visit statements
        n.f15.accept(this, newSymbolTable);

        // reset current class
        currentClass = null;
        return "";
    }

    // updates symbol table with class fields
    public String visit(ClassDeclaration n, SymbolTable symbolTable) {
        // get current class
        String className = n.f1.f0.accept(this, symbolTable);
        currentClass = classTable.getClass(className);

        // copy symbol table and update with all fields (including inherited)
        SymbolTable newSymbolTable = new SymbolTable(symbolTable);
        newSymbolTable.update(classTable.fields(className));

        // visit method declarations
        n.f4.accept(this, newSymbolTable);

        // reset current class
        currentClass = null;
        return "";
    }

    public String visit(ClassExtendsDeclaration n, SymbolTable symbolTable) {
        // get current class
        String className = n.f1.f0.accept(this, symbolTable);
        currentClass = classTable.getClass(className);

        // copy symbol table and update with all fields (including inherited)
        SymbolTable newSymbolTable = new SymbolTable(symbolTable);
        newSymbolTable.update(classTable.fields(className));

        // visit method declarations (f6, not f4 — f4 is "{", f5 is fields, f6 is
        // methods)
        n.f6.accept(this, newSymbolTable);

        // reset current class
        currentClass = null;
        return "";
    }

    public String visit(MethodDeclaration n, SymbolTable symbolTable) {
        // get current method
        String methodName = n.f2.f0.accept(this, symbolTable);
        MethodInfo methodInfo = currentClass.getMethod(methodName);

        // update symbol table with method params and locals
        SymbolTable newSymbolTable = new SymbolTable(symbolTable);
        newSymbolTable.update(methodInfo.getParams());
        newSymbolTable.update(methodInfo.getLocals());

        // visit statements
        n.f8.accept(this, newSymbolTable);

        // visit return expression and check it matches declared return type
        String returnExprType = n.f10.accept(this, newSymbolTable);
        String declaredReturnType = methodInfo.getReturnType();
        if (!classTable.subtype(returnExprType, declaredReturnType))
            throw new TypeException(String.format(
                    "Method %s declared return type %s but returns %s",
                    methodName, declaredReturnType, returnExprType));

        // reset current method
        currentMethod = null;
        return "";
    }

    public String visit(AssignmentStatement n, SymbolTable symbolTable) {
        String idType = n.f0.accept(this, symbolTable); // goes through Identifier visitor (checks scope)
        String exprType = n.f2.accept(this, symbolTable);
        if (classTable.subtype(exprType, idType))
            return "";

        throw new TypeException(String.format("Assigning expression of type %s to id of type %s", exprType, idType));
    }

    public String visit(ArrayAssignmentStatement n, SymbolTable symbolTable) {
        String idType = n.f0.accept(this, symbolTable); // goes through Identifier visitor (checks scope)
        String typeExpr1 = n.f2.accept(this, symbolTable);
        String typeExpr2 = n.f5.accept(this, symbolTable);
        if (idType.equals("int[]") && typeExpr1.equals("int") && typeExpr2.equals("int"))
            return "";

        throw new TypeException(
                String.format("Array assignment statement expected types int[], int, and int but got %s, %s, and %s",
                        idType, typeExpr1, typeExpr2));
    }

    public String visit(IfStatement n, SymbolTable symbolTable) {
        String type0 = n.f2.accept(this, symbolTable);
        n.f4.accept(this, symbolTable);
        n.f6.accept(this, symbolTable);
        if (type0.equals("boolean")) {
            return "";
        }
        throw new TypeException(String.format("If statement requires boolean expression but got", type0));
    }

    public String visit(WhileStatement n, SymbolTable symbolTable) {
        String type0 = n.f2.accept(this, symbolTable);
        n.f4.accept(this, symbolTable);
        if (type0.equals("boolean")) {
            return "";
        }
        throw new TypeException(String.format("While statement requires boolean expression but got", type0));
    }

    public String visit(PrintStatement n, SymbolTable symbolTable) {
        String type0 = n.f2.accept(this, symbolTable);
        if (type0.equals("int")) {
            return "";
        }
        throw new TypeException(String.format("Print statement requires int expression but got %s", type0));
    }

    public String visit(AndExpression n, SymbolTable symbolTable) {
        String type0 = n.f0.accept(this, symbolTable);
        String type1 = n.f2.accept(this, symbolTable);

        if (type0.equals("boolean") && type1.equals("boolean")) {
            return "boolean";
        }

        throw new TypeException(
                String.format("Expected boolean arguments to \"and\" expression but got %s and %s", type0, type1));
    }

    public String visit(CompareExpression n, SymbolTable symbolTable) {
        String type0 = n.f0.accept(this, symbolTable);
        String type1 = n.f2.accept(this, symbolTable);

        if (type0.equals("int") && type1.equals("int")) {
            return "boolean";
        }

        throw new TypeException(
                String.format("Expected int arguments to \"<\" expression but got %s and %s", type0, type1));
    }

    public String visit(PlusExpression n, SymbolTable symbolTable) {
        String type0 = n.f0.accept(this, symbolTable);
        String type1 = n.f2.accept(this, symbolTable);

        if (type0.equals("int") && type1.equals("int")) {
            return "int";
        }

        throw new TypeException(
                String.format("Expected int arguments to \"plus\" expression but got %s and %s", type0, type1));
    }

    public String visit(MinusExpression n, SymbolTable symbolTable) {
        String type0 = n.f0.accept(this, symbolTable);
        String type1 = n.f2.accept(this, symbolTable);

        if (type0.equals("int") && type1.equals("int")) {
            return "int";
        }

        throw new TypeException(
                String.format("Expected int arguments to \"minus\" expression but got %s and %s", type0, type1));
    }

    public String visit(TimesExpression n, SymbolTable symbolTable) {
        String type0 = n.f0.accept(this, symbolTable);
        String type1 = n.f2.accept(this, symbolTable);

        if (type0.equals("int") && type1.equals("int")) {
            return "int";
        }

        throw new TypeException(
                String.format("Expected int arguments to \"times\" expression but got %s and %s", type0, type1));
    }

    public String visit(ArrayLookup n, SymbolTable symbolTable) {
        String type0 = n.f0.accept(this, symbolTable);
        String type1 = n.f2.accept(this, symbolTable);

        if (type0.equals("int[]") && type1.equals("int")) {
            return "int";
        }

        throw new TypeException(
                String.format("Expected int[] and int arguments to \"array lookup\" expression but got %s and %s",
                        type0, type1));
    }

    public String visit(ArrayLength n, SymbolTable symbolTable) {
        String type0 = n.f0.accept(this, symbolTable);

        if (type0.equals("int[]")) {
            return "int";
        }

        throw new TypeException(
                String.format("Expected int[] argument to \"array length\" expression but got %s",
                        type0));
    }

    public String visit(MessageSend n, SymbolTable symbolTable) {
        String objectType = n.f0.accept(this, symbolTable);
        String methodName = n.f2.f0.accept(this, symbolTable);
        // Walk the inheritance chain — classInfo.getMethod() only checks the direct
        // class
        MethodInfo methodInfo = classTable.methodType(objectType, methodName);
        if (methodInfo == null)
            throw new TypeException(String.format(
                    "Method %s not found in class %s or any superclass", methodName, objectType));

        // get actual parameter type list
        List<String> actualParameterTypes = getActualParameterTypes(n.f4, symbolTable);
        List<String> formalParameterTypes = new ArrayList<String>();

        // iterate through list then lookup in table to ensure order
        for (String id : methodInfo.getParamIds()) {
            formalParameterTypes.add(methodInfo.getParamType(id));
        }

        // check number of arguments
        if (actualParameterTypes.size() != formalParameterTypes.size()) {
            throw new TypeException("Number of actual and formal params do not match.");
        }

        // check actual args subtype formal args
        for (int i = 0; i < actualParameterTypes.size(); ++i) {
            String actualParamType = actualParameterTypes.get(i);
            String formalParamType = formalParameterTypes.get(i);
            if (!classTable.subtype(actualParamType, formalParamType)) {
                throw new TypeException(
                        String.format("Actual param of type %s not a subtype of formal param of type %s.",
                                actualParamType, formalParamType));
            }
        }

        // get return type
        return methodInfo.getReturnType();
    }

    List<String> getActualParameterTypes(NodeOptional maybeList, SymbolTable symbolTable) {
        List<String> args = new ArrayList<String>();
        if (!maybeList.present())
            return args;

        ExpressionList el = (ExpressionList) maybeList.node;
        args.add(el.f0.accept(this, symbolTable)); // first Expression

        for (Node node : el.f1.nodes) { // ExpressionRest*
            ExpressionRest rest = (ExpressionRest) node;
            args.add(rest.f1.accept(this, symbolTable));
        }
        return args;
    }

    public String visit(IntegerLiteral n, SymbolTable symbolTable) {
        return "int";
    }

    public String visit(TrueLiteral n, SymbolTable symbolTable) {
        return "boolean";
    }

    public String visit(FalseLiteral n, SymbolTable symbolTable) {
        return "boolean";
    }

    public String visit(Identifier n, SymbolTable symbolTable) {
        // don't check class name ids
        if (currentClass == null) {
            return "";
        }

        String id = n.f0.accept(this, symbolTable);
        if (symbolTable.contains(id)) {
            return symbolTable.getType(id);
        }
        throw new TypeException(String.format("Identifier %s not declared", id));
    }

    public String visit(ThisExpression n, SymbolTable symbolTable) {
        String name = currentClass.getName();
        if (!name.equals("main")) {
            return name;
        }
        throw new TypeException("Cannot use \"this\" in main class");
    }

    public String visit(ArrayAllocationExpression n, SymbolTable symbolTable) {
        String eType = n.f3.accept(this, symbolTable);
        if (eType.equals("int")) {
            return "int[]";
        }
        throw new TypeException(String.format("Array allocation requires int expression but got %s", eType));
    }

    public String visit(AllocationExpression n, SymbolTable symbolTable) {
        return n.f1.f0.accept(this, symbolTable);
    }

    public String visit(NotExpression n, SymbolTable symbolTable) {
        String eType = n.f1.accept(this, symbolTable);
        if (eType.equals("boolean")) {
            return "boolean";
        }
        throw new TypeException(String.format("Not expression requires boolean argument, got %s", eType));
    }

    public String visit(Expression n, SymbolTable symbolTable) {
        return n.f0.accept(this, symbolTable);
    }

    public String visit(PrimaryExpression n, SymbolTable symbolTable) {
        return n.f0.accept(this, symbolTable);
    }

    public String visit(BracketExpression n, SymbolTable symbolTable) {
        return n.f1.accept(this, symbolTable);
    }

    public String visit(NodeToken n, SymbolTable symbolTable) {
        return n.toString();
    }
}
