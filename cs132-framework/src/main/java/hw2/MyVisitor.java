package hw2;

public class MyVisitor extends DepthFirstVisitor {
    // TODO: field to track the current boolean value being computed
    boolean val;

    // TODO: visit methods — which ones?
    // Hint: you need one per LEAF class you care about,
    // plus one for NotExpression (which has a subexpression).
    // Do NOT override visit(Expression) — DepthFirstVisitor
    // already handles NodeChoice dispatch for you.
    public void visit(TrueLiteral e) {
        val = true;
    }

    public void visit(FalseLiteral e) {
        val = false;
    }

    public void visit(NotExpression e) {
        System.out.println("Visiting NotExpression");
        e.subExpression.accept(this);
        val = !val;
    }

}
