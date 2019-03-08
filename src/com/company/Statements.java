package com.company;

import opennlp.tools.parser.Parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Statements {
    private Parse sentence;
    private Parse replaceable;

    public static List<Statements> statementsFactory(Parse sentence) {
        List<Statements> allPossibleStatements = new ArrayList<Statements>();

        //find all noun phrases
        addThoseFromTag("NP", sentence, allPossibleStatements);
        //find all prepositional phrases
        //addThoseFromTag("PP", sentence, allPossibleStatements);

        return allPossibleStatements;
    }

    private static void addThoseFromTag(String type, Parse sentence, List<Statements> allPossibleStatements) {

        boolean foundAllPhrases = false;
        int found = 0;
        while (!foundAllPhrases) {
            Parse phrase = findType(type, sentence, found);

            if (phrase == null) {
                foundAllPhrases = true;
                if (found > 1) {
                    System.out.println("Found " + found + " phrases of type " + type + ".");
                } else if (found == 1) {
                    System.out.println("Found 1 phrase of type " + type + ".");
                } else if (found == 0) {
                    System.out.println("Found no phrases of type " + type + ".");
                }
                return;
            } else {
                found++;
            }

            allPossibleStatements.add(new Statements(sentence, phrase));
        }
    }

    private Statements(Parse sentence, Parse replaceable) {
        this.sentence = sentence;
        this.replaceable = replaceable;
    }

    private static Parse findType(String type, Parse parse, int alreadyFound) {
        if (parse == null) {
            return null;
        }
        Stack<Parse> parseStack = new Stack<Parse>();
        parseStack.push(parse);

        while (parseStack.empty() == false) {
            // Pop the top item from stack and print it
            Parse myParse = parseStack.peek();

            if (myParse.getType().equals(type)) {
                if (alreadyFound == 0) {
                    System.out.println("Found " + type + ": \"" + myParse.getCoveredText() + "\"");
                    return myParse;
                } else alreadyFound--;
            }

            parseStack.pop();
            // Push all children of the popped node to stack
            for (int i = 0; i < myParse.getChildCount(); i++) {
                parseStack.push(myParse.getChildren()[i]);
            }
        }
        return null;
    }

    public Parse getSentence() {
        return sentence;
    }

    public Parse getReplaceable() {
        return replaceable;
    }
}
