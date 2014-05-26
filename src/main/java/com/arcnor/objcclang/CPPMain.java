package com.arcnor.objcclang;

import com.arcnor.objcclang.parser.CHandler;
import com.arcnor.objcclang.parser.CLangHandler;
import com.arcnor.objcclang.parser.CLangTreeParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CPPMain {
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.println("Usage: Objc2RoboVM <Library Name> <Library AST Dump>");
			return;
		}
		CLangTreeParser parser = new CLangTreeParser();
		CLangHandler parser1 = new CHandler(args[0]);
		parser.parse(new BufferedReader(new FileReader(args[1])), parser1);
	}
}
