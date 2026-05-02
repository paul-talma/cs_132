package hw2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ClassTable
 * Contains information about the class hierarchy.
 * Specifically, contains a list of class names and a map from class names to
 * class info.
 */
public class ClassTable {
    HashMap<String, ClassInfo> classTable = new HashMap<String, ClassInfo>();
    List<String> classList = new ArrayList<String>();

    public ClassInfo getClass(String key) {
        return classTable.get(key);
    }

    public void addClassInfo(String key, ClassInfo info) {
        classTable.put(key, info);
        classList.add(key);
    }

    void allClassesUnique() throws ClassNotUniqueException {
        if (classList.size() != classTable.size())
            throw new ClassNotUniqueException();
    }

    void acyclicTypes() throws CyclicClassesException {
        // for each class, check for cycle
        for (ClassInfo c : classTable.values()) {
            Set<String> visited = new HashSet<String>();
            if (detectCycle(c, visited)) {
                throw new CyclicClassesException(c.name);
            }
        }
    }

    boolean detectCycle(ClassInfo c, Set<String> visited) {
        if (visited.contains(c.name))
            return true;
        if (!c.hasParent)
            return false;
        ClassInfo parent = getClass(c.parent);
        if (parent == null)
            return true; // extends an undefined class — treat as error
        visited.add(c.name);
        return detectCycle(parent, visited);
    }

    void allFieldNamesUnique() throws FieldNamesNotUniqueException {
        for (ClassInfo c : classTable.values()) {
            if (!c.fieldNamesUnique())
                throw new FieldNamesNotUniqueException(c.name);
        }
    }

    void allMethodNamesUnique() throws MethodNamesNotUniqueException {
        for (ClassInfo c : classTable.values()) {
            if (!c.methodNamesUnique()) {
                throw new MethodNamesNotUniqueException(c.name);
            }
        }

    }

    void allMethodParamsAndLocalsUnique() throws MethodParamsAndLocalsNotUniqueException {
        for (ClassInfo c : classTable.values()) {
            if (!c.methodParamsAndLocalsUnique())
                throw new MethodParamsAndLocalsNotUniqueException(c.name);
        }
    }

    public MethodInfo methodType(String className, String methodName) {
        ClassInfo classInfo = getClass(className);
        MethodInfo method = classInfo.methods.get(methodName);
        if (method == null && classInfo.hasParent)
            return methodType(classInfo.parent, methodName);
        return method;
    }

    public void noOverloads() throws MethodOverloadException {
        for (ClassInfo c : classTable.values()) {
            if (c.hasParent) {
                for (MethodInfo m : c.methods.values()) {
                    if (overloading(c.name, c.parent, m.name))
                        throw new MethodOverloadException(c.name, c.parent, m.name);
                }
            }
        }
    }

    boolean overloading(String baseClassName, String parentClassName, String methodName) {
        MethodInfo methodType2 = methodType(parentClassName, methodName);
        if (methodType2 == null) {
            return false;
        }
        MethodInfo methodType1 = methodType(baseClassName, methodName);
        return !methodType1.equals(methodType2);
    }

    private Map<String, Id> updateFieldsMap(Map<String, Id> newFields,
            Map<String, Id> inheritedFields) {
        inheritedFields.putAll(newFields);
        return inheritedFields;
    }

    public Map<String, Id> fields(String className) {
        ClassInfo thisClass = getClass(className);
        Map<String, Id> fieldMap = thisClass.fields;
        if (thisClass.hasParent) {
            String parentName = thisClass.parent;
            Map<String, Id> inheritedFields = new HashMap<String, Id>(fields(parentName));
            fieldMap = updateFieldsMap(fieldMap, inheritedFields);
        }
        return fieldMap;
    }

    public boolean subtype(String typeName0, String typeName1) {
        if (typeName0.equals(typeName1))
            return true;
        ClassInfo class0 = getClass(typeName0);
        if (class0 == null || !class0.hasParent) {
            return false;
        }
        String parentName = class0.parent;
        return subtype(parentName, typeName1);
    }
}
