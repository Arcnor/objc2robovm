package com.arcnor.objcclang.parser;

import com.arcnor.objcclang.meta.GenericMetaMember;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static com.arcnor.objcclang.parser.CLangHandler.State.*;

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
		INIT_LIST_EXPR, MAY_ALIAS_ATTR, SHUFFLE_VECTOR_EXPR, GCC_ASM_STMT("GCCAsmStmt"), UNUSED_ATTR,

		I_B_OUTLET_ATTR, I_B_ACTION_ATTR, DO_STMT,

		FULL_COMMENT, PARAGRAPH_COMMENT, TEXT_COMMENT, VERBATIM_LINE_COMMENT, VERBATIM_BLOCK_COMMENT, BLOCK_COMMAND_COMMENT,
		H_T_M_L_START_TAG_COMMENT, H_T_M_L_END_TAG_COMMENT, PARAM_COMMAND_COMMENT, INLINE_COMMAND_COMMENT, VERBATIM_BLOCK_LINE_COMMENT,

		C_F_AUDITED_TRANSFER_ATTR, OBJ_C_ROOT_CLASS_ATTR, N_S_CONSUMES_SELF_ATTR, N_S_CONSUMED_ATTR, WEAK_IMPORT_ATTR, AVAILABILITY_ATTR,

		// C++
		NAMESPACE_DECL,
		CXX_RECORD_DECL("CXXRecordDecl"), CXX_CONSTRUCTOR_DECL("CXXConstructorDecl"), CXX_THIS_EXPR("CXXThisExpr"), CXX_UNRESOLVED_CONSTRUCT_EXPR("CXXUnresolvedConstructExpr"),
		CXX_DESTRUCTOR_DECL("CXXDestructorDecl"), CXX_METHOD_DECL("CXXMethodDecl"), CXX_DEPENDENT_SCOPE_MEMBER_EXPR("CXXDependentScopeMemberExpr"),
		CXX_CONVERSION_DECL("CXXConversionDecl"),  CXX_OPERATOR_CALL_EXPR("CXXOperatorCallExpr"), CXX_DELETE_EXPR("CXXDeleteExpr"),
		CXX_CTOR_INITIALIZER("CXXCtorInitializer"), CXX_MEMBER_CALL_EXPR("CXXMemberCallExpr"), CXX_FUNCTIONAL_CAST_EXPR("CXXFunctionalCastExpr"),
		CXX_BIND_TEMPORARY_EXPR("CXXBindTemporaryExpr"), CXX_CONSTRUCT_EXPR("CXXConstructExpr"), CXX_NEW_EXPR("CXXNewExpr"),
		CXX_PSEUDO_DESTRUCTOR_EXPR("CXXPseudoDestructorExpr"), CXX_CONST_CAST_EXPR("CXXConstCastExpr"), CXX_BOOL_LITERAL_EXPR("CXXBoolLiteralExpr"),
		CXX_DEFAULT_ARG_EXPR("CXXDefaultArgExpr"), CXX_REINTERPRET_CAST_EXPR("CXXReinterpretCastExpr"), CXX_TEMPORARY_OBJECT_EXPR("CXXTemporaryObjectExpr"),
		CXX_STATIC_CAST_EXPR("CXXStaticCastExpr"),
		FUNCTION_TEMPLATE_DECL, TEMPLATE_TYPE_PARM_DECL, TEMPLATE_ARGUMENT,
		CLASS_TEMPLATE_DECL, CLASS_TEMPLATE_SPECIALIZATION_DECL, CLASS_TEMPLATE_SPECIALIZATION, CLASS_TEMPLATE_PARTIAL_SPECIALIZATION_DECL,
		ACCESS_SPEC_DECL,
		ORIGINAL("original"), PUBLIC("public"), PRIVATE("private"), PROTECTED("protected"),  // FIXME: These are not really valid nodes
		UNRESOLVED_LOOKUP_EXPR, PAREN_LIST_EXPR, EXPR_WITH_CLEANUPS, DEPENDENT_SCOPE_DECL_REF_EXPR, NON_TYPE_TEMPLATE_PARM_DECL, FOR_STMT,

		LINKAGE_SPEC_DECL, NULL_STMT, WHILE_STMT, MATERIALIZE_TEMPORARY_EXPR, UNRESOLVED_MEMBER_EXPR, FRIEND_DECL, IMPLICIT_VALUE_INIT_EXPR,

		GNU_NULL_EXPR("GNUNullExpr"), USING_DECL, USING_SHADOW_DECL, STRING_LITERAL, FUNCTION, T_PARAM_COMMAND_COMMENT, USING_DIRECTIVE_DECL;

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

	protected StateMachine<State> stateMachine = new StateMachine<State>(START, possibleStates);

	private static final Map<State, State[]> possibleStates = new HashMap<State, State[]>();

	protected CLangHandler() {
		possibleStates.put(START, new State[]{TRANSLATION_UNIT_DECL});
		possibleStates.put(TRANSLATION_UNIT_DECL, new State[]{
				TYPEDEF_DECL, RECORD_DECL,
				FUNCTION_DECL, VAR_DECL, ENUM_DECL,
				OBJ_C_INTERFACE_DECL, OBJ_C_PROTOCOL_DECL, OBJ_C_CATEGORY_DECL,
				NAMESPACE_DECL, LINKAGE_SPEC_DECL,
				CXX_RECORD_DECL, USING_DIRECTIVE_DECL,
				EMPTY_DECL
		});
		possibleStates.put(TYPEDEF_DECL, new State[]{DEPRECATED_ATTR, ALIGNED_ATTR, FULL_COMMENT});
		possibleStates.put(RECORD_DECL, new State[]{
				FIELD_DECL, MAX_FIELD_ALIGNMENT_ATTR, RECORD_DECL, TRANSPARENT_UNION_ATTR,
				ALIGNED_ATTR, MAY_ALIAS_ATTR, PACKED_ATTR, FULL_COMMENT
		});
		possibleStates.put(ALIGNED_ATTR, new State[]{INTEGER_LITERAL});
		possibleStates.put(FIELD_DECL, new State[]{INTEGER_LITERAL, PACKED_ATTR, DEPRECATED_ATTR, FULL_COMMENT});
		possibleStates.put(FUNCTION_DECL, new State[]{
				PARM_VAR_DECL, COMPOUND_STMT, NO_THROW_ATTR, CONST_ATTR, VISIBILITY_ATTR,
				N_S_RETURNS_RETAINED_ATTR, N_S_RETURNS_NOT_RETAINED_ATTR, FORMAT_ATTR,
				ALWAYS_INLINE_ATTR, C_F_RETURNS_RETAINED_ATTR, RETURNS_TWICE_ATTR, ASM_LABEL_ATTR,
				DEPRECATED_ATTR, FORMAT_ARG_ATTR, NON_NULL_ATTR, WARN_UNUSED_RESULT_ATTR,
				PURE_ATTR, MALLOC_ATTR, C_F_RETURNS_NOT_RETAINED_ATTR, NO_DEBUG_ATTR,
				UNAVAILABLE_ATTR, FULL_COMMENT, C_F_AUDITED_TRANSFER_ATTR, WEAK_IMPORT_ATTR,
				AVAILABILITY_ATTR,
				TEMPLATE_ARGUMENT
		});
		possibleStates.put(COMPOUND_STMT, new State[]{
				COMPOUND_STMT, RETURN_STMT, DECL_STMT, BINARY_OPERATOR, IF_STMT,
				SWITCH_STMT, CASE_STMT, DEFAULT_STMT, CALL_EXPR,
				COMPOUND_ASSIGN_OPERATOR, GCC_ASM_STMT, DO_STMT,
				CXX_OPERATOR_CALL_EXPR, CXX_DELETE_EXPR, PAREN_EXPR,
				EXPR_WITH_CLEANUPS, CXX_MEMBER_CALL_EXPR, UNARY_OPERATOR,
				FOR_STMT, CXX_NEW_EXPR, NULL_STMT, WHILE_STMT,
				BREAK_STMT
		});
		possibleStates.put(GCC_ASM_STMT, new State[]{
				DECL_REF_EXPR, IMPLICIT_CAST_EXPR
		});
		possibleStates.put(COMPOUND_ASSIGN_OPERATOR, new State[]{
				DECL_REF_EXPR, IMPLICIT_CAST_EXPR, ARRAY_SUBSCRIPT_EXPR, MEMBER_EXPR, PAREN_EXPR,
				CXX_DEPENDENT_SCOPE_MEMBER_EXPR, INTEGER_LITERAL, CXX_UNRESOLVED_CONSTRUCT_EXPR,
				UNARY_EXPR_OR_TYPE_TRAIT_EXPR
		});
		possibleStates.put(SWITCH_STMT, new State[]{
				NULL, IMPLICIT_CAST_EXPR, COMPOUND_STMT, BINARY_OPERATOR,
				UNARY_EXPR_OR_TYPE_TRAIT_EXPR
		});
		possibleStates.put(CASE_STMT, new State[]{
				NULL, DECL_REF_EXPR, RETURN_STMT, IMPLICIT_CAST_EXPR, DO_STMT, BINARY_OPERATOR,
				INTEGER_LITERAL, COMPOUND_STMT, PAREN_EXPR, CXX_MEMBER_CALL_EXPR, CXX_OPERATOR_CALL_EXPR
		});
		possibleStates.put(DEFAULT_STMT, new State[]{
				BREAK_STMT, RETURN_STMT, PAREN_EXPR
		});
		possibleStates.put(IF_STMT, new State[]{
				NULL, BINARY_OPERATOR, RETURN_STMT, IMPLICIT_CAST_EXPR, COMPOUND_STMT,
				CALL_EXPR, DO_STMT, UNARY_OPERATOR, CXX_MEMBER_CALL_EXPR, CXX_DEPENDENT_SCOPE_MEMBER_EXPR,
				MEMBER_EXPR, DECL_REF_EXPR, PAREN_EXPR, IF_STMT, BREAK_STMT,
				ARRAY_SUBSCRIPT_EXPR, CXX_OPERATOR_CALL_EXPR, EXPR_WITH_CLEANUPS
		});
		possibleStates.put(DO_STMT, new State[]{
				COMPOUND_STMT, INTEGER_LITERAL, UNARY_OPERATOR, BINARY_OPERATOR
		});
		possibleStates.put(RETURN_STMT, new State[]{
				IMPLICIT_CAST_EXPR, CALL_EXPR, PAREN_EXPR, CONDITIONAL_OPERATOR, OBJ_C_MESSAGE_EXPR,
				BINARY_OPERATOR, C_STYLE_CAST_EXPR, INTEGER_LITERAL, DECL_REF_EXPR, SHUFFLE_VECTOR_EXPR,
				FLOATING_LITERAL, ARRAY_SUBSCRIPT_EXPR, UNARY_OPERATOR, MEMBER_EXPR, CXX_NEW_EXPR,
				CXX_BOOL_LITERAL_EXPR, CXX_MEMBER_CALL_EXPR, CXX_UNRESOLVED_CONSTRUCT_EXPR, CXX_DEPENDENT_SCOPE_MEMBER_EXPR,
				CXX_OPERATOR_CALL_EXPR, EXPR_WITH_CLEANUPS, CXX_CONSTRUCT_EXPR, CXX_FUNCTIONAL_CAST_EXPR,
				GNU_NULL_EXPR
		});
		possibleStates.put(OBJ_C_MESSAGE_EXPR, new State[]{C_STYLE_CAST_EXPR});
		possibleStates.put(IMPLICIT_CAST_EXPR, new State[]{
				PAREN_EXPR, IMPLICIT_CAST_EXPR, DECL_REF_EXPR, ARRAY_SUBSCRIPT_EXPR, MEMBER_EXPR,
				INTEGER_LITERAL, BINARY_OPERATOR, C_STYLE_CAST_EXPR, UNARY_OPERATOR, CALL_EXPR,
				COMPOUND_LITERAL_EXPR, UNARY_EXPR_OR_TYPE_TRAIT_EXPR, CONDITIONAL_OPERATOR,
				CXX_OPERATOR_CALL_EXPR, CXX_MEMBER_CALL_EXPR, CXX_THIS_EXPR, CXX_BIND_TEMPORARY_EXPR,
				CHARACTER_LITERAL, GNU_NULL_EXPR, CXX_TEMPORARY_OBJECT_EXPR,
				CXX_FUNCTIONAL_CAST_EXPR, STRING_LITERAL, CXX_CONSTRUCT_EXPR, COMPOUND_ASSIGN_OPERATOR,
				CXX_BOOL_LITERAL_EXPR, FLOATING_LITERAL
		});
		possibleStates.put(COMPOUND_LITERAL_EXPR, new State[]{INIT_LIST_EXPR});
		possibleStates.put(INIT_LIST_EXPR, new State[]{
				INIT_LIST_EXPR, INTEGER_LITERAL, IMPLICIT_CAST_EXPR, C_STYLE_CAST_EXPR, DECL_REF_EXPR
		});
		possibleStates.put(PAREN_EXPR, new State[]{
				BINARY_OPERATOR, CONDITIONAL_OPERATOR, C_STYLE_CAST_EXPR, INTEGER_LITERAL,
				CALL_EXPR, DECL_REF_EXPR, UNARY_OPERATOR, MEMBER_EXPR, PAREN_EXPR,
				CHARACTER_LITERAL, ARRAY_SUBSCRIPT_EXPR, CXX_DEPENDENT_SCOPE_MEMBER_EXPR,
				FLOATING_LITERAL, CXX_OPERATOR_CALL_EXPR, CXX_MEMBER_CALL_EXPR
		});
		possibleStates.put(BINARY_OPERATOR, new State[]{
				PAREN_EXPR, IMPLICIT_CAST_EXPR, INTEGER_LITERAL, UNARY_EXPR_OR_TYPE_TRAIT_EXPR,
				MEMBER_EXPR, UNARY_OPERATOR, BINARY_OPERATOR, DECL_REF_EXPR, C_STYLE_CAST_EXPR,
				CALL_EXPR, FLOATING_LITERAL, CHARACTER_LITERAL, ARRAY_SUBSCRIPT_EXPR,
				SHUFFLE_VECTOR_EXPR, CXX_UNRESOLVED_CONSTRUCT_EXPR, CXX_DEPENDENT_SCOPE_MEMBER_EXPR,
				CXX_THIS_EXPR, CXX_MEMBER_CALL_EXPR, CONDITIONAL_OPERATOR, CXX_REINTERPRET_CAST_EXPR,
				CXX_FUNCTIONAL_CAST_EXPR, CXX_NEW_EXPR, CXX_BOOL_LITERAL_EXPR, CXX_OPERATOR_CALL_EXPR,
				GNU_NULL_EXPR, CXX_BIND_TEMPORARY_EXPR, CXX_TEMPORARY_OBJECT_EXPR, CXX_CONST_CAST_EXPR
		});
		possibleStates.put(CALL_EXPR, new State[]{
				CALL_EXPR, IMPLICIT_CAST_EXPR, C_STYLE_CAST_EXPR, INTEGER_LITERAL, BINARY_OPERATOR,
				DECL_REF_EXPR, PAREN_EXPR, UNARY_OPERATOR, MEMBER_EXPR, UNRESOLVED_LOOKUP_EXPR,
				CXX_DEPENDENT_SCOPE_MEMBER_EXPR, UNARY_EXPR_OR_TYPE_TRAIT_EXPR, CXX_PSEUDO_DESTRUCTOR_EXPR,
				CXX_CONST_CAST_EXPR, CONDITIONAL_OPERATOR, CXX_BOOL_LITERAL_EXPR, ARRAY_SUBSCRIPT_EXPR,
				MATERIALIZE_TEMPORARY_EXPR, UNRESOLVED_MEMBER_EXPR, CXX_UNRESOLVED_CONSTRUCT_EXPR,
				CXX_MEMBER_CALL_EXPR, CXX_OPERATOR_CALL_EXPR, DEPENDENT_SCOPE_DECL_REF_EXPR, CXX_REINTERPRET_CAST_EXPR,
				CXX_STATIC_CAST_EXPR, CXX_THIS_EXPR, CXX_DEFAULT_ARG_EXPR
		});
		possibleStates.put(ARRAY_SUBSCRIPT_EXPR, new State[]{
				IMPLICIT_CAST_EXPR, BINARY_OPERATOR, INTEGER_LITERAL, DECL_REF_EXPR,
				MEMBER_EXPR, CXX_DEPENDENT_SCOPE_MEMBER_EXPR, PAREN_EXPR, UNARY_OPERATOR,
				CALL_EXPR, CXX_MEMBER_CALL_EXPR
		});
		possibleStates.put(MEMBER_EXPR, new State[]{
				MEMBER_EXPR, IMPLICIT_CAST_EXPR, DECL_REF_EXPR, PAREN_EXPR,
				CXX_THIS_EXPR, CXX_FUNCTIONAL_CAST_EXPR, ARRAY_SUBSCRIPT_EXPR,
				CXX_OPERATOR_CALL_EXPR, CXX_MEMBER_CALL_EXPR, CXX_CONST_CAST_EXPR,
				CALL_EXPR
		});

		possibleStates.put(PARM_VAR_DECL, new State[]{
				C_F_CONSUMED_ATTR, UNUSED_ATTR, FULL_COMMENT, N_S_CONSUMED_ATTR, INTEGER_LITERAL,
				CXX_BOOL_LITERAL_EXPR, IMPLICIT_CAST_EXPR, CHARACTER_LITERAL, DECL_REF_EXPR,
				FLOATING_LITERAL, MATERIALIZE_TEMPORARY_EXPR, PAREN_EXPR, C_STYLE_CAST_EXPR,
				EXPR_WITH_CLEANUPS, UNARY_OPERATOR, CXX_CONSTRUCT_EXPR
		});
		possibleStates.put(ENUM_DECL, new State[]{
				ENUM_CONSTANT_DECL, FULL_COMMENT, VISIBILITY_ATTR, UNAVAILABLE_ATTR, AVAILABILITY_ATTR
		});
		possibleStates.put(ENUM_CONSTANT_DECL, new State[]{
				UNARY_OPERATOR, IMPLICIT_CAST_EXPR, INTEGER_LITERAL, PAREN_EXPR, DECL_REF_EXPR,
				BINARY_OPERATOR, C_STYLE_CAST_EXPR, CHARACTER_LITERAL, VISIBILITY_ATTR, FULL_COMMENT,
				UNAVAILABLE_ATTR, DEPRECATED_ATTR, AVAILABILITY_ATTR, DEPENDENT_SCOPE_DECL_REF_EXPR
		});
		possibleStates.put(UNARY_OPERATOR, new State[]{
				INTEGER_LITERAL, UNARY_OPERATOR, PAREN_EXPR, CALL_EXPR, MEMBER_EXPR, C_STYLE_CAST_EXPR,
				IMPLICIT_CAST_EXPR, DECL_REF_EXPR,
				CXX_THIS_EXPR, ARRAY_SUBSCRIPT_EXPR, CXX_MEMBER_CALL_EXPR,
				CXX_OPERATOR_CALL_EXPR
		});

		possibleStates.put(VAR_DECL, new State[]{
				VISIBILITY_ATTR, CALL_EXPR, ASM_LABEL_ATTR, DEPRECATED_ATTR, UNAVAILABLE_ATTR,
				IMPLICIT_CAST_EXPR, SHUFFLE_VECTOR_EXPR, C_STYLE_CAST_EXPR, FULL_COMMENT,
				WEAK_IMPORT_ATTR, INIT_LIST_EXPR, AVAILABILITY_ATTR, PAREN_EXPR, BINARY_OPERATOR,
				CXX_DEPENDENT_SCOPE_MEMBER_EXPR, MEMBER_EXPR, INTEGER_LITERAL, DECL_REF_EXPR, CONDITIONAL_OPERATOR,
				CXX_MEMBER_CALL_EXPR, PAREN_LIST_EXPR, CXX_THIS_EXPR, CXX_BOOL_LITERAL_EXPR,
				CXX_CONSTRUCT_EXPR, ARRAY_SUBSCRIPT_EXPR, GNU_NULL_EXPR, UNARY_OPERATOR, EXPR_WITH_CLEANUPS,
				CXX_FUNCTIONAL_CAST_EXPR
		});
		possibleStates.put(SHUFFLE_VECTOR_EXPR, new State[]{IMPLICIT_CAST_EXPR, INTEGER_LITERAL, C_STYLE_CAST_EXPR, BINARY_OPERATOR});
		possibleStates.put(DECL_STMT, new State[]{
				VAR_DECL, RECORD_DECL, TYPEDEF_DECL,
				CXX_RECORD_DECL
		});

		possibleStates.put(CONDITIONAL_OPERATOR, new State[]{
				CONDITIONAL_OPERATOR, INTEGER_LITERAL, IMPLICIT_CAST_EXPR, C_STYLE_CAST_EXPR, UNARY_OPERATOR,
				PAREN_EXPR, CALL_EXPR, BINARY_OPERATOR, DECL_REF_EXPR, MEMBER_EXPR,
				GNU_NULL_EXPR, CXX_UNRESOLVED_CONSTRUCT_EXPR, CXX_OPERATOR_CALL_EXPR, CXX_BOOL_LITERAL_EXPR,
				CXX_MEMBER_CALL_EXPR, CXX_CONSTRUCT_EXPR
		});
		possibleStates.put(C_STYLE_CAST_EXPR, new State[]{
				C_STYLE_CAST_EXPR,
				CALL_EXPR, INTEGER_LITERAL, IMPLICIT_CAST_EXPR, PAREN_EXPR, UNARY_OPERATOR,
				FLOATING_LITERAL, UNARY_EXPR_OR_TYPE_TRAIT_EXPR, SHUFFLE_VECTOR_EXPR,
				CXX_THIS_EXPR, DECL_REF_EXPR, DEPENDENT_SCOPE_DECL_REF_EXPR, ARRAY_SUBSCRIPT_EXPR, MEMBER_EXPR,
				CXX_MEMBER_CALL_EXPR, CXX_DEPENDENT_SCOPE_MEMBER_EXPR, GNU_NULL_EXPR
		});

		possibleStates.put(OBJ_C_INTERFACE_DECL, new State[]{
				SUPER, OBJ_C_IVAR_DECL,
				OBJ_C_PROTOCOL, OBJ_C_METHOD_DECL, OBJ_C_PROPERTY_DECL,
				VISIBILITY_ATTR, OBJ_C_EXCEPTION_ATTR, ARC_WEAKREF_UNAVAILABLE_ATTR,
				FULL_COMMENT, OBJ_C_ROOT_CLASS_ATTR, UNAVAILABLE_ATTR, AVAILABILITY_ATTR
		});
		possibleStates.put(OBJ_C_IVAR_DECL, new State[]{I_B_OUTLET_ATTR});
		possibleStates.put(OBJ_C_PROTOCOL_DECL, new State[]{
				OBJ_C_PROTOCOL, OBJ_C_METHOD_DECL, OBJ_C_PROPERTY_DECL, DEPRECATED_ATTR,
				FULL_COMMENT, AVAILABILITY_ATTR
		});
		possibleStates.put(OBJ_C_METHOD_DECL, new State[]{
				PARM_VAR_DECL, UNAVAILABLE_ATTR, DEPRECATED_ATTR, OBJ_C_RETURNS_INNER_POINTER_ATTR,
				VARARG, SENTINEL_ATTR, FORMAT_ATTR, I_B_ACTION_ATTR,
				C_F_RETURNS_RETAINED_ATTR, N_S_RETURNS_RETAINED_ATTR, FULL_COMMENT, N_S_CONSUMES_SELF_ATTR,
				FORMAT_ARG_ATTR, VISIBILITY_ATTR, AVAILABILITY_ATTR
		});
		possibleStates.put(OBJ_C_PROPERTY_DECL, new State[]{
				GETTER, SETTER, I_B_OUTLET_ATTR, FULL_COMMENT, UNAVAILABLE_ATTR, AVAILABILITY_ATTR
		});
		possibleStates.put(OBJ_C_CATEGORY_DECL, new State[]{
				OBJ_C_INTERFACE,
				OBJ_C_PROTOCOL, OBJ_C_METHOD_DECL, OBJ_C_PROPERTY_DECL, FULL_COMMENT, AVAILABILITY_ATTR
		});

		possibleStates.put(FULL_COMMENT, new State[]{
				PARAGRAPH_COMMENT, VERBATIM_LINE_COMMENT, VERBATIM_BLOCK_COMMENT, BLOCK_COMMAND_COMMENT, PARAM_COMMAND_COMMENT,
				T_PARAM_COMMAND_COMMENT
		});
		possibleStates.put(T_PARAM_COMMAND_COMMENT, new State[]{
				PARAGRAPH_COMMENT
		});
		possibleStates.put(BLOCK_COMMAND_COMMENT, new State[]{PARAGRAPH_COMMENT});
		possibleStates.put(PARAM_COMMAND_COMMENT, new State[]{PARAGRAPH_COMMENT});
		possibleStates.put(PARAGRAPH_COMMENT, new State[]{
				TEXT_COMMENT, H_T_M_L_START_TAG_COMMENT, H_T_M_L_END_TAG_COMMENT, INLINE_COMMAND_COMMENT
		});
		possibleStates.put(VERBATIM_BLOCK_COMMENT, new State[]{VERBATIM_BLOCK_LINE_COMMENT});

		// C++
		possibleStates.put(NAMESPACE_DECL, new State[] {
				NAMESPACE_DECL, CXX_RECORD_DECL, TYPEDEF_DECL, FUNCTION_DECL, FUNCTION_TEMPLATE_DECL,
				EMPTY_DECL, CLASS_TEMPLATE_DECL, CLASS_TEMPLATE_SPECIALIZATION_DECL,
				CLASS_TEMPLATE_PARTIAL_SPECIALIZATION_DECL, USING_DECL, USING_SHADOW_DECL,
				VAR_DECL, ENUM_DECL,
				ORIGINAL  // FIXME: This is not really a valid node
		});
		possibleStates.put(FUNCTION_TEMPLATE_DECL, new State[] {
				TEMPLATE_TYPE_PARM_DECL, FUNCTION_DECL, FULL_COMMENT, CXX_CONSTRUCTOR_DECL, FUNCTION, CXX_METHOD_DECL
		});
		possibleStates.put(CLASS_TEMPLATE_DECL, new State[] {
				TEMPLATE_TYPE_PARM_DECL, CXX_RECORD_DECL, CLASS_TEMPLATE_SPECIALIZATION_DECL,
				CLASS_TEMPLATE_SPECIALIZATION, FULL_COMMENT, NON_TYPE_TEMPLATE_PARM_DECL
		});
		possibleStates.put(CXX_RECORD_DECL, new State[] {
				CXX_RECORD_DECL, CXX_CONSTRUCTOR_DECL, MAX_FIELD_ALIGNMENT_ATTR, ACCESS_SPEC_DECL,
				CXX_DESTRUCTOR_DECL, CXX_METHOD_DECL, FIELD_DECL, CXX_CONVERSION_DECL,
				ENUM_DECL, TYPEDEF_DECL, FRIEND_DECL,
				FULL_COMMENT, ALIGNED_ATTR, VISIBILITY_ATTR, VAR_DECL, CLASS_TEMPLATE_DECL,
				FUNCTION_TEMPLATE_DECL,
				PUBLIC, PRIVATE, PROTECTED  // FIXME: These are not really valid nodes
		});
		possibleStates.put(CXX_CONSTRUCTOR_DECL, new State[] {
				COMPOUND_STMT, PARM_VAR_DECL, CXX_CTOR_INITIALIZER, FULL_COMMENT
		});
		possibleStates.put(CXX_UNRESOLVED_CONSTRUCT_EXPR, new State[] {
				INTEGER_LITERAL, DECL_REF_EXPR, CALL_EXPR, CXX_DEPENDENT_SCOPE_MEMBER_EXPR, UNARY_OPERATOR, DEPENDENT_SCOPE_DECL_REF_EXPR,
				FLOATING_LITERAL, BINARY_OPERATOR
		});
		possibleStates.put(CXX_DESTRUCTOR_DECL, new State[] {
				COMPOUND_STMT, FULL_COMMENT
		});
		possibleStates.put(CXX_METHOD_DECL, new State[] {
				PARM_VAR_DECL, COMPOUND_STMT, FULL_COMMENT, DEPRECATED_ATTR, TEMPLATE_ARGUMENT
		});
		possibleStates.put(CXX_DEPENDENT_SCOPE_MEMBER_EXPR, new State[] {
				CXX_DEPENDENT_SCOPE_MEMBER_EXPR, DECL_REF_EXPR, PAREN_EXPR, CXX_UNRESOLVED_CONSTRUCT_EXPR, MEMBER_EXPR,
				CXX_THIS_EXPR, CALL_EXPR, ARRAY_SUBSCRIPT_EXPR
		});
		possibleStates.put(CLASS_TEMPLATE_SPECIALIZATION_DECL, new State[] {
				TEMPLATE_ARGUMENT, MAX_FIELD_ALIGNMENT_ATTR, ACCESS_SPEC_DECL,
				CXX_RECORD_DECL, CXX_CONSTRUCTOR_DECL, CXX_DESTRUCTOR_DECL, CXX_METHOD_DECL,
				CXX_CONVERSION_DECL,
				ENUM_DECL, FIELD_DECL, TYPEDEF_DECL, FRIEND_DECL, VAR_DECL,
				PUBLIC
		});
		possibleStates.put(CLASS_TEMPLATE_PARTIAL_SPECIALIZATION_DECL, new State[] {
				TEMPLATE_ARGUMENT, TEMPLATE_TYPE_PARM_DECL, MAX_FIELD_ALIGNMENT_ATTR, CXX_RECORD_DECL,
				ENUM_DECL, NON_TYPE_TEMPLATE_PARM_DECL, FULL_COMMENT, FIELD_DECL, TYPEDEF_DECL
		});
		possibleStates.put(CXX_CONVERSION_DECL, new State[] {
				COMPOUND_STMT, FULL_COMMENT
		});
		possibleStates.put(CXX_OPERATOR_CALL_EXPR, new State[] {
				CXX_OPERATOR_CALL_EXPR, IMPLICIT_CAST_EXPR, DECL_REF_EXPR, INTEGER_LITERAL,
				ARRAY_SUBSCRIPT_EXPR, UNARY_OPERATOR, MATERIALIZE_TEMPORARY_EXPR, UNRESOLVED_LOOKUP_EXPR,
				CALL_EXPR, CXX_MEMBER_CALL_EXPR, MEMBER_EXPR, CXX_DEPENDENT_SCOPE_MEMBER_EXPR, CXX_UNRESOLVED_CONSTRUCT_EXPR,
				PAREN_EXPR, BINARY_OPERATOR, CONDITIONAL_OPERATOR
		});
		possibleStates.put(CXX_DELETE_EXPR, new State[] {
				UNARY_OPERATOR
		});
		possibleStates.put(CXX_CTOR_INITIALIZER, new State[] {
				PAREN_LIST_EXPR, IMPLICIT_CAST_EXPR, CXX_CONSTRUCT_EXPR, INTEGER_LITERAL,
				IMPLICIT_VALUE_INIT_EXPR, CXX_CONST_CAST_EXPR, UNARY_OPERATOR, DECL_REF_EXPR,
				CXX_BOOL_LITERAL_EXPR
		});
		possibleStates.put(PAREN_LIST_EXPR, new State[] {
				DECL_REF_EXPR, INTEGER_LITERAL, CXX_DEPENDENT_SCOPE_MEMBER_EXPR, UNARY_EXPR_OR_TYPE_TRAIT_EXPR,
				ARRAY_SUBSCRIPT_EXPR, UNARY_OPERATOR, CALL_EXPR, GNU_NULL_EXPR, CXX_THIS_EXPR
		});
		possibleStates.put(EXPR_WITH_CLEANUPS, new State[] {
				CXX_MEMBER_CALL_EXPR, CXX_OPERATOR_CALL_EXPR, CXX_NEW_EXPR, CXX_CONSTRUCT_EXPR,
				MATERIALIZE_TEMPORARY_EXPR, BINARY_OPERATOR, CONDITIONAL_OPERATOR, CXX_BIND_TEMPORARY_EXPR
		});
		possibleStates.put(CXX_MEMBER_CALL_EXPR, new State[] {
				CXX_MEMBER_CALL_EXPR, MEMBER_EXPR, UNARY_OPERATOR, IMPLICIT_CAST_EXPR, DECL_REF_EXPR,
				CXX_DEFAULT_ARG_EXPR, BINARY_OPERATOR, MATERIALIZE_TEMPORARY_EXPR, INTEGER_LITERAL,
				C_STYLE_CAST_EXPR, CALL_EXPR, CXX_BOOL_LITERAL_EXPR, CXX_CONSTRUCT_EXPR, PAREN_EXPR,
				CXX_THIS_EXPR, CONDITIONAL_OPERATOR
		});
		possibleStates.put(CXX_FUNCTIONAL_CAST_EXPR, new State[] {
				CXX_BIND_TEMPORARY_EXPR, IMPLICIT_CAST_EXPR, CXX_CONSTRUCT_EXPR, BINARY_OPERATOR
		});
		possibleStates.put(CXX_BIND_TEMPORARY_EXPR, new State[] {
				CXX_CONSTRUCT_EXPR, CXX_OPERATOR_CALL_EXPR, CXX_MEMBER_CALL_EXPR,
				IMPLICIT_CAST_EXPR, CXX_TEMPORARY_OBJECT_EXPR, CALL_EXPR
		});
		possibleStates.put(CXX_CONSTRUCT_EXPR, new State[] {
				IMPLICIT_CAST_EXPR, UNARY_EXPR_OR_TYPE_TRAIT_EXPR, DECL_REF_EXPR,
				MATERIALIZE_TEMPORARY_EXPR, CXX_BIND_TEMPORARY_EXPR, MEMBER_EXPR,
				CXX_MEMBER_CALL_EXPR, CXX_DEFAULT_ARG_EXPR, C_STYLE_CAST_EXPR, PAREN_EXPR
		});
		possibleStates.put(CXX_NEW_EXPR, new State[] {
				PAREN_LIST_EXPR, DECL_REF_EXPR, BINARY_OPERATOR, UNARY_OPERATOR,
				CXX_CONSTRUCT_EXPR, IMPLICIT_CAST_EXPR
		});
		possibleStates.put(CXX_PSEUDO_DESTRUCTOR_EXPR, new State[] {
				PAREN_EXPR, ARRAY_SUBSCRIPT_EXPR, DECL_REF_EXPR, MEMBER_EXPR
		});
		possibleStates.put(CXX_CONST_CAST_EXPR, new State[] {
				DECL_REF_EXPR, CXX_THIS_EXPR
		});
		possibleStates.put(CXX_REINTERPRET_CAST_EXPR, new State[] {
				IMPLICIT_CAST_EXPR, UNARY_OPERATOR
		});
		possibleStates.put(FOR_STMT, new State[] {
				DECL_STMT, NULL, BINARY_OPERATOR, UNARY_OPERATOR, COMPOUND_STMT,
				CXX_OPERATOR_CALL_EXPR, CALL_EXPR, CXX_MEMBER_CALL_EXPR
		});
		possibleStates.put(WHILE_STMT, new State[] {
				IMPLICIT_CAST_EXPR, NULL, BINARY_OPERATOR, UNARY_OPERATOR, COMPOUND_STMT,
				DECL_REF_EXPR, CALL_EXPR, CXX_MEMBER_CALL_EXPR
		});
		possibleStates.put(LINKAGE_SPEC_DECL, new State[] {
				FUNCTION_DECL, TYPEDEF_DECL, VAR_DECL,
				CXX_RECORD_DECL, NAMESPACE_DECL
		});
		possibleStates.put(MATERIALIZE_TEMPORARY_EXPR, new State[] {
				BINARY_OPERATOR, IMPLICIT_CAST_EXPR, CALL_EXPR, C_STYLE_CAST_EXPR, FLOATING_LITERAL, CXX_FUNCTIONAL_CAST_EXPR,
				CXX_BIND_TEMPORARY_EXPR, CXX_MEMBER_CALL_EXPR, CXX_THIS_EXPR, CXX_CONSTRUCT_EXPR, UNARY_OPERATOR
		});
		possibleStates.put(FRIEND_DECL, new State[] {
				FUNCTION_DECL, FULL_COMMENT, FUNCTION_TEMPLATE_DECL
		});
		possibleStates.put(UNARY_EXPR_OR_TYPE_TRAIT_EXPR, new State[] {
				PAREN_EXPR
		});
		possibleStates.put(CXX_TEMPORARY_OBJECT_EXPR, new State[] {
				IMPLICIT_CAST_EXPR, DECL_REF_EXPR, MATERIALIZE_TEMPORARY_EXPR, CXX_DEFAULT_ARG_EXPR
		});
		possibleStates.put(NON_TYPE_TEMPLATE_PARM_DECL, new State[] {
				INTEGER_LITERAL
		});
		possibleStates.put(UNRESOLVED_MEMBER_EXPR, new State[] {
				MEMBER_EXPR, IMPLICIT_CAST_EXPR, CXX_MEMBER_CALL_EXPR, DECL_REF_EXPR
		});
		possibleStates.put(CXX_STATIC_CAST_EXPR, new State[] {
				CXX_THIS_EXPR
		});
		possibleStates.put(ACCESS_SPEC_DECL, new State[]{
				FULL_COMMENT
		});
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
