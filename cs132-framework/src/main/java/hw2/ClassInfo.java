package hw2;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores all information about a single MiniJava class: its name, optional
 * parent, fields (name → type), and methods (name → MethodInfo).
 *
 * Both maps are LinkedHashMaps to preserve declaration order, which matters
 * for code generation (field layout) and overload checking (parameter order).
 * Duplicate names are rejected eagerly in addField/addMethod.
 */
public class ClassInfo {
    String name;
    String parent;
    boolean hasParent;
    boolean isMain = false;
    Map<String, String> fields = new LinkedHashMap<String, String>();
    Map<String, MethodInfo> methods = new LinkedHashMap<String, MethodInfo>();

    public ClassInfo(String name) {
        this.name = name;
        this.parent = null;
        this.hasParent = false;
    }

    public ClassInfo(String name, String parent) {
        this.name = name;
        this.parent = parent;
        this.hasParent = true;
    }

    public String getName() {
        return name;
    }

    /** Adds a field; throws immediately if the name is already declared in this class. */
    public void addField(String fieldName, String type) {
        if (fields.containsKey(fieldName))
            throw new FieldNamesNotUniqueException(fieldName);
        fields.put(fieldName, type);
    }

    /** Returns the declared type of fieldName, or null if not found in this class. */
    public String getFieldType(String fieldName) {
        return fields.get(fieldName);
    }

    /** Returns the field map (name → type) in declaration order. */
    public Map<String, String> getFields() {
        return fields;
    }

    /** Adds a method; throws immediately if the name is already declared in this class. */
    public void addMethod(String methodName, MethodInfo methodInfo) {
        if (methods.containsKey(methodName))
            throw new MethodNamesNotUniqueException(methodName);
        methods.put(methodName, methodInfo);
    }

    /** Returns the MethodInfo for methodName, or null if not declared in this class. */
    public MethodInfo getMethod(String methodName) {
        return methods.get(methodName);
    }

    /**
     * Returns true if no method in this class has a param name that also
     * appears as a local variable name in the same method.
     * (Param-param and local-local duplicates are caught eagerly in MethodInfo.)
     */
    public boolean methodParamsAndLocalsUnique() {
        for (MethodInfo m : methods.values()) {
            if (!m.paramsAndLocalsDistinct()) {
                return false;
            }
        }
        return true;
    }

    /** Marks this as the main class (the one containing the main method). */
    public void setMain() {
        isMain = true;
    }

    public boolean isMain() {
        return isMain;
    }
}
