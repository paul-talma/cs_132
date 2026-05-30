package hw4;

import java.util.Map;

// Maps each variable in a function to its allocated home (register or stack slot).
class FunctionAllocation {
    Map<String, Home> homeOf;

    FunctionAllocation(Map<String, Home> homeOf) {
        this.homeOf = homeOf;
    }

    Home get(String var) {
        return homeOf.get(var);
    }

    void show() {
        homeOf.forEach((var, home) -> {
            String loc = home.isRegister()
                ? home.getReg().toString()
                : (home.getId() != null ? home.getId().toString() : "stack[" + home.getStackSlot() + "]");
            System.out.println("  " + var + " -> " + loc);
        });
    }
}
