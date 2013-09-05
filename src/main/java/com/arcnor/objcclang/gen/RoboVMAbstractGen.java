package com.arcnor.objcclang.gen;

import com.arcnor.objcclang.meta.GenericMetaMember;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class RoboVMAbstractGen<T extends GenericMetaMember> extends AbstractJavaGen<T, String> {
	private final Map<String, GenericMetaMember> protocolDecls;

	protected RoboVMAbstractGen(T metaMember, Map<String, GenericMetaMember> memberDecls, Map<String, GenericMetaMember> protocolDecls, Map<String, GenericMetaMember> typedefs) {
		super("org.robovm.cocoatouch", metaMember, memberDecls, typedefs);

		this.protocolDecls = protocolDecls;
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

	private static final Pattern patProtocol = Pattern.compile("(\\w+)\\s*<(.*)>");

	protected String clang2javatypeBasic(String type) {
		if ("void".equals(type)) return type;
		if ("float".equals(type)) return type;
		if ("double".equals(type)) return type;
		if ("long".equals(type)) return type;
		if ("int".equals(type)) return type;

		return null;
	}

	@Override
	protected String clang2javatypeCustom(String type, String refType) {
		if (type.equals(metaMember.name)) {
			return type;
		}

		if (type.equals("BOOL")) return "boolean";

		// Basic types
		String result = clang2javatypeBasic(type);
		if (result != null) {
			return result;
		}

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
				sb.append("? extends ").append(clang2javatype(type));
				String[] protocols = matcher.group(2).split(",");
				for (int j = 0; j < protocols.length; j++) {
					sb.append(" & ").append(protocol2javatype(protocols[j].trim()));
				}
				return sb.toString();
			} else {
				GenericMetaMember member = addMemberUsage(type, refType);
				return member != null ? fullyQualify(member) : type;
			}
		}
	}

	private String protocol2javatype(String type) {
		GenericMetaMember member = addMemberProtocolUsage(type);
		return member != null ? fullyQualify(member) : type;
	}

	protected GenericMetaMember addMemberProtocolUsage(String type) {
		GenericMetaMember member;
		if (protocolDecls.containsKey(type)) {
			member = protocolDecls.get(type);
		} else {
			throw new RuntimeException("Unknown protocol: " + type);
		}
		return addMemberUsage(member);
	}

	// FIXME: Bad signature (qualifyAsProtocols), but we're working on it...
	protected void joinNames(Collection<? extends GenericMetaMember> members, boolean qualifyAsProtocols) {
		boolean first = true;

		final Set<String> argNames = new HashSet<String>();
		int idx = 1;

		for (GenericMetaMember member : members) {
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
}
