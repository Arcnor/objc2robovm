package com.arcnor.objcclang.parser;

import com.arcnor.objcclang.gen.SWIGGen;
import com.arcnor.objcclang.meta.CPPMetaClass;
import com.arcnor.objcclang.meta.GenericMetaField;
import com.arcnor.objcclang.meta.GenericMetaMember;

import java.util.EmptyStackException;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class CPPHandler extends CLangHandler {
	private static class Namespace {
		private final String name;
		private final Namespace parent;

		private final String fullName;

		private Namespace(String name, Namespace parent) {
			this.name = name;
			this.parent = parent;
			this.fullName = (parent != null ? (parent.toString() + "::") : "") + name;
		}

		@Override
		public String toString() {
			return fullName;
		}
	}

	private final String libraryName;
	private final String namespaceLimit;

	private Namespace currentNamespace = null;
	private Stack<Object> stack = new Stack<Object>();

	public CPPHandler(final String libraryName, final String namespaceLimit) {
		this.libraryName = libraryName;
		this.namespaceLimit = namespaceLimit;
	}

	@Override
	public void startDocument() {

	}

	@Override
	public void startElement(String tag, String content, int lineNum) {
		stateMachine.pushState(tag, lineNum);

		switch (stateMachine.getState()) {
			case NAMESPACE_DECL:
				pushNamespace(content);
				stack.push(currentNamespace);
				break;
			case CXX_RECORD_DECL:
				stack.push(null);
				if (currentNamespace == null || !currentNamespace.toString().startsWith(namespaceLimit)) {
					break;
				}
				if (stateMachine.getParentState() != State.NAMESPACE_DECL) {
					break;
				}
				if (!content.endsWith(" definition")) {
					break;
				}
				int idxGt = content.indexOf('>');
				if (idxGt < 0) {
					throw new RuntimeException("Invalid record definition");
				}
				String definition = content.substring(idxGt + 2);
				if (!definition.startsWith("class")) {
					break;
				}
				int idxSpace = content.indexOf(' ');
				Long address = Long.decode(content.substring(0, idxSpace));
				String[] parts = definition.split(" ");
				String className = parts[1];
				lastMetaMember = new CPPMetaClass(className);
				decls.put(address, lastMetaMember);
				stack.pop();
				stack.push(lastMetaMember);
				break;
			case PUBLIC:
				if (stack.peek() != null && lastMetaMember == stack.peek()) {
					// FIXME: Obviously, we're not parsing the content
					CPPMetaClass classMember = (CPPMetaClass) lastMetaMember;
					classMember.parent = "public " + content;
				}
				stack.push(null);
				break;
			case PROTECTED:
				if (stack.peek() != null && lastMetaMember == stack.peek()) {
					// FIXME: Obviously, we're not parsing the content
					CPPMetaClass classMember = (CPPMetaClass) lastMetaMember;
					classMember.parent = "protected " + content;
				}
				stack.push(null);
				break;
			case PRIVATE:
				if (stack.peek() != null && lastMetaMember == stack.peek()) {
					// FIXME: Obviously, we're not parsing the content
					CPPMetaClass classMember = (CPPMetaClass) lastMetaMember;
					classMember.parent = "private " + content;
				}
				stack.push(null);
				break;
			case FIELD_DECL:
				if (stack.peek() != null && lastMetaMember == stack.peek()) {
					// FIXME: We need better parsing, this is too naive
					int idx = content.lastIndexOf('\'', content.length() - 2);
					String type = content.substring(idx + 1, content.length() - 2);
					int idx2 = content.lastIndexOf(' ', idx - 2);
					String name = content.substring(idx2 + 1, idx - 1);
					GenericMetaField field = new GenericMetaField(name, type);
					((CPPMetaClass)lastMetaMember).fields.add(field);
					stack.push(field);
				} else {
					stack.push(null);
				}
				break;
			default:
				stack.push(null);
				break;
		}
	}

	@Override
	public void endElement(String tag) {
		switch (stateMachine.getState()) {
			case NAMESPACE_DECL:
				popNamespace();
				break;
		}

		try {
			Object last = stack.pop();
			if (last == lastMetaMember) {
				lastMetaMember = null;
			}
		} catch (EmptyStackException ex) {
			System.out.println(ex);
		}
		stateMachine.popState(tag);
	}

	private void pushNamespace(String content) {
		String[] parts = split(content, 1, 1);
		currentNamespace = new Namespace(parts[1], currentNamespace);
	}

	private void popNamespace() {
		currentNamespace = currentNamespace.parent;
	}

	@Override
	public void endDocument() {
		StringBuilder sb = new StringBuilder();
		sb.append("%module ").append(libraryName).append("\n");
		sb.append("%{").append("\n");
		// FIXME: Add imports here
		sb.append("%}").append("\n");

		Set<GenericMetaMember> parsed = new HashSet<GenericMetaMember>();
		for (GenericMetaMember member : decls.values()) {
			if (parsed.contains(member)) {
				continue;
			}
			if (!(member instanceof CPPMetaClass)) {
				continue;
			}
			parsed.add(member);

			SWIGGen gen = new SWIGGen((CPPMetaClass)member, null, null);
			sb.append(gen.getOutput());
			sb.append("\n");
		}

		System.out.println(sb.toString());
	}
}
