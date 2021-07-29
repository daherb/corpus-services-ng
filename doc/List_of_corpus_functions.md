# List of corpus functions

# Doxygen documentation

The most recent list of validation functions can be found in the [Doxygen documentation](https://gitlab.rrz.uni-hamburg.de/corpus-services/corpus-services/-/tree/develop/doc/doxygen)

# List of corpus functions

An automatically derived list can be viewed by calling corpus services with the help flag: 
<pre>
java -jar target\corpus-services-1.0.jar -h
</pre>

This is the help output list for Corpus Services v1.0 (12.05.2020, 07:00):

<pre>
<code>
usage: corpus-services -i <FILE PATH> -o <FILE PATH> -c <CORPUS FUNCTION> [-p <property=value>] [-f] [-h] [-e] [-j] [-s <FILE PATH>]
</code>
</pre>

Specify a corpus folder or file and a function to be applied

```
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
```

the available functions are:



<pre>
AddCSVMetadataToComa
CalculateAnnotatedTime
CmdiChecker
ComaAddTiersFromExbsCorrector
ComaApostropheChecker
ComaChartsGeneration
ComaFedoraIdentifierLengthChecker
ComaFileCoverageChecker
ComaKmlForLocations
ComaNSLinksChecker
ComaOverviewGeneration
ComaSegmentCountChecker
ComaTierOverviewCreator
ComaTiersDescriptionAnnotationPanelChecker
ComaTranscriptionsNameChecker
ComaXsdChecker
CorpusDataRegexReplacer
CorpusHTML
DuplicateTierContentChecker
EXB2HIATISOTEI
EXB2INELISOTEI
ElanPunctuationChecker
ExbAnnotationPanelCheck
ExbEventLinebreaksChecker
ExbEventTokenizationChecker
ExbFileCoverageChecker
ExbFileReferenceChecker
ExbForbiddenSymbolsChecker
ExbMP3Next2WavAdder
ExbRefTierChecker
ExbScriptMixChecker
ExbSegmentationChecker
ExbSegmenter
ExbSeparateTiersForDifferentSpeakers
ExbStructureChecker
ExbTierDisplayNameChecker
ExbTimestampsChecker
FilenameChecker
FlextextPunctuationChecker
GeneralTransformer
GenerateAnnotationPanel
HScoreHTML
HandlePidRegistration
IAAFunctionality
LanguageToolChecker
ListHTML
MakeTimelineConsistent
NgTierCheckerWithAnnotation
NgexmaraldaCorpusChecker
NormalizeEXB
PrettyPrintData
RemoveAbsolutePaths
RemoveAutoSaveExb
RemoveEmptyEvents
RemoveUnlinkedFiles
ReportStatistics
ScoreHTML
VikusViewer
XSLTChecker
ZipCorpus
</pre>

Descriptions of the available functions follow:

AddCSVMetadataToComa:   this class can be used from the command line to
insert data in a csv file   into an existing coma file there needs to be a
header with information of the   information in the columns the first line
has to consist of the sigle of the   speaker or name of the communication
the metadata should be assigned to
The function has a fixing option: false
The function can be used on:
ComaData

ExbCalculateAnnotatedTime:   This class calculates annotated time for an
exb file and computes the duration of each annotation in the exb.
The function has a fixing option: false
The function can be used on:
BasicTranscriptionData

CmdiChecker:   This class loads cmdi data and check for potential problems
with HZSK repository depositing.
The function has a fixing option: false
The function can be used on:
CmdiData

ComaAddTiersFromExbsCorrector:   This class loads coma data and for all
communications adds all tiers found in the linked exb as a key value pairs
to the description.
The function has a fixing option: true
The function can be used on:
ComaData

ComaApostropheChecker:   This class checks whether or not the coma file
contains an apostrophe '. If it does then these all apostrophes ' are
changed to apostrophes ’.
The function has a fixing option: true
The function can be used on:
ComaData

ComaChartsGeneration:   This class creates a sort- and filterable html
overview in table form  of the content of the coma file to make error
checking and harmonizing easier.
The function has a fixing option: false
The function can be used on:
ComaData

ComaFedoraIdentifierLengthChecker:   This class loads coma data and check
for potential problems with HZSK repository depositing; it checks the
Exmaralda .coma file for ID's that violate Fedora's PID limits.
The function has a fixing option: false
The function can be used on:
ComaData

ComaFileCoverageChecker:   This class is a validator for Coma file
references; it checks Exmaralda coma file for file references if a
referenced file does not exist, issues a warning;
The function has a fixing option: false
The function can be used on:
ComaData

ComaKmlForLocations:   This class identifies and lists fields which
contain location information; creates a list of different location names;
gets geo-coordinates for the location names via Google API.
The function has a fixing option: true
The function can be used on:
ComaData

ComaNSLinksChecker:   This class checks for existence of files linked in
the coma file.
The function has a fixing option: false
The function can be used on:
ComaData

ComaOverviewGeneration:   This class creates a sort- and filterable html
overview in table form  of the content of the coma file to make error
checking and harmonizing easier.
The function has a fixing option: false
The function can be used on:
ComaData

ComaSegmentCountChecker:   This class checks whether there are more than
one segmentation algorithms used in the coma file. If that is the case, it
issues warnings. If it ihas the fix option, it updates the segment counts
from the exbs.
The function has a fixing option: true
The function can be used on:
ComaData

ComaTierOverviewCreator:   This class creates a sort- and filterable html
overview in table form  of all tiers existing in the exbs linked in the
coma file to make error checking and harmonizing easier.
The function has a fixing option: false
The function can be used on:
ComaData

ComaTiersDescriptionAnnotationPanelChecker:   This class checks out that
all annotations are from the annotation specification file and that there
are no annotations in the coma file not existing in the annotation
specification file.
The function has a fixing option: false
The function can be used on:
ComaData AnnotationSpecification

ComaTranscriptionsNameChecker:   This class checks whether or not there is
a mismatch between basic and segmented names, basic and segmented file
names, plus their NSLinks for each communication in the coma file.
The function has a fixing option: true
The function can be used on:
ComaData

ComaXsdChecker:   This class validates the coma file with the respective
XML schema.
The function has a fixing option: false
The function can be used on:
ComaData

CorpusDataRegexReplacer:   This class issues warnings if a file contains a
certain RegEx and can also replace
The function has a fixing option: true
The function can be used on:
BasicTranscriptionData

CorpusHTML:   This class creates an html overview of the corpus needed for
the ingest into the repository.
The function has a fixing option: false
The function can be used on:
ComaData

DuplicateTierContentChecker:   This class takes a coma file, reads all
exbs linked there, reads them and checks if there are duplicate or
near-duplicate exbs in the corpus.
The function has a fixing option: false
The function can be used on:
ComaData

EXB2HIATISOTEI:   This class takes an exb as input and converts it into
ISO standard TEI format.
The function has a fixing option: false
The function can be used on:
BasicTranscriptionData

EXB2HIATISOTEI:   This class takes an exb as input and converts it into
ISO standard TEI format.
The function has a fixing option: false
The function can be used on:
BasicTranscriptionData

ElanPunctuationChecker:   This class issues warnings if the ELAN file
contains punctuation marks causing problems when import to FLEX is being
done.
The function has a fixing option: false
The function can be used on:
ELANData

ExbAnnotationPanelCheck:   This class checks whether the annotations in
exb files comply with the annotation specification panel.
The function has a fixing option: false
The function can be used on:
AnnotationSpecification BasicTranscriptionData

ExbEventLinebreaksChecker:   This class issues warnings if the exb file
contains linebreaks or fixes linebreaks in the events and adds those
warnings to the report which it returns.
The function has a fixing option: true
The function can be used on:
BasicTranscriptionData

ExbEventTokenizationChecker:   This class issues warnings if the
tokenization tier contains events with internal whitespace characters
The function has a fixing option: false
The function can be used on:
BasicTranscriptionData

ExbFileCoverageChecker:   This class checks whether files are both in the
exb file and file system.
The function has a fixing option: false
The function can be used on:
BasicTranscriptionData

ExbFileReferenceChecker:   This class is a validator for EXB-file's
references; it checks Exmaralda .exb file for file references if a
referenced file does not exist, issues a warning;
The function has a fixing option: false
The function can be used on:
BasicTranscriptionData

ExbForbiddenSymbolsChecker:   This class checks if there are forbidden
symbols in the transcription.
The function has a fixing option: false
The function can be used on:
BasicTranscriptionData

ExbMP3Next2WavAdder:   This class adds the path to an MP3 file next to the
WAV file linked as a recording in an exb file.
The function has a fixing option: true
The function can be used on:
BasicTranscriptionData

ExbRefTierChecker:   This class checks reference tiers in exb files and
finds out whether or not the order of the numbering and speaker reference
are correct and if there are any mistakes in the ref tiers, it corrects
them thanks to its fix function.
The function has a fixing option: true
The function can be used on:
BasicTranscriptionData

ExbScriptMixChecker:   A functions that checks for mixed scripts (e.g.
Cyrillic/Latin) in the transcription tiers of EXMARaLDA basic
transcriptions and issues warnings if they are found
The function has a fixing option: false
The function can be used on:
BasicTranscriptionData

ExbSegmentationChecker:   This class checks Exmaralda exb files for
segmentation problems, returns the errors in the Report and in the
ExmaErrors and if the fix option is specified it creates segmented exs
from the exbs that don't contain errors.
The function has a fixing option: true
The function can be used on:
BasicTranscriptionData

ExbSegmentationChecker:   This class checks Exmaralda exb files for
segmentation problems, returns the errors in the Report and in the
ExmaErrors and if the fix option is specified it creates segmented exs
from the exbs that don't contain errors.
The function has a fixing option: true
The function can be used on:
BasicTranscriptionData

ExbSeparateTiersForDifferentSpeakers:   This class fixes tier alignment
for basic transctiption files with multiple speakers
The function has a fixing option: true
The function can be used on:
BasicTranscriptionData

ExbStructureChecker:   This class checks basic transcription files for
structural anomalies.
The function has a fixing option: false
The function can be used on:
BasicTranscriptionData

ExbTierDisplayNameChecker:   This class checks exb tiers and finds out if
there is a mismatch between category, speaker abbreviation and display
name for each tier.
The function has a fixing option: false
The function can be used on:
BasicTranscriptionData

ExbTimestampsChecker:   This class issues warnings if it finds missing
timestamps in the timeline at the beginning or end of a segment chein
The function has a fixing option: false
The function can be used on:
SegmentedTranscriptionData

ComaFilenameChecker:   This class checks if all file names linked in the
coma file to be deposited in HZSK repository; checks if there is a file
which is not named according to coma file.
The function has a fixing option: false
The function can be used on:
ComaData

FlextextPunctuationChecker:   This class issues warnings if the flextext
file contains punctuation marks causing problems when conversion to
EXMARaLDA is being done
The function has a fixing option: true
The function can be used on:
FlextextData

GeneralTransformer:   This class runs an xsl transformation on files.
The function has a fixing option: true
The function can be used on:


GenerateAnnotationPanel:   This class generates an annotation
specification panel from the basic transcription files (exb).
The function has a fixing option: false
The function can be used on:
BasicTranscriptionData ComaData

HScoreHTML:   This class creates an html visualization in the HScore
format from an exb.
The function has a fixing option: false
The function can be used on:
BasicTranscriptionData

HandlePidRegistration:   This class loads CMDI data and retrieves already
existing or newly registered Handle PIDs for URLs from specific XML
elements.
The function has a fixing option: false
The function can be used on:
CmdiData

IAAFunctionality:   This class calculates IAA according to Krippendorff's
alpha for exb files; only cares for annotation labels, assuming that
transcription structure and text remains the same. Checks and puts them in
the error lists if different versions of the same file have different
annotations for the same event/token. Moreover, this functionality
includes the inter-annotator agreement: percentage of overlapping choices
between the annotators.
The function has a fixing option: false
The function can be used on:
BasicTranscriptionData

LanguageToolChecker:   This class takes a CorpusDataObject that is an Exb,
checks if there are spell or grammar errors in German, English or Russian
using LanguageTool and returns the errors in the Report and in the
ExmaErrors.
The function has a fixing option: false
The function can be used on:
BasicTranscriptionData

ListHTML:   This class creates an html visualization in the List format
from an exb.
The function has a fixing option: false
The function can be used on:
BasicTranscriptionData

ExbMakeTimelineConsistent:   This class makes the timeline of exbs
consistent by removing incorrect timepoints and interpolates timeline
items without time info if the parameter is set.
The function has a fixing option: true
The function can be used on:
BasicTranscriptionData

NgTierCheckerWithAnnotation:   This class checks out if all annotations
for Nganasan Corpus are from the annotation specification file and there
are no annotations in the coma file not present in the annotation
specification file.
The function has a fixing option: false
The function can be used on:
ComaData AnnotationSpecification

NgexmaraldaCorpusChecker:   This class is the check procedure for the
Nganasan Corpus and checks if the file names in the corpus comply with the
coma file.
The function has a fixing option: false
The function can be used on:
ComaData

ExbNormalize:   This class normalises the basic transcription data using
the EXMARaLDA function and fixes white spaces if set by a parameter.
The function has a fixing option: true
The function can be used on:
BasicTranscriptionData

PrettyPrintData:   This class takes XML corpusdata and formats it in the
same way to avoid merge conflicts.
The function has a fixing option: true
The function can be used on:
BasicTranscriptionData UnspecifiedXMLData ComaData
SegmentedTranscriptionData

RemoveAbsolutePaths:   This class finds paths that are absolute in files
and replaces them with paths relative to the corpus folder.
The function has a fixing option: true
The function can be used on:
BasicTranscriptionData SegmentedTranscriptionData ComaData

RemoveAutoSaveExb:   This class removes auto save information present in
exb and exs files.
The function has a fixing option: true
The function can be used on:
BasicTranscriptionData SegmentedTranscriptionData

RemoveEmptyEvents:   This class removes empty events present in exb and
exs files.
The function has a fixing option: true
The function can be used on:
BasicTranscriptionData SegmentedTranscriptionData

RemoveUnlinkedFiles:   This class takes a coma file and removes all files
from the directory/subdirectories which are not linked somewhere in the
coma file.
The function has a fixing option: false
The function can be used on:
ComaData

ReportStatistics:   This class creates or updates the html statistics
report from the report output file outputted by the corpus services.
The function has a fixing option: false
The function can be used on:
ComaData

ScoreHTML:   This class creates an html visualization in the Score format
from an exb.
The function has a fixing option: false
The function can be used on:
BasicTranscriptionData

VikusViewer:   This class creates an config files needed for the
vikus-viewer software.
The function has a fixing option: false
The function can be used on:
ComaData

XSLTChecker:   This class runs many little checks specified in a XSLT
stylesheet and adds them to the report.
The function has a fixing option: false
The function can be used on:
BasicTranscriptionData ComaData

ZipCorpus:   This class takes a coma file and creates a zip file
containing all important corpus file in the resources folder. It only
takes exb, exs, coma, pdf and optionally mp3, and the folder structure.
The function has a fixing option: false
The function can be used on:
ComaData
