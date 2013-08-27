package com.arcnor.objcclang.meta;

public abstract class GenericMetaMember implements Comparable<GenericMetaMember> {
	public String name;
	public String framework;
	public String docAbstract, docDiscussion;

	public GenericMetaMember(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int compareTo(GenericMetaMember o) {
		return name.compareTo(o.name);
	}
}
