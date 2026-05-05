package hw2;

import java.util.LinkedHashMap;
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

    public void addField(String fieldName, String type) {
        if (fields.containsKey(fieldName))
            throw new FieldNamesNotUniqueException(fieldName);
        fields.put(fieldName, type);
    }

    public String getFieldType(String fieldName) {
        return fields.get(fieldName);
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void addMethod(String methodName, MethodInfo methodInfo) {
        if (methods.containsKey(methodName))
            throw new MethodNamesNotUniqueException(methodName);
        methods.put(methodName, methodInfo);
    }

    public MethodInfo getMethod(String methodName) {
        return methods.get(methodName);
    }

    public boolean methodParamsAndLocalsUnique() {
        for (MethodInfo m : methods.values()) {
            if (!m.paramsAndLocalsDistinct()) {
                return false;
            }
        }
        return true;
    }

    public void setMain() {
        isMain = true;
    }

    public boolean isMain() {
        return isMain;
    }
}
