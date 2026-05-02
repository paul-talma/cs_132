// VALID: transitivity of subtyping — C <= B <= A, so C <= A.
// Assign a C to a variable of type A; pass a C where A is expected.
class Main {
    public static void main(String[] a) {
        System.out.println(new Tester().run());
    }
}

class A {
    public int val() { return 1; }
}

class B extends A {
    public int val() { return 2; }
}

class C extends B {
    public int val() { return 3; }
}

class Tester {
    A a;
    public int consume(A x) { return x.val(); }
    public int run() {
        a = new C();          // C <= A (transitive)
        return this.consume(new C()); // pass C where A expected
    }
}
