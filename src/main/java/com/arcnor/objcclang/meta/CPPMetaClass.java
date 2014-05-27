package com.arcnor.objcclang.meta;

import java.util.ArrayList;
import java.util.List;

public class CPPMetaClass extends GenericMetaRecord {
	public String parent;
	public List<GenericMetaMethod> constructors = new ArrayList<GenericMetaMethod>();
	public List<GenericMetaMethod> methods = new ArrayList<GenericMetaMethod>();

	public CPPMetaClass(final String name) {
		super(name);
	}
}
