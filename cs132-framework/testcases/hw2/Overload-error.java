// ERROR: child defines method with same name as parent but different signature.
// noOverloading (rule 20) requires that if parent has method id^M, child must
// use the identical signature.
class Main {
    public static void main(String[] a) {
        System.out.println(new Child().run());
    }
}

class Parent {
    public int val() { return 1; }
}

class Child extends Parent {
    public int val(int x) { return x; }
}
