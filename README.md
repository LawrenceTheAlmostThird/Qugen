# Qugen
A question generator built with OpenNLP.

To use, download the latest build and unzip it. Then type:
`java -jar QuestionGenerator.jar -h`

This should print the help file, providing further instruction.

For a demonstration of the program's capabilities, try running it with eg.
```
java -jar QuestionGenerator.jar output.txt "The question generator is meant to generate questions from declarative statements."
java -jar QuestionGenerator.jar -f output.txt input.txt
java -jar QuestionGenerator.jar -n output.txt "Words I am wishing to be normalized."
```