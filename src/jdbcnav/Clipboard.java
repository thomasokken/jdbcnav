package jdbcnav;

import java.util.*;

public class Clipboard {
    public static final Object EMPTY = new Object();
    private Object data = EMPTY;
    private ArrayList listeners = new ArrayList();

    public void put(Object data) {
	this.data = data;
	for (Iterator iter = listeners.iterator(); iter.hasNext();) {
	    Listener listener = (Listener) iter.next();
	    listener.clipboardUpdated(data);
	}
    }

    public Object get() {
	return data;
    }

    public void addListener(Listener listener) {
	listeners.add(listener);
    }

    public void removeListener(Listener listener) {
	listeners.remove(listener);
    }

    public interface Listener {
	void clipboardUpdated(Object data);
    }
}
