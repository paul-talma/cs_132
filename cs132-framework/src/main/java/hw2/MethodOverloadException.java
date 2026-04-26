package hw2;

public class MethodOverloadException extends TypeException {
    public MethodOverloadException(String baseClass, String parentClass, String method) {
        super(String.format("Class %s overloads method %s declared in class %s", baseClass, method, parentClass));
    }
}
