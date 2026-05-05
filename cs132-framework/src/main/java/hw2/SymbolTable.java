package hw2;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    Map<String, String> table = new HashMap<String, String>();

    public SymbolTable() {
    };

    public SymbolTable(SymbolTable parentTable) {
        table = new HashMap<String, String>(parentTable.table);
    }

    public void addSymbol(String id, String type) {
        table.put(id, type);
    }

    public String getType(String id) {
        return table.get(id);
    }

    public void update(Map<String, String> fields) {
        for (String fieldName : fields.keySet()) {
            table.put(fieldName, fields.get(fieldName));
        }
    }

    public boolean contains(String id) {
        return table.containsKey(id);
    }
}
