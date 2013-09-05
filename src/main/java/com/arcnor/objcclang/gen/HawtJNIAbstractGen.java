package com.arcnor.objcclang.gen;

import com.arcnor.objcclang.meta.GenericMetaMember;

import java.util.HashMap;
import java.util.Map;

public abstract class HawtJNIAbstractGen<T extends GenericMetaMember> extends AbstractJavaGen<T, HawtJNIAbstractGen.HawtType> {
	private final Map<String, HawtType> customTypes;

	protected HawtJNIAbstractGen(String packagePrefix, T metaMember, Map<String, ? extends GenericMetaMember> memberDecls, Map<String, ? extends GenericMetaMember> typedefs, Map<String, HawtType> customTypes) {
		super(packagePrefix, metaMember, memberDecls, typedefs);
		this.customTypes = customTypes;
	}

	private static final Map<String, HawtType> commonTypes = new HashMap<String, HawtType>();
	private static final Map<String, HawtType> commonRefTypes = new HashMap<String, HawtType>();

	static {
		// Basic types
		commonTypes.put(HawtType.H_VOID.origType, HawtType.H_VOID);
		commonTypes.put(HawtType.H_BYTE.origType, HawtType.H_BYTE);
		commonTypes.put(HawtType.H_SHORT.origType, HawtType.H_SHORT);
		commonTypes.put(HawtType.H_CHAR.origType, HawtType.H_CHAR);
		commonTypes.put(HawtType.H_INT.origType, HawtType.H_INT);
		commonTypes.put(HawtType.H_LONG.origType, HawtType.H_LONG);
//		commonTypes.put(HawtType.H_BOOLEAN.origType, HawtType.H_BOOLEAN);
		commonTypes.put(HawtType.H_FLOAT.origType, HawtType.H_FLOAT);
		commonTypes.put(HawtType.H_DOUBLE.origType, HawtType.H_DOUBLE);

		// Pointers
		commonTypes.put(HawtType.H_PTR_VOID.origType, HawtType.H_PTR_VOID);
		commonTypes.put(HawtType.H_STRING.origType, HawtType.H_STRING);
		commonTypes.put(HawtType.H_ARR_SHORT.origType, HawtType.H_ARR_SHORT);
		commonTypes.put(HawtType.H_ARR_CHAR.origType, HawtType.H_ARR_CHAR);
		commonTypes.put(HawtType.H_ARR_INT.origType, HawtType.H_ARR_INT);
		commonTypes.put(HawtType.H_ARR_LONG.origType, HawtType.H_ARR_LONG);
		commonTypes.put(HawtType.H_ARR_FLOAT.origType, HawtType.H_ARR_FLOAT);
		commonTypes.put(HawtType.H_ARR_DOUBLE.origType, HawtType.H_ARR_DOUBLE);

		// Other
		commonRefTypes.put("size_t", new HawtType("long", "size_t", false, true));
		commonRefTypes.put("size_t *", new HawtType("long", "size_t *", true, true));
	}

	@Override
	protected HawtType clang2javatypeCustom(String type, String refType) {
		if (type.startsWith("unsigned")) {
			type = type.substring(9);
			return clang2javatypeCustom(type, refType);
		}
		if (type.startsWith("const")) {
			type = type.substring(6);
			return clang2javatypeCustom(type, refType);
		}
		if (customTypes.containsKey(type)) {
			return customTypes.get(type);
		}
		if (commonRefTypes.containsKey(refType)) {
			return commonRefTypes.get(refType);
		}
		if (commonTypes.containsKey(type)) {
			return commonTypes.get(type);
		}
		// FIXME: Do this properly
		if (type.equals("void **")) {
			return HawtType.H_PTR_VOID;
		}
		// FIXME: Do this properly
		if (type.equals("__va_list_tag *")) {
			return HawtType.H_PTR_VOID;
		}
		// FIXME: Do this properly
		if (type.equals("FILE *")) {
			return HawtType.H_PTR_VOID;
		}

		boolean isPtr = false;
		if (type.endsWith(" *")) {
			type = type.substring(0, type.length() - 2);
			isPtr = true;
		}
		// FIXME: Do this properly
		if (type.endsWith(" **")) {
			type = type.substring(0, type.length() - 3);
			isPtr = true;
		}
		GenericMetaMember genericMetaMember = addMemberUsage(type, refType);

		return null;
	}

	public static class HawtType {
		public static final HawtType H_VOID = new HawtType("void", "void", false);
		public static final HawtType H_BYTE = new HawtType("byte", "char", false);
		public static final HawtType H_SHORT = new HawtType("short", "short", false);
		public static final HawtType H_CHAR = new HawtType("char", "wchar_t", false);
		public static final HawtType H_INT = new HawtType("int", "int", false);
		public static final HawtType H_LONG = new HawtType("long", "long long", false);
		public static final HawtType H_BOOLEAN = new HawtType("boolean", "??", false);
		public static final HawtType H_FLOAT = new HawtType("float", "float", false);
		public static final HawtType H_DOUBLE = new HawtType("double", "double", false);

		public static final HawtType H_PTR_VOID = new HawtType("long", "void *", true);
		public static final HawtType H_STRING = new HawtType("String", "char *", false);
		public static final HawtType H_ARR_CHAR = new HawtType("char[]", "wchar_t *", false);
		public static final HawtType H_ARR_SHORT = new HawtType("short[]", "short *", false);
		public static final HawtType H_ARR_INT = new HawtType("int[]", "int *", false);
		public static final HawtType H_ARR_LONG = new HawtType("long[]", "long long *", false);
		public static final HawtType H_ARR_FLOAT = new HawtType("float[]", "float *", false);
		public static final HawtType H_ARR_DOUBLE = new HawtType("double[]", "double *", false);

		public final String javaType;
		public final String origType;
		public final boolean isPtr;
		public boolean needsCast;

		public HawtType(String javaType, String origType, boolean ptr) {
			this.javaType = javaType;
			this.origType = origType;
			this.isPtr = ptr;
		}

		public HawtType(String javaType, String origType, boolean ptr, boolean needsCast) {
			this.javaType = javaType;
			this.origType = origType;
			this.isPtr = ptr;
			this.needsCast = needsCast;
		}

		public HawtType cloneWithOrigType(String newOrigType) {
			return new HawtType(javaType, newOrigType, isPtr);
		}

		@Override
		public String toString() {
			return javaType;
		}
	}
}
