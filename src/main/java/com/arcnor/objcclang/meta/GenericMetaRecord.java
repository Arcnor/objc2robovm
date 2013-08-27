package com.arcnor.objcclang.meta;

import java.util.ArrayList;
import java.util.List;

public class GenericMetaRecord extends GenericMetaMember {
	public List<GenericMetaField> fields = new ArrayList<GenericMetaField>();

	public GenericMetaRecord(final String name) {
		super(name);
	}
}
