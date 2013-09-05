package com.arcnor.objcclang.gen;

import com.arcnor.objcclang.meta.GenericMetaMember;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractJavaGen<T extends GenericMetaMember, U> extends AbstractGen<T, U> {
	private final String packagePrefix;

	protected AbstractJavaGen(String packagePrefix, T metaMember, Map<String, ? extends GenericMetaMember> memberDecls, Map<String, ? extends GenericMetaMember> typedefs) {
		super(metaMember, memberDecls, typedefs);

		this.packagePrefix = packagePrefix;
	}

	@Override
	final void generateOutput() {
		// First, generate body declarations so we get hold of the used members
		generateBodyDecl();
		String body = sb.toString();
		sb.setLength(0);
		generatePackageDecl();
		generateUsedImports();
		generateImports();
		sb.append(body);
	}

	protected abstract void generateImports();
	protected abstract void generateBodyDecl();

	private void generatePackageDecl() {
		_("package ")._(packagePrefix)._('.')._(metaMember.library.toLowerCase())._(';')._nl();
		_nl();
	}

	private void generateUsedImports() {
		if (usedMembers.isEmpty()) {
			return;
		}

		ArrayList<GenericMetaMember> members = new ArrayList<GenericMetaMember>(usedMembers.values());
		Collections.sort(members);

		boolean added = false;
		for (GenericMetaMember member : members) {
			if (!member.library.equalsIgnoreCase(metaMember.library)) {
				added = true;
				_("import ")._(packagePrefix)._('.')._(member.library.toLowerCase())._('.')._(member.name)._(';')._nl();
			}
		}
		if (added) {
			_nl();
		}
	}

	protected void addDoc(GenericMetaMember member) {
		_("/**")._nl();
		if (member.docAbstract != null) {
			_(" * ")._(member.docAbstract)._nl();
			if (member.docDiscussion != null) {
				_(" *")._nl();
			}
		}
		if (member.docDiscussion != null) {
			_(" * ")._(member.docDiscussion)._nl();
		}
		_(" */")._nl();
	}

	private final StringBuilder genericsSb = new StringBuilder();

	protected String processGenerics(LinkedHashMap<String, String> types) {
		if (types == null || types.isEmpty()) {
			return "";
		}

		genericsSb.setLength(0);
		char generic = 'T';
		for (Map.Entry<String, String> pair : types.entrySet()) {
			String value = pair.getValue();
			if (value.contains("?")) {
				genericsSb.append(value.replace('?', generic)).append(", ");
				pair.setValue(String.valueOf(generic++));
			}
			if (generic == 'T') {
				generic = 'A';
			}
		}

		if (genericsSb.length() > 0) {
			genericsSb.insert(0, "<");
			genericsSb.replace(genericsSb.length() - 2, genericsSb.length(), "> ");
		}
		return genericsSb.toString();
	}

	protected String fullyQualify(GenericMetaMember member) {
		return packagePrefix + '.' + member.name;
	}
}
