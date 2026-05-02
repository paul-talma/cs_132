// VALID: pass a child (Derived) where a parent (Base) is expected.
// Tests rule 39: actual arg type must be <= formal param type.
class Main {
    public static void main(String[] a) {
        System.out.println(new Tester().run());
    }
}

class Base {
    public int val() { return 1; }
}

class Derived extends Base {
    public int val() { return 99; }
}

class Tester {
    public int consume(Base b) {
        return b.val();
    }
    public int run() {
        return this.consume(new Derived());
    }
}
