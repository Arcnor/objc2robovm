package com.arcnor.objcclang.meta;

public class GenericMetaField extends GenericMetaMember {
	public final String type;
	public Object value;

	public GenericMetaField(final String name, final String type) {
		super(name);
		this.type = type;
	}
}
