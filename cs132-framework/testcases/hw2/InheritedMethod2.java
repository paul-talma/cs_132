// VALID: three-level inheritance — GrandChild inherits getValue from GrandParent
// via Parent, which also doesn't define it.
class Main {
    public static void main(String[] a) {
        System.out.println(new GrandChild().getValue());
    }
}

class GrandParent {
    public int getValue() {
        return 7;
    }
}

class Parent extends GrandParent {
}

class GrandChild extends Parent {
}
