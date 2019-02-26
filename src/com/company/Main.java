package com.company;

public class Main {
    public static void main(String[] args) {
        picocli.CommandLine.call(new Qugen(), args);
        return;
    }
}
