package hw3;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Stores layout information for a single MiniJava class.
 *
 * After pass 2 (LayoutSecondPassVisitor), `fields` contains only the fields
 * declared directly in this class and `methods` contains only the methods
 * declared directly in this class.
 *
 * After pass 3 (ClassTable.closeFieldsUnderInheritance /
 * closeMethodsUnderInheritance), `fields` and `methods` are replaced with the
 * full inheritance-closed versions: parent members come first (in
 * declaration order), followed by this class's own members. This is the
 * layout used for Sparrow object/vtable allocation.
 */
public class ClassInfo {
    String className;
    String parentName;
    boolean hasParent;
    boolean isMain = false;

    // Populated in pass 2; replaced with the inheritance closure in pass 3.
    LinkedHashMap<String, String> fields = new LinkedHashMap<>();
    LinkedHashMap<String, MethodInfo> methods = new LinkedHashMap<>();

    // Own fields only (never modified after pass 2); used for correct offset computation
    // when subclasses shadow parent fields with the same name.
    LinkedHashMap<String, String> ownFields = new LinkedHashMap<>();

    // Used by the closure algorithm to avoid reprocessing a class whose
    // parent chain has already been merged in.
    boolean fieldsClosed = false;
    boolean methodsClosed = false;

    // Byte offsets into the object block / vtable; computed after pass 3.
    HashMap<String, Integer> fieldOffsets = new HashMap<>();
    HashMap<String, Integer> methodOffsets = new HashMap<>();

    public ClassInfo(String name) {
        this.className = name;
        this.hasParent = false;
    }

    public ClassInfo(String name, String parentName) {
        this.className = name;
        this.parentName = parentName;
        this.hasParent = true;
    }

    public String getClassName() { return className; }
    public boolean hasParent()   { return hasParent; }
    public String getParentName(){ return parentName; }
    public boolean isMain()      { return isMain; }
    public void setMain()        { isMain = true; }

    // --- fields ---

    public void addField(String id, String type) {
        fields.put(id, type);
        ownFields.put(id, type);
    }

    /** Returns fields declared directly in this class (never modified by inheritance closure). */
    public LinkedHashMap<String, String> getOwnFields() {
        return ownFields;
    }

    /** Returns the type of the given field, or null if not found. */
    public String getFieldType(String id) {
        return fields.get(id);
    }

    /** Returns the field map (name → type) in layout order. */
    public LinkedHashMap<String, String> getFields() {
        return fields;
    }

    /** Replaces the field map during inheritance closure (pass 3). */
    public void setFields(LinkedHashMap<String, String> fields) {
        this.fields = fields;
    }

    public boolean fieldsAreClosed() { return fieldsClosed; }
    public void closeFields()        { fieldsClosed = true; }

    // --- methods ---

    public void addMethod(String methodName, MethodInfo info) {
        methods.put(methodName, info);
    }

    /** Returns the MethodInfo for the given method, or null if not found. */
    public MethodInfo getMethodInfo(String methodName) {
        return methods.get(methodName);
    }

    /** Returns the method map (name → MethodInfo) in layout order. */
    public LinkedHashMap<String, MethodInfo> getMethods() {
        return methods;
    }

    /** Replaces the method map during inheritance closure (pass 3). */
    public void setMethods(LinkedHashMap<String, MethodInfo> methods) {
        this.methods = methods;
    }

    public boolean methodsAreClosed() { return methodsClosed; }
    public void closeMethods()        { methodsClosed = true; }
}
