package com.company;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class NLPResourcesSingleton {
    private static final NLPResourcesSingleton instance;
    public Tokenizer tokenizer;
    public POSTaggerME tagger;
    public ChunkerME chunker;
    public Parser parser;
    public DictionaryLemmatizer lemmatizer;

    private NLPResourcesSingleton() {

        try (InputStream modelIn = new FileInputStream("opennlpmodels/en-token.bin")) {
            TokenizerModel tokmodel = new TokenizerModel(modelIn);
            tokenizer = new TokenizerME(tokmodel);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (InputStream posmodelIn = new FileInputStream("opennlpmodels/en-pos-maxent.bin")) {
            POSModel posmodel = new POSModel(posmodelIn);
            tagger = new POSTaggerME(posmodel);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (InputStream chunkmodelIn = new FileInputStream("opennlpmodels/en-chunker.bin")) {
            ChunkerModel chunkmodel = new ChunkerModel(chunkmodelIn);
            chunker = new ChunkerME(chunkmodel);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (InputStream parsemodelIn = new FileInputStream("opennlpmodels/en-parser-chunking.bin")) {
            ParserModel parsermodel = new ParserModel(parsemodelIn);
            parser = ParserFactory.create(parsermodel);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (InputStream lemModelIn = new FileInputStream("opennlpmodels/en-lemmatizer.dict")) {
            lemmatizer = new DictionaryLemmatizer(lemModelIn);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static{
        try{
            instance = new NLPResourcesSingleton();
        }catch(Exception e){
            throw new RuntimeException("Could not load resources.");
        }
    }

    public static NLPResourcesSingleton getInstance() {
        return instance;
    }
}
