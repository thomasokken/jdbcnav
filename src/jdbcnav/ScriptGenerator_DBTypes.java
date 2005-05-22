package jdbcnav;

import jdbcnav.model.Table;

public class ScriptGenerator_DBTypes extends ScriptGenerator {
    protected String printType(Table table, int column) {
	String name = table.getDbTypes()[column];
	Integer size = table.getColumnSizes()[column];
	Integer scale = table.getColumnScales()[column];

	if (size == null)
	    return name;
	else if (scale == null)
	    return name + "(" + size + ")";
	else
	    return name + "(" + size + ", " + scale + ")";
    }
}
