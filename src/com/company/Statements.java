package com.company;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.parser.Parse;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

import java.io.FileInputStream;
import java.io.InputStream;
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

    private void setNamed(TokenNameFinderModel dateFinderModel, TokenNameFinderModel orgFinderModel, TokenNameFinderModel personFinderModel, TokenNameFinderModel timeFinderModel) throws Exception {
        NameFinderME dateFinder = new NameFinderME(dateFinderModel);
        NameFinderME orgFinder = new NameFinderME(orgFinderModel);
        NameFinderME personFinder = new NameFinderME(personFinderModel);
        NameFinderME timeFinder = new NameFinderME(timeFinderModel);

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
                System.out.println("tokenSpan is " + tokenSpan.toString());
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

    public static Span convertCharSpanToTokenSpan(Parse parentSentence, Span charSpan) throws Exception {

        try (InputStream modelIn = new FileInputStream("opennlpmodels/en-token.bin")) {
            TokenizerModel tokmodel = new TokenizerModel(modelIn);
            Tokenizer tokenizer = new TokenizerME(tokmodel);
            String[] tokens = tokenizer.tokenize(parentSentence.getCoveredText());
            int startToken = 0;
            int endToken = 0;
            int i = 0;
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
