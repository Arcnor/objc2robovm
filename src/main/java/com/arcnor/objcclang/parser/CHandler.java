package com.arcnor.objcclang.parser;

public class CHandler extends CLangHandler {
	private final String expectedLibrary;
	private String library;

	public CHandler(String library) {
		this.expectedLibrary = library;
	}

	@Override
	public void startDocument() {

	}

	@Override
	public void startElement(String tag, String content) {
		stateMachine.pushState(tag);

		switch (stateMachine.getState()) {
			case FUNCTION_DECL: {
				if (!expectedLibrary.equals(library)) {
					break;
				}
				String[] parts = tempSplitNameType(content);
				System.out.println("NameType: " + parts[0] + " -> " + parts[1]);
				break;
			}
		}
	}

	// FIXME: Rename/move
	protected String[] tempSplitNameType(final String content) {
		String[] result = new String[2];
		int idx, lastIdx;

		// Name
		lastIdx = idx = content.indexOf('>') + 1;
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
		result[1] = content.substring(lastIdx, idx);

		return result;
	}

	@Override
	public void endElement(String tag) {
		stateMachine.popState(tag);
	}

	@Override
	public void endDocument() {

	}

	@Override
	public void setFramework(String framework, boolean isFramework) {
		if (!isFramework) {
			this.library = framework;
		}
	}
}
