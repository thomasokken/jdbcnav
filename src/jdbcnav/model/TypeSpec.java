package jdbcnav.model;

/**
 * This class is used as an intermediate representation of a column type.
 * It is used in preference to SQL in order to avoid any ambiguities; this
 * class captures all the information needed to accurately replicate a
 * data type without information loss. The ScriptGenerator class, when
 * generating CREATE TABLE statements, calls Table.getTypeSpec() to get an
 * accurate description of column types; it then calls
 * ScriptGenerator.printType() to find the appropriate DB-specific type.
 */
public abstract class TypeSpec {
    // For the FIXED_x_y and FLOAT_x_y types, 'x' is the representation of the
    // number or mantissa (and 'size' is the number of bits or digits); 'y' is
    // the number that is raised to the scale (or exponent) to scale the
    // number. The scale (FIXED) or exponent range (FLOAT) is given in 'scale'
    // or 'min_exp'..'max_exp', respectively.

    public static final int CLASS = -1;
    public static final int UNKNOWN = 0;
    public static final int FIXED = 1;
    public static final int FLOAT = 2;
    public static final int CHAR = 3;
    public static final int VARCHAR = 4;
    public static final int LONGVARCHAR = 5;
    public static final int NCHAR = 6;
    public static final int VARNCHAR = 7;
    public static final int LONGVARNCHAR = 8;
    public static final int RAW = 9;
    public static final int VARRAW = 10;
    public static final int LONGVARRAW = 11;
    public static final int DATE = 12;
    public static final int TIME = 13;
    public static final int TIME_TZ = 14;
    public static final int TIMESTAMP = 15;
    public static final int TIMESTAMP_TZ = 16;
    public static final int INTERVAL_YM = 17;
    public static final int INTERVAL_DS = 18;

    public int type;
    public int size; // chars, digits, or bits; for FLOAT, mantissa size
    public boolean size_in_bits; // for FIXED & FLOAT
    public int scale; // for FIXED
    public boolean scale_in_bits;
    public int min_exp, max_exp; // for FLOAT
    public boolean exp_of_2;

    public boolean part_of_key;
    public boolean part_of_index;
    public String native_representation;

    public String jdbcDbType;
    public Integer jdbcSize;
    public Integer jdbcScale;
    public int jdbcSqlType;
    public String jdbcJavaType;
    public Class jdbcJavaClass;

    public abstract String objectToString(Object o);
    public abstract Object stringToObject(String s);
}