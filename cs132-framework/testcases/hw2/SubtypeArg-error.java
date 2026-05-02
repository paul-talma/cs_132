// ERROR: pass a parent (Base) where a child (Derived) is expected.
// Base is NOT <= Derived.
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
    public int consume(Derived d) {
        return d.val();
    }
    public int run() {
        return this.consume(new Base()); // TE: Base is not <= Derived
    }
}
