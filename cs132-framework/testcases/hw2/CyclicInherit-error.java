// ERROR: cyclic inheritance — A extends B, B extends A.
// Rule 21: acyclic(linkset(d1) U ... U linkset(dn)) must hold.
class Main {
    public static void main(String[] a) {
        System.out.println(0);
    }
}

class A extends B {
    public int foo() { return 1; }
}

class B extends A {
    public int bar() { return 2; }
}
