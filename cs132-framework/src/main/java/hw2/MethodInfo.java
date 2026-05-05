package hw2;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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

    public void addParam(String id, String param) {
        if (params.containsKey(id))
            throw new MethodParamsAndLocalsNotUniqueException(id);
        params.put(id, param);
    }

    public String getParamType(String id) {
        return params.get(id);
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void addLocal(String id, String local) {
        if (locals.containsKey(id))
            throw new MethodParamsAndLocalsNotUniqueException(id);
        locals.put(id, local);
    }

    public Map<String, String> getLocals() {
        return locals;
    }

    public boolean paramsAndLocalsDistinct() {
        return Collections.disjoint(params.keySet(), locals.keySet());
    }

    public String getReturnType() {
        return returnType;
    }
}
