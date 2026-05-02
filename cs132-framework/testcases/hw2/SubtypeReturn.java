// VALID: method declared to return Base but actually returns Derived (subtype).
// Also tests calling a method on the result of another call via bracket expression.
// Tests rule 25: return expr type must be <= declared return type.
class Main {
    public static void main(String[] a) {
        System.out.println((new Tester().make()).val());
    }
}

class Base {
    public int val() { return 10; }
}

class Derived extends Base {
    public int val() { return 20; }
}

class Tester {
    public Base make() {
        return new Derived(); // Derived <= Base, valid
    }
}
