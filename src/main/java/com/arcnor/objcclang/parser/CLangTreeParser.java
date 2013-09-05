package com.arcnor.objcclang.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Stack;

public class CLangTreeParser {
	private int level, lastLevel;
	private String lastState;

	private Stack<String> state = new Stack<String>();

	public void parse(BufferedReader r, CLangParser parser) throws IOException {
		parser.startDocument();

		String line;
		while ((line = r.readLine()) != null) {
			if (line.endsWith("<<<NULL>>>")) {
				line += " 0x0";
			} else if (line.endsWith("-...")) {
				line += " 0x0";
			}
			level = 0;
			char tempC = line.charAt(level);
			if (tempC == '|' || tempC == '`' || tempC == ' ') {
				while (line.charAt(level) != '-') {
					level++;
				}
				level--;
			}
			switch (line.charAt(level)) {
				case '|':
				case '`':
					level += 2;
					break;
			}
			if (level > lastLevel) {
				lastLevel += 2;
				state.push(lastState);
			} else {
				if (lastState != null) {
					parser.endElement(lastState);
					while (lastLevel != level) {
						lastLevel -= 2;
						parser.endElement(state.pop());
					}
				}
			}
			// Get current tokens
			String[] buf = line.substring(level).split(" ", 2);
			lastState = buf[0];
			if (buf.length < 2) {
				System.out.println("WHAT!");
			}
			int fileStartIdx = line.indexOf('<');
			if (fileStartIdx >= 0) {
				int fileEndIdx = line.indexOf('>');
				extractFramework(line.substring(fileStartIdx + 1, fileEndIdx), parser);
			}
			parser.startElement(lastState, buf[1]);
		}
		parser.endElement(lastState);
		while (!state.isEmpty()) {
			parser.endElement(state.pop());
		}

		parser.endDocument();
	}

	private void extractFramework(final String str, final CLangParser parser) {
		int colonIdx = str.indexOf(':');
		if (colonIdx < 0 || str.startsWith("line:") || str.startsWith("col:")) {
			return;
		}
		String headerPath = str.substring(0, colonIdx);
		File f = new File(headerPath);
		try {
			Path p = f.getCanonicalFile().toPath();
			String path = p.toString();
			if (!path.contains(".framework")) {
				parser.setLibrary(p.getName(p.getNameCount() - 2).toString(), false);
			} else {
				for (int j = p.getNameCount() - 1; j > 0; j--) {
					String part = p.getName(j).toString();
					if (part.contains(".framework")) {
						parser.setLibrary(part.substring(0, part.indexOf('.')), true);
						break;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
