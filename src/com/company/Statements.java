package com.company;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.parser.Parse;
import opennlp.tools.util.Span;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Statements {
    private Parse sentence;
    private Parse replaceable;
    private String named = "";

    public static List<Statements> statementsFactory(Parse sentence, boolean quiet) throws Exception {
        List<Statements> allPossibleStatements = new ArrayList<Statements>();

        //find all noun phrases
        addThoseFromTag("NP", sentence, allPossibleStatements, quiet);
        //find all prepositional phrases
        //addThoseFromTag("PP", sentence, allPossibleStatements);

        InputStream dateFinderIn = new FileInputStream("opennlpmodels/en-ner-date.bin");
        TokenNameFinderModel dateFinder = new TokenNameFinderModel(dateFinderIn);
        InputStream orgFinderIn = new FileInputStream("opennlpmodels/en-ner-organization.bin");
        TokenNameFinderModel orgFinder = new TokenNameFinderModel(orgFinderIn);
        InputStream personFinderIn = new FileInputStream("opennlpmodels/en-ner-person.bin");
        TokenNameFinderModel personFinder = new TokenNameFinderModel(personFinderIn);
        InputStream timeFinderIn = new FileInputStream("opennlpmodels/en-ner-time.bin");
        TokenNameFinderModel timeFinder = new TokenNameFinderModel(timeFinderIn);
        for (int i = 0; i < allPossibleStatements.size(); i++) {
            allPossibleStatements.get(i).setNamed(dateFinder, orgFinder, personFinder, timeFinder);
        }

        return allPossibleStatements;
    }

    private void setNamed(TokenNameFinderModel dateFinderModel, TokenNameFinderModel orgFinderModel, TokenNameFinderModel personFinderModel, TokenNameFinderModel timeFinderModel) {
        NameFinderME dateFinder = new NameFinderME(dateFinderModel);
        NameFinderME orgFinder = new NameFinderME(orgFinderModel);
        NameFinderME personFinder = new NameFinderME(personFinderModel);
        NameFinderME timeFinder = new NameFinderME(timeFinderModel);
        Span dateSpans[] = dateFinder.find(replaceable.getCoveredText().split(" "));
        Span orgSpans[] = orgFinder.find(replaceable.getCoveredText().split(" "));
        Span personSpans[] = personFinder.find(replaceable.getCoveredText().split(" "));
        Span timeSpans[] = timeFinder.find(replaceable.getCoveredText().split(" "));

        if (dateSpans.length != 0) {
            named = "DATE";
        } else if (orgSpans.length != 0) {
            named = "ORGANIZATION";
        } else if (personSpans.length != 0) {
            named = "PERSON";
        } else if (timeSpans.length != 0) {
            named = "TIME";
        }
    }

    private static void addThoseFromTag(String type, Parse sentence, List<Statements> allPossibleStatements, boolean quiet) {

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
            }

            allPossibleStatements.add(new Statements(sentence, phrase));
        }
    }

    private Statements(Parse sentence, Parse replaceable) {
        this.sentence = sentence;
        this.replaceable = replaceable;
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

    public Parse getSentence() {
        return sentence;
    }

    public Parse getReplaceable() {
        return replaceable;
    }

    public String getNamed() {
        return named;
    }
}
