package hw2;

import java.util.HashMap;
import java.util.Map;

/**
 * A flat name → type map used during phase 3 type-checking.
 *
 * Scoping is handled by creating a new SymbolTable that copies the parent's
 * entries (via the copy constructor) and then adding the new scope's bindings.
 * This means inner scopes shadow outer ones, and changes to an inner table do
 * not affect the parent.
 */
public class SymbolTable {
    Map<String, String> table = new HashMap<String, String>();

    public SymbolTable() {}

    /** Creates a new scope that inherits all bindings from parentTable. */
    public SymbolTable(SymbolTable parentTable) {
        table = new HashMap<String, String>(parentTable.table);
    }

    public void addSymbol(String id, String type) {
        table.put(id, type);
    }

    /** Returns the type for id, or null if not in scope. */
    public String getType(String id) {
        return table.get(id);
    }

    /** Bulk-adds all name → type bindings from the given map. */
    public void update(Map<String, String> bindings) {
        table.putAll(bindings);
    }

    public boolean contains(String id) {
        return table.containsKey(id);
    }
}
