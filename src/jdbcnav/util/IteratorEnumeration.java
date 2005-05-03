package jdbcnav.util;

import java.util.*;


public class IteratorEnumeration implements Enumeration {
    private Iterator iter;
    public IteratorEnumeration(Iterator iter) {
	this.iter = iter;
    }
    public boolean hasMoreElements() {
	return iter.hasNext();
    }
    public Object nextElement() {
	return iter.next();
    }
}
