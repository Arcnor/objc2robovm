package com.arcnor.objcclang.meta;

import java.util.ArrayList;
import java.util.List;

public class AppleMetaRecord extends AppleMetaMember {
	public List<AppleMetaField> fields = new ArrayList<AppleMetaField>();

	public AppleMetaRecord(final String name) {
		super(name);
	}
}
