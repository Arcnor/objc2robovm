package com.arcnor.objcclang.gen;

import com.arcnor.objcclang.meta.AppleMetaInterface;
import com.arcnor.objcclang.meta.AppleMetaMember;
import com.arcnor.objcclang.meta.AppleMetaMethod;
import com.arcnor.objcclang.meta.AppleMetaProperty;

import java.util.LinkedHashMap;
import java.util.Map;

public class RoboVMClassGen extends AbstractGen<AppleMetaInterface> {
	private static final String SELF_ARG = "__self__";
	private static final String SEL_ARG = "__cmd__";
	public static final String RETURN_TYPE_ARG = "*RETURNTYPE*";

	public RoboVMClassGen(AppleMetaInterface meta, Map<String, AppleMetaMember> memberDecls, Map<String, AppleMetaMember> protocolDecls, Map<String, AppleMetaMember> typedefs) {
		super(meta, memberDecls, protocolDecls, typedefs);
	}

	@Override
	protected void generateImports() {
		_("import java.util.*;")._nl();
		_("import org.robovm.objc.*;")._nl();
		_("import org.robovm.objc.annotation.*;")._nl();
		_("import org.robovm.objc.block.*;")._nl();
		_("import org.robovm.rt.bro.*;")._nl();
		_("import org.robovm.rt.bro.annotation.*;")._nl();
		_("import org.robovm.rt.bro.ptr.*;")._nl();
		_nl();
	}

	@Override
	protected void generateBodyDecl() {
		_("@Library(\"")._(metaMember.framework)._("\");")._nl();
		_("@NativeClass public class ")._(metaMember.name)._nl();
		if (metaMember.parent != null) {
			_("\textends ")._(objc2javatype(metaMember.parent.name))._nl();
		}
		if (!metaMember.protocols.isEmpty()) {
			_("\timplements ");
			joinNames(metaMember.protocols, true);
			_nl();
		}
		_brace();

		_("static ")._brace();
		_("ObjCRuntime.bind(")._(metaMember.name)._(".class);")._nl();
		_braceEnd()._nl();

		_("private static final ObjCClass objCClass = ObjCClass.getByType(")._(metaMember.name)._(".class);")._nl();

		_nl();

		// Create parent constructors
		AppleMetaInterface parent = metaMember.parent;
		while (parent != null && !parent.name.equals("NSObject")) {
			addParentConstructors(parent);
			parent = parent.parent;
		}

		addConstructors();

		addMethods();

		addCallbacks();

		_braceEnd()._nl();
	}

	private void addConstructors() {
		// Empty one
		_("protected ")._(metaMember.name)._("(SkipInit skipInit) { super(skipInit); }")._nl();
		_("public ")._(metaMember.name)._("() {}")._nl();

		_nl();
		for (AppleMetaMethod constructor : metaMember.constructors) {
			String selectorName = registerSelector(constructor.name);
			String javaMethodName = getJavaMethodName(constructor.name);
			LinkedHashMap<String,String> types = objc2javatypeMap(constructor.args);
			msgSend(javaMethodName, "@Pointer long", false, "", types);

			_nl();

			_("public ")._(metaMember.name)._('(');
			joinNameTypes(types);
			_(") ")._brace();

			_("super((SkipInit) null);")._nl();
			_("setHandle(objc_")._(javaMethodName)._("(this, ")._(selectorName)._(", ");
			joinNames(constructor.args, false);
			_("));")._nl();
			_braceEnd()._nl();

			_nl();
		}
	}

	private void addMethods() {
		for (AppleMetaMethod method : metaMember.methods) {
			LinkedHashMap<String, String> args = objc2javatypeMap(method.args);
			if (args == null) {
				args = new LinkedHashMap<String, String>();
			}
			args.put(RETURN_TYPE_ARG, objc2javatype(method.type));
			String generics = processGenerics(args);
			String methodType = args.remove(RETURN_TYPE_ARG);

			if (!generics.isEmpty()) {
				System.out.println();
			}

			boolean nonVoidType = !methodType.equals("void");

			String selectorName = registerSelector(method.name);

			String javaMethodName = getJavaMethodName(method.name);
			msgSend(javaMethodName, methodType, false, generics, args);
			msgSend(javaMethodName, methodType, true, generics, args);

			_nl();

			_("public ")._(generics)._(methodType)._(' ')._(javaMethodName)._('(');
			joinNameTypes(args);
			_(") ")._brace();

			_("if (customClass) { ");
			if (nonVoidType) {
				_("return ");
			}
			_("objc_")._(javaMethodName)._("Super(getSuper(), ")._(selectorName);
			if (!method.args.isEmpty()) {
				_(", ");
				joinNames(method.args, false);
			}
			_("); } else { ");
			if (nonVoidType) {
				_("return ");
			}
			_("objc_")._(javaMethodName)._("(this, ")._(selectorName);
			if (!method.args.isEmpty()) {
				_(", ");
				joinNames(method.args, false);
			}
			_("); }")._nl();

			_braceEnd()._nl();

			_nl();
		}
	}

	private void addCallbacks() {
		_("static class Callbacks ")._brace();

		for (AppleMetaMethod method : metaMember.methods) {
			addCallback(method);
		}

		_braceEnd()._nl();
	}

	private void addCallback(AppleMetaMethod method) {
		LinkedHashMap<String, String> args = objc2javatypeMap(method.args);
		if (args == null) {
			args = new LinkedHashMap<String, String>();
		}
		args.put(RETURN_TYPE_ARG, objc2javatype(method.type));
		String generics = processGenerics(args);
		String methodType = args.remove(RETURN_TYPE_ARG);

		String javaMethodName = getJavaMethodName(method.name);
		boolean nonVoidType = !methodType.equals("void");

		_("@Callback @BindSelector(\"")._(method.name)._("\") public static ")._(generics)._(methodType)._(' ')._(javaMethodName)._("(");
		_(metaMember.name)._(' ')._(SELF_ARG)._(", Selector ")._(SEL_ARG);
		if (!method.args.isEmpty()) {
			_(", ");
			joinNameTypes(args);
		}
		_(") { ");
		if (nonVoidType) {
			_("return ");
		}
		_(SELF_ARG)._(".")._(javaMethodName)._('(');
		joinNames(method.args, false);
		_("); }")._nl();
	}

	private String getJavaMethodName(String name) {
		AppleMetaProperty prop = metaMember.properties.get(name);
		if (prop != null) {
			return prop.getter;
		}
		int colonIdx = name.indexOf(':');
		if (colonIdx >= 0) {
			return name.substring(0, colonIdx);
		}
		return name;
	}

	private void addParentConstructors(AppleMetaInterface parent) {
		for (AppleMetaMethod constructor : parent.constructors) {
			LinkedHashMap<String, String> args = objc2javatypeMap(constructor.args);
			_("public ")._(metaMember.name)._('(');
			joinNameTypes(args);
			_(") ")._brace();

			_("super(");
			joinNames(constructor.args, false);
			_(");")._nl();

			_braceEnd()._nl();
		}
	}
}
