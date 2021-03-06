package app.bcrt.compile;

import java.util.Optional;

public class Var extends Val {

    public String name;
    Var holder = null;

    public Var(String name) {
        this.name = name;
    }

    public Var(Var v) {
        super(v);
        this.name = v.name;
    }

    public Var(String name, Val val) {
        this(name);
        value.add(val);
    }

    public String toString() {
        return name + " - " + super.toString();
    }

    public Var get(String s) {
        Optional<Var> newvar = subelems.stream().filter(n -> n.name.equals(s)).findAny();
        Var v;
        if(newvar.isPresent()) v = newvar.get();
        else {
            v = new Var(s);
            subelems.add(v);
        }
        v.holder = this;
        return v;
    }

    protected Var clone() {
        return new Var(this);
    }
}