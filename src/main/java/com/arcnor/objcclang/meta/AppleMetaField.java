package com.arcnor.objcclang.meta;

public class AppleMetaField extends AppleMetaMember {
	public final String type;
	public Object value;

	public AppleMetaField(final String name, final String type) {
		super(name);
		this.type = type;
	}
}
