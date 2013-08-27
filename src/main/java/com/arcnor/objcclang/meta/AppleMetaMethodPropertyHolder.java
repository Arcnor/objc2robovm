package com.arcnor.objcclang.meta;

import java.util.*;

// FIXME: Stupid name
public abstract class AppleMetaMethodPropertyHolder extends GenericMetaMember {
	public List<GenericMetaMethod> constructors = new ArrayList<GenericMetaMethod>();
	public List<GenericMetaMethod> methods = new ArrayList<GenericMetaMethod>();
	public Map<String, AppleMetaProperty> properties = new HashMap<String, AppleMetaProperty>();
	public Set<AppleMetaProtocol> protocols = new HashSet<AppleMetaProtocol>();

	public AppleMetaMethodPropertyHolder(final String name) {
		super(name);
	}

	public void addMethod(GenericMetaMethod method) {
		if (method.name.startsWith("init")) {
			constructors.add(method);
		} else {
			methods.add(method);
		}
	}
}
