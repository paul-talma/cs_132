package hw2;

/** Thrown when a method has duplicate param names, duplicate local names, or a local that shadows a param. */
public class MethodParamsAndLocalsNotUniqueException extends TypeException {
    public MethodParamsAndLocalsNotUniqueException(String className) {
        super(String.format("Method params and locals not unique in class %s", className));
    }
}
