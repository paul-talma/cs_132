package visitor;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import IR.token.FunctionName;
import IR.token.Identifier;
import IR.token.Label;
import hw3.*;

import minijava.syntaxtree.*;
import minijava.visitor.GJDepthFirst;

import sparrow.*;
import sparrow.Block;

public class TranslateVisitor extends GJDepthFirst<Result, Context> {
    ClassTable classTable;
    String outOfBoundsAccessMessage = "\"array index out of bounds\"";

    List<FunctionDecl> functions = new ArrayList<FunctionDecl>();

    public Program getProgram() {
        return new Program(functions);
    }

    public TranslateVisitor(ClassTable ct) {
        this.classTable = ct;
    }

    // program structure
    public Result visit(Goal n, Context c) {
        n.f0.accept(this, c);
        n.f1.accept(this, c);
        return null;
    }

    public Result visit(NodeListOptional n, Context c) {
        List<Instruction> instr = new ArrayList<Instruction>();
        if (n.present()) {
            for (Enumeration<Node> e = n.elements(); e.hasMoreElements();) {
                Result res = e.nextElement().accept(this, c);
                if (res != null && res.getCode() != null)
                    instr.addAll(res.getCode());
            }
        }
        Identifier id = new Identifier(c.nextTemp());
        return new Result(instr, id);
    }

    // declarations
    public Result visit(MainClass n, Context c) {
        // setup
        String className = n.f1.f0.toString();
        ClassInfo classInfo = classTable.getClassInfo(className);
        c.setCurrentClass(classTable.getClassInfo(className));
        MethodInfo methodInfo = classInfo.getMethodInfo("main");
        c.setCurrentMethod(methodInfo);

        // translate statements
        Result statementsRes = n.f15.accept(this, c);
        List<Instruction> instr = new ArrayList<Instruction>(statementsRes.getCode());

        // make block
        Identifier returnId = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(returnId, 0));
        Block block = new Block(instr, returnId);

        // add main function to functions
        FunctionName fName = new FunctionName("main");
        FunctionDecl f = new FunctionDecl(fName, new ArrayList<Identifier>(), block);
        this.functions.add(f);

        c.clearCurrentMethod();
        c.clearCurrentClass();

        return null;
    }

    public Result visit(ClassDeclaration n, Context c) {
        // set current class
        String className = n.f1.f0.toString();
        ClassInfo currentClass = classTable.getClassInfo(className);
        c.setCurrentClass(currentClass);

        // compile method declarations
        n.f4.accept(this, c);

        c.clearCurrentClass();
        return null;
    }

    public Result visit(ClassExtendsDeclaration n, Context c) {
        // set current class
        String className = n.f1.f0.toString();
        ClassInfo currentClass = classTable.getClassInfo(className);
        c.setCurrentClass(currentClass);

        // compile method declarations
        n.f6.accept(this, c);

        return null;
    }

    public Result visit(MethodDeclaration n, Context c) {
        // update context
        String funcString = n.f2.f0.toString();
        ClassInfo currentClass = c.getCurrentClass();
        MethodInfo currentMethod = currentClass.getMethodInfo(funcString);
        c.setCurrentMethod(currentMethod);

        // get formal params
        List<Identifier> params = new ArrayList<Identifier>();
        params.add(new Identifier("this"));
        for (String paramName : currentMethod.getParams()) {
            params.add(new Identifier("u_" + paramName));
        }

        // make block
        List<Instruction> instr = new ArrayList<Instruction>();
        Result statementsRes = n.f8.accept(this, c);
        if (statementsRes != null && statementsRes.getCode() != null) {
            instr.addAll(statementsRes.getCode());
        }
        // get return expr
        Result retExprRes = n.f10.accept(this, c);
        instr.addAll(retExprRes.getCode());
        Identifier retId = retExprRes.getIdentifier();
        Block block = new Block(instr, retId);

        FunctionName fName = new FunctionName(currentClass.getClassName() + "_" + funcString);
        FunctionDecl fun = new FunctionDecl(fName, params, block);
        functions.add(fun);

        c.clearCurrentMethod();
        return null;
    }

    // expressions

    public Result visit(Expression n, Context c) {
        return n.f0.accept(this, c);
    }

    public Result visit(AndExpression n, Context c) {
        Result r1 = n.f0.accept(this, c);
        List<Instruction> instr = new ArrayList<Instruction>(r1.getCode());

        String falseLabel = "L" + c.nextLabel();
        String endLabel = "L" + c.nextLabel();
        Identifier dest = new Identifier(c.nextTemp());

        // Short-circuit: if x==0 (false) skip y
        instr.add(new IfGoto(r1.getIdentifier(), new Label(falseLabel)));
        Result r2 = n.f2.accept(this, c);
        instr.addAll(r2.getCode());
        instr.add(new IfGoto(r2.getIdentifier(), new Label(falseLabel)));
        instr.add(new Move_Id_Integer(dest, 1));
        instr.add(new Goto(new Label(endLabel)));
        instr.add(new LabelInstr(new Label(falseLabel)));
        instr.add(new Move_Id_Integer(dest, 0));
        instr.add(new LabelInstr(new Label(endLabel)));
        return new Result(instr, dest);
    }

    public Result visit(CompareExpression n, Context c) {
        Result r0 = n.f0.accept(this, c);
        Result r1 = n.f2.accept(this, c);
        List<Instruction> instr = new ArrayList<Instruction>(r0.getCode());
        instr.addAll(r1.getCode());
        Identifier dest = new Identifier(c.nextTemp());
        instr.add(new LessThan(dest, r0.getIdentifier(), r1.getIdentifier()));
        return new Result(instr, dest);
    }

    public Result visit(PlusExpression n, Context c) {
        Result leftOperand = n.f0.accept(this, c);
        Result rightOperand = n.f2.accept(this, c);
        Identifier dest = new Identifier(c.nextTemp());
        List<Instruction> instr = new ArrayList<Instruction>(leftOperand.getCode());
        instr.addAll(rightOperand.getCode());
        instr.add(new Add(dest, leftOperand.getIdentifier(), rightOperand.getIdentifier()));
        return new Result(instr, dest);
    }

    public Result visit(MinusExpression n, Context c) {
        Result leftOperand = n.f0.accept(this, c);
        Result rightOperand = n.f2.accept(this, c);
        Identifier dest = new Identifier(c.nextTemp());
        List<Instruction> instr = new ArrayList<Instruction>(leftOperand.getCode());
        instr.addAll(rightOperand.getCode());
        instr.add(new Subtract(dest, leftOperand.getIdentifier(), rightOperand.getIdentifier()));
        return new Result(instr, dest);
    }

    public Result visit(TimesExpression n, Context c) {
        Result leftOperand = n.f0.accept(this, c);
        Result rightOperand = n.f2.accept(this, c);
        Identifier dest = new Identifier(c.nextTemp());
        List<Instruction> instr = new ArrayList<Instruction>(leftOperand.getCode());
        instr.addAll(rightOperand.getCode());
        instr.add(new Multiply(dest, leftOperand.getIdentifier(), rightOperand.getIdentifier()));
        return new Result(instr, dest);
    }

    public Result visit(ArrayLookup n, Context c) {
        Result arrResult = n.f0.accept(this, c);
        Result idxResult = n.f2.accept(this, c);
        List<Instruction> instr = new ArrayList<Instruction>();

        instr.addAll(arrResult.getCode());
        Identifier arr = arrResult.getIdentifier();
        instr.addAll(nullPtrCheck(arr, c));

        instr.addAll(idxResult.getCode());
        Identifier idx = idxResult.getIdentifier();

        instr.addAll(boundsCheck(arr, idx, c));

        Identifier one = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(one, 1)); // one = 1
        Identifier idxp1 = new Identifier(c.nextTemp());
        instr.add(new Add(idxp1, idx, one)); // idxp1 = idx + one
        Identifier four = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(four, 4)); // four = 4
        Identifier offset = new Identifier(c.nextTemp());
        instr.add(new Multiply(offset, idxp1, four)); // offset = idxp1 * four
        Identifier addr = new Identifier(c.nextTemp());
        instr.add(new Add(addr, arr, offset)); // addr = arr + offset
        Identifier dest = new Identifier(c.nextTemp());
        instr.add(new Load(dest, addr, 0)); // dest = [addr + 0]

        return new Result(instr, dest);
    }

    public Result visit(ArrayLength n, Context c) {
        Result arrResult = n.f0.accept(this, c);
        List<Instruction> instr = new ArrayList<Instruction>(arrResult.getCode());
        Identifier arr = arrResult.getIdentifier();
        instr.addAll(nullPtrCheck(arr, c));
        Identifier len = new Identifier(c.nextTemp());
        instr.add(new Load(len, arr, 0));
        return new Result(instr, len);
    }

    public Result visit(MessageSend n, Context c) {
        // a.m(p0, p1, ...)
        Result aResult = n.f0.accept(this, c);
        Identifier a = aResult.getIdentifier();
        List<Instruction> instr = new ArrayList<Instruction>(aResult.getCode());
        instr.addAll(nullPtrCheck(a, c)); // nullptr check on a

        // get vtable ptr
        Identifier vtablePtr = new Identifier(c.nextTemp());
        instr.add(new Load(vtablePtr, a, 0)); // vtablePtr = [a + 0]

        // find method offset in class, get methodptr
        String className = getPrimaryExprType(n.f0, c);
        String methodName = n.f2.f0.toString();
        int offset = classTable.getMethodOffset(className, methodName);
        Identifier methodPtr = new Identifier(c.nextTemp());
        instr.add(new Load(methodPtr, vtablePtr, offset)); // methodPtr = [vtablePtr + offset]
        // instr.addAll(nullPtrCheck(methodPtr, c));

        // get params
        NodeOptional optNode = n.f4;
        List<Identifier> args = new ArrayList<Identifier>();
        args.add(a);
        if (optNode.present()) {
            // visit first expression
            ExpressionList paramList = (ExpressionList) optNode.node;
            Result expResult = paramList.f0.accept(this, c);
            instr.addAll(expResult.getCode());
            args.add(expResult.getIdentifier());

            // visit remaining expressions
            NodeListOptional list = paramList.f1;
            if (list.present()) {
                for (Enumeration<Node> e = list.elements(); e.hasMoreElements();) {
                    ExpressionRest er = (ExpressionRest) e.nextElement();
                    Result expRes = er.f1.accept(this, c);
                    instr.addAll(expRes.getCode());
                    args.add(expRes.getIdentifier());
                }
            }
        }

        Identifier dest = new Identifier(c.nextTemp());
        instr.add(new Call(dest, methodPtr, args));

        return new Result(instr, dest);

    }

    public Result visit(PrimaryExpression n, Context c) {
        return n.f0.accept(this, c);
    }

    // primary expressions

    public Result visit(IntegerLiteral n, Context c) {
        Identifier dest = new Identifier(c.nextTemp());
        List<Instruction> instr = new ArrayList<Instruction>();
        instr.add(new Move_Id_Integer(dest, Integer.parseInt(n.f0.tokenImage)));
        return new Result(instr, dest);
    }

    public Result visit(TrueLiteral n, Context c) {
        Identifier dest = new Identifier(c.nextTemp());
        List<Instruction> instr = new ArrayList<Instruction>();
        instr.add(new Move_Id_Integer(dest, 1));
        return new Result(instr, dest);
    }

    public Result visit(FalseLiteral n, Context c) {
        Identifier dest = new Identifier(c.nextTemp());
        List<Instruction> instr = new ArrayList<Instruction>();
        instr.add(new Move_Id_Integer(dest, 0));
        return new Result(instr, dest);
    }

    public Result visit(minijava.syntaxtree.Identifier n, Context c) {
        List<Instruction> instr = new ArrayList<Instruction>();
        String idName = n.f0.tokenImage.toString();
        if (isLocalOrParam(n, c)) {
            return new Result(instr, new Identifier("u_" + idName));
        }
        String className = c.getCurrentClass().getClassName();
        int fieldOffset = classTable.getFieldOffset(className, idName);
        Identifier ts = new Identifier("this");
        Identifier dest = new Identifier(c.nextTemp());
        instr.add(new Load(dest, ts, fieldOffset));
        return new Result(instr, dest);
    }

    public Result visit(ThisExpression n, Context c) {
        List<Instruction> instr = new ArrayList<Instruction>();
        Identifier id = new Identifier("this");
        // Identifier dest = new Identifier(c.nextTemp());
        // instr.add(new Move_Id_Id(dest, id));
        return new Result(instr, id);
    }

    public Result visit(ArrayAllocationExpression n, Context c) {
        Result eRes = n.f3.accept(this, c);
        List<Instruction> instr = new ArrayList<Instruction>(eRes.getCode());

        // bounds check
        Identifier size = eRes.getIdentifier();
        Identifier zero = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(zero, 0)); // zero = 0
        Identifier lessThanZero = new Identifier(c.nextTemp());
        instr.add(new LessThan(lessThanZero, size, zero)); // lTZ = e < 0
        Label validLabel = new Label("L" + c.nextLabel());
        instr.add(new IfGoto(lessThanZero, validLabel)); // if0 lTZ goto valid
        instr.add(new ErrorMessage(outOfBoundsAccessMessage)); // error ("Out of bounds")
        instr.add(new LabelInstr(validLabel)); // valid:
        Identifier one = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(one, 1)); // one = 1
        Identifier sizep1 = new Identifier(c.nextTemp());
        instr.add(new Add(sizep1, size, one)); // sizep1 = size + one
        Identifier four = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(four, 4)); // four = 4
        Identifier bytes = new Identifier(c.nextTemp());
        instr.add(new Multiply(bytes, sizep1, four)); // bytes = sizep1 * four
        Identifier dest = new Identifier(c.nextTemp());
        instr.add(new Alloc(dest, bytes)); // dest = alloc(bytes)
        instr.add(new Store(dest, 0, size)); // [dest + 0] = size

        return new Result(instr, dest);
    }

    public Result visit(AllocationExpression n, Context c) {
        List<Instruction> instr = new ArrayList<Instruction>();
        String className = n.f1.f0.toString();

        // allocate object block
        int numFields = classTable.getTotalFieldCount(className);
        Identifier objectBlockBytes = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(objectBlockBytes, (numFields + 1) * 4)); // objectBlockBytes = (numFields + 1) * 4
        Identifier dest = new Identifier(c.nextTemp());
        instr.add(new Alloc(dest, objectBlockBytes)); // dest = alloc(objectBlockSize)

        // allocate vtable
        Map<String, MethodInfo> methods = classTable.getClassInfo(className).getMethods();
        int numMethods = methods.size();
        Identifier vtableBytes = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(vtableBytes, numMethods * 4)); // vtableBytes = 4 * numMethods
        Identifier vtable = new Identifier(c.nextTemp());
        instr.add(new Alloc(vtable, vtableBytes)); // vtable = alloc(vtableBytes)

        // populate vtable
        int offset = 0;
        for (MethodInfo methodInfo : methods.values()) {
            FunctionName fName = new FunctionName(methodInfo.getOwnerClassName() + "_" + methodInfo.getMethodName());
            Identifier f = new Identifier(c.nextTemp());
            instr.add(new Move_Id_FuncName(f, fName)); // f = @className_methodName
            instr.add(new Store(vtable, offset, f)); // [vtable + offset] = f
            offset += 4;
        }

        // link object block to vtable
        instr.add(new Store(dest, 0, vtable));

        return new Result(instr, dest);
    }

    public Result visit(NotExpression n, Context c) {
        Result operand = n.f1.accept(this, c);
        List<Instruction> instr = new ArrayList<Instruction>(operand.getCode());
        Identifier one = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(one, 1));
        Identifier dest = new Identifier(c.nextTemp());
        instr.add(new Subtract(dest, one, operand.getIdentifier()));
        return new Result(instr, dest);
    }

    public Result visit(BracketExpression n, Context c) {
        return n.f1.accept(this, c);
    }

    // statements
    public Result visit(Statement n, Context c) {
        return n.f0.accept(this, c);
    }

    public Result visit(minijava.syntaxtree.Block n, Context c) {
        return n.f1.accept(this, c);
    }

    public Result visit(AssignmentStatement n, Context c) {
        List<Instruction> instr = new ArrayList<Instruction>();

        // rhs
        Result rhsRes = n.f2.accept(this, c);
        instr.addAll(rhsRes.getCode());
        Identifier rhs = rhsRes.getIdentifier();

        // get dest
        if (isLocalOrParam(n.f0, c)) {
            Result idRes = n.f0.accept(this, c);
            Identifier dest = idRes.getIdentifier();
            instr.add(new Move_Id_Id(dest, rhs)); // dest = rhs
        } else {
            int fieldOffset = classTable.getFieldOffset(c.getCurrentClass().getClassName(), n.f0.f0.toString());
            instr.add(new Store(new Identifier("this"), fieldOffset, rhs)); // [this + offset] = rhs
        }
        Identifier temp = new Identifier(c.nextTemp());
        return new Result(instr, temp);
    }

    public Result visit(ArrayAssignmentStatement n, Context c) {
        List<Instruction> instr = new ArrayList<Instruction>();

        // arr
        Result arrResult = n.f0.accept(this, c);
        instr.addAll(arrResult.getCode()); // arr = ...
        Identifier arr = arrResult.getIdentifier();
        instr.addAll(nullPtrCheck(arr, c));

        // idx
        Result idxResult = n.f2.accept(this, c);
        instr.addAll(idxResult.getCode()); // idx = ...
        Identifier idx = idxResult.getIdentifier();

        // bounds check
        instr.addAll(boundsCheck(arr, idx, c));
        Identifier idxp1 = new Identifier(c.nextTemp());
        Identifier one = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(one, 1)); // one = 1
        instr.add(new Add(idxp1, idx, one)); // idxp1 = idx + one
        Identifier four = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(four, 4)); // four = 4
        Identifier offset = new Identifier(c.nextTemp());
        instr.add(new Multiply(offset, idxp1, four)); // offset = idxp1 * 4
        Identifier basePlusOffset = new Identifier(c.nextTemp());
        instr.add(new Add(basePlusOffset, arr, offset));

        // rhs
        Result rhsResult = n.f5.accept(this, c);
        instr.addAll(rhsResult.getCode());
        Identifier rhs = rhsResult.getIdentifier();

        instr.add(new Store(basePlusOffset, 0, rhs));

        return new Result(instr, null);
    }

    public Result visit(IfStatement n, Context c) {
        List<Instruction> instr = new ArrayList<Instruction>();
        Result conditionResult = n.f2.accept(this, c);
        instr.addAll(conditionResult.getCode());
        Identifier condition = conditionResult.getIdentifier();

        String falseLabel = "L" + c.nextLabel();
        String endLabel = "L" + c.nextLabel();
        instr.add(new IfGoto(condition, new Label(falseLabel))); // if0 cond goto false
        Result trueResult = n.f4.accept(this, c);
        instr.addAll(trueResult.getCode()); // truecase
        instr.add(new Goto(new Label(endLabel))); // goto end
        instr.add(new LabelInstr(new Label(falseLabel))); // false:
        Result falseResult = n.f6.accept(this, c);
        instr.addAll(falseResult.getCode()); // falsecase
        instr.add(new LabelInstr(new Label(endLabel))); // end:

        return new Result(instr, null);
    }

    public Result visit(WhileStatement n, Context c) {
        List<Instruction> instr = new ArrayList<Instruction>();
        String topLabel = "L" + c.nextLabel();
        String endLabel = "L" + c.nextLabel();
        instr.add(new LabelInstr(new Label(topLabel))); // top:
        Result conditionResult = n.f2.accept(this, c);
        instr.addAll(conditionResult.getCode()); // conditioncode
        Identifier condition = conditionResult.getIdentifier();
        instr.add(new IfGoto(condition, new Label(endLabel))); // if0 cond goto end
        Result statementRes = n.f4.accept(this, c);
        instr.addAll(statementRes.getCode());
        instr.add(new Goto(new Label(topLabel))); // goto top
        instr.add(new LabelInstr(new Label(endLabel))); // end:

        return new Result(instr, null);
    }

    public Result visit(PrintStatement n, Context c) {
        Result eRes = n.f2.accept(this, c);
        List<Instruction> instr = new ArrayList<Instruction>(eRes.getCode());
        Identifier e = eRes.getIdentifier();
        instr.add(new Print(e));
        return new Result(instr, e);
    }

    // helpers
    List<Instruction> boundsCheck(Identifier arr, Identifier i, Context c) {
        String nonNegative = "L" + c.nextLabel();
        String tooLarge = "L" + c.nextLabel();
        String endLabel = "L" + c.nextLabel();

        List<Instruction> instr = new ArrayList<Instruction>();

        Identifier zero = new Identifier(c.nextTemp());
        Identifier negative = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(zero, 0)); // zero = 0
        instr.add(new LessThan(negative, i, zero)); // isNeg = i < 0
        instr.add(new IfGoto(negative, new Label(nonNegative))); // if0 negative goto nonNegative
        instr.add(new ErrorMessage(outOfBoundsAccessMessage)); // error(out of bounds)
        instr.add(new LabelInstr(new Label(nonNegative))); // nonNegative:

        Identifier len = new Identifier(c.nextTemp());
        instr.add(new Load(len, arr, 0)); // len = [a + 0]
        Identifier inBounds = new Identifier(c.nextTemp());
        instr.add(new LessThan(inBounds, i, len)); // inBounds = i < len
        instr.add(new IfGoto(inBounds, new Label(tooLarge))); // if0 inBounds goto tooLarge
        instr.add(new Goto(new Label(endLabel))); // goto end
        instr.add(new LabelInstr(new Label(tooLarge))); // tooLarge
        instr.add(new ErrorMessage(outOfBoundsAccessMessage)); // error
        instr.add(new LabelInstr(new Label(endLabel))); // end:

        return instr;
    }

    List<Instruction> nullPtrCheck(Identifier ptr, Context c) {
        String nullPtrError = "L" + c.nextLabel();
        String validPtr = "L" + c.nextLabel();
        List<Instruction> instr = new ArrayList<Instruction>();
        instr.add(new IfGoto(ptr, new Label(nullPtrError)));
        instr.add(new Goto(new Label(validPtr)));
        instr.add(new LabelInstr(new Label(nullPtrError)));
        instr.add(new ErrorMessage("\"null pointer\""));
        instr.add(new LabelInstr(new Label(validPtr)));
        return instr;
    }

    String getExpressionType(Expression n, Context c) {
        NodeChoice f = n.f0;
        switch (f.which) {
            case 0:
            case 1:
                return "boolean";
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
                return "int";
            case 7:
                // get owner class
                MessageSend mess = (MessageSend) f.choice;
                String ownerClassName = getPrimaryExprType(mess.f0, c);
                ClassInfo ownerClassInfo = classTable.getClassInfo(ownerClassName);

                // get method name
                minijava.syntaxtree.Identifier methodId = (minijava.syntaxtree.Identifier) mess.f2;
                String methodName = methodId.f0.toString();

                // lookup return type
                MethodInfo methodInfo = ownerClassInfo.getMethodInfo(methodName);
                return methodInfo.getReturnType();
            case 8:
                PrimaryExpression e = (PrimaryExpression) f.choice;
                return getPrimaryExprType(e, c);

        }
        throw new RuntimeException("No type found for expression");
    }

    String getPrimaryExprType(PrimaryExpression n, Context c) {
        NodeChoice f = n.f0;
        switch (f.which) {
            case 0:
                return "int";
            case 1:
            case 2:
            case 7:
                return "boolean";
            case 3:
                minijava.syntaxtree.Identifier id = (minijava.syntaxtree.Identifier) f.choice;
                return getIdentifierType(id, c);
            case 4:
                return c.getCurrentClass().getClassName();
            case 5:
                return "int[]"; // array type? or just int for ptr?
                                // depends what we use this for
            case 6:
                AllocationExpression aE = (AllocationExpression) f.choice;
                minijava.syntaxtree.Identifier id0 = (minijava.syntaxtree.Identifier) aE.f1;
                return id0.f0.toString();
            case 8:
                BracketExpression e = (BracketExpression) f.choice;
                return getExpressionType(e.f1, c);
        }
        throw new RuntimeException("No type found for primary expression");
    }

    String getIdentifierType(minijava.syntaxtree.Identifier n, Context c) {
        String idName = n.f0.toString();
        String t;

        // check params and locals of current method
        MethodInfo currentMethod = c.getCurrentMethod();
        if (currentMethod != null) {
            t = currentMethod.getParamType(idName);
            if (t != null)
                return t;
            t = currentMethod.getLocalType(idName);
            if (t != null)
                return t;
        }

        // check fields of current class
        ClassInfo currentClass = c.getCurrentClass();
        if (currentClass != null) {
            t = currentClass.getFieldType(idName);
            if (t != null)
                return t;
        }

        // id is not a field, param, or local var
        throw new RuntimeException("Identifier is not a field, param, or local var");
        // return "int";
    }

    boolean isLocalOrParam(minijava.syntaxtree.Identifier n, Context c) {
        String idName = n.f0.toString();
        MethodInfo currentMethod = c.getCurrentMethod();
        if (currentMethod == null)
            return false;
        return currentMethod.getParams().contains(idName) || currentMethod.getLocals().contains(idName);
    }

}
