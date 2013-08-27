package com.arcnor.objcclang.meta;

import java.util.ArrayList;
import java.util.List;

public class GenericMetaMethod extends GenericMetaMember {
	public final String type;
	public final List<GenericMetaField> args = new ArrayList<GenericMetaField>();
	public final char accessType;

	public GenericMetaMethod(final String name, final String type, final char accessType) {
		super(name);
		this.type = type;
		this.accessType = accessType;
	}
}
