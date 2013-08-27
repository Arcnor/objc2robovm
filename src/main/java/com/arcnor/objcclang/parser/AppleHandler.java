package com.arcnor.objcclang.parser;

import com.arcnor.objcclang.gen.RoboVMClassGen;
import com.arcnor.objcclang.gen.RoboVMEnumGen;
import com.arcnor.objcclang.gen.RoboVMStructGen;
import com.arcnor.objcclang.meta.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class AppleHandler extends CLangHandler implements CLangParser {
	private String expectedFramework;
	private String framework;
	private boolean isFramework;

	private final Map<Long, AppleMetaMember> decls = new HashMap<Long, AppleMetaMember>();
	private final Map<String, AppleMetaMember> typedefs = new HashMap<String, AppleMetaMember>();
	private AppleMetaMember lastMetaMember;
	private AppleMetaMethod lastMetaMethod;
	private AppleMetaProperty lastMetaProperty;

	public AppleHandler(final String frameworkName) {
		super();
		expectedFramework = frameworkName;
	}

	@Override
	public void startDocument() {
	}

	@Override
	public void startElement(final String tag, final String content) {
		stateMachine.pushState(tag);

		switch (stateMachine.getState()) {
			case OBJ_C_INTERFACE_DECL: {
				createMember(AppleMetaInterface.class, content);
				break;
			}
			case OBJ_C_PROTOCOL_DECL: {
				createMember(AppleMetaProtocol.class, content);
				break;
			}
			case OBJ_C_CATEGORY_DECL: {
				createMember(AppleMetaCategory.class, content);
				break;
			}
			case RECORD_DECL: {
				createMember(AppleMetaRecord.class, content, !content.endsWith("struct"));
				break;
			}
			case ENUM_DECL: {
				createMember(AppleMetaEnum.class, content, !content.endsWith(">"));
				break;
			}

			// Records
			case FIELD_DECL: {
				String[] parts = splitNameType(content);
				AppleMetaField field = new AppleMetaField(parts[0], parts[1]);
				((AppleMetaRecord) lastMetaMember).fields.add(field);
				break;
			}
			// Enums
			case ENUM_CONSTANT_DECL: {
				String[] parts = splitNameType(content);
				AppleMetaField field = new AppleMetaField(parts[0], parts[1]);
				((AppleMetaEnum) lastMetaMember).fields.add(field);
				break;
			}
			// Records / Enums
			case TYPEDEF_DECL: {
				String[] parts = splitNameType(content);
				if ((lastMetaMember instanceof AppleMetaEnum || lastMetaMember instanceof AppleMetaRecord) && lastMetaMember.name == null) {
					lastMetaMember.name = parts[0];
				}
				if (stateMachine.getParentState() == State.TRANSLATION_UNIT_DECL) {
					typedefs.put(parts[0], lastMetaMember);
					lastMetaMember = null;
				}
				break;
			}
			// Interfaces
			case SUPER: {
				String[] parts = split(content, 0, 2);
				Long address = Long.decode(parts[1]);

				String name = parts[0];
				if (name.charAt(0) != '\'' || name.charAt(name.length() - 1) != '\'') {
					throw new RuntimeException("Expected name");
				}

				AppleMetaInterface parent = (AppleMetaInterface) getOrCreateMember(AppleMetaInterface.class, address, name.substring(1, name.length() - 1));

//				AppleMetaInterface lastParent = ((AppleMetaInterface) lastMetaMember).parent;
//				if (lastParent != null && lastParent != parent) {
//					throw new RuntimeException("Interface already has a parent, and is different than this one!");
//				}

				((AppleMetaInterface) lastMetaMember).parent = parent;
				break;
			}
			case OBJ_C_PROTOCOL: {
				String[] parts = split(content, 2, 0);
				Long address = Long.decode(parts[0]);

				String name = parts[1];
				if (name.charAt(0) != '\'') {
					throw new RuntimeException("Expected name");
				}

				AppleMetaProtocol protocol = (AppleMetaProtocol) getOrCreateMember(AppleMetaProtocol.class, address, name.substring(1, name.length()));

				((AppleMetaMethodPropertyHolder) lastMetaMember).protocols.add(protocol);
				break;
			}
			case OBJ_C_PROPERTY_DECL: {
				String[] parts = splitProperty(content);
				String name = parts[0];
				Map<String, AppleMetaProperty> properties = ((AppleMetaMethodPropertyHolder) lastMetaMember).properties;
				if (properties.containsKey(name)) {
					throw new RuntimeException("Duplicated property, something is wrong!");
				}
				lastMetaProperty = new AppleMetaProperty(name, parts[1], parts[2], parts[2].contains("readonly"));
				properties.put(name, lastMetaProperty);
				break;
			}
			// Functions / Methods
			case PARM_VAR_DECL: {
				if (lastMetaMethod != null) {
					String[] parts = splitNameType(content);
					lastMetaMethod.args.add(new AppleMetaField(parts[0], parts[1]));
				}
				break;
			}
			// Interface / Protocol / Category
			case OBJ_C_METHOD_DECL: {
				String[] parts = splitMethodNameType(content);
				String methodType = parts[0];
				lastMetaMethod = new AppleMetaMethod(parts[1], parts[2], methodType.charAt(0));
				((AppleMetaMethodPropertyHolder) lastMetaMember).addMethod(lastMetaMethod);
				break;
			}
			// Properties
			case GETTER: {
				String[] parts = split(content, 0, 1);
				// FIXME: Check quoted name
				lastMetaProperty.getter = parts[0].substring(1, parts[0].length() - 1);
				break;
			}
			case SETTER: {
				String[] parts = split(content, 0, 1);
				// FIXME: Check quoted name
				lastMetaProperty.setter = parts[0].substring(1, parts[0].length() - 1);
				break;
			}
			// Everything
			case INTEGER_LITERAL: {
				switch (stateMachine.getParentState()) {
					case ENUM_CONSTANT_DECL:
						String[] parts = split(content, 0, 1);
						List<AppleMetaField> fields = ((AppleMetaEnum) lastMetaMember).fields;
						fields.get(fields.size() - 1).value = Long.valueOf(parts[0]);
						break;
				}

				break;
			}
		}
	}

	private AppleMetaMember getOrCreateMember(final Class<? extends AppleMetaMember> clazz, final Long address, final String name) {
		AppleMetaMember member = decls.get(address);

		if (member == null) {
			member = createMemberFromClass(clazz, name);
			decls.put(address, member);
		}

		return member;
	}

	private void createMember(final Class<? extends AppleMetaMember> clazz, final String content) {
		createMember(clazz, content, true);
	}

	private void createMember(final Class<? extends AppleMetaMember> clazz, final String content, final boolean hasName) {
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
			AppleMetaMember member = createMemberFromClass(clazz, name);
			addMemberDecl(member, address, prevAddress);
		}

		// Add framework
		if (lastMetaMember.framework == null && isFramework) {
			lastMetaMember.framework = framework;
		}
	}

	private static final Map<Class<? extends AppleMetaMember>, Constructor<? extends AppleMetaMember>> constructors = new HashMap<Class<? extends AppleMetaMember>, Constructor<? extends AppleMetaMember>>();

	private AppleMetaMember createMemberFromClass(Class<? extends AppleMetaMember> clazz, String name) {
		Constructor<? extends AppleMetaMember> constructor;
		try {
			constructor = constructors.get(clazz);
			if (constructor == null) {
				constructor = clazz.getConstructor(String.class);
			}
			return constructor.newInstance(name);
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

	private void addMemberDecl(final AppleMetaMember member, final Long address, final Long prevAddress) {
		lastMetaMember = member;
		if (prevAddress != null) {
			lastMetaMember = decls.get(prevAddress);
		}
		decls.put(address, lastMetaMember);
	}

	@Override
	public void endElement(final String tag) {
		switch (stateMachine.getState()) {
			case OBJ_C_METHOD_DECL:
				lastMetaMethod = null;
				break;
			case OBJ_C_PROPERTY_DECL:
				lastMetaProperty = null;
				break;
		}
		stateMachine.popState(tag);
	}

	@Override
	public void endDocument() {
		Map<String, AppleMetaMember> interfaces = new HashMap<String, AppleMetaMember>();
		Map<String, AppleMetaMember> protocols = new HashMap<String, AppleMetaMember>();
		for (AppleMetaMember metaMember : decls.values()) {
			if (metaMember.name == null) {
				continue;
			}
			if (metaMember instanceof AppleMetaInterface) {
				addNamedMember(interfaces, metaMember);
			} else if (metaMember instanceof AppleMetaProtocol) {
				addNamedMember(protocols, metaMember);
			}
		}

		final Set<AppleMetaMember> parsed = new HashSet<AppleMetaMember>();
		for (AppleMetaMember appleMetaMember : decls.values()) {
			if (parsed.contains(appleMetaMember)) {
				continue;
			}
			parsed.add(appleMetaMember);
			if (!expectedFramework.equals(appleMetaMember.framework)) {
				continue;
			}
			if (appleMetaMember instanceof AppleMetaMethodPropertyHolder) {
				if (appleMetaMember instanceof AppleMetaInterface) {
					AppleMetaInterface metaInterface = (AppleMetaInterface) appleMetaMember;
					if (metaInterface.parent == null && metaInterface.methods.isEmpty() && metaInterface.properties.isEmpty() && metaInterface.protocols.isEmpty()) {
						continue;
					}
					RoboVMClassGen gen = new RoboVMClassGen(metaInterface, interfaces, protocols, typedefs);
					System.out.println("***");
					System.out.println(gen.getOutput());
					System.out.println("***");
				}
			} else if (appleMetaMember instanceof AppleMetaEnum) {
				RoboVMEnumGen gen = new RoboVMEnumGen((AppleMetaEnum) appleMetaMember, interfaces, protocols, typedefs);
				System.out.println("***");
				System.out.println(gen.getOutput());
				System.out.println("***");
			} else if (appleMetaMember instanceof AppleMetaRecord) {
				RoboVMStructGen gen = new RoboVMStructGen((AppleMetaRecord) appleMetaMember, interfaces, protocols, typedefs);
				System.out.println("***");
				System.out.println(gen.getOutput());
				System.out.println("***");
			}
		}
	}

	private void addNamedMember(Map<String, AppleMetaMember> members, AppleMetaMember metaMember) {
		AppleMetaMember existing = members.get(metaMember.name);
		if (existing != null && existing != metaMember) {
			throw new RuntimeException("A member with the same name already exists! (" + metaMember.name + ")");
		}
		members.put(metaMember.name, metaMember);
	}

	@Override
	public void setFramework(final String framework, boolean isFramework) {
		this.framework = framework.intern();
		this.isFramework = isFramework;
	}
}
