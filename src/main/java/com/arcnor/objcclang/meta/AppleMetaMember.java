package com.arcnor.objcclang.meta;

public class AppleMetaMember implements Comparable<AppleMetaMember> {
	public String name;
	public String framework;
	public String docAbstract, docDiscussion;

	public AppleMetaMember(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int compareTo(AppleMetaMember o) {
		return name.compareTo(o.name);
	}
}
