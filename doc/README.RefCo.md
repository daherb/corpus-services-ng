# The Refco Checker

## How to use:

The RefCo checker is part of the corpus services.

### Download

Releases are available on Gitlab: [https://gitlab.rrz.uni-hamburg.de/bba1792/corpus-services/-/releases](https://gitlab.rrz.uni-hamburg.de/bba1792/corpus-services/-/releases)

### Compiling

To get the most recent version you can compile corpus services yourself. Compile using `mvn clean compile assembly:single` or following the instructions in [Build_with_Maven.md](Build_with_Maven.md). After running `mvn assembly:single` you can find `corpus-services-1.0.jar` in the `target/` folder.

### Running

You can run the corpus services using `java -jar corpus-services-1.0.jar` which gives you a list of all included checkers.
To run the RefCo checks use the command `java -jar corpus-services-1.0.jar -c RefcoChecker -i <PathToYourCorpus> -o <ReportOutputFile -p refco-file=<RefCoCorpusDocumentationFile> --corpus <CorpusName>` after adjusting the pathes and file names to your corpus.

## Resources:

- The souce file for the RefCoChecker: [../src/main/java/de/uni_hamburg/corpora/validation/quest/RefcoChecker.java](../src/main/java/de/uni_hamburg/corpora/validation/quest/RefcoChecker.java)
- The RefCo documentation is available on Zenodo: [https://zenodo.org/record/6242355](https://zenodo.org/record/6242355)
