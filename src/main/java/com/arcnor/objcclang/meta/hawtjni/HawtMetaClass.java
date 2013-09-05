package com.arcnor.objcclang.meta.hawtjni;

import com.arcnor.objcclang.meta.GenericMetaMember;
import com.arcnor.objcclang.meta.GenericMetaMethod;

import java.util.ArrayList;
import java.util.List;

public class HawtMetaClass extends GenericMetaMember {
	public List<GenericMetaMethod> functions = new ArrayList<GenericMetaMethod>();

	public HawtMetaClass(String name) {
		super(name);
	}
}
