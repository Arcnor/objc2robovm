package com.arcnor.objcclang.meta;

import com.arcnor.objcclang.gen.Utils;

public class AppleMetaProperty extends GenericMetaField {
	public final String modifiers;
	private final boolean readOnly;
	public String getter;
	public String setter;

	public AppleMetaProperty(final String name, final String type, final String modifiers, final boolean readOnly) {
		super(name, type);
		this.modifiers = modifiers;
		this.readOnly = readOnly;
		String capName = Utils.capitalize(name);
		this.getter = "get" + capName;
		this.setter = "set" + capName + ':';
	}
}
