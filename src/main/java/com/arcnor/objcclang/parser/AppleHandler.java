package com.arcnor.objcclang.parser;

import com.arcnor.objcclang.gen.RoboVMClassGen;
import com.arcnor.objcclang.gen.RoboVMEnumGen;
import com.arcnor.objcclang.gen.RoboVMStructGen;
import com.arcnor.objcclang.meta.*;

import java.util.*;

public class AppleHandler extends CLangHandler implements CLangParser {
	private String expectedFramework;

	private final Map<String, GenericMetaMember> typedefs = new HashMap<String, GenericMetaMember>();
	private GenericMetaMethod lastMetaMethod;
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
				createMember(GenericMetaRecord.class, content, !content.endsWith("struct"));
				break;
			}
			case ENUM_DECL: {
				createMember(GenericMetaEnum.class, content, !content.endsWith(">"));
				break;
			}

			// Records
			case FIELD_DECL: {
				String[] parts = splitNameType(content);
				GenericMetaField field = new GenericMetaField(parts[0], parts[1]);
				((GenericMetaRecord) lastMetaMember).fields.add(field);
				break;
			}
			// Enums
			case ENUM_CONSTANT_DECL: {
				String[] parts = splitNameType(content);
				GenericMetaField field = new GenericMetaField(parts[0], parts[1]);
				((GenericMetaEnum) lastMetaMember).fields.add(field);
				break;
			}
			// Records / Enums
			case TYPEDEF_DECL: {
				String[] parts = splitNameType(content);
				if ((lastMetaMember instanceof GenericMetaEnum || lastMetaMember instanceof GenericMetaRecord) && lastMetaMember.name == null) {
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

				AppleMetaInterface parent = getOrCreateMember(AppleMetaInterface.class, address, name.substring(1, name.length() - 1));

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

				AppleMetaProtocol protocol = getOrCreateMember(AppleMetaProtocol.class, address, name.substring(1, name.length()));

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
					lastMetaMethod.args.add(new GenericMetaField(parts[0], parts[1]));
				}
				break;
			}
			// Interface / Protocol / Category
			case OBJ_C_METHOD_DECL: {
				String[] parts = splitMethodNameType(content);
				String methodType = parts[0];
				lastMetaMethod = new GenericMetaMethod(parts[1], parts[2], methodType.charAt(0));
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
						List<GenericMetaField> fields = ((GenericMetaEnum) lastMetaMember).fields;
						fields.get(fields.size() - 1).value = Long.valueOf(parts[0]);
						break;
				}

				break;
			}
		}
	}

	@Override
	protected <T extends GenericMetaMember> void createMember(Class<T> clazz, String content, boolean hasName) {
		super.createMember(clazz, content, hasName);

		// Add framework
		if (lastMetaMember.library == null && isFramework) {
			lastMetaMember.library = library;
		}
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
		Map<String, GenericMetaMember> interfaces = new HashMap<String, GenericMetaMember>();
		Map<String, GenericMetaMember> protocols = new HashMap<String, GenericMetaMember>();
		for (GenericMetaMember metaMember : decls.values()) {
			if (metaMember.name == null) {
				continue;
			}
			if (metaMember instanceof AppleMetaInterface) {
				addNamedMember(interfaces, metaMember);
			} else if (metaMember instanceof AppleMetaProtocol) {
				addNamedMember(protocols, metaMember);
			}
		}

		final Set<GenericMetaMember> parsed = new HashSet<GenericMetaMember>();
		for (GenericMetaMember decl : decls.values()) {
			if (parsed.contains(decl)) {
				continue;
			}
			parsed.add(decl);
			if (!expectedFramework.equals(decl.library)) {
				continue;
			}
			if (decl instanceof AppleMetaMethodPropertyHolder) {
				if (decl instanceof AppleMetaInterface) {
					AppleMetaInterface metaInterface = (AppleMetaInterface) decl;
					if (metaInterface.parent == null && metaInterface.methods.isEmpty() && metaInterface.properties.isEmpty() && metaInterface.protocols.isEmpty()) {
						continue;
					}
					RoboVMClassGen gen = new RoboVMClassGen(metaInterface, interfaces, protocols, typedefs);
					System.out.println("***");
					System.out.println(gen.getOutput());
					System.out.println("***");
				}
			} else if (decl instanceof GenericMetaEnum) {
				RoboVMEnumGen gen = new RoboVMEnumGen((GenericMetaEnum) decl, interfaces, protocols, typedefs);
				System.out.println("***");
				System.out.println(gen.getOutput());
				System.out.println("***");
			} else if (decl instanceof GenericMetaRecord) {
				RoboVMStructGen gen = new RoboVMStructGen((GenericMetaRecord) decl, interfaces, protocols, typedefs);
				System.out.println("***");
				System.out.println(gen.getOutput());
				System.out.println("***");
			}
		}
	}

	private void addNamedMember(Map<String, GenericMetaMember> members, GenericMetaMember metaMember) {
		GenericMetaMember existing = members.get(metaMember.name);
		if (existing != null && existing != metaMember) {
			throw new RuntimeException("A member with the same name already exists! (" + metaMember.name + ")");
		}
		members.put(metaMember.name, metaMember);
	}
}
