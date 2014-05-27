package com.arcnor.objcclang.gen;

import com.arcnor.objcclang.meta.CPPMetaClass;
import com.arcnor.objcclang.meta.GenericMetaField;
import com.arcnor.objcclang.meta.GenericMetaMember;
import com.arcnor.objcclang.meta.GenericMetaMethod;

import java.util.Map;

public class SWIGGen extends AbstractGen<CPPMetaClass, String> {
	public SWIGGen(final CPPMetaClass metaMember, final Map<String, ? extends GenericMetaMember> memberDecls, final Map<String, ? extends GenericMetaMember> typedefs) {
		super(metaMember, memberDecls, typedefs);
	}

	@Override
	void generateOutput() {
		_("class ")._(metaMember.name)._(' ');
		if (metaMember.parent != null) {
			_(" : ")._(metaMember.parent)._(' ');
		}
		_brace();
		_("public:")._indent()._nl();
		for (GenericMetaMethod method : metaMember.methods) {
			_(method.type)._(' ')._(method.name)._('(');
			generateArgs(method);
			_(");")._nl();
		}
		for (GenericMetaField field : metaMember.fields) {
			_(field.type)._(' ')._(field.name)._(';')._nl();
		}
		_indentEnd();
		_braceEnd();
	}

	private void generateArgs(final GenericMetaMethod method) {
		boolean first = true;
		for (GenericMetaField arg : method.args) {
			if (first) {
				first = false;
			} else {
				_(", ");
			}
			_(arg.type)._(' ')._(arg.name);
		}
	}

	protected void generateBodyDecl() {

	}

	@Override
	protected String clang2javatypeCustom(final String type, final String refType) {
		return null;
	}
}
