package app;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class App {
    static ArrayList<Var> vars = new ArrayList<>();
    public static void main(String[] args) {
		String[] lines = null;
		try {
			lines = readFile("src/app/test.bcrt").split(";");
		} catch (Exception e) {
			e.printStackTrace();
        }
        for(String line : lines) execute(line);
        vars.forEach(v -> v.print());
    }
    static void execute(String s){
        s = s.trim()+";";
        System.out.println(s);
        int mode = 0;
        String temp = "";
        Val tempval = null;
        int vallevel = 0;
        for(char c : s.toCharArray()) switch(mode){
            case 0://check what to do with var
                if(c == '{') vallevel++;
                if(c == '}') vallevel--;
                if(vallevel == 0 && c == '@'){
                    tempval = interpret(temp);
                    temp = "";
                    mode = 1;//set var
                    break;
                }else if(c == ';'){
                    int size = vars.size();
                    tempval = interpret(temp);
                    if(tempval.vals == null); //do nothing
                    else if(tempval.vals.length == 0); //do nothing
                    else if(tempval.vals[0] instanceof Bit) execute(tempval);
                    else for(Val v : tempval.vals) if(v.vals[0] instanceof Bit) execute(v);
                    if(size != vars.size()){
                        vars.remove(0);
                        vars.remove(0);
                    }
                    return;
                }
                temp += c;
            break;
            case 1://assign to value
                if(c == ';'){
                    tempval.set(interpret(temp));
                    if(tempval instanceof Var) setadd((Var) tempval);
                    return;
                }
                temp += c;
            break;
        }
    }
    static void execute(Val v){
        execute(StringTool.toString(v));
    }
    static Val interpret(String s){
        s += ";";
        Val ret = new Val();
        int mode = 0;
        int vallevel = 0;
        int indexlevel = 0;
        String temp = "";
        String op = "";
        for(char c : s.toCharArray()){
            if(c == '{') vallevel++;
            else if(c == '}') vallevel--;
            else if(c == '[') indexlevel++;
            else if(c == ']') indexlevel--;
            switch(mode){
                case 0://start
                    if(c == '{'){
                        mode = 1;
                        break;
                    }else if(c == '\''){
                        mode = 2;
                        break;
                    }
                break;
                case 1://value or array
                    if(vallevel == 0){
                        if(StringTool.isList(temp)){
                            ArrayList<Val> newval = new ArrayList<Val>();
                            for(String str : StringTool.splitList(temp))
                                newval.add(interpret(str));
                            ret = new Val();
                            ret.vals = new Val[newval.size()];
                            newval.toArray(ret.vals);
                        }else{
                            ret = new Val(temp);
                        }
                        temp = "";
                        mode = 3;
                    }
                    temp += c;
                break;
                case 2: //Variable
                    if(c == '`'){
                        ret = get(temp);
                        if(ret == null) ret = new Var(temp);
                        temp = "";
                        mode = 3;
                        break;
                    }
                    temp += c;
                break;
                case 3://read what to do with value or Variable
                    if(c == '['){
                        temp = "";
                        mode = 4;
                    }else{
                        temp = ""+c;
                        mode = 5;
                    }
                break;
                case 4: //end index acess
                if(indexlevel == 0){
                    if(temp.matches("\\d+")) ret = ret.vals[Integer.parseInt(temp)];
                    else ret = ret.vals[interpret(temp).toInt()];
                    temp = "";
                    mode = 3;
                    break;
                }
                temp += c;
                break;
                case 5:
                if(c == '{' || c == '\''){
                    mode = 6;
                    op = temp;
                    temp = "";
                }
                temp += c;
                break;
                case 6:
                if(c == ';'){
                    Var a = new Var("a");
                    Var b = new Var("b");
                    a.set(ret);
                    b.set(interpret(temp));
                    vars.add(0, a);
                    vars.add(0, b);
                    ret = get(op);
                }
                temp += c;
                break;
            }
        }
        return ret;
    }
    static Val get(String name){
        if(contains(new Var(name)))
            return vars.get(indexOf(new Var(name)));
        return null;
    } 
    static void setadd(Var v){
        if(contains(v)) vars.set(indexOf(v), v);
        else vars.add(v);
    }
    static boolean contains(Var v){
        for (Var var : vars) 
            if(var.name.equals(v.name)) return true;
        return false;
    }
    static int indexOf(Var v){
        for (int i = 0; i < vars.size(); i++)
            if(vars.get(i).name.equals(v.name)) return i;
        return -1;
    }
	private static String readFile(String file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		StringBuilder stringBuilder = new StringBuilder();
		String ls = System.getProperty("line.separator");
		try {
			while((line = reader.readLine()) != null) {
				stringBuilder.append(line);
				stringBuilder.append(ls);
			}
			stringBuilder.delete(stringBuilder.lastIndexOf(ls), stringBuilder.length()) ;
			return stringBuilder.toString();
		} finally {
			reader.close();
		}
	}
}