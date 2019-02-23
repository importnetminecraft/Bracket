import app.bcrt.compile.*;

public class Print extends Var {
    public Print(){
        super("print");
    }

    public Val execute(Val context){
        System.out.println(new Val(App.get("b")));
        App.setVar(new Var("b"));
        return App.get("b");
    }

    protected Var clone(){
        return new Print();
    }
}