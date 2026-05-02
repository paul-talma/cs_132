package hw2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassInfo
 * Contains information about a class
 * Specifically:
 * - The class name
 * - The class parent, if any
 * - The class's fields
 * - The class's methods
 */
public class ClassInfo {
    String name;
    String parent;
    boolean hasParent;
    boolean isMain = false;
    List<String> fieldNames = new ArrayList<String>();
    Map<String, Id> fields = new HashMap<String, Id>();
    List<String> methodNames = new ArrayList<String>();
    Map<String, MethodInfo> methods = new HashMap<String, MethodInfo>();

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

    public void addField(String fieldName, Id id) {
        fields.put(fieldName, id);
        fieldNames.add(fieldName);
    }

    public Id getField(String fieldName) {
        return fields.get(fieldName);
    }

    public Map<String, Id> getFields() {
        return fields;
    }

    public void addMethod(String methodName, MethodInfo methodInfo) {
        methods.put(methodName, methodInfo);
        methodNames.add(methodName);
    }

    public MethodInfo getMethod(String methodName) {
        return methods.get(methodName);
    }

    public boolean fieldNamesUnique() {
        return fieldNames.size() == fields.size();
    }

    public boolean methodNamesUnique() {
        return methodNames.size() == methods.size();

    }

    public boolean methodParamsAndLocalsUnique() {
        for (MethodInfo m : methods.values()) {
            if (!m.paramsAndLocalsUnique()) {
                return false;
            }
        }
        return true;
    }

    public void setMain() {
        isMain = true;
    }

}
