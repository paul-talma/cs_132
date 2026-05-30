package hw4;

import IR.token.Register;
import IR.token.Identifier;

class Home {
    boolean isRegister;
    Register reg;
    Identifier id;
    int stackSlot;

    public Home() {

    }

    public Home(Register reg) {
        this.reg = reg;
        this.isRegister = true;
    }

    public Home(Identifier id) {
        this.id = id;
        this.isRegister = false;
    }

    public Home(int stackSlot) {
        this.stackSlot = stackSlot;
        this.isRegister = false;
    }

    Register getReg() {
        return reg;
    }

    Identifier getId() {
        return id;
    }

    int getStackSlot() {
        return stackSlot;
    }

    boolean isRegister() {
        return isRegister;
    }
}
