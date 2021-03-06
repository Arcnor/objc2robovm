package com.arcnor.objcclang.parser;

import com.arcnor.objcclang.meta.GenericMetaMember;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public abstract class CLangHandler implements CLangParser {
	protected String library;
	protected boolean isFramework;

	protected final Map<Long, GenericMetaMember> decls = new HashMap<Long, GenericMetaMember>();

	protected GenericMetaMember lastMetaMember;

	protected enum State implements NamedEnum {
		START(""), NULL("<<<NULL>>>"), VARARG("..."), TRANSLATION_UNIT_DECL, TYPEDEF_DECL, RECORD_DECL, VAR_DECL, ENUM_DECL,
		FIELD_DECL, FUNCTION_DECL, PARM_VAR_DECL, COMPOUND_STMT, RETURN_STMT, IMPLICIT_CAST_EXPR, PAREN_EXPR,
		BINARY_OPERATOR, DECL_REF_EXPR, INTEGER_LITERAL, CALL_EXPR, NO_THROW_ATTR, CONST_ATTR,
		ARRAY_SUBSCRIPT_EXPR, MEMBER_EXPR, UNARY_EXPR_OR_TYPE_TRAIT_EXPR, VISIBILITY_ATTR,

		C_F_CONSUMED_ATTR, N_S_RETURNS_RETAINED_ATTR, N_S_RETURNS_NOT_RETAINED_ATTR, FORMAT_ATTR,

		ENUM_CONSTANT_DECL, UNARY_OPERATOR, MAX_FIELD_ALIGNMENT_ATTR, DECL_STMT, ALWAYS_INLINE_ATTR, CONDITIONAL_OPERATOR,
		C_STYLE_CAST_EXPR, UNAVAILABLE_ATTR, DEPRECATED_ATTR,

		OBJ_C_INTERFACE_DECL, OBJ_C_INTERFACE,
		OBJ_C_PROTOCOL_DECL, OBJ_C_PROTOCOL,
		OBJ_C_METHOD_DECL, OBJ_C_IVAR_DECL,
		OBJ_C_CATEGORY_DECL, C_F_RETURNS_RETAINED_ATTR, OBJ_C_MESSAGE_EXPR, OBJ_C_PROPERTY_DECL,
		SUPER("super"), GETTER("getter"), SETTER("setter"),

		IF_STMT, FLOATING_LITERAL, RETURNS_TWICE_ATTR, ASM_LABEL_ATTR, CHARACTER_LITERAL,
		SWITCH_STMT, CASE_STMT, DEFAULT_STMT, BREAK_STMT, COMPOUND_ASSIGN_OPERATOR,
		FORMAT_ARG_ATTR, TRANSPARENT_UNION_ATTR, NON_NULL_ATTR, WARN_UNUSED_RESULT_ATTR, PURE_ATTR,
		MALLOC_ATTR, C_F_RETURNS_NOT_RETAINED_ATTR, OBJ_C_RETURNS_INNER_POINTER_ATTR, SENTINEL_ATTR, OBJ_C_EXCEPTION_ATTR,

		ARC_WEAKREF_UNAVAILABLE_ATTR, EMPTY_DECL, ALIGNED_ATTR, PACKED_ATTR, NO_DEBUG_ATTR, COMPOUND_LITERAL_EXPR,
		INIT_LIST_EXPR, MAY_ALIAS_ATTR, SHUFFLE_VECTOR_EXPR, G_C_C_ASM_STMT, UNUSED_ATTR,

		I_B_OUTLET_ATTR, I_B_ACTION_ATTR, DO_STMT,

		FULL_COMMENT, PARAGRAPH_COMMENT, TEXT_COMMENT, VERBATIM_LINE_COMMENT, VERBATIM_BLOCK_COMMENT, BLOCK_COMMAND_COMMENT,
		H_T_M_L_START_TAG_COMMENT, H_T_M_L_END_TAG_COMMENT, PARAM_COMMAND_COMMENT, INLINE_COMMAND_COMMENT, VERBATIM_BLOCK_LINE_COMMENT,

		C_F_AUDITED_TRANSFER_ATTR, OBJ_C_ROOT_CLASS_ATTR, N_S_CONSUMES_SELF_ATTR, N_S_CONSUMED_ATTR, WEAK_IMPORT_ATTR, AVAILABILITY_ATTR;

		private final String name;

		private State(String name) {
			this.name = name;
		}

		private State() {
			StringBuilder sb = new StringBuilder();
			String name = name();
			boolean upper = true;
			char[] chars = name.toCharArray();
			for (char c : chars) {
				if (upper) {
					upper = false;
					sb.append(Character.toUpperCase(c));
					continue;
				}
				if (c == '_') {
					upper = true;
				} else {
					sb.append(Character.toLowerCase(c));
				}
			}
			this.name = sb.toString();
		}

		@Override
		public String getName() {
			return name;
		}
	}

	protected StateMachine<State> stateMachine = new StateMachine<State>(State.START, possibleStates);

	private static final Map<State, State[]> possibleStates = new HashMap<State, State[]>();

	protected CLangHandler() {
		possibleStates.put(State.START, new State[]{State.TRANSLATION_UNIT_DECL});
		possibleStates.put(State.TRANSLATION_UNIT_DECL, new State[]{
				State.TYPEDEF_DECL, State.RECORD_DECL,
				State.FUNCTION_DECL, State.VAR_DECL, State.ENUM_DECL,
				State.OBJ_C_INTERFACE_DECL, State.OBJ_C_PROTOCOL_DECL, State.OBJ_C_CATEGORY_DECL,
				State.EMPTY_DECL
		});
		possibleStates.put(State.TYPEDEF_DECL, new State[]{State.DEPRECATED_ATTR, State.ALIGNED_ATTR, State.FULL_COMMENT});
		possibleStates.put(State.RECORD_DECL, new State[]{
				State.FIELD_DECL, State.MAX_FIELD_ALIGNMENT_ATTR, State.RECORD_DECL, State.TRANSPARENT_UNION_ATTR,
				State.ALIGNED_ATTR, State.MAY_ALIAS_ATTR, State.PACKED_ATTR, State.FULL_COMMENT
		});
		possibleStates.put(State.ALIGNED_ATTR, new State[]{State.INTEGER_LITERAL});
		possibleStates.put(State.FIELD_DECL, new State[]{State.INTEGER_LITERAL, State.PACKED_ATTR, State.DEPRECATED_ATTR, State.FULL_COMMENT});
		possibleStates.put(State.FUNCTION_DECL, new State[]{
				State.PARM_VAR_DECL, State.COMPOUND_STMT, State.NO_THROW_ATTR, State.CONST_ATTR, State.VISIBILITY_ATTR,
				State.N_S_RETURNS_RETAINED_ATTR, State.N_S_RETURNS_NOT_RETAINED_ATTR, State.FORMAT_ATTR,
				State.ALWAYS_INLINE_ATTR, State.C_F_RETURNS_RETAINED_ATTR, State.RETURNS_TWICE_ATTR, State.ASM_LABEL_ATTR,
				State.DEPRECATED_ATTR, State.FORMAT_ARG_ATTR, State.NON_NULL_ATTR, State.WARN_UNUSED_RESULT_ATTR,
				State.PURE_ATTR, State.MALLOC_ATTR, State.C_F_RETURNS_NOT_RETAINED_ATTR, State.NO_DEBUG_ATTR,
				State.UNAVAILABLE_ATTR, State.FULL_COMMENT, State.C_F_AUDITED_TRANSFER_ATTR, State.WEAK_IMPORT_ATTR,
				State.AVAILABILITY_ATTR
		});
		possibleStates.put(State.COMPOUND_STMT, new State[]{
				State.RETURN_STMT, State.DECL_STMT, State.BINARY_OPERATOR, State.IF_STMT,
				State.SWITCH_STMT, State.CASE_STMT, State.DEFAULT_STMT, State.CALL_EXPR,
				State.COMPOUND_ASSIGN_OPERATOR, State.G_C_C_ASM_STMT, State.DO_STMT
		});
		possibleStates.put(State.G_C_C_ASM_STMT, new State[]{
				State.DECL_REF_EXPR, State.IMPLICIT_CAST_EXPR
		});
		possibleStates.put(State.COMPOUND_ASSIGN_OPERATOR, new State[]{
				State.DECL_REF_EXPR, State.IMPLICIT_CAST_EXPR, State.ARRAY_SUBSCRIPT_EXPR, State.MEMBER_EXPR, State.PAREN_EXPR
		});
		possibleStates.put(State.SWITCH_STMT, new State[]{State.NULL, State.IMPLICIT_CAST_EXPR, State.COMPOUND_STMT, State.BINARY_OPERATOR});
		possibleStates.put(State.CASE_STMT, new State[]{
				State.NULL, State.DECL_REF_EXPR, State.RETURN_STMT, State.IMPLICIT_CAST_EXPR, State.DO_STMT, State.BINARY_OPERATOR
		});
		possibleStates.put(State.DEFAULT_STMT, new State[]{State.BREAK_STMT});
		possibleStates.put(State.IF_STMT, new State[]{
				State.NULL, State.BINARY_OPERATOR, State.RETURN_STMT, State.IMPLICIT_CAST_EXPR, State.COMPOUND_STMT,
				State.CALL_EXPR, State.DO_STMT
		});
		possibleStates.put(State.DO_STMT, new State[]{State.COMPOUND_STMT, State.INTEGER_LITERAL, State.UNARY_OPERATOR});
		possibleStates.put(State.RETURN_STMT, new State[]{
				State.IMPLICIT_CAST_EXPR, State.CALL_EXPR, State.PAREN_EXPR, State.CONDITIONAL_OPERATOR, State.OBJ_C_MESSAGE_EXPR,
				State.BINARY_OPERATOR, State.C_STYLE_CAST_EXPR, State.INTEGER_LITERAL, State.DECL_REF_EXPR, State.SHUFFLE_VECTOR_EXPR
		});
		possibleStates.put(State.OBJ_C_MESSAGE_EXPR, new State[]{State.C_STYLE_CAST_EXPR});
		possibleStates.put(State.IMPLICIT_CAST_EXPR, new State[]{
				State.PAREN_EXPR, State.IMPLICIT_CAST_EXPR, State.DECL_REF_EXPR, State.ARRAY_SUBSCRIPT_EXPR, State.MEMBER_EXPR,
				State.INTEGER_LITERAL, State.BINARY_OPERATOR, State.C_STYLE_CAST_EXPR, State.UNARY_OPERATOR, State.CALL_EXPR,
				State.COMPOUND_LITERAL_EXPR, State.UNARY_EXPR_OR_TYPE_TRAIT_EXPR, State.CONDITIONAL_OPERATOR
		});
		possibleStates.put(State.COMPOUND_LITERAL_EXPR, new State[]{State.INIT_LIST_EXPR});
		possibleStates.put(State.INIT_LIST_EXPR, new State[]{
				State.INTEGER_LITERAL, State.IMPLICIT_CAST_EXPR, State.C_STYLE_CAST_EXPR
		});
		possibleStates.put(State.PAREN_EXPR, new State[]{
				State.BINARY_OPERATOR, State.CONDITIONAL_OPERATOR, State.C_STYLE_CAST_EXPR, State.INTEGER_LITERAL,
				State.CALL_EXPR, State.DECL_REF_EXPR, State.UNARY_OPERATOR, State.MEMBER_EXPR, State.PAREN_EXPR,
				State.CHARACTER_LITERAL
		});
		possibleStates.put(State.BINARY_OPERATOR, new State[]{
				State.PAREN_EXPR, State.IMPLICIT_CAST_EXPR, State.INTEGER_LITERAL, State.UNARY_EXPR_OR_TYPE_TRAIT_EXPR,
				State.MEMBER_EXPR, State.UNARY_OPERATOR, State.BINARY_OPERATOR, State.DECL_REF_EXPR, State.C_STYLE_CAST_EXPR,
				State.CALL_EXPR, State.FLOATING_LITERAL, State.CHARACTER_LITERAL, State.ARRAY_SUBSCRIPT_EXPR,
				State.SHUFFLE_VECTOR_EXPR
		});
		possibleStates.put(State.CALL_EXPR, new State[]{
				State.CALL_EXPR, State.IMPLICIT_CAST_EXPR, State.C_STYLE_CAST_EXPR, State.INTEGER_LITERAL, State.BINARY_OPERATOR,
				State.DECL_REF_EXPR, State.PAREN_EXPR, State.UNARY_OPERATOR
		});
		possibleStates.put(State.ARRAY_SUBSCRIPT_EXPR, new State[]{
				State.IMPLICIT_CAST_EXPR, State.BINARY_OPERATOR, State.INTEGER_LITERAL, State.DECL_REF_EXPR
		});
		possibleStates.put(State.MEMBER_EXPR, new State[]{
				State.IMPLICIT_CAST_EXPR, State.DECL_REF_EXPR, State.MEMBER_EXPR, State.PAREN_EXPR
		});

		possibleStates.put(State.PARM_VAR_DECL, new State[]{
				State.C_F_CONSUMED_ATTR, State.UNUSED_ATTR, State.FULL_COMMENT, State.N_S_CONSUMED_ATTR
		});
		possibleStates.put(State.ENUM_DECL, new State[]{
				State.ENUM_CONSTANT_DECL, State.FULL_COMMENT, State.VISIBILITY_ATTR, State.UNAVAILABLE_ATTR, State.AVAILABILITY_ATTR
		});
		possibleStates.put(State.ENUM_CONSTANT_DECL, new State[]{
				State.UNARY_OPERATOR, State.IMPLICIT_CAST_EXPR, State.INTEGER_LITERAL, State.PAREN_EXPR, State.DECL_REF_EXPR,
				State.BINARY_OPERATOR, State.C_STYLE_CAST_EXPR, State.CHARACTER_LITERAL, State.VISIBILITY_ATTR, State.FULL_COMMENT,
				State.UNAVAILABLE_ATTR, State.DEPRECATED_ATTR, State.AVAILABILITY_ATTR
		});
		possibleStates.put(State.UNARY_OPERATOR, new State[]{
				State.INTEGER_LITERAL, State.UNARY_OPERATOR, State.PAREN_EXPR, State.CALL_EXPR, State.MEMBER_EXPR, State.C_STYLE_CAST_EXPR,
				State.IMPLICIT_CAST_EXPR, State.DECL_REF_EXPR
		});

		possibleStates.put(State.VAR_DECL, new State[]{
				State.VISIBILITY_ATTR, State.CALL_EXPR, State.ASM_LABEL_ATTR, State.DEPRECATED_ATTR, State.UNAVAILABLE_ATTR,
				State.IMPLICIT_CAST_EXPR, State.SHUFFLE_VECTOR_EXPR, State.C_STYLE_CAST_EXPR, State.FULL_COMMENT,
				State.WEAK_IMPORT_ATTR, State.INIT_LIST_EXPR, State.AVAILABILITY_ATTR, State.PAREN_EXPR, State.BINARY_OPERATOR
		});
		possibleStates.put(State.SHUFFLE_VECTOR_EXPR, new State[]{State.IMPLICIT_CAST_EXPR, State.INTEGER_LITERAL, State.C_STYLE_CAST_EXPR, State.BINARY_OPERATOR});
		possibleStates.put(State.DECL_STMT, new State[]{State.VAR_DECL, State.RECORD_DECL, State.TYPEDEF_DECL});

		possibleStates.put(State.CONDITIONAL_OPERATOR, new State[]{
				State.INTEGER_LITERAL, State.IMPLICIT_CAST_EXPR, State.C_STYLE_CAST_EXPR, State.UNARY_OPERATOR,
				State.PAREN_EXPR, State.CALL_EXPR, State.BINARY_OPERATOR, State.DECL_REF_EXPR
		});
		possibleStates.put(State.C_STYLE_CAST_EXPR, new State[]{
				State.C_STYLE_CAST_EXPR,
				State.CALL_EXPR, State.INTEGER_LITERAL, State.IMPLICIT_CAST_EXPR, State.PAREN_EXPR, State.UNARY_OPERATOR,
				State.FLOATING_LITERAL, State.UNARY_EXPR_OR_TYPE_TRAIT_EXPR, State.SHUFFLE_VECTOR_EXPR
		});

		possibleStates.put(State.OBJ_C_INTERFACE_DECL, new State[]{
				State.SUPER, State.OBJ_C_IVAR_DECL,
				State.OBJ_C_PROTOCOL, State.OBJ_C_METHOD_DECL, State.OBJ_C_PROPERTY_DECL,
				State.VISIBILITY_ATTR, State.OBJ_C_EXCEPTION_ATTR, State.ARC_WEAKREF_UNAVAILABLE_ATTR,
				State.FULL_COMMENT, State.OBJ_C_ROOT_CLASS_ATTR, State.UNAVAILABLE_ATTR, State.AVAILABILITY_ATTR
		});
		possibleStates.put(State.OBJ_C_IVAR_DECL, new State[]{State.I_B_OUTLET_ATTR});
		possibleStates.put(State.OBJ_C_PROTOCOL_DECL, new State[]{
				State.OBJ_C_PROTOCOL, State.OBJ_C_METHOD_DECL, State.OBJ_C_PROPERTY_DECL, State.DEPRECATED_ATTR,
				State.FULL_COMMENT, State.AVAILABILITY_ATTR
		});
		possibleStates.put(State.OBJ_C_METHOD_DECL, new State[]{
				State.PARM_VAR_DECL, State.UNAVAILABLE_ATTR, State.DEPRECATED_ATTR, State.OBJ_C_RETURNS_INNER_POINTER_ATTR,
				State.VARARG, State.SENTINEL_ATTR, State.FORMAT_ATTR, State.I_B_ACTION_ATTR,
				State.C_F_RETURNS_RETAINED_ATTR, State.N_S_RETURNS_RETAINED_ATTR, State.FULL_COMMENT, State.N_S_CONSUMES_SELF_ATTR,
				State.FORMAT_ARG_ATTR, State.VISIBILITY_ATTR, State.AVAILABILITY_ATTR
		});
		possibleStates.put(State.OBJ_C_PROPERTY_DECL, new State[]{
				State.GETTER, State.SETTER, State.I_B_OUTLET_ATTR, State.FULL_COMMENT, State.UNAVAILABLE_ATTR, State.AVAILABILITY_ATTR
		});
		possibleStates.put(State.OBJ_C_CATEGORY_DECL, new State[]{
				State.OBJ_C_INTERFACE,
				State.OBJ_C_PROTOCOL, State.OBJ_C_METHOD_DECL, State.OBJ_C_PROPERTY_DECL, State.FULL_COMMENT, State.AVAILABILITY_ATTR
		});

		possibleStates.put(State.FULL_COMMENT, new State[]{
				State.PARAGRAPH_COMMENT, State.VERBATIM_LINE_COMMENT, State.VERBATIM_BLOCK_COMMENT, State.BLOCK_COMMAND_COMMENT, State.PARAM_COMMAND_COMMENT
		});
		possibleStates.put(State.BLOCK_COMMAND_COMMENT, new State[]{State.PARAGRAPH_COMMENT});
		possibleStates.put(State.PARAM_COMMAND_COMMENT, new State[]{State.PARAGRAPH_COMMENT});
		possibleStates.put(State.PARAGRAPH_COMMENT, new State[]{
				State.TEXT_COMMENT, State.H_T_M_L_START_TAG_COMMENT, State.H_T_M_L_END_TAG_COMMENT, State.INLINE_COMMAND_COMMENT
		});
		possibleStates.put(State.VERBATIM_BLOCK_COMMENT, new State[]{State.VERBATIM_BLOCK_LINE_COMMENT});
	}

	protected <T extends GenericMetaMember> T getOrCreateMember(final Class<T> clazz, final Long address, final String name) {
		T member = (T) decls.get(address);

		if (member == null) {
			member = createMemberFromClass(clazz, name);
			decls.put(address, member);
		}

		return member;
	}

	protected void createMember(final Class<? extends GenericMetaMember> clazz, final String content) {
		createMember(clazz, content, true);
	}

	protected <T extends GenericMetaMember> void createMember(final Class<T> clazz, final String content, final boolean hasName) {
		String[] parts = split(content, 3, 0);
		Long address = Long.decode(parts[0]), prevAddress = null;
		if (parts[1].equals("prev")) {
			prevAddress = Long.decode(parts[2]);
		}
		String name = null;
		if (hasName) {
			String[] nameType = splitNameType(content);
			name = nameType[0];
		}
		if (name != null) {
			if (name.contains(":")) {
				throw new RuntimeException("Bad name for member (contains ':')");
			}
			if ("struct".equals(name)) {
				name = null;
			} else if ("union".equals(name)) {
				name = null;
			}
		}
		// This member can exist already (we used it in some class before declaring it)
		if (name != null && decls.containsKey(address)) {
			// We update the address with the existing one
			addMemberDecl(decls.get(address), address, prevAddress);
			if (lastMetaMember.name == null) {
				lastMetaMember.name = name;
			}
		} else {
			// Member didn't exist, create a new one
			T member = createMemberFromClass(clazz, name);
			addMemberDecl(member, address, prevAddress);
		}
	}

	private static final Map<Class<? extends GenericMetaMember>, Constructor<? extends GenericMetaMember>> constructors = new HashMap<Class<? extends GenericMetaMember>, Constructor<? extends GenericMetaMember>>();

	private <T extends GenericMetaMember> T createMemberFromClass(Class<T> clazz, String name) {
		Constructor<? extends GenericMetaMember> constructor;
		try {
			constructor = constructors.get(clazz);
			if (constructor == null) {
				constructor = clazz.getConstructor(String.class);
				constructors.put(clazz, constructor);
			}
			return (T) constructor.newInstance(name);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void addMemberDecl(final GenericMetaMember member, final Long address, final Long prevAddress) {
		lastMetaMember = member;
		if (prevAddress != null) {
			lastMetaMember = decls.get(prevAddress);
		}
		decls.put(address, lastMetaMember);
	}

	protected String[] splitProperty(final String content) {
		String[] result = new String[3];
		int idx = content.length(), lastIdx = content.length();

		// Property types
		while (content.charAt(--idx) != '\'') {
		}
		result[2] = content.substring(idx + 2, lastIdx);
		lastIdx = idx;

		// Type
		do {
			idx -= 2;
			while (content.charAt(--idx) != '\'') {
			}
		} while (content.charAt(idx - 1) == ':');
		result[1] = content.substring(idx + 1, lastIdx);

		lastIdx = --idx;

		// Name
		while (content.charAt(--idx) != ' ') {
		}
		result[0] = content.substring(idx + 1, lastIdx);

		return result;
	}

	protected String[] splitNameType(final String content) {
		String[] result = new String[2];
		int idx = content.length(), lastIdx = content.length();

		// Type
		if (content.charAt(content.length() - 1) == '\'') {
			lastIdx--;
			idx = content.length() + 1;
			do {
				idx -= 2;
				while (content.charAt(--idx) != '\'') {
				}
			} while (content.charAt(idx - 1) == ':');
			result[1] = content.substring(idx + 1, lastIdx);

			lastIdx = --idx;
		}

		// Name
		while (content.charAt(--idx) != ' ') {
		}
		result[0] = content.substring(idx + 1, lastIdx);

		return result;
	}

	protected String[] splitMethodNameType(final String content) {
		String[] result = new String[3];
		String[] oldResult = splitNameType(content);
		result[1] = oldResult[0];
		result[2] = oldResult[1];
		result[0] = Character.toString(content.charAt(content.length() - (result[1].length() + result[2].length() + 5)));

		return result;
	}

	protected static String[] split(final String content, final int startLimit, final int endLimit) {
		final String[] result = new String[startLimit + endLimit];
		int j = 0;

		int idx = 0, lastIdx = 0;
		int length = content.length();
		for (int k = startLimit; k > 0; k--) {
			// FIXME: If this reaches the end of input, the last word will have a character less
			// (like "'NSObject" (notice the missing quote))
			while ((idx < length - 1 && content.charAt(++idx) != ' ')) {}
			result[j++] = content.substring(lastIdx, idx);
			lastIdx = idx + 1;
		}
		idx = lastIdx = content.length();
		for (int k = endLimit; k > 0; k--) {
			while (content.charAt(--idx) != ' ') {}
			result[j++] = content.substring(idx + 1, lastIdx);
			lastIdx = idx;
		}

		return result;
	}

	@Override
	public void setLibrary(final String library, boolean isFramework) {
		this.library = library.intern();
		// TODO: Maybe we can remove this boolean to be totally ObjC agnostic (and just check the ending of the library string itself)
		this.isFramework = isFramework;
	}
}
