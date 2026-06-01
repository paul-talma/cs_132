package visitor;

import hw3.*;
import minijava.syntaxtree.*;
import minijava.visitor.GJDepthFirst;
import IR.token.Identifier;
import IR.token.FunctionName;
import IR.token.Label;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import sparrow.Add;
import sparrow.Alloc;
import sparrow.Call;
import sparrow.ErrorMessage;
import sparrow.FunctionDecl;
import sparrow.Goto;
import sparrow.IfGoto;
import sparrow.Instruction;
import sparrow.LabelInstr;
import sparrow.LessThan;
import sparrow.Load;
import sparrow.Move_Id_FuncName;
import sparrow.Move_Id_Id;
import sparrow.Move_Id_Integer;
import sparrow.Multiply;
import sparrow.Print;
import sparrow.Program;
import sparrow.Store;
import sparrow.Subtract;

public class CodegenVisitor extends GJDepthFirst<Result, Context> {
    ClassTable classTable;
    List<FunctionDecl> functions = new ArrayList<FunctionDecl>();

    public CodegenVisitor(ClassTable ct) {
        this.classTable = ct;
    }

    public Program getProgram() {
        return new Program(functions);
    }

    // ================================================================
    // Program structure
    // ================================================================

    @Override
    public Result visit(Goal n, Context c) {
        n.f0.accept(this, c);
        n.f1.accept(this, c);
        return null;
    }

    @Override
    public Result visit(MainClass n, Context c) {
        String className = n.f1.f0.toString();
        c.setCurrentClass(classTable.getClassInfo(className));
        c.setCurrentMethod(c.getCurrentClass().getMethodInfo("main"));
        c.setTempCounter(0);
        c.setLabelCounter(0);

        List<Instruction> instr = collectStatements(n.f15, c);
        Identifier retId = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(retId, 0));

        functions.add(new FunctionDecl(
                new FunctionName("main"),
                new ArrayList<Identifier>(),
                new sparrow.Block(instr, retId)));
        c.clearCurrentMethod();
        return null;
    }

    @Override
    public Result visit(ClassDeclaration n, Context c) {
        c.setCurrentClass(classTable.getClassInfo(n.f1.f0.toString()));
        n.f4.accept(this, c);
        return null;
    }

    @Override
    public Result visit(ClassExtendsDeclaration n, Context c) {
        c.setCurrentClass(classTable.getClassInfo(n.f1.f0.toString()));
        n.f6.accept(this, c);
        return null;
    }

    @Override
    public Result visit(MethodDeclaration n, Context c) {
        String methodName = n.f2.f0.toString();
        ClassInfo cls = c.getCurrentClass();
        String className = cls.getClassName();
        MethodInfo method = cls.getMethodInfo(methodName);
        c.setCurrentMethod(method);
        c.setTempCounter(0);
        c.setLabelCounter(0);

        List<Identifier> params = new ArrayList<Identifier>();
        params.add(new Identifier("this"));
        for (String p : method.getParams()) {
            params.add(new Identifier("u_" + p));
        }

        List<Instruction> instr = collectStatements(n.f8, c);
        Result retResult = n.f10.accept(this, c);
        instr.addAll(retResult.getCode());

        functions.add(new FunctionDecl(
                new FunctionName(className + "_" + methodName),
                params,
                new sparrow.Block(instr, retResult.getIdentifier())));
        c.clearCurrentMethod();
        return null;
    }

    // ================================================================
    // Statements
    // ================================================================

    @Override
    public Result visit(Statement n, Context c) {
        return n.f0.accept(this, c);
    }

    @Override
    public Result visit(Block n, Context c) {
        return new Result(collectStatements(n.f1, c), null);
    }

    @Override
    public Result visit(AssignmentStatement n, Context c) {
        String varName = n.f0.f0.toString();
        Result rhs = n.f2.accept(this, c);
        List<Instruction> instr = new ArrayList<Instruction>(rhs.getCode());

        if (isLocalOrParam(varName, c)) {
            instr.add(new Move_Id_Id(new Identifier("u_" + varName), rhs.getIdentifier()));
        } else {
            int offset = classTable.getFieldOffset(c.getCurrentClass().getClassName(), varName);
            instr.add(new Store(new Identifier("this"), offset, rhs.getIdentifier()));
        }
        return new Result(instr, null);
    }

    @Override
    public Result visit(ArrayAssignmentStatement n, Context c) {
        String arrName = n.f0.f0.toString();
        List<Instruction> instr = new ArrayList<Instruction>();

        Identifier arrId;
        if (isLocalOrParam(arrName, c)) {
            arrId = new Identifier("u_" + arrName);
        } else {
            int fieldOffset = classTable.getFieldOffset(c.getCurrentClass().getClassName(), arrName);
            arrId = new Identifier(c.nextTemp());
            instr.add(new Load(arrId, new Identifier("this"), fieldOffset));
        }

        Result idxResult = n.f2.accept(this, c);
        Result valResult = n.f5.accept(this, c);
        instr.addAll(idxResult.getCode());
        instr.addAll(valResult.getCode());
        Identifier idx = idxResult.getIdentifier();

        instr.addAll(boundsCheck(arrId, idx, c));

        Identifier one = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(one, 1));
        Identifier idxp1 = new Identifier(c.nextTemp());
        instr.add(new Add(idxp1, idx, one));
        Identifier four = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(four, 4));
        Identifier byteOff = new Identifier(c.nextTemp());
        instr.add(new Multiply(byteOff, idxp1, four));
        Identifier addr = new Identifier(c.nextTemp());
        instr.add(new Add(addr, arrId, byteOff));
        instr.add(new Store(addr, 0, valResult.getIdentifier()));
        return new Result(instr, null);
    }

    @Override
    public Result visit(IfStatement n, Context c) {
        Result condResult = n.f2.accept(this, c);
        List<Instruction> instr = new ArrayList<Instruction>(condResult.getCode());

        String elseLabel = "L" + c.nextLabel();
        String endLabel = "L" + c.nextLabel();

        // if0 cond goto else: jump when cond==0 (false) → execute else branch
        instr.add(new IfGoto(condResult.getIdentifier(), new Label(elseLabel)));
        Result thenResult = n.f4.accept(this, c);
        instr.addAll(thenResult.getCode());
        instr.add(new Goto(new Label(endLabel)));
        instr.add(new LabelInstr(new Label(elseLabel)));
        Result elseResult = n.f6.accept(this, c);
        instr.addAll(elseResult.getCode());
        instr.add(new LabelInstr(new Label(endLabel)));
        return new Result(instr, null);
    }

    @Override
    public Result visit(WhileStatement n, Context c) {
        String topLabel = "L" + c.nextLabel();
        String endLabel = "L" + c.nextLabel();
        List<Instruction> instr = new ArrayList<Instruction>();

        instr.add(new LabelInstr(new Label(topLabel)));
        Result condResult = n.f2.accept(this, c);
        instr.addAll(condResult.getCode());
        // if0 cond goto end: exit loop when cond==0 (false)
        instr.add(new IfGoto(condResult.getIdentifier(), new Label(endLabel)));
        Result bodyResult = n.f4.accept(this, c);
        instr.addAll(bodyResult.getCode());
        instr.add(new Goto(new Label(topLabel)));
        instr.add(new LabelInstr(new Label(endLabel)));
        return new Result(instr, null);
    }

    @Override
    public Result visit(PrintStatement n, Context c) {
        Result exprResult = n.f2.accept(this, c);
        List<Instruction> instr = new ArrayList<Instruction>(exprResult.getCode());
        instr.add(new Print(exprResult.getIdentifier()));
        return new Result(instr, null);
    }

    // ================================================================
    // Expressions — dispatch wrappers
    // ================================================================

    @Override
    public Result visit(Expression n, Context c) {
        return n.f0.accept(this, c);
    }

    @Override
    public Result visit(PrimaryExpression n, Context c) {
        return n.f0.accept(this, c);
    }

    // ================================================================
    // Binary / unary expressions
    // ================================================================

    @Override
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

    @Override
    public Result visit(CompareExpression n, Context c) {
        Result r1 = n.f0.accept(this, c);
        Result r2 = n.f2.accept(this, c);
        List<Instruction> instr = new ArrayList<Instruction>(r1.getCode());
        instr.addAll(r2.getCode());
        Identifier dest = new Identifier(c.nextTemp());
        instr.add(new LessThan(dest, r1.getIdentifier(), r2.getIdentifier()));
        return new Result(instr, dest);
    }

    @Override
    public Result visit(PlusExpression n, Context c) {
        Result r1 = n.f0.accept(this, c);
        Result r2 = n.f2.accept(this, c);
        List<Instruction> instr = new ArrayList<Instruction>(r1.getCode());
        instr.addAll(r2.getCode());
        Identifier dest = new Identifier(c.nextTemp());
        instr.add(new Add(dest, r1.getIdentifier(), r2.getIdentifier()));
        return new Result(instr, dest);
    }

    @Override
    public Result visit(MinusExpression n, Context c) {
        Result r1 = n.f0.accept(this, c);
        Result r2 = n.f2.accept(this, c);
        List<Instruction> instr = new ArrayList<Instruction>(r1.getCode());
        instr.addAll(r2.getCode());
        Identifier dest = new Identifier(c.nextTemp());
        instr.add(new Subtract(dest, r1.getIdentifier(), r2.getIdentifier()));
        return new Result(instr, dest);
    }

    @Override
    public Result visit(TimesExpression n, Context c) {
        Result r1 = n.f0.accept(this, c);
        Result r2 = n.f2.accept(this, c);
        List<Instruction> instr = new ArrayList<Instruction>(r1.getCode());
        instr.addAll(r2.getCode());
        Identifier dest = new Identifier(c.nextTemp());
        instr.add(new Multiply(dest, r1.getIdentifier(), r2.getIdentifier()));
        return new Result(instr, dest);
    }

    @Override
    public Result visit(ArrayLookup n, Context c) {
        Result arrResult = n.f0.accept(this, c);
        Result idxResult = n.f2.accept(this, c);
        List<Instruction> instr = new ArrayList<Instruction>(arrResult.getCode());
        instr.addAll(idxResult.getCode());
        Identifier arr = arrResult.getIdentifier();
        Identifier idx = idxResult.getIdentifier();

        instr.addAll(boundsCheck(arr, idx, c));

        Identifier one = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(one, 1));
        Identifier idxp1 = new Identifier(c.nextTemp());
        instr.add(new Add(idxp1, idx, one));
        Identifier four = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(four, 4));
        Identifier byteOff = new Identifier(c.nextTemp());
        instr.add(new Multiply(byteOff, idxp1, four));
        Identifier addr = new Identifier(c.nextTemp());
        instr.add(new Add(addr, arr, byteOff));
        Identifier dest = new Identifier(c.nextTemp());
        instr.add(new Load(dest, addr, 0));
        return new Result(instr, dest);
    }

    @Override
    public Result visit(ArrayLength n, Context c) {
        Result arrResult = n.f0.accept(this, c);
        List<Instruction> instr = new ArrayList<Instruction>(arrResult.getCode());
        Identifier dest = new Identifier(c.nextTemp());
        instr.add(new Load(dest, arrResult.getIdentifier(), 0));
        return new Result(instr, dest);
    }

    @Override
    public Result visit(MessageSend n, Context c) {
        String methodName = n.f2.f0.toString();
        String receiverClass = getPrimaryType(n.f0, c);
        int methodOffset = classTable.getMethodOffset(receiverClass, methodName);

        Result recvResult = n.f0.accept(this, c);
        List<Instruction> instr = new ArrayList<Instruction>(recvResult.getCode());
        Identifier recvId = recvResult.getIdentifier();

        Identifier vtablePtr = new Identifier(c.nextTemp());
        instr.add(new Load(vtablePtr, recvId, 0));
        Identifier methodPtr = new Identifier(c.nextTemp());
        instr.add(new Load(methodPtr, vtablePtr, methodOffset));

        List<Identifier> args = new ArrayList<Identifier>();
        args.add(recvId);
        if (n.f4.present()) {
            ExpressionList exprList = (ExpressionList) n.f4.node;
            Result a0 = exprList.f0.accept(this, c);
            instr.addAll(a0.getCode());
            args.add(a0.getIdentifier());
            for (Enumeration<Node> e = exprList.f1.elements(); e.hasMoreElements();) {
                ExpressionRest er = (ExpressionRest) e.nextElement();
                Result ar = er.f1.accept(this, c);
                instr.addAll(ar.getCode());
                args.add(ar.getIdentifier());
            }
        }
        Identifier dest = new Identifier(c.nextTemp());
        instr.add(new Call(dest, methodPtr, args));
        return new Result(instr, dest);
    }

    // ================================================================
    // Primary expressions
    // ================================================================

    @Override
    public Result visit(IntegerLiteral n, Context c) {
        Identifier dest = new Identifier(c.nextTemp());
        List<Instruction> instr = new ArrayList<Instruction>();
        instr.add(new Move_Id_Integer(dest, Integer.parseInt(n.f0.tokenImage)));
        return new Result(instr, dest);
    }

    @Override
    public Result visit(TrueLiteral n, Context c) {
        Identifier dest = new Identifier(c.nextTemp());
        List<Instruction> instr = new ArrayList<Instruction>();
        instr.add(new Move_Id_Integer(dest, 1));
        return new Result(instr, dest);
    }

    @Override
    public Result visit(FalseLiteral n, Context c) {
        Identifier dest = new Identifier(c.nextTemp());
        List<Instruction> instr = new ArrayList<Instruction>();
        instr.add(new Move_Id_Integer(dest, 0));
        return new Result(instr, dest);
    }

    // Override with fully qualified type to avoid conflict with IR.token.Identifier
    // import
    @Override
    public Result visit(minijava.syntaxtree.Identifier n, Context c) {
        String name = n.f0.toString();
        List<Instruction> instr = new ArrayList<Instruction>();
        if (isLocalOrParam(name, c)) {
            return new Result(instr, new Identifier("u_" + name));
        }
        int offset = classTable.getFieldOffset(c.getCurrentClass().getClassName(), name);
        Identifier dest = new Identifier(c.nextTemp());
        instr.add(new Load(dest, new Identifier("this"), offset));
        return new Result(instr, dest);
    }

    @Override
    public Result visit(ThisExpression n, Context c) {
        return new Result(new ArrayList<Instruction>(), new Identifier("this"));
    }

    @Override
    public Result visit(ArrayAllocationExpression n, Context c) {
        Result sizeResult = n.f3.accept(this, c);
        List<Instruction> instr = new ArrayList<Instruction>(sizeResult.getCode());
        Identifier nId = sizeResult.getIdentifier();

        Identifier one = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(one, 1));
        Identifier slots = new Identifier(c.nextTemp());
        instr.add(new Add(slots, nId, one));
        Identifier four = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(four, 4));
        Identifier byteSize = new Identifier(c.nextTemp());
        instr.add(new Multiply(byteSize, slots, four));
        Identifier arr = new Identifier(c.nextTemp());
        instr.add(new Alloc(arr, byteSize));
        instr.add(new Store(arr, 0, nId));
        return new Result(instr, arr);
    }

    @Override
    public Result visit(AllocationExpression n, Context c) {
        String className = n.f1.f0.toString();
        ClassInfo ci = classTable.getClassInfo(className);
        int numFields = classTable.getTotalFieldCount(className);
        int numMethods = ci.getMethods().size();

        List<Instruction> instr = new ArrayList<Instruction>();

        Identifier vtable;
        if (numMethods == 0) {
            // alloc(0) is invalid in Sparrow; store integer 0 as the vtable pointer
            vtable = new Identifier(c.nextTemp());
            instr.add(new Move_Id_Integer(vtable, 0));
        } else {
            // Allocate vtable and fill with function pointers
            Identifier vtableSize = new Identifier(c.nextTemp());
            instr.add(new Move_Id_Integer(vtableSize, numMethods * 4));
            vtable = new Identifier(c.nextTemp());
            instr.add(new Alloc(vtable, vtableSize));

            for (java.util.Map.Entry<String, MethodInfo> entry : ci.getMethods().entrySet()) {
                String mName = entry.getKey();
                String owner = entry.getValue().getOwnerClassName();
                int mOffset = classTable.getMethodOffset(className, mName);
                Identifier fp = new Identifier(c.nextTemp());
                instr.add(new Move_Id_FuncName(fp, new FunctionName(owner + "_" + mName)));
                instr.add(new Store(vtable, mOffset, fp));
            }
        }

        // Allocate object: slot 0 = vtable ptr, then one slot per field
        Identifier objSize = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(objSize, (1 + numFields) * 4));
        Identifier obj = new Identifier(c.nextTemp());
        instr.add(new Alloc(obj, objSize));
        instr.add(new Store(obj, 0, vtable));
        return new Result(instr, obj);
    }

    @Override
    public Result visit(NotExpression n, Context c) {
        Result operand = n.f1.accept(this, c);
        List<Instruction> instr = new ArrayList<Instruction>(operand.getCode());
        Identifier one = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(one, 1));
        Identifier dest = new Identifier(c.nextTemp());
        instr.add(new Subtract(dest, one, operand.getIdentifier()));
        return new Result(instr, dest);
    }

    @Override
    public Result visit(BracketExpression n, Context c) {
        return n.f1.accept(this, c);
    }

    // ================================================================
    // Helpers
    // ================================================================

    private boolean isLocalOrParam(String name, Context c) {
        MethodInfo m = c.getCurrentMethod();
        if (m == null)
            return false;
        return m.getParams().contains(name) || m.getLocals().contains(name);
    }

    // Emits bounds-check instructions for arr[idx].
    // Terminates via error() if idx < 0 or idx >= arr.length.
    private List<Instruction> boundsCheck(Identifier arr, Identifier idx, Context c) {
        List<Instruction> instr = new ArrayList<Instruction>();
        String okLabel1 = "L" + c.nextLabel();
        String oobLabel = "L" + c.nextLabel();
        String okLabel2 = "L" + c.nextLabel();

        // Check idx >= 0: isNeg = (idx < 0); if0 isNeg goto okLabel1 skips error when
        // not negative
        Identifier zero = new Identifier(c.nextTemp());
        instr.add(new Move_Id_Integer(zero, 0));
        Identifier isNeg = new Identifier(c.nextTemp());
        instr.add(new LessThan(isNeg, idx, zero));
        instr.add(new IfGoto(isNeg, new Label(okLabel1)));
        instr.add(new ErrorMessage("\"array index out of bounds\""));
        instr.add(new LabelInstr(new Label(okLabel1)));

        // Check idx < length: inBounds = (idx < len); if0 inBounds goto oobLabel when
        // invalid
        Identifier len = new Identifier(c.nextTemp());
        instr.add(new Load(len, arr, 0));
        Identifier inBounds = new Identifier(c.nextTemp());
        instr.add(new LessThan(inBounds, idx, len));
        instr.add(new IfGoto(inBounds, new Label(oobLabel)));
        instr.add(new Goto(new Label(okLabel2)));
        instr.add(new LabelInstr(new Label(oobLabel)));
        instr.add(new ErrorMessage("\"array index out of bounds\""));
        instr.add(new LabelInstr(new Label(okLabel2)));
        return instr;
    }

    private List<Instruction> collectStatements(NodeListOptional stmts, Context c) {
        List<Instruction> result = new ArrayList<Instruction>();
        if (stmts.present()) {
            for (Enumeration<Node> e = stmts.elements(); e.hasMoreElements();) {
                Result r = e.nextElement().accept(this, c);
                if (r != null && r.getCode() != null) {
                    result.addAll(r.getCode());
                }
            }
        }
        return result;
    }

    // Returns the static MiniJava type of a PrimaryExpression for vtable dispatch.
    private String getPrimaryType(PrimaryExpression n, Context c) {
        switch (n.f0.which) {
            case 0:
                return "int"; // IntegerLiteral
            case 1:
                return "boolean"; // TrueLiteral
            case 2:
                return "boolean"; // FalseLiteral
            case 3: { // Identifier → look up variable type
                minijava.syntaxtree.Identifier id = (minijava.syntaxtree.Identifier) n.f0.choice;
                return lookupVarType(id.f0.toString(), c);
            }
            case 4:
                return c.getCurrentClass().getClassName(); // ThisExpression
            case 5:
                return "int[]"; // ArrayAllocationExpression
            case 6: { // AllocationExpression
                AllocationExpression ae = (AllocationExpression) n.f0.choice;
                return ae.f1.f0.toString();
            }
            case 7:
                return "boolean"; // NotExpression
            case 8: { // BracketExpression
                BracketExpression be = (BracketExpression) n.f0.choice;
                return getExprType(be.f1, c);
            }
            default:
                return "int";
        }
    }

    private String getExprType(Expression n, Context c) {
        switch (n.f0.which) {
            case 0:
                return "boolean"; // AndExpression
            case 1:
                return "boolean"; // CompareExpression
            case 2:
                return "int"; // PlusExpression
            case 3:
                return "int"; // MinusExpression
            case 4:
                return "int"; // TimesExpression
            case 5:
                return "int"; // ArrayLookup
            case 6:
                return "int"; // ArrayLength
            case 7: { // MessageSend
                MessageSend ms = (MessageSend) n.f0.choice;
                String rcvType = getPrimaryType(ms.f0, c);
                return classTable.getClassInfo(rcvType)
                        .getMethodInfo(ms.f2.f0.toString())
                        .getReturnType();
            }
            case 8:
                return getPrimaryType((PrimaryExpression) n.f0.choice, c);
            default:
                return "int";
        }
    }

    private String lookupVarType(String name, Context c) {
        MethodInfo m = c.getCurrentMethod();
        if (m != null) {
            String t = m.getParamType(name);
            if (t != null)
                return t;
            t = m.getLocalType(name);
            if (t != null)
                return t;
        }
        String t = c.getCurrentClass().getFieldType(name);
        if (t != null)
            return t;
        return "int";
    }
}
