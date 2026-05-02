// VALID: call a method defined only in the parent, via a child-typed variable.
// Tests that MessageSend walks the inheritance chain for method lookup.
class Main {
    public static void main(String[] a) {
        System.out.println(new Child().getValue());
    }
}

class Parent {
    public int getValue() {
        return 42;
    }
}

class Child extends Parent {
}
