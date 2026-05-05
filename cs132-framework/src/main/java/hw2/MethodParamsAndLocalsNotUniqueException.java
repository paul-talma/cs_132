package hw2;

public class MethodParamsAndLocalsNotUniqueException extends TypeException {
    public MethodParamsAndLocalsNotUniqueException(String className) {
        super(String.format("Method params and locals not unique in class %s", className));
    }
}
