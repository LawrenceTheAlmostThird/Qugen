package com.company;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class NERResourcesSingleton {

    private static final NERResourcesSingleton instance;
    public NameFinderME dateFinder;
    public NameFinderME orgFinder;
    public NameFinderME personFinder;
    public NameFinderME timeFinder;

    private NERResourcesSingleton() {
        try {
            InputStream dateFinderIn = new FileInputStream("opennlpmodels/en-ner-date.bin");
            TokenNameFinderModel dateFinderModel = new TokenNameFinderModel(dateFinderIn);
            dateFinder = new NameFinderME(dateFinderModel);
        } catch (IOException e) {
            e.printStackTrace();
        } try {
            InputStream orgFinderIn = new FileInputStream("opennlpmodels/en-ner-organization.bin");
            TokenNameFinderModel orgFinderModel = new TokenNameFinderModel(orgFinderIn);
            orgFinder = new NameFinderME(orgFinderModel);
        } catch (IOException e) {
            e.printStackTrace();
        } try {
            InputStream personFinderIn = new FileInputStream("opennlpmodels/en-ner-person.bin");
            TokenNameFinderModel personFinderModel = new TokenNameFinderModel(personFinderIn);
            personFinder = new NameFinderME(personFinderModel);
        } catch (IOException e) {
            e.printStackTrace();
        } try {
            InputStream timeFinderIn = new FileInputStream("opennlpmodels/en-ner-time.bin");
            TokenNameFinderModel timeFinderModel = new TokenNameFinderModel(timeFinderIn);
            timeFinder = new NameFinderME(timeFinderModel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static{
        try{
            instance = new NERResourcesSingleton();
        }catch(Exception e){
            throw new RuntimeException("Could not load resources.");
        }
    }

    public static NERResourcesSingleton getInstance() {
        return instance;
    }
}
