package com.arcnor.objcclang.parser;

public interface CLangParser {
	void startDocument();

	void startElement(String tag, final String content);

	void endElement(String tag);

	void endDocument();

	/**
	 * This sets a framework or a normal library path
	 *
	 * @param framework
	 * @param isFramework true if this is a framework name (ends with .framework), false otherwise
	 */
	void setFramework(String framework, boolean isFramework);
}
