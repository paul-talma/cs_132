package hw3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Registry of every class in the MiniJava program, keyed by name.
 *
 * Populated during pass 1 (LayoutFirstPassVisitor) with class names and
 * parent relationships. Augmented during pass 2 (LayoutSecondPassVisitor)
 * with fields and methods. Pass 3 closes the maps under inheritance so
 * each ClassInfo's fields/methods reflect the complete Sparrow object layout.
 */
public class ClassTable {
    LinkedHashMap<String, ClassInfo> table = new LinkedHashMap<>();

    /** Adds a class; throws if the name is already registered. */
    public void addClassInfo(String className, ClassInfo info) {
        if (table.containsKey(className))
            throw new ClassNotUniqueException(className);
        table.put(className, info);
    }

    /** Returns the ClassInfo for the given name, or null if not found. */
    public ClassInfo getClassInfo(String className) {
        return table.get(className);
    }

    /**
     * Replaces each class's `fields` map with the inheritance-closed version:
     * parent fields first (in declaration order), then this class's own fields.
     * Must be called after pass 2.
     */
    public void closeFieldsUnderInheritance() {
        for (ClassInfo c : table.values())
            closeFields(c);
    }

    private void closeFields(ClassInfo c) {
        if (c.fieldsAreClosed())
            return;
        if (!c.hasParent()) {
            c.closeFields();
            return;
        }
        ClassInfo parent = table.get(c.getParentName());
        closeFields(parent); // ensure parent is closed first
        LinkedHashMap<String, String> merged = new LinkedHashMap<>(parent.getFields());
        merged.putAll(c.getFields()); // own fields shadow/follow parent fields
        c.setFields(merged);
        c.closeFields();
    }

    /**
     * Replaces each class's `methods` map with the inheritance-closed version:
     * parent methods first, then new methods added by this class. Overridden
     * methods replace the parent's entry at the same map position (LinkedHashMap
     * preserves the parent's insertion order for inherited keys, and putAll
     * replaces values without changing key order).
     * Must be called after pass 2.
     */
    public void closeMethodsUnderInheritance() {
        for (ClassInfo c : table.values())
            closeMethods(c);
    }

    private void closeMethods(ClassInfo c) {
        if (c.methodsAreClosed())
            return;
        if (!c.hasParent()) {
            c.closeMethods();
            return;
        }
        ClassInfo parent = table.get(c.getParentName());
        closeMethods(parent); // ensure parent is closed first
        LinkedHashMap<String, MethodInfo> merged = new LinkedHashMap<>(parent.getMethods());
        merged.putAll(c.getMethods()); // overrides replace parent entry; new methods append
        c.setMethods(merged);
        c.closeMethods();
    }

    /**
     * Returns the byte offset of fieldName within the Sparrow object block for
     * className. Slot 0 (offset 0) is the vtable pointer; own fields of each
     * class follow in hierarchy order (root first, then subclass). When a
     * subclass redeclares a field with the same name as an ancestor, both
     * occupy distinct slots — this method returns the slot of the field as
     * declared in the nearest ancestor of className (including className itself).
     */
    public int getFieldOffset(String className, String fieldName) {
        // Build chain from root to className
        List<ClassInfo> chain = new ArrayList<>();
        ClassInfo ci = table.get(className);
        while (ci != null) {
            chain.add(0, ci);
            ci = ci.hasParent() ? table.get(ci.getParentName()) : null;
        }

        // Find the nearest ancestor (starting from className) that declares fieldName
        int declaringIndex = -1;
        for (int i = chain.size() - 1; i >= 0; i--) {
            if (chain.get(i).getOwnFields().containsKey(fieldName)) {
                declaringIndex = i;
                break;
            }
        }
        if (declaringIndex == -1)
            throw new IllegalArgumentException("Field " + fieldName + " not found in class " + className);

        // Sum ownFields counts for all classes before the declaring class
        int slotsBefore = 0;
        for (int i = 0; i < declaringIndex; i++)
            slotsBefore += chain.get(i).getOwnFields().size();

        // Index of fieldName within the declaring class's own fields
        int indexInOwn = 0;
        for (String f : chain.get(declaringIndex).getOwnFields().keySet()) {
            if (f.equals(fieldName)) break;
            indexInOwn++;
        }

        return (1 + slotsBefore + indexInOwn) * 4; // slot 0 = vtable
    }

    /**
     * Returns the total number of field slots across the entire class hierarchy
     * for className. Used to compute object allocation size.
     */
    public int getTotalFieldCount(String className) {
        int count = 0;
        ClassInfo ci = table.get(className);
        while (ci != null) {
            count += ci.getOwnFields().size();
            ci = ci.hasParent() ? table.get(ci.getParentName()) : null;
        }
        return count;
    }

    /**
     * Returns the byte offset of methodName within the vtable for className.
     * Vtable slots are 4 bytes each, starting at offset 0.
     * Must be called after closeMethodsUnderInheritance().
     */
    public int getMethodOffset(String className, String methodName) {
        int index = 0;
        for (String name : table.get(className).getMethods().keySet()) {
            if (name.equals(methodName))
                return index * 4;
            index++;
        }
        throw new IllegalArgumentException("Method " + methodName + " not found in class " + className);
    }
}
