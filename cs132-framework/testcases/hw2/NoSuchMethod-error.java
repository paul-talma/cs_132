// ERROR: call a method that doesn't exist in the class or any ancestor.
class Main {
    public static void main(String[] a) {
        System.out.println(new Child().nonexistent());
    }
}

class Parent {
    public int getValue() { return 42; }
}

class Child extends Parent {
}
