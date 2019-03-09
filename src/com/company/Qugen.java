package com.company;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.parser.*;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.*;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.Scanner;
import opennlp.tools.sentdetect.*;

@Command(description = "Transform statements of fact into questions.", name = "qugen",  mixinStandardHelpOptions = true, version = "Question Generator 0.1")
public class Qugen implements Callable<Void> {

    @Parameters(paramLabel = "OUTPUT", index = "0", description = "File to write to.")
    File output;

    @Parameters(paramLabel = "INPUT", index = "1..*", description = "Sentence to transform.")
    String input;

    @Option(names = {"-q", "--quiet"}, description = "Produce no output. Never query for user input.")
    boolean quiet;

    @Option(names = {"-i", "--non-interactive"}, description = "Never query for user input.")
    boolean nonInteractive;

    @Option(names = {"-f", "--file"}, description = "Indicate that INPUT is a file path, not a sentence to be transformed. Files should be in a human-readable format, with each sentence to be transformed written in natural language, one after the other.")
    boolean inputIsFilePath;

    @Option(names = {"-g", "--gui"}, description = "Open the AWT/Swing gui.")
    boolean launchGui;

    @Option(names = {"-n", "--normalizer"}, description = "Test the normalizer on the input.")
    boolean testNormalization;

    @Option(names={"-c", "--csvformat"}, description = "Indicate that the file path leads to a file in comma-seperated-variable format, as opposed to natural language. Files should be written with each sentence on its own line, enclosed in quotation marks.")
    boolean CSVFormat;

    Scanner scanner = new Scanner(System.in);

    public Void call() throws Exception {
        if (CSVFormat && !inputIsFilePath) {
            throw new Exception("If CSV format is specified, input must be a file.");
        }
        File inputfile;
        List<String> sentenceList = new ArrayList<>();

        if (inputIsFilePath && !CSVFormat) {
            inputfile = new File(input);
            sentenceList = ListSentences(new String(Files.readAllBytes(inputfile.toPath())));
        } else if (CSVFormat) {
            inputfile = new File(input);
            FileReader reader = new FileReader(inputfile.getAbsolutePath());
            CsvPreference prefs = CsvPreference.STANDARD_PREFERENCE;
            CsvListReader csvreader = new CsvListReader(new BufferedReader(reader), prefs);
            List<String> thisLine = csvreader.read();
            while (thisLine != null) {
                String thisSentence = "";
                for (int i = 0; i < thisLine.size(); i++) {
                    thisSentence += thisLine.get(i);
                }
                sentenceList.add(thisSentence);
                thisLine = csvreader.read();
            }
        } else {
            inputfile = null;
            sentenceList = ListSentences(input);
        }

        if (!quiet) {
            if (inputfile != null) {
                System.out.print("The input file is: ");
                System.out.println(inputfile.getAbsolutePath());
            } else {
                System.out.println("Getting input from stdin.");
            }

            System.out.println("The input contains " + sentenceList.size() + " sentences.");

            if (output != null) {
                System.out.print("The output file is: ");
                System.out.println(output.getAbsolutePath());
            }
            System.out.println(sentenceList.toString());
        }

        if (testNormalization) {
            for (int i = 0; i < sentenceList.size(); i++) {
                testNormalization(sentenceList.get(i));
            }
            return null;
        }

        if (output != null) {
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), "utf-8"))) {
                for (int i = 0; i < sentenceList.size(); i++) {
                    writer.write("\"" + sentenceList.get(i) + "\"\n");
                }
            }
        }

        for (int i = 0; i < sentenceList.size(); i++) {
            String question = genQuestion(sentenceList.get(i));
            //System.out.println(question); //should be the best candidate
            if (!quiet) System.out.println(); //new line for readability
        }
        return null;

    }

    private void testNormalization(String statement) throws Exception {
        try (InputStream modelIn = new FileInputStream("opennlpmodels/en-token.bin")) {
            TokenizerModel tokmodel = new TokenizerModel(modelIn);
            Tokenizer tokenizer = new TokenizerME(tokmodel);
            String[] tokens = tokenizer.tokenize(statement);
            String whitespaced = tokens[0];
            for (int i = 1; i < tokens.length; i++) {
                whitespaced = whitespaced.concat(" " + tokens[i]);
            }
            if (!quiet) System.out.println(whitespaced);

            //tag pos
            try (InputStream posmodelIn = new FileInputStream("opennlpmodels/en-pos-maxent.bin")) {
                POSModel posmodel = new POSModel(posmodelIn);
                POSTaggerME tagger = new POSTaggerME(posmodel);
                String[] tags = tagger.tag(tokens);
                //TODO determine pos error

                try (InputStream lemModelIn = new FileInputStream("opennlpmodels/en-lemmatizer.dict")) {
                    DictionaryLemmatizer lemmatizer = new DictionaryLemmatizer(lemModelIn);
                    String[] lemmas = lemmatizer.lemmatize(tokens, tags);
                    System.out.println(Arrays.toString(lemmas));
                }
            }
        }

        return;
    }

    public List<String> ListSentences(String content) throws Exception {
        char[] eoschars = {'.', '?', '!'};
        DefaultEndOfSentenceScanner detector = new DefaultEndOfSentenceScanner(eoschars);
        List<Integer> positions = detector.getPositions(content);

        List<String> contentList = new ArrayList<>();

        contentList.add(content.substring(0, positions.get(0)));
        for (int i = 0; i < positions.size()-1; i++) {
            String next = content.substring(positions.get(i)+1, positions.get(i+1)); //from previous position plus one to next position
            next = next.trim();
            contentList.add(next);
        }

        return contentList;
    }

    public String genQuestion(String statement) throws Exception {
        //tokenize
        try (InputStream modelIn = new FileInputStream("opennlpmodels/en-token.bin")) {
            TokenizerModel tokmodel = new TokenizerModel(modelIn);
            Tokenizer tokenizer = new TokenizerME(tokmodel);
            String[] tokens = tokenizer.tokenize(statement);
            String whitespaced = tokens[0];
            for (int i = 1; i < tokens.length; i++) {
                whitespaced = whitespaced.concat(" " + tokens[i]);
            }
            if (!quiet) System.out.println(whitespaced);

            //tag pos
            try (InputStream posmodelIn = new FileInputStream("opennlpmodels/en-pos-maxent.bin")) {
                POSModel posmodel = new POSModel(posmodelIn);
                POSTaggerME tagger = new POSTaggerME(posmodel);
                String[] tags = tagger.tag(tokens);
                //TODO determine pos error

                //chunk sentance
                try (InputStream chunkmodelIn = new FileInputStream("opennlpmodels/en-chunker.bin")) {
                    ChunkerModel chunkmodel = new ChunkerModel(chunkmodelIn);
                    ChunkerME chunker = new ChunkerME(chunkmodel);
                    String chunks[] = chunker.chunk(tokens, tags);
                    //TODO determine chunking error

                    //TODO rank chunks on predicted entropy

                    //possibly use named entity recognition?
                    //named entities probably have high entropy

                }

            }

            //parse
            InputStream parsemodelIn = new FileInputStream("opennlpmodels/en-parser-chunking.bin");
            try {
                ParserModel parsermodel = new ParserModel(parsemodelIn);
                Parser parser = ParserFactory.create(parsermodel);
                Parse[] parses = ParserTool.parseLine(whitespaced, parser, 1);
                if (!quiet) parses[0].show();
                //determine if it's a question word by a quick scan of the nodes
                Parse[] tagNodes = parses[0].getTagNodes();
                for (int i = 0; i < tagNodes.length; i++) {
                    if (tagNodes[i].getType().equals("WDT") || tagNodes[i].getType().equals("WP") || tagNodes[i].getType().equals("WP$") || tagNodes[i].getType().equals("WRB")) {
                        if (!quiet) System.out.println("Warning: This statement contains WH- words. Please use statements of fact.");
                        if (!nonInteractive && !quiet) {
                            boolean confirmation = askUserForConfirmation();
                            if (!confirmation) {
                                return "";
                            }
                        }
                    }
                }
                //scan nodes for SBARQ
                //catches non-statements (questions) and throw an error
                if (searchFor("SBARQ", parses[0])) {
                    if (!quiet) System.out.println("Warning: This statement contains an SBARQ tag. Please use statements of fact.");
                    if (!nonInteractive && !quiet) {
                        boolean confirmation = askUserForConfirmation();
                        if (!confirmation) {
                            return "";
                        }
                    }
                }
                if (!quiet) System.out.println(parses[0].getProb());
                if (parses[0].getProb() <= -2) {
                    if (!quiet) System.out.println("Warning: This statement may be incorrectly parsed.");
                    if (!nonInteractive && !quiet) {
                        boolean confirmation = askUserForConfirmation();
                        if (!confirmation) {
                            return "";
                        }
                    }
                }

                //a statement contains the parse, and the sub-parse that can be replaced.
                List<Statements> statements = Statements.statementsFactory(parses[0], quiet);
                List<String> candidates = new ArrayList<String>();

                for (int i = 0; i < statements.size(); i++) {
                    String candidate = statements.get(i).getSentence().toString();

                    String replacement = new String();
                    switch (statements.get(i).getReplaceable().getType()) {
                        case "NP":
                            replacement = "what";
                            break;
                        case "PP":
                            replacement = "how";
                            break;
                    }

                    candidate = candidate.substring(0, statements.get(i).getReplaceable().getSpan().getStart())
                            + replacement
                            + candidate.substring(statements.get(i).getReplaceable().getSpan().getEnd())
                            + "?";
                    candidate = candidate.substring(0,1).toUpperCase() + candidate.substring(1);

                    if (!quiet) System.out.println(candidate);
                    candidates.add(candidate);
                }

                //afterwards, parse the new sentence. is it OK?
                for (int i = 0; i < candidates.size(); i++) {
                    Parse[] candidateParses = ParserTool.parseLine(whitespaced, parser, 1);
                }
                //create question by rearranging parse tree based on entropy!
                //estimate error
                //ask user for confirmation if error too high
                //perhaps use a message pattern to talk to UI?
                if (!candidates.isEmpty()) {
                    return candidates.get(0);
                } else throw new Exception("Candidate could not be found.");

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (parsemodelIn != null) {
                    try {
                        parsemodelIn.close();
                    } catch (IOException e) {
                        //no op
                    }
                }
            }
        }

        //return error
        throw new Exception("Candidate could not be found.");
    }

    private boolean askUserForConfirmation() {
        while (true) {
            System.out.println("Continue with this statement? (Enter Y or N.)");
            String in = scanner.next();
            if (in.equalsIgnoreCase("Y")) {
                return true;
            } else if (in.equalsIgnoreCase("N")) {
                return false;
            }
            System.out.println("Please enter Y or N, then a new line.");
        }
    }

    private boolean searchFor(String type, Parse parse) {
        if (parse.getType().equals(type)) {
            return true;
        } else {
            for (int i = 0; i < parse.getChildCount(); i++) {
                searchFor(type, parse.getChildren()[i]);
            }
            return false;
        }
    }
}
