package hw3;

/**
 * Mutable per-visitor state threaded through the compilation passes.
 *
 * During passes 1 and 2, only currentClass and currentMethod are used —
 * they track which class/method the visitor is currently inside so that
 * VarDeclaration and FormalParameter nodes know where to register their names.
 *
 * tempCounter and labelCounter are reserved for the code-generation pass
 * (pass 3) to produce unique Sparrow identifier and label names.
 */
public class Context {
    ClassInfo currentClass;
    MethodInfo currentMethod;
    int tempCounter = 0;
    int labelCounter = 0;

    public ClassInfo getCurrentClass() {
        return currentClass;
    }

    public void setCurrentClass(ClassInfo currentClass) {
        this.currentClass = currentClass;
    }

    public void clearCurrentClass() {
        this.currentClass = null;
    }

    public MethodInfo getCurrentMethod() {
        return currentMethod;
    }

    public void setCurrentMethod(MethodInfo currentMethod) {
        this.currentMethod = currentMethod;
    }

    /** Clears the current method; call after finishing a method declaration. */
    public void clearCurrentMethod() {
        this.currentMethod = null;
    }

    /** Returns the next fresh temporary index and advances the counter. */
    public String nextTemp() {
        return "v" + tempCounter++;
    }

    /** Returns the next fresh label index and advances the counter. */
    public int nextLabel() {
        return labelCounter++;
    }

    public int getTempCounter() {
        return tempCounter;
    }

    public void setTempCounter(int tempCounter) {
        this.tempCounter = tempCounter;
    }

    public int getLabelCounter() {
        return labelCounter;
    }

    public void setLabelCounter(int labelCounter) {
        this.labelCounter = labelCounter;
    }
}
