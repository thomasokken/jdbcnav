package jdbcnav.javascript;

import java.io.*;
import org.mozilla.javascript.*;

import jdbcnav.MessageBox;


public class JavaScriptGlobal extends ScriptableObject {
    private Pipe pipe;

    public JavaScriptGlobal() {
	String[] names = { "print", "println" };
	try {
	    defineFunctionProperties(names, JavaScriptGlobal.class,
				     ScriptableObject.DONTENUM);
	} catch (PropertyException e) {
	    MessageBox.show("Problem constructing JavaScriptGlobal", e);
	}
    }

    public String getClassName() {
	return "global";
    }

    public void setOut(Pipe pipe) {
	this.pipe = pipe;
    }

    public static void print(Context ctx, Scriptable thisObj, Object[] args,
			     Function funObj) {
	Pipe pipe = ((JavaScriptGlobal) thisObj).pipe;
	for (int i = 0; i < args.length; i++) {
	    if (i > 0)
		pipe.print(" ");
	    pipe.print(ctx.toString(args[i]));
	}
    }

    public static void println(Context ctx, Scriptable thisObj, Object[] args,
			     Function funObj) {
	Pipe pipe = ((JavaScriptGlobal) thisObj).pipe;
	for (int i = 0; i < args.length; i++) {
	    if (i > 0)
		pipe.print(" ");
	    pipe.print(ctx.toString(args[i]));
	}
	pipe.println();
    }

    public interface Pipe {
	void print(String s);
	void println(String s);
	void println();
    }
}
