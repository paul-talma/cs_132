package hw3;

import java.util.List;
import sparrow.Instruction;
import IR.token.Identifier;

/**
 * Return value from expression-compiling visitor methods.
 *
 * `code` holds the Sparrow instructions emitted to evaluate the expression.
 * `temp` is the index of the Sparrow temporary that holds the result value,
 * or null for void-typed sub-expressions (e.g. statement compilation).
 */
public class Result {
    List<Instruction> code;
    Identifier temp;

    public Result(List<Instruction> code, Identifier temp) {
        this.code = code;
        this.temp = temp;
    }

    public List<Instruction> getCode() {
        return code;
    }

    public Identifier getIdentifier() {
        return temp;
    }
}
