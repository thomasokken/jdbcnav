package jdbcnav;

import jdbcnav.model.TypeSpec;

public class ScriptGenerator_SameAsSource extends ScriptGenerator {
    protected String printType(TypeSpec td) {
	return td.native_representation;
    }
}
