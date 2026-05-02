// ERROR: assign a supertype (Base) to a subtype-declared field (Derived d).
// Base is NOT a subtype of Derived.
class Main {
    public static void main(String[] a) {
        System.out.println(new Tester().run());
    }
}

class Base {
    public int val() { return 1; }
}

class Derived extends Base {
    public int val() { return 2; }
}

class Tester {
    Derived d;
    public int run() {
        d = new Base(); // TE: Base is not <= Derived
        return 0;
    }
}
