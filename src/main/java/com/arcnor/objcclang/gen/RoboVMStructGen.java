package com.arcnor.objcclang.gen;

import com.arcnor.objcclang.meta.AppleMetaField;
import com.arcnor.objcclang.meta.AppleMetaMember;
import com.arcnor.objcclang.meta.AppleMetaRecord;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RoboVMStructGen extends AbstractGen<AppleMetaRecord> {
	public RoboVMStructGen(AppleMetaRecord meta, Map<String, AppleMetaMember> memberDecls, Map<String, AppleMetaMember> protocolDecls, Map<String, AppleMetaMember> typedefs) {
		super(meta, memberDecls, protocolDecls, typedefs);
	}

	@Override
	protected void generateImports() {
		_("import org.robovm.rt.bro.Struct")._nl();
		_("import org.robovm.rt.bro.annotation.StructMember")._nl();
		_nl();
	}

	@Override
	protected void generateBodyDecl() {
		final List<AppleMetaField> fields = metaMember.fields;
		LinkedHashMap<String,String> types = objc2javatypeMap(fields);

		_("public final class ")._(metaMember.name)._(" extends Struct<")._(metaMember.name)._("> ")._brace();

		_("public static final ")._(metaMember.name)._(" Zero = new ")._(metaMember.name)._("();")._nl();
		_nl();
		_("public ")._(metaMember.name)._("() {}")._nl();

		_("public ")._(metaMember.name)._('(');
		joinNameTypes(types);
		_(") ")._brace();

		for (AppleMetaField field : fields) {
			_(field.name)._('(')._(field.name)._(");")._nl();
		}
		_braceEnd()._nl();

		_nl();

		int idx = 0;
		for (Map.Entry<String, String> field : types.entrySet()) {
			String name = field.getKey();
			String type = field.getValue();
			_("@StructMember(")._(idx)._(')')._nl();
			_("public native ")._(type)._(' ')._(name)._("();")._nl();
			_("@StructMember(")._(idx)._(')')._nl();
			_("public native ")._(metaMember.name)._(' ')._(name)._('(')._(type)._(' ')._(name)._(");")._nl();
			idx++;
		}

		_braceEnd()._nl();
	}
}
