package com.arcnor.objcclang.gen;

import com.arcnor.objcclang.meta.GenericMetaMember;
import com.arcnor.objcclang.meta.GenericMetaMethod;
import com.arcnor.objcclang.meta.hawtjni.HawtMetaClass;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class HawtJNIFunctionsGen extends HawtJNIAbstractGen<HawtMetaClass> {
	public HawtJNIFunctionsGen(String packagePrefix, HawtMetaClass metaMember, Map<String, ? extends GenericMetaMember> memberDecls, Map<String, ? extends GenericMetaMember> typedefs, Map<String, HawtType> customTypes) {
		super(packagePrefix, metaMember, memberDecls, typedefs, customTypes);
	}

	@Override
	protected void generateImports() {
		_("import org.fusesource.hawtjni.runtime.*;")._nl();
		_nl();
	}

	@Override
	protected void generateBodyDecl() {
		_("@JniClass")._nl();
		_("public class ")._(metaMember.name)._(' ')._brace();
		_("private static final Library LIBRARY = new Library(\"")._(metaMember.library)._("\", ")._(metaMember.name)._(".class);")._nl();
		_nl();
		_("static ")._brace();
		_("LIBRARY.load();")._nl();
		_braceEnd()._nl();
		_nl();

		generateMethods();

		_braceEnd()._nl();
	}

	private void generateMethods() {
		for (GenericMetaMethod function : metaMember.functions) {
			HawtType javaType = clang2javatype(function.type);
			LinkedHashMap<String, HawtType> types = clang2javatypeMap(function.args);

			_("@JniMethod(");
			if (javaType != null && javaType.isPtr) {
				_("cast=\"")._(javaType.origType)._("\", flag={ MethodFlag.POINTER_RETURN }");
			}
			_(")")._nl();
			_("public static native ")._(javaType == null ? "UNKNOWN" : javaType.javaType)._(' ')._(function.name)._('(');
			joinNameTypes(types);
			_(");")._nl();
		}
	}

	@Override
	protected void joinNameTypes(LinkedHashMap<String, HawtType> args) {
		if (args == null || args.isEmpty()) {
			return;
		}
		boolean first = true;
		final Set<String> argNames = new HashSet<String>();
		int idx = 1;
		for (Map.Entry<String, HawtType> arg : args.entrySet()) {
			if (first) {
				first = false;
			} else {
				_(JOINER_NAME);
			}

			String name = arg.getKey();
			if (argNames.contains(name)) {
				name += idx++;
			}
			argNames.add(name);
			HawtType value = arg.getValue();
			if (value  == null) {
				_("UNKNOWN");
			} else {
				if (value.isPtr) {
					_("@JniArg(cast=\"")._(value.origType)._("\", flag={ ArgFlag.POINTER_ARG }) ");
				} else if (value.needsCast) {
					_("@JniArg(cast=\"")._(value.origType)._("\") ");
				}
				_(value.javaType);
			}
			_(' ')._(name);
		}
	}
}
