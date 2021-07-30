# How to use the CorpusFunctions in the Corpus Services

#  Use of the corpus functions

Download the generated .jar and execute something similar to:

`java -Xmx3g -jar ../corpus-services-1.0.jar -i E:\user\corpus -o E:\user\corpus\report-output.html -c function1 -c function2 -f `

Output of the help parameter:
<pre>
usage: corpus-services -i <FILE PATH> -o <FILE PATH> -c <CORPUS
       FUNCTION> [-p <property=value>] [-f] [-h] [-e] [-j] [-s <FILE
       PATH>]
Specify a corpus folder or file and a function to be applied

 -i,--input <FILE PATH>                  input file path (coma file for
                                         corpus, folder or other file for
                                         unstructured data)
 -o,--output <FILE PATH>                 output file
 -c,--corpusfunction <CORPUS FUNCTION>   corpus function
 -p,--property <property=value>          use value for given properties
 -f,--fix                                fixes problems automatically
 -h,--help                               display help
 -e,--errorsonly                         output only errors
 -j,--fixesjson                          output json file for fixes
 -s,--settingsfile <FILE PATH>           settings file path
 -n,--nocuration						 no curation folder
</pre>

*-i* is the input, it should be the name of the folder or single corpus file you want to process. Functions can be run on a folder, a metadata file representing a list of files, or a single file (e.g. exb). If the input is a folder or a single file that is not an EXMARaLDA coma file, it will be treated as unstructured data. If the input file is an EXMARaLDA coma file, it will be treated as a structured corpus and all files linked in it will also be checked, depending on the chosen functions.

*-o* is the location, where the output will be found. Supported output formats are .html, .txt and .csv. It is possible to create several output files at the same time. Depending if you end the filename with ".txt", ".html" or ".csv" a simple text file, a sort- and filterable HTML table or a CSV (tabs and double quotes as delimiters) table will be generated. If you want to obtain several outputs, provide the path to each file separated by -o or commas (no space after the coma). At the same place an additional a curation folder with EXMARaLDA error list will be created. 

*-c* are the corpus functions you want to run. See the method getAllExistingCFs() in the class src/main/java/de/uni_hamburg/corpora/CorpusMagician.java for all available function strings. 

*-f* this flag is for fixing, if it exists, all possible functions will fix the data automatically (that means it changes the input data and rewrites them).

*-h* is the help flag to generate a very detailed help output

*-e* outputs only errors (Warnings and Criticals), no Notes or Correct ReportItems

Only use one of the following:

*-j* outputs an additional json file for fixes that can be used for statistical evaluation of the curation

*-n* use this parameter if you don't want a curation folder with EXMARaLDA error list to be created

Only use one of the following:

*-p* key=value is used for different parameters of the checks

OR

*-s* specifies a path to a settings file containing all needed p parameter pairs
The settings file needs to look like:

```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties> 
    <entry key="SEGMENTATION">HIAT</entry>
    <entry key="FSM">L:\\utilities\\inelutilities\\INEL_Segmentation_FSM_noColons.xml</entry>
    <entry key="CORPUSNAME">Kamas Corpus 1.0</entry>
</properties>
```


## Listing Functions

To get a list of all the available functions with their description and if they have a fixing option use the help parameter:

`java -Xmx3g -jar corpus-services-1.0.jar -h `

The usable functions can be found here: [List of corpus functions](https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/tree/develop/doc/List_of_corpus_functions.md).

## Help

You can see the complete command line functionality by running
`
java -Xmx3g -jar corpus-services-1.0.jar -h
`

## Examples

* Conversion to ISO/TEI for HIAT
`
java -Xmx3g -jar corpus-services-1.0.jar -i euroWiss -o eurowiss.html -c EXB2HIATISOTEI
`

For further example scripts see the [utilities folder](https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/tree/develop/utilities).

# Trouble Shooting

* the correct oracle java version needs to be installed (64bit) for the jar to be allowed to be allocated enough heap space!
