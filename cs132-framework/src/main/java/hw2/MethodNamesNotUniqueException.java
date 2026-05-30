package hw2;

/** Thrown when a class declares two methods with the same name. */
public class MethodNamesNotUniqueException extends TypeException {
    public MethodNamesNotUniqueException(String className) {
        super(String.format("Method names not unique in class %s", className));
    }
}
