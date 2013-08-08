package com.arcnor.objcclang.meta;

import java.util.ArrayList;
import java.util.List;

public class AppleMetaEnum extends AppleMetaMember {
	public List<AppleMetaField> fields = new ArrayList<AppleMetaField>();

	public AppleMetaEnum(final String name) {
		super(name);
	}
}
