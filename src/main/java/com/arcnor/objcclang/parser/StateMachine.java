package com.arcnor.objcclang.parser;

import java.util.Arrays;
import java.util.Map;
import java.util.Stack;

public class StateMachine<S extends NamedEnum> {
	private Stack<S> states = new Stack<S>();
	private S state;
	private final Map<S, S[]> possibleStates;

	public StateMachine(final S initialState, Map<S, S[]> possibleStates) {
		this.state = initialState;
		this.possibleStates = possibleStates;
	}

	public void pushState(String qName, int lineNum) {
		S[] newStates = possibleStates.get(state);
		if (newStates == null) {
			throw new RuntimeException(String.format("Unhandled state '%1$s' (got '%2$s') : %3$d", state.getName(), qName, lineNum));
		}

		boolean found = newStates.length == 0;
		for (int j = 0; j < newStates.length; j++) {
			S newState = newStates[j];
			String n = newState.getName();
			if (n.equals(qName)) {
				states.push(this.state);
				this.state = newState;
				found = true;
				break;
			}
		}
		if (!found) {
			StringBuilder sb = new StringBuilder("[");
			for (int j = 0; j < newStates.length; j++) {
				sb.append(newStates[j]).append(' ');
			}
			sb.append(']');
			String expected = sb.toString();
			throw new RuntimeException(String.format("Not a valid XML file (\n\texpected '%1$s',\n\tgot '%2$s'\n\tfor '%3$s') : %4$d", expected, qName, state.getName(), lineNum));
		}
	}

	public void popState(String qName) {
		if (!state.getName().equals(qName)) {
			throw new RuntimeException(String.format("Not a valid XML file (\n\texpected '%1$s',\n\tgot '%2$s')", qName, state.getName()));
		}
		state = states.pop();
	}

	public S getState() {
		return state;
	}

	public S getParentState() {
		return states.peek();
	}
}
