package com.arcnor.objcclang.gen;

import com.arcnor.objcclang.meta.AppleMetaField;
import com.arcnor.objcclang.meta.AppleMetaMember;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractGen<T extends AppleMetaMember> {
	private static String packagePrefix = "org.robovm.cocoatouch";
	private StringBuilder sb;

	private static final String SOFT_INDENT = "    ";
	private String indent = "";
	private boolean startLine = true;
	protected final T metaMember;

	private final Map<String, AppleMetaMember> memberDecls;
	private final Map<String, AppleMetaMember> protocolDecls;
	private final Map<String, AppleMetaMember> typedefs;
	protected Map<String, AppleMetaMember> usedMembers = new HashMap<String, AppleMetaMember>();

	protected AbstractGen(T metaMember, Map<String, AppleMetaMember> memberDecls, Map<String, AppleMetaMember> protocolDecls, Map<String, AppleMetaMember> typedefs) {
		this.metaMember = metaMember;
		this.memberDecls = memberDecls;
		this.protocolDecls = protocolDecls;
		this.typedefs = typedefs;
	}

	public String getOutput() {
		if (sb == null) {
			sb = new StringBuilder();
			generateOutput();
		}
		return sb.toString();
	}

	protected abstract void generateImports();
	protected abstract void generateBodyDecl();

	private void generateOutput() {
		// First, generate body declarations so we get hold of the used members
		generateBodyDecl();
		String body = sb.toString();
		sb.setLength(0);
		generatePackageDecl();
		generateUsedImports();
		generateImports();
		sb.append(body);
	}

	private void generatePackageDecl() {
		_("package ")._(packagePrefix)._('.')._(metaMember.framework.toLowerCase())._(';')._nl();
		_nl();
	}

	private void generateUsedImports() {
		if (usedMembers.isEmpty()) {
			return;
		}

		ArrayList<AppleMetaMember> members = new ArrayList<AppleMetaMember>(usedMembers.values());
		Collections.sort(members);

		boolean added = false;
		for (AppleMetaMember member : members) {
			if (!member.framework.equalsIgnoreCase(metaMember.framework)) {
				added = true;
				_("import ")._(packagePrefix)._('.')._(member.framework.toLowerCase())._('.')._(member.name)._(';')._nl();
			}
		}
		if (added) {
			_nl();
		}
	}

	StringBuilder msgSendSb = new StringBuilder();

	protected String processGenerics(LinkedHashMap<String, String> types) {
		if (types == null || types.isEmpty()) {
			return "";
		}

		msgSendSb.setLength(0);
		char generic = 'T';
		for (Map.Entry<String, String> pair : types.entrySet()) {
			String value = pair.getValue();
			if (value.contains("?")) {
				msgSendSb.append(value.replace('?', generic)).append(", ");
				pair.setValue(String.valueOf(generic++));
			}
			if (generic == 'T') {
				generic = 'A';
			}
		}

		if (msgSendSb.length() > 0) {
			msgSendSb.insert(0, "<");
			msgSendSb.replace(msgSendSb.length() - 2, msgSendSb.length(), "> ");
		}
		return msgSendSb.toString();
	}

	protected String msgSend(String simpleName, String type, boolean superCall, String generics, LinkedHashMap<String, String> arguments) {
		_("@Bridge private native static ")._(generics)._(type)._(" objc_")._(simpleName);
		if (superCall) {
			_("Super(ObjCSuper __super__");
		} else {
			_('(')._(metaMember.name)._(" __self__");
		}
		_(", Selector __cmd__");

		if (arguments != null && !arguments.isEmpty()) {
			_(", ");
			joinNameTypes(arguments);
		}
		_(");")._nl();

		return simpleName;
	}

	protected String registerSelector(final String name) {
		final String selectorName = name.replace(':', '$');
		_("private static final Selector ")._(selectorName)._(" = Selector.register(\"")._(name)._("\");")._nl();

		return selectorName;
	}

	protected void addDoc(AppleMetaMember member) {
		_("/**")._nl();
		if (member.docAbstract != null) {
			_(" * ")._(member.docAbstract)._nl();
			if (member.docDiscussion != null) {
				_(" *")._nl();
			}
		}
		if (member.docDiscussion != null) {
			_(" * ")._(member.docDiscussion)._nl();
		}
		_(" */")._nl();
	}

	protected LinkedHashMap<String, String> objc2javatypeMap(Collection<AppleMetaField> arguments) {
		if (arguments == null || arguments.isEmpty()) {
			return null;
		}
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		int idx = 1;
		for (AppleMetaField argument : arguments) {
			String name = argument.name;
			if (result.containsKey(name)) {
				System.err.println("WARNING: Duplicated argument '" + argument.name + "'");
				name = name + (idx++);
			}
			String type = objc2javatype(argument.type);
			result.put(name, type);
		}
		return result;
	}

	private static final Pattern patProtocol = Pattern.compile("(\\w+)\\s*<(.*)>");

	protected String objc2javatype(String type) {
		if (type == null) {
			return type;
		}
		if (type.equals(metaMember.name)) {
			return type;
		}
		if (type.equals("BOOL") || type.startsWith("BOOL':")) return "boolean";
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

		// Basic types
		if ("void".equals(type)) return type;
		if ("float".equals(type)) return type;
		if ("double".equals(type)) return type;
		if ("long".equals(type)) return type;
		if ("int".equals(type)) return type;

		// Basic type pointers
		if ("NSInteger *".equals(type)) return "IntPtr";
		if ("NSUInteger *".equals(type)) return "IntPtr";
		if ("CGFloat *".equals(type)) return "FloatPtr";
		if ("unichar *".equals(type)) return "CharPtr";
		if ("void *".equals(type)) return "VoidPtr";

		// Not a basic type, check the rest without a pointer
		if (type.endsWith("*")) {
			type = type.substring(0, type.length() - 1);
		}
		type = type.trim();

		if ("id".equals(type)) return "NSObject";
		if ("Class".equals(type)) return "ObjCClass";
		if ("NSString".equals(type)) return "String";
		if ("char".equals(type)) return "byte";
		if ("unsigned char".equals(type)) return "byte";
		if ("long long".equals(type)) return "long";
		if ("unsigned long long".equals(type)) return "long";
		if ("unsigned short".equals(type)) return "char";
		if ("unsigned int".equals(type)) return "int";
		if ("unsigned long".equals(type)) return "int";
		if ("unsigned".equals(type)) return "int";
		if ("SEL".equals(type)) return "Selector";

		// Blocks
		if ("void (^)(void)".equals(type)) return "VoidBlock";
		if ("void (^)(BOOL)".equals(type)) return "VoidBooleanBlock";

		if (type.endsWith("*")) {
			String realType = type.substring(0, type.length() - 1).trim();
			addMemberUsage(realType, refType);
			type = "Ptr<" + realType + '>';
			return type;
		} else {
			Matcher matcher = patProtocol.matcher(type);
			if (matcher.matches()) {
				type = matcher.group(1).trim();
				StringBuilder sb = new StringBuilder();
				sb.append("? extends ").append(objc2javatype(type));
				String[] protocols = matcher.group(2).split(",");
				for (int j = 0; j < protocols.length; j++) {
					sb.append(" & ").append(protocol2javatype(protocols[j].trim()));
				}
				return sb.toString();
			} else {
				AppleMetaMember member = addMemberUsage(type, refType);
				return member != null ? fullyQualify(member) : type;
			}
		}
	}

	private String protocol2javatype(String type) {
		AppleMetaMember member = addMemberProtocolUsage(type);
		return member != null ? fullyQualify(member) : type;
	}

	protected String fullyQualify(AppleMetaMember member) {
		return packagePrefix + '.' + member.name;
	}

	protected AppleMetaMember addMemberProtocolUsage(String type) {
		AppleMetaMember member;
		if (protocolDecls.containsKey(type)) {
			member = protocolDecls.get(type);
		} else {
			throw new RuntimeException("Unknown protocol: " + type);
		}
		return addMemberUsage(member);
	}

	protected AppleMetaMember addMemberUsage(String type, String refType) {
		AppleMetaMember member = null;
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

	private AppleMetaMember addMemberUsage(AppleMetaMember member) {
		// FIXME: Check that members are also equals (framework, type, whatever...)
		if (usedMembers.containsKey(member.name) && usedMembers.get(member.name) != member) {
			// Member is already used somewhere else, we need to fully qualify it!
			return member;
		}
		usedMembers.put(member.name, member);
		return null;
	}

	protected void joinNameTypes(LinkedHashMap<String, String> args) {
		if (args == null || args.isEmpty()) {
			return;
		}
		boolean first = true;
		final Set<String> argNames = new HashSet<String>();
		int idx = 1;
		for (Map.Entry<String, String> arg : args.entrySet()) {
			if (first) {
				first = false;
			} else {
				_(", ");
			}

			String name = arg.getKey();
			if (argNames.contains(name)) {
				name += idx++;
			}
			argNames.add(name);
			_(arg.getValue())._(' ')._(name);
		}
	}

	// FIXME: Bad signature (qualifyAsProtocols), but we're working on it...
	protected void joinNames(Collection<? extends AppleMetaMember> members, boolean qualifyAsProtocols) {
		boolean first = true;

		final Set<String> argNames = new HashSet<String>();
		int idx = 1;

		for (AppleMetaMember member : members) {
			if (first) {
				first = false;
			} else {
				_(", ");
			}
			if (!qualifyAsProtocols) {
				String name = member.name;
				if (argNames.contains(name)) {
					name += idx++;
				}
				_(name);
				argNames.add(name);
			} else {
				String type = protocol2javatype(member.name);
				_(type);
			}
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
