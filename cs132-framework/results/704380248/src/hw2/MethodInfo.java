package hw2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MethodInfo {
    String name;
    List<String> paramIds = new ArrayList<String>();
    Map<String, Id> params = new HashMap<String, Id>();
    List<String> localIds = new ArrayList<String>();
    Map<String, Id> locals = new HashMap<String, Id>();
    String returnType;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof MethodInfo))
            return false;
        MethodInfo other = (MethodInfo) o;
        if (!other.name.equals(this.name) || other.paramIds.size() != this.paramIds.size()
                || !other.returnType.equals(this.returnType))
            return false;
        for (int i = 0; i < this.paramIds.size(); ++i) {
            String thisType = this.params.get(this.paramIds.get(i)).type;
            String otherType = other.params.get(other.paramIds.get(i)).type;
            if (!thisType.equals(otherType))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, returnType, params);
    }

    public MethodInfo(String name) {
        this.name = name;
        params = new HashMap<String, Id>();
    }

    public MethodInfo(String name, String returnType) {
        this.name = name;
        this.returnType = returnType;
        params = new HashMap<String, Id>();
    }

    public void addParam(String id, Id param) {
        params.put(id, param);
        paramIds.add(id);
    }

    public List<String> getParamIds() {
        return paramIds;
    }

    public String getParamType(String id) {
        return params.get(id).type;
    }

    public Map<String, Id> getParams() {
        return params;
    }

    public void addLocal(String id, Id param) {
        localIds.add(id);
        locals.put(id, param);
    }

    public Map<String, Id> getLocals() {
        return locals;
    }

    public boolean paramsAndLocalsUnique() {
        HashSet<String> paramsAndLocals = new HashSet<String>(paramIds);
        paramsAndLocals.addAll(localIds);
        return paramIds.size() + localIds.size() == paramsAndLocals.size();
    }

    public String getReturnType() {
        return returnType;
    }
}
