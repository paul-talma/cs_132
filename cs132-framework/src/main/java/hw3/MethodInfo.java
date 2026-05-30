package hw3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Stores all information about a single MiniJava method needed for layout
 * and Sparrow code generation: its name, owning class, return type, formal
 * parameters (name → type), and local variables (name → type).
 *
 * Both maps are LinkedHashMaps to preserve declaration order. Parameter order
 * matters for call-site argument matching; local order matters for consistent
 * Sparrow identifier naming during code generation.
 */
public class MethodInfo {
    String methodName;
    String ownerClassName;  // the class that declares this method (not the caller)
    String returnType;
    LinkedHashMap<String, String> params = new LinkedHashMap<>();
    LinkedHashMap<String, String> locals = new LinkedHashMap<>();

    public MethodInfo(String methodName) {
        this.methodName = methodName;
    }

    public MethodInfo(String methodName, String ownerClassName, String returnType) {
        this.methodName = methodName;
        this.ownerClassName = ownerClassName;
        this.returnType = returnType;
    }

    public String getMethodName()    { return methodName; }
    public String getOwnerClassName(){ return ownerClassName; }
    public String getReturnType()    { return returnType; }

    /** Adds a formal parameter in declaration order. */
    public void addParam(String id, String type) {
        params.put(id, type);
    }

    /** Returns the declared type of the given parameter, or null if not found. */
    public String getParamType(String id) {
        return params.get(id);
    }

    /** Returns parameter names in declaration order. */
    public List<String> getParams() {
        return new ArrayList<>(params.keySet());
    }

    /** Adds a local variable in declaration order. */
    public void addLocal(String id, String type) {
        locals.put(id, type);
    }

    /** Returns the declared type of the given local variable, or null if not found. */
    public String getLocalType(String id) {
        return locals.get(id);
    }

    /** Returns local variable names in declaration order. */
    public List<String> getLocals() {
        return new ArrayList<>(locals.keySet());
    }
}
