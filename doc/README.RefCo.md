# The Refco Checker

## How to use:

The RefCo checker is part of the corpus services. Compile using `mvn clean compile assembly:single` or following the instructions in [Build_with_Maven.md].
After running `mvn assembly:single` you can run the corpus services using `java -jar target/corpus-services-1.0.jar` which gives you a list of all included checkers.
To run the RefCo checks use the command `java -jar target/corpus-services-1.0.jar -c RefcoChecker -i <PathToYourCorpus> -o <ReportOutputFile -p refco-file=<RefCoCorpusDocumentationFile> --corpus <CorpusName>` after adjusting the pathes and file names to your corpus.

## Resources:
- The souce file for the RefCoChecker: [../src/main/java/de/uni_hamburg/corpora/validation/quest/RefcoChecker.java]
- The RefCo documentation is available on Zenodo: [https://zenodo.org/record/5825304]
