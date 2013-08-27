package com.arcnor.objcclang.meta;

import java.util.ArrayList;
import java.util.List;

public class GenericMetaEnum extends GenericMetaMember {
	public List<GenericMetaField> fields = new ArrayList<GenericMetaField>();

	public GenericMetaEnum(final String name) {
		super(name);
	}
}
