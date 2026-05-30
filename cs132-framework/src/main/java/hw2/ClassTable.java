package hw2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores information about every class in a MiniJava program.
 *
 * Two structures are maintained in parallel:
 *   - classTable: name → ClassInfo, for O(1) lookup
 *   - classList:  insertion-ordered list of names, used to detect duplicates
 *     (a duplicate name is put() into the map but the list grows longer)
 *
 * After phase 1 (ClassTableBuilderVisitor), call allClassesUnique() and
 * acyclicTypes() to validate the class hierarchy.
 * After phase 2 (ClassTableUpdaterVisitor), call allMethodParamsAndLocalsUnique()
 * and noOverloads() to validate method signatures.
 */
public class ClassTable {
    HashMap<String, ClassInfo> classTable = new HashMap<String, ClassInfo>();
    List<String> classList = new ArrayList<String>();

    /** Returns the ClassInfo for the given class name, or null if not found. */
    public ClassInfo getClass(String key) {
        return classTable.get(key);
    }

    public void addClassInfo(String key, ClassInfo info) {
        classTable.put(key, info);
        classList.add(key);
    }

    /**
     * Throws if any class name was declared more than once.
     * Detected by comparing list length (counts duplicates) to map size (dedupes).
     */
    void allClassesUnique() throws ClassNotUniqueException {
        if (classList.size() != classTable.size())
            throw new ClassNotUniqueException();
    }

    /** Throws if any class is part of a circular inheritance chain. */
    void acyclicTypes() throws CyclicClassesException {
        for (ClassInfo c : classTable.values()) {
            Set<String> visited = new HashSet<String>();
            if (detectCycle(c, visited)) {
                throw new CyclicClassesException(c.name);
            }
        }
    }

    /** DFS cycle detection; also catches extension of an undefined class. */
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

    /** Throws if any method has a param name that collides with a local name. */
    void allMethodParamsAndLocalsUnique() throws MethodParamsAndLocalsNotUniqueException {
        for (ClassInfo c : classTable.values()) {
            if (!c.methodParamsAndLocalsUnique())
                throw new MethodParamsAndLocalsNotUniqueException(c.name);
        }
    }

    /**
     * Walks up the inheritance chain to find the MethodInfo for methodName,
     * starting at className. Returns null if the method is not found.
     */
    public MethodInfo methodType(String className, String methodName) {
        ClassInfo classInfo = getClass(className);
        MethodInfo method = classInfo.methods.get(methodName);
        if (method == null && classInfo.hasParent)
            return methodType(classInfo.parent, methodName);
        return method;
    }

    /**
     * Throws if any subclass method has the same name as a parent method but
     * a different signature (return type or parameter types).
     * Valid overriding (identical signatures) is permitted.
     */
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

    /**
     * Returns true if methodName exists in the parent chain with a different
     * signature than the one declared in baseClassName.
     */
    boolean overloading(String baseClassName, String parentClassName, String methodName) {
        MethodInfo parentMethod = methodType(parentClassName, methodName);
        if (parentMethod == null)
            return false; // method is new in this class, not an override
        MethodInfo baseMethod = methodType(baseClassName, methodName);
        return !methodTypesEqual(baseMethod, parentMethod);
    }

    /**
     * Structural equality check for method signatures: same return type and
     * same parameter types in declaration order.
     */
    boolean methodTypesEqual(MethodInfo m1, MethodInfo m2) {
        if (!m1.returnType.equals(m2.returnType))
            return false;
        if (m1.params.size() != m2.params.size())
            return false;
        List<String> m1params = new ArrayList<String>(m1.params.values());
        List<String> m2params = new ArrayList<String>(m2.params.values());
        for (int i = 0; i < m1params.size(); i++) {
            if (!m1params.get(i).equals(m2params.get(i)))
                return false;
        }
        return true;
    }

    /**
     * Returns the combined field map for className, merging inherited fields
     * (from the parent chain) with the class's own fields. Own fields shadow
     * inherited ones of the same name.
     */
    public Map<String, String> fields(String className) {
        ClassInfo thisClass = getClass(className);
        if (!thisClass.hasParent)
            return thisClass.fields;
        Map<String, String> inherited = new HashMap<String, String>(fields(thisClass.parent));
        inherited.putAll(thisClass.fields);
        return inherited;
    }

    /**
     * Returns true if typeName0 is a subtype of typeName1, i.e. typeName0 == typeName1
     * or typeName0 extends ... extends typeName1.
     */
    public boolean subtype(String typeName0, String typeName1) {
        if (typeName0.equals(typeName1))
            return true;
        ClassInfo class0 = getClass(typeName0);
        if (class0 == null || !class0.hasParent)
            return false;
        return subtype(class0.parent, typeName1);
    }
}
