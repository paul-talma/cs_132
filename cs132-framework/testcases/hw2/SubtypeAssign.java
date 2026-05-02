// VALID: assign a subtype (Derived) to a supertype-declared field (Base b).
// Tests rule 27: t2 <= t1 allows assigning expression of type t2 to id of type t1.
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
    Base b;
    public int run() {
        b = new Derived();
        return b.val();
    }
}
