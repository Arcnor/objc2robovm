package com.arcnor.objcclang.meta;

import java.util.*;

// FIXME: Stupid name
public abstract class AppleMetaMethodPropertyHolder extends AppleMetaMember {
	public List<AppleMetaMethod> constructors = new ArrayList<AppleMetaMethod>();
	public List<AppleMetaMethod> methods = new ArrayList<AppleMetaMethod>();
	public Map<String, AppleMetaProperty> properties = new HashMap<String, AppleMetaProperty>();
	public Set<AppleMetaProtocol> protocols = new HashSet<AppleMetaProtocol>();

	public AppleMetaMethodPropertyHolder(final String name) {
		super(name);
	}

	public void addMethod(AppleMetaMethod method) {
		if (method.name.startsWith("init")) {
			constructors.add(method);
		} else {
			methods.add(method);
		}
	}
}
