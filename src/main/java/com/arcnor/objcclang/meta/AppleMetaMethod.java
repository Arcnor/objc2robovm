package com.arcnor.objcclang.meta;

import java.util.ArrayList;
import java.util.List;

public class AppleMetaMethod extends AppleMetaMember {
	public final String type;
	public final List<AppleMetaField> args = new ArrayList<AppleMetaField>();
	public final char accessType;

	public AppleMetaMethod(final String name, final String type, final char accessType) {
		super(name);
		this.type = type;
		this.accessType = accessType;
	}
}
