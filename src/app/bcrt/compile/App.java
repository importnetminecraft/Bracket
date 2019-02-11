package app.bcrt.compile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import app.bcrt.methods.Execute;

public class App {

    public static final ArrayList<Var> vars = new ArrayList<>();

    public static void main(String[] args) {
        vars.add(new Execute());
        executeFile("src/app/bcrt/tests/test.bcrt");
        vars.forEach(System.out::println);
    }

    public static void executeFile(String path){
        if(path.split("\\.").length>1 && path.split("\\.")[1].equals("bcrt")){
            String[] lines = null;
            try {
                lines = StringTool.commentFilter(readFile(path)).split(";");
            } catch (IOException e) {
                e.printStackTrace();
            }
            for(String line : lines) execute(line, null);
        }else{
            loadMeth(path);
        }
    }

    public static void loadMeth(String path){
        File newMeth = new File(path);
        if (newMeth.getParentFile().exists() || newMeth.getParentFile().mkdirs()) {
            try {
                // Compilation Requirements
                DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
                JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

                Iterable<? extends JavaFileObject> compilationUnit = fileManager
                        .getJavaFileObjectsFromFiles(Arrays.asList(newMeth));
                JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null,
                        compilationUnit);
                if (task.call()) {
                    // Load and execute
                    URLClassLoader classLoader = new URLClassLoader(new URL[] { new File("./").toURI().toURL() });
                    Object newmethod = classLoader.loadClass(path.replaceAll("/", ".").replace("src.", "").replace(".java", "")).getConstructors()[0].newInstance();

                    classLoader.close();
                    if (newmethod instanceof Var) vars.add((Var) newmethod);
                }
                fileManager.close();
            } catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException | 
                    IllegalArgumentException | InvocationTargetException | SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    static Val execute(String s, Val context){
        s = s.trim()+";";
        // System.out.println(s);
        int mode = 0;
        String temp = "";
        Val tempval = null;
        int vallevel = 0;
        for(char c : s.toCharArray()) {
            switch(mode){
                case 0://check what to do with var
                    if(c == '{') vallevel++;
                    if(c == '}') vallevel--;
                    if(vallevel == 0) {
                        if(c == '@'){
                            tempval = interpret(temp, context);
                            temp = "";
                            mode = 1;//set var
                            break;
                        }else if(c == ';'){
                            tempval = interpret(temp, context);
                            if(tempval != null){
                                Val ret = tempval.execute(context);
                                if(ret != null) return ret;
                            }
                        }else if(c == '~'){
                            temp = "";
                            mode = 2;
                            break;
                        }
                    }
                break;
                case 1://assign to value
                    if(c == ';'){
                        tempval.set(interpret(temp, context));
                        if(tempval instanceof Var && ((Var) tempval).holder == null) setadd((Var) tempval);
                        return null;
                    }
                break;
                case 2: if(c == ';') return new Val(interpret(temp, context)); //return value
                break;
            }
            temp += c;
        }
        return null;
    }

    public static Val interpret(String s, Val context){
        s += ";";
        Val ret = null;
        int mode = 0;
        int vallevel = 0,
          indexlevel = 0,
          parenlevel = 0;
        String temp = "";
        String op = "";
        for(char c : s.toCharArray()){
            if(c == '{') vallevel++;
            else if(c == '}') vallevel--;
            else if(c == '[') indexlevel++;
            else if(c == ']') indexlevel--;
            else if(c == '(') parenlevel++;
            else if(c == ')') parenlevel--;
            switch(mode){
                case 0://start
                    if(c == '{') mode = 1;
                    else if(c == '\'') mode = 2;
                    else if(c == '(')  mode = 7;
                    else if(c == ':') {
                        mode = 3;
                        ret = context;
                    }
                break;
                case 1://value or array
                    if(vallevel == 0){
                        if(StringTool.isList(temp)){
                            ret = new Val();
                            ret.vals = new ArrayList<>(StringTool.splitList(temp).stream().map(n -> interpret(n, context)).collect(Collectors.toList()));
                        }else ret = new Val(temp);
                        temp = "";
                        mode = 3;
                    }
                    temp += c;
                break;
                case 2: //Variable
                    if(c == '\''){
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
                    }else if(c == '.'){
                        temp = "";
                        mode = 8;
                    }else if(c == '_'){
                        int sum = 0;
                        for(int i = 0; i < ret.vals.size(); i++){
                            sum += ret.vals.get(i).vals.size();
                        }
                        ArrayList<Val> flatvals = new ArrayList<Val>(sum);
                        for(int i = 0; i < ret.vals.size(); i++){
                            for(int j = 0; j < ret.vals.get(i).vals.size(); j++){
                                flatvals.add(ret.vals.get(i).vals.get(j));
                            }
                        }
                        ret.vals = flatvals;
                    }else{
                        temp = ""+c;
                        mode = 5;
                    }
                break;
                case 4: //end index acess
                    if(indexlevel == 0){
                        if(temp.matches("\\d+")) ret = ret.get(Integer.parseInt(temp));
                        else if(temp.contains(":")){
                            Val newret = new Val();
                            for (int i = 0; i < ret.vals.size(); i++)
                                if(interpret(temp, new Val(i)).toInt() == 1) newret.vals.add(ret.vals.get(i).clone());
                            ret = newret;
                        }else ret = ret.get(interpret(temp, context).toInt());
                        temp = "";
                        mode = 3;
                        break;
                    }
                    temp += c;
                break;
                case 5://read operation
                    if(c == '{' || c == '\'' || c == '(' || c == ':'){
                        mode = 6;
                        op = temp;
                        temp = "";
                    }
                    temp += c;
                break;
                case 6://read second input to operation
                    if(c == ';'){
                        Val tempb = interpret(temp, context);
                        Var a = new Var("a");
                        Var b = new Var("b");
                        if(ret instanceof Var){
                            a = (Var) ret.clone();
                            a.name = "a";
                        }else 
                            a.set(ret);
                        if(tempb instanceof Var){
                            b = (Var) tempb.clone();
                            b.name = "b";
                        }else 
                            b.set(tempb);
                        Val v = get(op);
                        vars.add(0, a);
                        vars.add(0, b);
                        ret = v.execute(v);
                        vars.remove(0);
                        vars.remove(0);
                    }
                    temp += c;
                break;
                case 7://end paren
                    if(parenlevel == 0){
                        ret = interpret(temp, context);
                        temp = "";
                        mode = 3;
                    }
                    temp += c;
                break;
                case 8://subelem acess
                    if(c == ';') return ((Var) ret).get(interpret(temp, context).toString());
                    temp += c;
                break;
            }
        }
        return ret;
    }
    
    static Val get(String name){
        return vars.stream().filter(n -> n.name.equals(name)).findAny().orElse(null);
    }

    static void setadd(Var v){
        int index = indexOf(v.name);
        if(index == -1) vars.add(v);
        else vars.set(index, v);
    }

    static int indexOf(String v){
        return vars.indexOf(get(v));
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
			stringBuilder.delete(stringBuilder.lastIndexOf(ls), stringBuilder.length());
			return stringBuilder.toString();
		} finally {
			reader.close();
		}
	}
}