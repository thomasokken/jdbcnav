package jdbcnav;

import jdbcnav.model.TypeDescription;

public class ScriptGenerator_SameAsSource extends ScriptGenerator {
    public TypeDescription getTypeDescription(String dbType, Integer size,
					      Integer scale) {
	// NOTE: We don't populate the part_of_key and part_of_index
	// that is left to our caller, BasicTable.getTypeDescription().
	// Populating native_representation is optional.

	// This implementation is just for the hell of it; it will never
	// be called, since getTypeDescription() is only ever called by the
	// JDBCDatabase instance with the same internal driver name --
	// and there is no JDBCDatabase_SameAsSource, for obvious reasons.

	TypeDescription td = new TypeDescription();
	td.type = TypeDescription.UNKNOWN;

	return td;
    }

    protected String printType(TypeDescription td) {
	return td.native_representation;
    }
}
