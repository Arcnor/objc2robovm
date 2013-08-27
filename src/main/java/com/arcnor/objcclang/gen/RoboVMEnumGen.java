package com.arcnor.objcclang.gen;

import com.arcnor.objcclang.meta.GenericMetaEnum;
import com.arcnor.objcclang.meta.GenericMetaField;
import com.arcnor.objcclang.meta.GenericMetaMember;

import java.util.List;
import java.util.Map;

public class RoboVMEnumGen extends AbstractGen<GenericMetaEnum> {
	public RoboVMEnumGen(GenericMetaEnum meta, Map<String, GenericMetaMember> memberDecls, Map<String, GenericMetaMember> protocolDecls, Map<String, GenericMetaMember> typedefs) {
		super(meta, memberDecls, protocolDecls, typedefs);
	}

	@Override
	protected void generateImports() {
		_("import org.robovm.rt.bro.ValuedEnum;")._nl();
		_nl();
	}

	@Override
	protected void generateBodyDecl() {
		_("public enum ")._(metaMember.name)._(" implements ValuedEnum ")._brace();
		List<GenericMetaField> fields = metaMember.fields;
		int size = fields.size();
		for (int j = 0; j < size; j++) {
			GenericMetaField field = fields.get(j);
			if (field.value == null) {
				System.err.println("WARNING: Value not present for field " + field.name + " in class " + metaMember.name);
				return;
			}
			_(field.name)._('(')._(field.value.toString())._(')');
			_(j < (size - 1) ? ',' : ';')._nl();
		}
		_nl();
		_("private final n;")._nl();
		_nl();
		_("private ")._(metaMember.name)._("(int n) { this.n = n; }")._nl();
		_("public int value() { return n; }")._nl();

		_("public static ")._(metaMember.name)._(" fromValue(int n) ")._brace();

		_("for (")._(metaMember.name)._(" v : values()) ")._brace();
		_("if (n == v.value()) ")._brace();
			_("return v;")._nl();
		_braceEnd()._nl();

		_braceEnd()._nl();

		_("throw new IllegalArgumentException(\"Unknown ")._(metaMember.name)._(" value: \" + n);")._nl();

		_braceEnd()._nl();

		_braceEnd()._nl();
	}
}
