package com.company;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.parser.Parse;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Statements {
    private Parse sentence;
    private Parse replaceable;
    private Span tokenSpan;
    private String named = "";

    public static List<Statements> statementsFactory(Parse sentence, boolean quiet) throws Exception {
        List<Statements> allPossibleStatements = new ArrayList<Statements>();
        List<Statements> baseStatements = new ArrayList<Statements>();

        //find statements
        addThoseFromTag("S", sentence, baseStatements, quiet);
        //find all noun phrases for each statement
        for (int i = 0; i < baseStatements.size(); i++) {
            if (!Qugen.searchFor("CC", baseStatements.get(i).replaceable.getChildren()[0])) { //if not connected by conjunction
                addThoseFromTag("NP", baseStatements.get(i).replaceable, allPossibleStatements, quiet);
            }
        }
        //find all prepositional phrases
        //addThoseFromTag("PP", sentence, allPossibleStatements);

        for (int i = 0; i < allPossibleStatements.size(); i++) {
            allPossibleStatements.get(i).setNamed();
        }

        return allPossibleStatements;
    }

    private void setNamed() {
        NameFinderME dateFinder = NERResourcesSingleton.getInstance().dateFinder;
        NameFinderME orgFinder = NERResourcesSingleton.getInstance().orgFinder;
        NameFinderME personFinder = NERResourcesSingleton.getInstance().personFinder;
        NameFinderME timeFinder = NERResourcesSingleton.getInstance().timeFinder;

        Span[] dateSpans = dateFinder.find(sentence.getCoveredText().split(" "));
        Span[] orgSpans = orgFinder.find(sentence.getCoveredText().split(" "));
        Span[] personSpans = personFinder.find(sentence.getCoveredText().split(" "));
        Span[] timeSpans = timeFinder.find(sentence.getCoveredText().split(" "));

        assignNameOrConflict(dateSpans, "DATE");
        assignNameOrConflict(orgSpans, "ORGANIZATION");
        assignNameOrConflict(personSpans, "PERSON");
        assignNameOrConflict(timeSpans, "TIME");
    }

    private static void addThoseFromTag(String type, Parse sentence, List<Statements> allPossibleStatements, boolean quiet) throws Exception {

        boolean foundAllPhrases = false;
        int found = 0;
        while (!foundAllPhrases) {
            Parse phrase = findType(type, sentence, found, quiet);

            if (phrase == null) {
                foundAllPhrases = true;
                if (!quiet) {
                    if (found > 1) {
                        System.out.println("Found " + found + " phrases of type " + type + ".");
                    } else if (found == 1) {
                        System.out.println("Found 1 phrase of type " + type + ".");
                    } else if (found == 0) {
                        System.out.println("Found no phrases of type " + type + ".");
                    }
                }
                return;
            } else {
                found++;
                Span tokenSpan = convertCharSpanToTokenSpan(sentence, phrase.getSpan());
                allPossibleStatements.add(new Statements(sentence, phrase, tokenSpan));
            }
        }
    }

    private Statements(Parse sentence, Parse replaceable, Span tokenSpan) {
        this.sentence = sentence;
        this.replaceable = replaceable;
        this.tokenSpan = tokenSpan;
    }

    private static Parse findType(String type, Parse parse, int alreadyFound, boolean quiet) {
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
                    if (!quiet) System.out.println("Found " + type + ": \"" + myParse.getCoveredText() + "\"");
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

    public static Span convertCharSpanToTokenSpan(Parse parentSentence, Span charSpan) {
        Tokenizer tokenizer = NLPResourcesSingleton.getInstance().tokenizer;
        String[] tokens = tokenizer.tokenize(parentSentence.getCoveredText());
        int startToken = 0;
        int endToken = 0;
        int i = parentSentence.getSpan().getStart(); //usually 0, except if the parent sentance was sliced up
        do {
            i += tokens[startToken].length() + 1;
            startToken++;
            endToken++;
        } while (i < charSpan.getStart());
        do {
            i += tokens[endToken].length() + 1;
            endToken++;
        } while (i < charSpan.getEnd());

        Span tokenSpan = new Span(startToken, endToken);
        return tokenSpan;
    }

    public void assignNameOrConflict(Span[] namedEntitySpans, String name) {
        for (int i = 0; i < namedEntitySpans.length; i++) {
            if (namedEntitySpans[i].intersects(tokenSpan)) {
                if (!named.equals(name) && !named.equals("") || named.equals("CONFLICTING NAMES")) {
                    named = "CONFLICTING NAMES";
                } else named = name;
            }
        }
        return;
    }

    public Parse getSentence() {
        return sentence;
    }

    public Parse getReplaceable() {
        return replaceable;
    }

    public Span getTokenSpan() {
        return tokenSpan;
    }

    public String getNamed() {
        return named;
    }
}
