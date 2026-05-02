package hw2;

public class MethodNamesNotUniqueException extends TypeException {
    public MethodNamesNotUniqueException(String className) {
        super(String.format("Method names not unique in class %s", className));
    }
}
