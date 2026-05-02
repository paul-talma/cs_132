// VALID: child class declares a field with the same name as parent.
// Rule 23/24 only require distinct within each class declaration.
// fields(Child) gives child's x precedence over parent's x (rule 15).
class Main {
    public static void main(String[] a) {
        System.out.println(new Child().run());
    }
}

class Parent {
    int x;
    public int getX() {
        x = 10;
        return x;
    }
}

class Child extends Parent {
    int x;
    public int run() {
        x = 20;
        return x;
    }
}
