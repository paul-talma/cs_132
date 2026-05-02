// VALID: call a method on `this` from within a method of the same class.
// Rule 44: this : C when C != bottom. Rule 39: p.id(...) MessageSend.
class Main {
    public static void main(String[] a) {
        System.out.println(new Obj().run());
    }
}

class Obj {
    public int helper(int x) {
        return x + 1;
    }
    public int run() {
        return this.helper(41);
    }
}
