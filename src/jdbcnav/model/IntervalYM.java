package jdbcnav.model;

/**
 * This class implements a database-neutral INTERVAL YEAR TO MONTH.
 */
public class IntervalYM {
    public IntervalYM(String s, TypeSpec spec) {
    }

    public String toString(TypeSpec spec) {
	return "N.Y.I.";
    }

    /**
     * The width of the YEAR part
     */
    public short yearWidth;

    /**
     * The interval, in months
     */
    public long months;
}
