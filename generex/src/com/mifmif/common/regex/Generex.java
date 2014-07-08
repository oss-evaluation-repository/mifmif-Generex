package com.mifmif.common.regex;

import java.util.ArrayList;
import java.util.List;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;

/**
 * A Java utility class that help generating string values that match a given
 * regular expression.It generate all values that are matched by the regex, a
 * random value, or you can generate only a specific string based on it's
 * lexicographical order .
 * 
 * @author y.mifrah
 *
 */
public class Generex {

	public Generex(String regex) {
		regExp = new RegExp(regex);
		automaton = regExp.toAutomaton();
	}

	public Generex(Automaton automaton) {
		this.automaton = automaton;
	}

	private RegExp regExp;
	private Automaton automaton;
	private List<String> matchedStrings = new ArrayList<String>();
	private Node rootNode;
	private boolean isTransactionNodeBuilt;

	/**
	 * @param indexOrder
	 *            ( 1<= indexOrder <=n)
	 * @return The matched string by the given pattern in the given it's order
	 *         in the sorted list of matched String.<br>
	 *         <code>indexOrder</code> between 1 and <code>n</code> where
	 *         <code>n</code> is the number of matched String.<br>
	 *         If indexOrder >= n , return an empty string.
	 */
	public String getMatchedString(int indexOrder) {
		buildTransactionNode();
		if (indexOrder == 0)
			indexOrder = 1;
		return buildStringFromNode(rootNode, indexOrder);
	}

	private String buildStringFromNode(Node node, int indexOrder) {
		String result = "";
		long passedStringNbr = 0;
		long step = node.getNbrMatchedString() / node.getNbrChar();
		for (char usedChar = node.getMinChar(); usedChar <= node.getMaxChar(); ++usedChar) {
			passedStringNbr += step;
			if (passedStringNbr >= indexOrder) {
				passedStringNbr -= step;
				indexOrder -= passedStringNbr;
				result += usedChar;
				break;
			}
		}
		long passedStringNbrInChildNode = 0;
		if (result.length() == 0)
			passedStringNbrInChildNode = passedStringNbr;
		for (Node childN : node.getNextNodes()) {
			passedStringNbrInChildNode += childN.getNbrMatchedString();
			if (passedStringNbrInChildNode >= indexOrder) {
				passedStringNbrInChildNode -= childN.getNbrMatchedString();
				indexOrder -= passedStringNbrInChildNode;
				result += buildStringFromNode(childN, indexOrder);
				break;
			}
		}
		return result;
	}

	/**
	 * @return first string in lexicographical order that is matched by the
	 *         given pattern.
	 */
	public String getFirstMatch() {
		buildTransactionNode();
		Node node = rootNode;
		String result = "";
		while (node.getNextNodes().size() > 0) {
			result += node.getMinChar();
			node = node.getNextNodes().get(0);
		}
		return result;
	}

	/**
	 * @return the number of strings that are matched by the given pattern.
	 */
	public long matchedStringsSize() {
		return rootNode.getNbrMatchedString();
	}

	/**
	 * Prepare the rootNode and it's child nodes so that we can get
	 * matchedString by index
	 */
	private void buildTransactionNode() {
		if (isTransactionNodeBuilt)
			return;
		isTransactionNodeBuilt = true;
		rootNode = new Node();
		rootNode.setNbrChar(1);
		List<Node> nextNodes = prepareTransactionNodes(automaton.getInitialState());
		rootNode.setNextNodes(nextNodes);
		rootNode.updateNbrMatchedString();
	}

	public void printMatchedStrings() {
		generate("", automaton.getInitialState(), true);
	}

	private void generate(String strMatch, State state, boolean debug) {
		List<Transition> transitions = state.getSortedTransitions(true);
		if (transitions.size() == 0) {
			if (debug)
				System.out.println(strMatch);
			matchedStrings.add(strMatch);
			return;
		}
		if (state.isAccept()) {
			if (debug)
				System.out.println(strMatch);
			matchedStrings.add(strMatch);
		}
		for (Transition transition : transitions) {
			for (char c = transition.getMin(); c <= transition.getMax(); ++c) {
				generate(strMatch + c, transition.getDest(), debug);
			}
		}
	}

	/**
	 * Build list of nodes that present possible transactions from the
	 * <code>state</code>.
	 * 
	 * @param state
	 * @return
	 */
	private List<Node> prepareTransactionNodes(State state) {

		List<Node> transactionNodes = new ArrayList<Node>();
		if (state.isAccept()) {
			Node acceptedNode = new Node();
			acceptedNode.setNbrChar(1);
			transactionNodes.add(acceptedNode);
		}
		List<Transition> transitions = state.getSortedTransitions(true);
		for (Transition transition : transitions) {
			Node trsNode = new Node();
			int nbrChar = transition.getMax() - transition.getMin() + 1;
			trsNode.setNbrChar(nbrChar);
			trsNode.setMaxChar(transition.getMax());
			trsNode.setMinChar(transition.getMin());
			List<Node> nextNodes = prepareTransactionNodes(transition.getDest());
			trsNode.setNextNodes(nextNodes);
			transactionNodes.add(trsNode);
		}
		return transactionNodes;
	}

	public List<String> getAllMatchedStrings() {
		if (matchedStrings.size() == 0)
			generate("", automaton.getInitialState(), false);
		return matchedStrings;

	}

	/**
	 * Generate and return a random String that match the pattern used in this
	 * Generex.
	 * 
	 * @return
	 */
	public String random() {
		return prepareRandom("", automaton.getInitialState(), 1, Integer.MAX_VALUE);
	}

	/**
	 * Generate and return a random String that match the pattern used in this
	 * Generex, and the string has a length >= <code>minLength</code>
	 * 
	 * @param minLength
	 * @return
	 */
	public String random(int minLength) {
		return prepareRandom("", automaton.getInitialState(), minLength, Integer.MAX_VALUE);
	}

	/**
	 * Generate and return a random String that match the pattern used in this
	 * Generex, and the string has a length >= <code>minLength</code> and <=
	 * <code>maxLength</code>
	 * 
	 * 
	 * @param minLength
	 * @param maxLength
	 * @return
	 */
	public String random(int minLength, int maxLength) {
		return prepareRandom("", automaton.getInitialState(), minLength, maxLength);
	}

	private String prepareRandom(String strMatch, State state, int minLength, int maxLength) {
		List<Transition> transitions = state.getSortedTransitions(false);

		if (state.isAccept()) {
			if (strMatch.length() == maxLength)
				return strMatch;
			if (Math.random() > 0.7)
				if (strMatch.length() >= minLength)
					return strMatch;
		}
		if (transitions.size() == 0) {
			return strMatch;
		}
		Transition randomTransition = transitions.get((int) (transitions.size() * Math.random()));
		char randomChar = (char) ((int) (Math.random() * (randomTransition.getMax() - randomTransition.getMin())) + randomTransition.getMin());
		return prepareRandom(strMatch + randomChar, randomTransition.getDest(), minLength, maxLength);

	}
}