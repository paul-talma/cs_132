package hw3;

import visitor.*;

import minijava.syntaxtree.*;
import sparrow.Program;

public class J2SCompiler {
    Goal goal;
    Program prog;
    public ClassTable classTable = new ClassTable();

    public J2SCompiler(Goal g) {
        this.goal = g;
    }

    public void compile() {
        firstPass();
        secondPass();
        thirdPass();
    }

    // builds class table
    private void firstPass() {
        LayoutFirstPassVisitor v = new LayoutFirstPassVisitor();
        v.visit(goal, classTable);
    }

    // augments class table with field and method info
    private void secondPass() {
        LayoutSecondPassVisitor v = new LayoutSecondPassVisitor();
        v.visit(goal, classTable);
        classTable.closeFieldsUnderInheritance();
        classTable.closeMethodsUnderInheritance();
    }

    private void thirdPass() {
        // CodegenVisitor v = new CodegenVisitor(classTable);
        TranslateVisitor v = new TranslateVisitor(classTable);
        Context c = new Context();
        v.visit(goal, c);
        prog = v.getProgram();
        System.out.print(v.getProgram().toString());
    }

    // helpers
    public void displayClassTable() {
        for (ClassInfo c : classTable.table.values()) {
            displayClass(c);
        }
    }

    void displayClass(ClassInfo c) {
        String className = c.getClassName();
        System.out.println(String.format("\nClass: %s", className));
        if (c.hasParent()) {
            String parentName = c.getParentName();
            System.out.println(String.format("  Parent: %s", parentName));
        }

        System.out.println("  Fields:");
        for (String f : c.getFields().keySet())
            System.out.println(String.format("\t  %s : %s", f, c.getFieldType(f)));

        System.out.println("  Methods:");
        for (MethodInfo m : c.getMethods().values())
            displayMethod(m);
    }

    void displayMethod(MethodInfo m) {
        System.out.println(String.format("\t%s : %s", m.getMethodName(), m.getReturnType()));
        System.out.println("\t  params:");
        for (String p : m.getParams()) {
            System.out.println(String.format("\t    %s : %s", p, m.getParamType(p)));
        }
        System.out.println("\t  locals:");
        for (String l : m.getLocals()) {
            System.out.println(String.format("\t    %s : %s", l, m.getLocalType(l)));
        }
    }

    public Program getProg() {
        return prog;
    }
}
