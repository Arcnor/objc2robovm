package com.arcnor.objcclang;

import com.arcnor.objcclang.parser.AppleHandler;
import com.arcnor.objcclang.parser.CLangHandler;
import com.arcnor.objcclang.parser.CLangTreeParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Main {
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.println("Usage: Objc2RoboVM <Framework Name> <Framework AST Dump>");
			return;
		}
		CLangTreeParser parser = new CLangTreeParser(true);
		CLangHandler parser1 = new AppleHandler(args[0]);
		parser.parse(new BufferedReader(new FileReader(args[1])), parser1);
	}
}
