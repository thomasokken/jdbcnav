package jdbcnav.util;

public class NavigatorException extends Exception {
    private Throwable rootCause;

    public NavigatorException(Throwable rootCause) {
	this.rootCause = rootCause;
    }

    public NavigatorException(String message, Throwable rootCause) {
	super(message);
	this.rootCause = rootCause;
    }

    public NavigatorException(String message) {
	super(message);
	this.rootCause = null;
    }

    public Throwable getRootCause() {
	return rootCause;
    }
}
