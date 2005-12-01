package jdbcnav.model;

/**
 * This class implements a database-neutral INTERVAL DAY TO SECOND.
 */
public class IntervalDS {
    public IntervalDS(String s, TypeSpec spec) {
    }

    public String toString(TypeSpec spec) {
	return "N.Y.I.";
    }

    /**
     * The width of the DAY part
     */
    public short dayWidth;

    /**
     * The precision of the SECOND part (0 means integral seconds,
     * 3 means milliseconds, etc.; the maximum allowed is 9
     * (nanoseconds)).
     */
    public short secondPrecision;

    /**
     * The DAY field
     */
    public long days;

    /**
     * The SECOND field (in nanoseconds)
     */
    public long nanos;
}
