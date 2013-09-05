package com.arcnor.objcclang.gen;

import com.arcnor.objcclang.meta.GenericMetaMember;
import com.arcnor.objcclang.meta.GenericMetaField;

import java.util.*;

public abstract class AbstractGen<T extends GenericMetaMember, U> {
	public static final String JOINER_NAME = ", ";
	StringBuilder sb;

	private static final String SOFT_INDENT = "    ";
	private String indent = "";
	private boolean startLine = true;
	protected final T metaMember;

	private final Map<String, ? extends GenericMetaMember> memberDecls;
	private final Map<String, ? extends GenericMetaMember> typedefs;
	protected Map<String, GenericMetaMember> usedMembers = new HashMap<String, GenericMetaMember>();

	protected AbstractGen(T metaMember, Map<String, ? extends GenericMetaMember> memberDecls, Map<String, ? extends GenericMetaMember> typedefs) {
		this.metaMember = metaMember;
		this.memberDecls = memberDecls;
		this.typedefs = typedefs;
	}

	public String getOutput() {
		if (sb == null) {
			sb = new StringBuilder();
			generateOutput();
		}
		return sb.toString();
	}

	abstract void generateOutput();

	protected LinkedHashMap<String, U> clang2javatypeMap(Collection<GenericMetaField> arguments) {
		if (arguments == null || arguments.isEmpty()) {
			return null;
		}
		LinkedHashMap<String, U> result = new LinkedHashMap<String, U>();
		int idx = 1;
		for (GenericMetaField argument : arguments) {
			String name = argument.name;
			if (result.containsKey(name)) {
				System.err.println("WARNING: Duplicated argument '" + argument.name + "'");
				name = name + (idx++);
			}
			U type = clang2javatype(argument.type);
			result.put(name, type);
		}
		return result;
	}

	protected abstract U clang2javatypeCustom(String type, String refType);

	protected U clang2javatype(String type) {
		if (type == null) {
			return null;
		}
		String refType = null;
		if (type.contains(":")) {
			int colonIdx = type.indexOf(':');
			refType = type.substring(0, colonIdx - 1);
			type = type.substring(colonIdx + 2);
		}
		if (type.startsWith("struct ")) {
			type = type.substring(7);
		} else if (type.startsWith("enum ")) {
			type = type.substring(5);
		}

		return clang2javatypeCustom(type, refType);
	}

	protected GenericMetaMember addMemberUsage(String type, String refType) {
		GenericMetaMember member = null;
		if (memberDecls.containsKey(type)) {
			member = memberDecls.get(type);
		} else if (typedefs.containsKey(type)) {
			member = typedefs.get(type);
		} else if (refType != null && typedefs.containsKey(refType)) {
			member = typedefs.get(refType);
		}
		if (member == null) {
			throw new RuntimeException("Unknown type: " + type);
		}
		return addMemberUsage(member);
	}

	// FIXME: This should be private!
	protected GenericMetaMember addMemberUsage(GenericMetaMember member) {
		// FIXME: Check that members are also equals (framework, type, whatever...)
		if (usedMembers.containsKey(member.name) && usedMembers.get(member.name) != member) {
			// Member is already used somewhere else, we need to fully qualify it!
			return member;
		}
		usedMembers.put(member.name, member);
		return null;
	}

	protected void joinNameTypes(LinkedHashMap<String, U> args) {
		if (args == null || args.isEmpty()) {
			return;
		}
		boolean first = true;
		final Set<String> argNames = new HashSet<String>();
		int idx = 1;
		for (Map.Entry<String, U> arg : args.entrySet()) {
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
			_(arg.getValue() != null ? arg.getValue().toString() : "UNKNOWN")._(' ')._(name);
		}
	}

	protected void joinNames(Collection<? extends GenericMetaMember> members) {
		boolean first = true;

		final Set<String> argNames = new HashSet<String>();
		int idx = 1;

		for (GenericMetaMember member : members) {
			if (first) {
				first = false;
			} else {
				_(JOINER_NAME);
			}

			String name = member.name;
			if (argNames.contains(name)) {
				name += idx++;
			}
			_(name);
			argNames.add(name);
		}
	}

	// String generators //

	protected AbstractGen _indent() {
		indent += '\t';
		return this;
	}

	protected AbstractGen _indentEnd() {
		indent = indent.substring(0, indent.length() - 1);
		return this;
	}

	protected AbstractGen _brace() {
		applyIndent();
		sb.append("{");
		_nl();
		_indent();
		return this;
	}

	protected AbstractGen _braceEnd() {
		_indentEnd();
		applyIndent();
		sb.append("}");
		return this;
	}

	protected AbstractGen _softIndent() {
		applyIndent();
		sb.append(SOFT_INDENT);
		return this;
	}

	protected AbstractGen _nl() {
		applyIndent();
		sb.append('\n');
		startLine = true;
		return this;
	}

	protected AbstractGen _(String str) {
		applyIndent();
		sb.append(str);
		return this;
	}

	protected AbstractGen _(char ch) {
		applyIndent();
		sb.append(ch);
		return this;
	}

	protected AbstractGen _(int num) {
		applyIndent();
		sb.append(num);
		return this;
	}

	private void applyIndent() {
		if (startLine) {
			startLine = false;
			sb.append(indent);
		}
	}
}
