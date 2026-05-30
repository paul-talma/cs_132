package hw2;

/** Thrown when a subclass method overrides a parent method with a different signature. */
public class MethodOverloadException extends TypeException {
    public MethodOverloadException(String baseClass, String parentClass, String method) {
        super(String.format("Class %s overloads method %s declared in class %s", baseClass, method, parentClass));
    }
}
