package hw2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores all information about a single MiniJava method: its name, return
 * type, formal parameters (name → type), and local variables (name → type).
 *
 * Both maps are LinkedHashMaps to preserve declaration order, which is
 * required for positional parameter matching during overload checking and
 * call type-checking.
 *
 * Duplicate param names and duplicate local names are rejected eagerly in
 * addParam/addLocal. Param-vs-local collisions are detected separately by
 * paramsAndLocalsDistinct(), called during phase 2 validation.
 */
public class MethodInfo {
    String name;
    Map<String, String> params = new LinkedHashMap<String, String>();
    Map<String, String> locals = new LinkedHashMap<String, String>();
    String returnType;

    public MethodInfo(String name) {
        this.name = name;
    }

    public MethodInfo(String name, String returnType) {
        this.name = name;
        this.returnType = returnType;
    }

    /** Adds a formal parameter; throws if the name is already declared. */
    public void addParam(String id, String type) {
        if (params.containsKey(id))
            throw new MethodParamsAndLocalsNotUniqueException(id);
        params.put(id, type);
    }

    /** Returns the type of the given parameter, or null if not found. */
    public String getParamType(String id) {
        return params.get(id);
    }

    /** Returns the parameter map (name → type) in declaration order. */
    public Map<String, String> getParams() {
        return params;
    }

    /** Adds a local variable; throws if the name is already declared. */
    public void addLocal(String id, String type) {
        if (locals.containsKey(id))
            throw new MethodParamsAndLocalsNotUniqueException(id);
        locals.put(id, type);
    }

    /** Returns the local variable map (name → type) in declaration order. */
    public Map<String, String> getLocals() {
        return locals;
    }

    /**
     * Returns true if no param name also appears as a local variable name.
     * Param-param and local-local collisions are already caught by addParam/addLocal.
     */
    public boolean paramsAndLocalsDistinct() {
        return Collections.disjoint(params.keySet(), locals.keySet());
    }

    public String getReturnType() {
        return returnType;
    }
}
