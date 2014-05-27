package com.arcnor.objcclang;

import com.arcnor.objcclang.parser.CLangHandler;
import com.arcnor.objcclang.parser.CLangTreeParser;
import com.arcnor.objcclang.parser.CPPHandler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CPPMain {
	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			System.err.println("Usage: Objc2RoboVM <Library Name> <Namespace> <Library AST Dump>");
			return;
		}
		CLangTreeParser parser = new CLangTreeParser(false);
		CLangHandler parser1 = new CPPHandler(args[0], args[1]);
		parser.parse(new BufferedReader(new FileReader(args[2])), parser1);
	}
}
