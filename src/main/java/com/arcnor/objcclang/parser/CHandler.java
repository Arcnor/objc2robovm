package com.arcnor.objcclang.parser;

import com.arcnor.objcclang.gen.HawtJNIAbstractGen;
import com.arcnor.objcclang.gen.HawtJNIFunctionsGen;
import com.arcnor.objcclang.meta.*;
import com.arcnor.objcclang.meta.hawtjni.HawtMetaClass;

import java.util.*;

public class CHandler extends CLangHandler {
	private final String expectedLibrary;

	private Map<String, GenericMetaField> typedefs = new HashMap<String, GenericMetaField>();
	private Map<String, GenericMetaMethod> decls = new HashMap<String, GenericMetaMethod>();

	private GenericMetaMethod lastFunction;
	private GenericMetaMember lastDecl;

	public CHandler(String library) {
		this.expectedLibrary = library;
	}

	@Override
	public void startDocument() {

	}

	@Override
	public void startElement(String tag, String content, int lineNum) {
		stateMachine.pushState(tag, lineNum);

		switch (stateMachine.getState()) {
			case FUNCTION_DECL: {
				if (!expectedLibrary.equals(library)) {
					break;
				}
				String[] parts = tempSplitNameType(content);
				lastFunction = new GenericMetaMethod(parts[0], parts[1], '+');
				if (decls.containsKey(lastFunction.name)) {
					throw new RuntimeException("Function already on the list! -> " + lastFunction.name);
				}
				decls.put(lastFunction.name, lastFunction);

				lastDecl = lastFunction;
				break;
			}
			case PARM_VAR_DECL: {
				if (!expectedLibrary.equals(library)) {
					break;
				}
				if (lastFunction == null) {
					break;
				}
				String[] parts = splitNameType(content);
				lastFunction.args.add(new GenericMetaField(parts[0], parts[1]));
				break;
			}
			// Records / Enums
			case TYPEDEF_DECL: {
				if (!expectedLibrary.equals(library)) {
					break;
				}
				String[] parts = splitNameType(content);
				if (typedefs.containsKey(parts[0])) {
					throw new RuntimeException("Typedef already on the list! -> " + parts[0]);
				}
				if ((lastMetaMember instanceof GenericMetaEnum || lastMetaMember instanceof GenericMetaRecord) && lastMetaMember.name == null) {
					lastMetaMember.name = parts[0];
				}
				lastMetaMember = new GenericMetaField(parts[0], parts[1]);
				if (stateMachine.getParentState() == State.TRANSLATION_UNIT_DECL) {
					typedefs.put(parts[0], (GenericMetaField) lastMetaMember);
//					lastMetaMember = null;
				}
				break;

			}
		}
	}

	// FIXME: Rename/move
	protected String[] tempSplitNameType(final String content) {
		String[] result = new String[2];
		int idx, lastIdx;

		// Name
		lastIdx = idx = content.indexOf('>') + 2;
		while (content.charAt(++idx) != ' ') {
		}
		result[0] = content.substring(lastIdx, idx);

		lastIdx = idx = idx + 2;

		// Type
		while (content.charAt(++idx) != '\'') {
		}
		if (content.charAt(idx + 1) == '\'') {
			lastIdx = idx + 1;
			idx += 2;
			while (content.charAt(++idx) != '\'') {
			}
		}
		int parNum = 1;
		int parIdx = idx - 2;
		while (parNum > 0) {
			char c = content.charAt(parIdx);
			if (c == ')') {
				parNum++;
			} else if (c == '(') {
				parNum--;
			}
			parIdx--;
		}
		result[1] = content.substring(lastIdx, parIdx + 1).trim();

		return result;
	}

	@Override
	public void endElement(String tag) {
		switch (stateMachine.getState()) {
			case FUNCTION_DECL:
				lastFunction = null;
				break;
		}

		stateMachine.popState(tag);
	}

	@Override
	public void endDocument() {
		ArrayList<GenericMetaMethod> functionList = new ArrayList<GenericMetaMethod>(decls.values());
		Collections.sort(functionList);

		HawtMetaClass newClass = new HawtMetaClass(expectedLibrary);
		newClass.library = expectedLibrary;
		for (GenericMetaMethod function : functionList) {
			newClass.functions.add(function);
		}

		Map<String, HawtJNIAbstractGen.HawtType> customTypes = new HashMap<String, HawtJNIAbstractGen.HawtType>();
		customTypes.put("SDL_bool", HawtJNIAbstractGen.HawtType.H_BOOLEAN.cloneWithOrigType("SDL_bool"));

		HawtJNIFunctionsGen gen = new HawtJNIFunctionsGen("com.arcnor", newClass, decls, typedefs, customTypes);
		System.out.println(gen.getOutput());
	}
}
