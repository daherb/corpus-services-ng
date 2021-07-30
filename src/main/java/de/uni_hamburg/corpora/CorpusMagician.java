package de.uni_hamburg.corpora;

import de.uni_hamburg.corpora.validation.ComaAddTiersFromExbsCorrector;
import de.uni_hamburg.corpora.validation.CmdiChecker;
import de.uni_hamburg.corpora.publication.ZipCorpus;
import de.uni_hamburg.corpora.conversion.EXB2HIATISOTEI;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import de.uni_hamburg.corpora.validation.ComaApostropheChecker;
import de.uni_hamburg.corpora.validation.ComaNSLinksChecker;
import de.uni_hamburg.corpora.validation.ComaOverviewGeneration;
import de.uni_hamburg.corpora.validation.ComaXsdChecker;
import de.uni_hamburg.corpora.validation.GenerateAnnotationPanel;
import de.uni_hamburg.corpora.validation.ComaFedoraIdentifierLengthChecker;
import de.uni_hamburg.corpora.validation.ComaSegmentCountChecker;
import de.uni_hamburg.corpora.validation.ExbFileReferenceChecker;
import de.uni_hamburg.corpora.validation.ExbFileCoverageChecker;
import de.uni_hamburg.corpora.validation.ExbAnnotationPanelCheck;
import de.uni_hamburg.corpora.validation.ExbRefTierChecker;
import de.uni_hamburg.corpora.validation.ExbCalculateAnnotatedTime;
import de.uni_hamburg.corpora.validation.ExbStructureChecker;
import de.uni_hamburg.corpora.validation.ComaFileCoverageChecker;
import de.uni_hamburg.corpora.validation.ComaFilenameChecker;
import de.uni_hamburg.corpora.validation.IAAFunctionality;
import de.uni_hamburg.corpora.validation.ExbNormalize;
import de.uni_hamburg.corpora.validation.NgexmaraldaCorpusChecker;
import de.uni_hamburg.corpora.validation.NgTierCheckerWithAnnotation;
import de.uni_hamburg.corpora.validation.PrettyPrintData;
import de.uni_hamburg.corpora.validation.RemoveAbsolutePaths;
import de.uni_hamburg.corpora.validation.RemoveAutoSaveExb;
import de.uni_hamburg.corpora.validation.ExbTierDisplayNameChecker;
import de.uni_hamburg.corpora.validation.ComaTiersDescriptionAnnotationPanelChecker;
import de.uni_hamburg.corpora.validation.XSLTChecker;
import de.uni_hamburg.corpora.validation.CorpusDataRegexReplacer;
import de.uni_hamburg.corpora.validation.ExbEventLinebreaksChecker;
import de.uni_hamburg.corpora.validation.ExbMakeTimelineConsistent;
import de.uni_hamburg.corpora.validation.ExbScriptMixChecker;
import de.uni_hamburg.corpora.visualization.CorpusHTML;
import de.uni_hamburg.corpora.visualization.ListHTML;
import de.uni_hamburg.corpora.visualization.ScoreHTML;
import de.uni_hamburg.corpora.validation.ComaKmlForLocations;
import de.uni_hamburg.corpora.conversion.AddCSVMetadataToComa;
import de.uni_hamburg.corpora.publication.HandlePidRegistration;
import de.uni_hamburg.corpora.publication.RemoveUnlinkedFiles;
import de.uni_hamburg.corpora.utilities.PrettyPrinter;
import de.uni_hamburg.corpora.validation.ComaChartsGeneration;
import de.uni_hamburg.corpora.validation.ComaTierOverviewCreator;
import de.uni_hamburg.corpora.validation.GeneralTransformer;
import de.uni_hamburg.corpora.validation.RemoveEmptyEvents;
import de.uni_hamburg.corpora.validation.ComaTranscriptionsNameChecker;
import de.uni_hamburg.corpora.validation.DuplicateTierContentChecker;
import de.uni_hamburg.corpora.validation.ExbMP3Next2WavAdder;
import de.uni_hamburg.corpora.validation.ExbSegmentationChecker;
import de.uni_hamburg.corpora.validation.LanguageToolChecker;
import de.uni_hamburg.corpora.validation.ElanPunctuationChecker;
import de.uni_hamburg.corpora.validation.FlextextPunctuationChecker;
import de.uni_hamburg.corpora.visualization.HScoreHTML;
import de.uni_hamburg.corpora.validation.ReportStatistics;
import de.uni_hamburg.corpora.visualization.VikusViewer;
import de.uni_hamburg.corpora.validation.ExbEventTokenizationChecker;
import de.uni_hamburg.corpora.validation.ExbTimestampsChecker;
import de.uni_hamburg.corpora.validation.ExbForbiddenSymbolsChecker;
import de.uni_hamburg.corpora.validation.ExbSeparateTiersForDifferentSpeakers;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.nio.file.Paths;
import java.util.Collections;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

/**
 * This class has a Corpus and a Corpus Function as a field and is able to run a
 * Corpus Function on a corpus in a main method.
 *
 * @author fsnv625
 */
public class CorpusMagician {

    //the whole corpus I want to run checks on
    static Corpus corpus;
    //a collection of unordered files I want to run checks on
    Collection<CorpusData> cdc;
    //a single file I want to run checks on
    CorpusData corpusData;
    //Basedirectory if it exists
    static URL basedirectory;
    //all functions there are in the code
    static Collection<String> allExistingCFs;
    //all functions that should be run
    static Collection<String> chosencorpusfunctions = new ArrayList<String>();
    static Collection<CorpusFunction> corpusfunctions = new ArrayList<CorpusFunction>();
    //need to have Map or something for this
    static Collection<Class<? extends CorpusData>> neededcorpusdatatypes = new ArrayList<Class<? extends CorpusData>>();
    //the final Report
    static Report report = new Report();
    //a list of all the available corpus data (no java objects, just URLs)
    static ArrayList<URL> alldata = new ArrayList<URL>();
    static CorpusIO cio = new CorpusIO();
    static boolean fixing = false;
    static boolean iserrorsonly = false;
    static boolean isfixesjson = false;
    static boolean nocurationfolder = false;
    static CommandLine cmd = null;
    //the final Exmaralda error list
    public static ExmaErrorList exmaError = new ExmaErrorList();
    static Properties cfProperties = new Properties();
    static PrettyPrinter pp = new PrettyPrinter();
    static String settingsfilepath = "settings.xml";
    //Properties Key Names
    static String fsm = "fsm";
    static String segmentation = "segmentation";
    static String lang = "lang";
    static String spelllang = "spelllang";
    static String corpusname = "corpusname";
    static String kml = "kml";
    static String mode = "mode";
    //static URL reportlocation;
    static ArrayList<URL> reportlocations = new ArrayList<>();
    static URL inputurl;
    static boolean isCorpus = false;
    static boolean isCollection = false;

    public CorpusMagician() {
    }

    //TODO we need a webservice for this functionality too
    //in the future (for repo and external users)
    public static void main(String[] args) {

        //first args needs to be the URL
        //check if it's a filepath, we could just convert it to an url    
        System.out.println("CorpusMagician is now doing its magic.");
        CorpusMagician corpuma = new CorpusMagician();
        try {
            //create the options for the commandline
            createCommandLineOptions(args);
            //read the options specified on the commandline
            readCommandLineOptions();
            //convert strings from commandline to corpusfunction objects
            corpusfunctions = corpusFunctionStrings2Classes(chosencorpusfunctions);
            //find out which files the chosencorpusfunctions need as input
            for (CorpusFunction cf : corpusfunctions) {
                for (Class<? extends CorpusData> cecd : cf.getIsUsableFor()) {
                    if (!neededcorpusdatatypes.contains(cecd)) {
                        neededcorpusdatatypes.add(cecd);
                    }
                }
            }
            //the input can be a filepath or an url pointing to a file or a folder
            //if the input is a coma file we have a structured corpus
            //if it is a folder or another corpus file we don't
            //we can maybe minmize the heapspace when having a structured corpus
            //we only want to have the data as objects that will be really needed in the functions
            corpuma.initDataWithURL(inputurl, neededcorpusdatatypes);
            //We can only init an corpus object if we know it's a structured corpus
            //now all chosen functions must be run
            //if we have the coma file, we just give Coma as Input and the Functions need to take care of using the
            //iterating function
            report = corpuma.runChosencorpusfunctions();
            createReports();
        } catch (MalformedURLException ex) {
            report.addException(ex, "The given URL was incorrect");
        } catch (IOException ex) {
            report.addException(ex, "A file could not be read");
        } catch (ParserConfigurationException ex) {
            report.addException(ex, "A file could not be parsed");
        } catch (TransformerException ex) {
            report.addException(ex, "A transformation error occured");
        } catch (SAXException ex) {
            report.addException(ex, "An XSLT error occured");
        } catch (JexmaraldaException ex) {
            report.addException(ex, "An Exmaralda file reading error occured");
        } catch (URISyntaxException ex) {
            report.addException(ex, "A URI was incorrect");
        } catch (XPathExpressionException ex) {
            report.addException(ex, "An Xpath expression was incorrect");
        } catch (ClassNotFoundException ex) {
            report.addException(ex, "Class not found");
        } catch (JDOMException ex) {
            report.addException(ex, "JDOM error");
        }

    }

//Give it a path to a parameters file that tells you
//which functions with which parameters should be
//run on which files
    public void readConfig(URL url) {
        //this depends on how this file will be structured
    }

    //this one can write a configfile with the workflow in the
    //selected format
    public void writeConfig(URL url) {
        //needs to have more params
        //this depends on how this file will be structured
    }

    public void registerCorpusFunction(CorpusFunction cf) {
        allExistingCFs.add(cf.getClass().getName());
    }

    //creates a corpus object from an URL (filepath or "real" url)
    //we need to make a difference between an unsorted folder, a miscellaneous file or a Coma file which represents a complete folder structure of the corpus
    public void initDataWithURL(URL url, Collection<Class<? extends CorpusData>> clcds) throws MalformedURLException, SAXException, JexmaraldaException, URISyntaxException, IOException, ClassNotFoundException, JDOMException {
        if (cio.isDirectory(url)) {
            //TODO
            //only read the filetypes from clcds!
            cdc = cio.read(url, clcds);
            basedirectory = url;
            isCollection = true;
        } else {
            CorpusData cdata = cio.readFileURL(url);
            //get the basedirectory
            basedirectory = cdata.getParentURL();
            //it could be a ComaFile if it is a Metadata file
            if (cdata instanceof ComaData) {
                //if it is we set the boolean
                isCorpus = true;
                System.out.println("It's a corpus");
                //TODO
                //only read the filetypes from clcds!
                corpus = new Corpus((ComaData) cdata, clcds);
                //otherwise it is a single file I want to check
            } else {
                corpusData = cdata;
            }
        }
    }

    //creates a list of all the available data from an url (being a file oder directory)
    public Collection<URL> createListofData(URL url) throws URISyntaxException, IOException {
        //add just that url if its a file
        //adds the urls recursively if its a directory
        return cio.URLtoList(url);
    }

    //checks which functions exist in the code by checking for implementations of the corpus function interface
    //this shows that it doesn't work to just check for implementations of corpus functions
    //probably need to check for implementations of CorpusFunction?
    //TODO
    public static Collection<String> getAllExistingCFs() {
        allExistingCFs = new ArrayList<String>();
        allExistingCFs.add("ComaApostropheChecker");
        allExistingCFs.add("ComaNSLinksChecker");
        allExistingCFs.add("ComaOverviewGeneration");
        allExistingCFs.add("ComaChartsGeneration");
        allExistingCFs.add("ZipCorpus");
        allExistingCFs.add("HandlePidRegistration");
        allExistingCFs.add("RemoveUnlinkedFiles");
        allExistingCFs.add("ComaSegmentCountChecker");
        allExistingCFs.add("ExbFileReferenceChecker");
        allExistingCFs.add("ExbFileCoverageChecker");
        allExistingCFs.add("ExbAnnotationPanelCheck");
        allExistingCFs.add("EXB2INELISOTEI");
        allExistingCFs.add("EXB2HIATISOTEI");
        allExistingCFs.add("ExbStructureChecker");
        allExistingCFs.add("ComaFileCoverageChecker");
        allExistingCFs.add("NormalizeEXB");
        allExistingCFs.add("PrettyPrintData");
        allExistingCFs.add("RemoveAbsolutePaths");
        allExistingCFs.add("RemoveAutoSaveExb");
        allExistingCFs.add("XSLTChecker");
        allExistingCFs.add("ComaAddTiersFromExbsCorrector");
        allExistingCFs.add("ComaXsdChecker");
        allExistingCFs.add("NgexmaraldaCorpusChecker");
        allExistingCFs.add("FilenameChecker");
        allExistingCFs.add("CmdiChecker");
        allExistingCFs.add("ComaTiersDescriptionAnnotationPanelChecker");
        allExistingCFs.add("ExbTierDisplayNameChecker");
        allExistingCFs.add("NgTierCheckerWithAnnotation");
        //allExistingCFs.add("XsltCheckerInel");
        allExistingCFs.add("GenerateAnnotationPanel");
        allExistingCFs.add("CorpusDataRegexReplacer");
        allExistingCFs.add("ScoreHTML");
        allExistingCFs.add("HScoreHTML");
        allExistingCFs.add("CorpusHTML");
        allExistingCFs.add("IAAFunctionality");
        allExistingCFs.add("ListHTML");
        allExistingCFs.add("ExbEventLinebreaksChecker");
        allExistingCFs.add("MakeTimelineConsistent");
        allExistingCFs.add("ExbSegmentationChecker");
        allExistingCFs.add("CalculateAnnotatedTime");
        allExistingCFs.add("AddCSVMetadataToComa");
        allExistingCFs.add("ComaKmlForLocations");
        allExistingCFs.add("RemoveEmptyEvents");
        allExistingCFs.add("ComaTranscriptionsNameChecker");
        allExistingCFs.add("ComaTierOverviewCreator");
        allExistingCFs.add("GeneralTransformer");
        allExistingCFs.add("ComaFedoraIdentifierLengthChecker");
        allExistingCFs.add("ExbMP3Next2WavAdder");
        allExistingCFs.add("ExbRefTierChecker");
        allExistingCFs.add("ReportStatistics");
        allExistingCFs.add("ExbSegmenter");
        allExistingCFs.add("ExbScriptMixChecker");
        allExistingCFs.add("DuplicateTierContentChecker");
        allExistingCFs.add("LanguageToolChecker");
        allExistingCFs.add("VikusViewer");
        allExistingCFs.add("ExbEventTokenizationChecker");
        allExistingCFs.add("ExbTimestampsChecker");
        allExistingCFs.add("ExbForbiddenSymbolsChecker");
        allExistingCFs.add("ElanPunctuationChecker");
        allExistingCFs.add("FlextextPunctuationChecker");
        allExistingCFs.add("ExbSeparateTiersForDifferentSpeakers");
        Collections.sort((List<String>) allExistingCFs);
        return allExistingCFs;
    }

    public static String getAllExistingCFsAsString() {
        String all = "";
        for (Iterator<String> it = getAllExistingCFs().iterator(); it.hasNext();) {
            String s = it.next();
            all = all + "\n" + s;
        }
        return all;
    }

    public static Collection<CorpusFunction> getAllExistingCFsAsCFs() {

        return corpusFunctionStrings2Classes(getAllExistingCFs());
    }

    //TODO checks which functions can be run on specified data
    public Collection<CorpusFunction> getUsableFunctions(CorpusData cd) {
        //cf.IsUsableFor();
        //some switch or if else statements for the possible java objects
        //and a list(?) which function can be apllied to what/which functions exist?
        Collection<CorpusFunction> usablecorpusfunctions = null;
        return usablecorpusfunctions;
    }

    //TODO return default functions, this is a list that needs to be somewhere
    //or maybe its an option a corpusfunction can have?
    public Collection<CorpusFunction> getDefaultUsableFunctions() {
        Collection<CorpusFunction> defaultcorpusfunctions = null;
        return defaultcorpusfunctions;
    }

    //TODO a dialog to choose functions you want to apply
    public Collection<String> chooseFunctionDialog() {
        chosencorpusfunctions = null;
        //add the chosen Functions
        return chosencorpusfunctions;
    }

    public static Collection<CorpusFunction> corpusFunctionStrings2Classes(Collection<String> corpusfunctionstrings) {
        Collection<CorpusFunction> cf2strcorpusfunctions = new ArrayList<CorpusFunction>();
        for (String function : corpusfunctionstrings) {
            switch (function.toLowerCase()) {
                case "comaapostrophechecker":
                    ComaApostropheChecker cac = new ComaApostropheChecker();
                    cf2strcorpusfunctions.add(cac);
                    break;
                case "comanslinkschecker":
                    ComaNSLinksChecker cnslc = new ComaNSLinksChecker();
                    cf2strcorpusfunctions.add(cnslc);
                    break;
                case "comaoverviewgeneration":
                    ComaOverviewGeneration cog = new ComaOverviewGeneration();
                    if (cfProperties != null) {
                        // Pass on the configuration parameter
                        if (cfProperties.containsKey(mode) && cfProperties.getProperty(mode).toLowerCase().equals("inel")) {
                            cog.setInel();
                            System.out.println("Mode set to inel");
                        }
                    }
                    cf2strcorpusfunctions.add(cog);
                    break;
                case "comachartsgeneration":
                    ComaChartsGeneration coc = new ComaChartsGeneration();if (cfProperties != null) {
                        // Pass on the configuration parameter
                        if (cfProperties.containsKey(mode) && cfProperties.getProperty(mode).toLowerCase().equals("inel")) {
                            coc.setInel();
                            System.out.println("Mode set to inel");
                        }
                    }
                    cf2strcorpusfunctions.add(coc);
                    break;
                case "comasegmentcountchecker":
                    ComaSegmentCountChecker cscc = new ComaSegmentCountChecker();
                    cf2strcorpusfunctions.add(cscc);
                    break;
                case "exbfilereferencechecker":
                    ExbFileReferenceChecker efrc = new ExbFileReferenceChecker();
                    cf2strcorpusfunctions.add(efrc);
                    break;
                case "exbfilecoveragechecker":
                    ExbFileCoverageChecker efcc = new ExbFileCoverageChecker();
                    cf2strcorpusfunctions.add(efcc);
                    break;
                case "exbannotationpanelcheck":
                    ExbAnnotationPanelCheck eapc = new ExbAnnotationPanelCheck();
                    cf2strcorpusfunctions.add(eapc);
                    break;
                case "comafilecoveragechecker":
                    ComaFileCoverageChecker fcc = new ComaFileCoverageChecker();
                    if (cfProperties != null) {
                        // Pass on the configuration parameter
                        if (cfProperties.containsKey(mode) && cfProperties.getProperty(mode).equals("inel")) {
                            fcc.addFileEndingWhiteListString("flextext");
                            fcc.addWhiteListString("report-output.html");
                            fcc.addWhiteListString("Segmentation_Errors.xml");
                            fcc.addWhiteListString("Structure_Errors.xml");
                            System.out.println("Mode set to inel");
                        }
                    }
                    cf2strcorpusfunctions.add(fcc);
                    break;
                case "prettyprintdata":
                    PrettyPrintData pd = new PrettyPrintData();
                    cf2strcorpusfunctions.add(pd);
                    break;
                case "removeabsolutepaths":
                    RemoveAbsolutePaths rap = new RemoveAbsolutePaths();
                    cf2strcorpusfunctions.add(rap);
                    break;
                case "removeautosaveexb":
                    RemoveAutoSaveExb rase = new RemoveAutoSaveExb();
                    cf2strcorpusfunctions.add(rase);
                    break;
                case "xsltchecker":
                    XSLTChecker xc = new XSLTChecker();
                    if (cfProperties != null) {
                        // Pass on the configuration parameter
                        if (cfProperties.containsKey(mode) && cfProperties.getProperty(mode).toLowerCase().equals("inel")) {
                            xc.setXSLresource("/xsl/inel-checks.xsl");
                            System.out.println("Mode set to inel");
                        }
                        if (cfProperties.containsKey(fsm)) {
                            xc.setFSMpath(cfProperties.getProperty(fsm));
                            System.out.println("FSM set to " + cfProperties.getProperty(fsm));
                        }
                    }
                    cf2strcorpusfunctions.add(xc);
                    break;
                case "comaaddtiersfromexbscorrector":
                    ComaAddTiersFromExbsCorrector catfec = new ComaAddTiersFromExbsCorrector();
                    cf2strcorpusfunctions.add(catfec);
                    break;
                case "comaxsdchecker":
                    ComaXsdChecker cxsd = new ComaXsdChecker();
                    cf2strcorpusfunctions.add(cxsd);
                    break;
                case "ngexmaraldacorpuschecker":
                    NgexmaraldaCorpusChecker ngex = new NgexmaraldaCorpusChecker();
                    cf2strcorpusfunctions.add(ngex);
                    break;
                case "filenamechecker":
                    ComaFilenameChecker fnc = new ComaFilenameChecker();
                    cf2strcorpusfunctions.add(fnc);
                    break;
                case "cmdichecker":
                    CmdiChecker cmdi = new CmdiChecker();
                    cf2strcorpusfunctions.add(cmdi);
                    break;
                case "comafedoraidentifierlengthchecker":
                    ComaFedoraIdentifierLengthChecker cplc = new ComaFedoraIdentifierLengthChecker();
                    cf2strcorpusfunctions.add(cplc);
                    break;
                case "comatranscriptionsnamechecker":
                    ComaTranscriptionsNameChecker cnc = new ComaTranscriptionsNameChecker();
                    cf2strcorpusfunctions.add(cnc);
                    break;
                case "comatiersdescriptionannotationpanelchecker":
                    ComaTiersDescriptionAnnotationPanelChecker tcwa = new ComaTiersDescriptionAnnotationPanelChecker();
                    cf2strcorpusfunctions.add(tcwa);
                    break;
                case "exbtierdisplaynamechecker":
                    ExbTierDisplayNameChecker tc = new ExbTierDisplayNameChecker();
                    cf2strcorpusfunctions.add(tc);
                    break;
                case "ngtiercheckerwithannotation":
                    NgTierCheckerWithAnnotation ngtcwa = new NgTierCheckerWithAnnotation();
                    cf2strcorpusfunctions.add(ngtcwa);
                    break;
                case "exb2inelisotei":
                    EXB2HIATISOTEI eiit = new EXB2HIATISOTEI();
                    eiit.setInel();
                    if (cfProperties != null) {
                        // Pass on the configuration parameter
                        if (cfProperties.containsKey(lang)) {
                            eiit.setLanguage(cfProperties.getProperty(lang));
                            System.out.println("Language set to " + cfProperties.getProperty(lang));
                        }
                        if (cfProperties.containsKey(fsm)) {
                            eiit.setFSM(cfProperties.getProperty(fsm));
                            System.out.println("FSM set to " + cfProperties.getProperty(fsm));
                        }
                    }
                    cf2strcorpusfunctions.add(eiit);
                    break;
                //Maybe get rid of those special cases too!
                case "exb2inelisoteisel":
                    EXB2HIATISOTEI eiitsel = new EXB2HIATISOTEI();
                    eiitsel.setInel();
                    if (cfProperties.containsKey(fsm)) {
                        eiitsel.setFSM(cfProperties.getProperty(fsm));
                        System.out.println("FSM set to " + cfProperties.getProperty(fsm));
                    }
                    eiitsel.setLanguage("sel");
                    cf2strcorpusfunctions.add(eiitsel);
                    break;
                case "exb2inelisoteidlg":
                    EXB2HIATISOTEI eiitdlg = new EXB2HIATISOTEI();
                    eiitdlg.setInel();
                    if (cfProperties.containsKey(fsm)) {
                        eiitdlg.setFSM(cfProperties.getProperty(fsm));
                        System.out.println("FSM set to " + cfProperties.getProperty(fsm));
                    }
                    eiitdlg.setLanguage("dlg");
                    cf2strcorpusfunctions.add(eiitdlg);
                    break;
                case "exb2inelisoteixas":
                    EXB2HIATISOTEI eiitxas = new EXB2HIATISOTEI();
                    eiitxas.setInel();
                    if (cfProperties.containsKey(fsm)) {
                        eiitxas.setFSM(cfProperties.getProperty(fsm));
                        System.out.println("FSM set to " + cfProperties.getProperty(fsm));
                    }
                    eiitxas.setLanguage("xas");
                    cf2strcorpusfunctions.add(eiitxas);
                    break;
                case "exb2hiatisotei":
                    EXB2HIATISOTEI ehit = new EXB2HIATISOTEI();
                    if (cfProperties != null) {
                        // Pass on the configuration parameter
                        if (cfProperties.containsKey(lang)) {
                            ehit.setLanguage(cfProperties.getProperty(lang));
                            System.out.println("Language set to " + cfProperties.getProperty(lang));
                        }
                        if (cfProperties.containsKey(mode)) {
                            if (cfProperties.getProperty(mode).toLowerCase().equals("inel")) {
                                ehit.setInel();
                                System.out.println("Mode set to inel");
                            } else if (cfProperties.getProperty(mode).toLowerCase().equals("token")) {
                                ehit.setToken();
                                System.out.println("Mode set to token");
                            }
                        }
                        if (cfProperties.containsKey(fsm)) {
                            ehit.setFSM(cfProperties.getProperty(fsm));
                            System.out.println("FSM set to " + cfProperties.getProperty(fsm));
                        }
                    }
                    cf2strcorpusfunctions.add(ehit);
                    break;
                case "normalizeexb":
                    ExbNormalize ne = new ExbNormalize();
                    if (cfProperties != null) {
                        // Pass on the configuration parameter
                        if (cfProperties.containsKey("whitespace")) {
                            ne.setfixWhiteSpaces(cfProperties.getProperty("whitespace"));
                            System.out.println("FixWhitespace set to " + cfProperties.getProperty("whitespace"));
                        }
                    }
                    cf2strcorpusfunctions.add(ne);
                    break;
                case "generateannotationpanel":
                    GenerateAnnotationPanel gap = new GenerateAnnotationPanel();
                    cf2strcorpusfunctions.add(gap);
                    break;
                case "iaafunctionality":
                    IAAFunctionality iaa = new IAAFunctionality();
                    cf2strcorpusfunctions.add(iaa);
                    break;
                case "comakmlforlocations":
                    ComaKmlForLocations ckml = new ComaKmlForLocations();
                    if (cfProperties != null) {
                        // Pass on the configuration parameter
                        if (cfProperties.containsKey(kml)) {
                            ckml.setKMLFilePath(cfProperties.getProperty(kml));
                            System.out.println("KML file path set to " + cfProperties.getProperty(kml));
                        }
                    }
                    cf2strcorpusfunctions.add(ckml);
                    break;
                case "reportstatistics":
                    ReportStatistics rs = new ReportStatistics();
                    cf2strcorpusfunctions.add(rs);
                    break;
                case "corpusdataregexreplacer":
                    //ToDo                   
                    CorpusDataRegexReplacer cdrr = new CorpusDataRegexReplacer();
                    //try custom properties for the different corpusfunctions
                    if (cfProperties != null) {
                        // Pass on the configuration parameter
                        if (cfProperties.containsKey("replace")) {
                            cdrr.setReplace(cfProperties.getProperty("replace"));
                            System.out.println("Replace set to " + cfProperties.getProperty("replace"));
                        }
                        if (cfProperties.containsKey("replacement")) {
                            cdrr.setReplacement(cfProperties.getProperty("replacement"));
                            System.out.println("Replacement set to " + cfProperties.getProperty("replacement"));
                        }
                        if (cfProperties.containsKey("xpathcontext")) {
                            cdrr.setXpathContext(cfProperties.getProperty("xpathcontext"));
                            System.out.println("Xpath set to " + cfProperties.getProperty("xpathcontext"));
                        }
                        if (cfProperties.containsKey("coma")) {
                            cdrr.setComa(cfProperties.getProperty("coma"));
                            System.out.println("Replace in Coma set to " + cfProperties.getProperty("coma"));
                        }
                    }
                    cf2strcorpusfunctions.add(cdrr);
                    break;
                case "zipcorpus":
                    ZipCorpus zc = new ZipCorpus();
                    if (cfProperties != null) {
                        // Pass on the configuration parameter
                        if (cfProperties.containsKey("source_folder")) {
                            zc.setSourceFolder(cfProperties.getProperty("source_folder"));
                            System.out.println("Location of source folder set to " + cfProperties.getProperty("source_folder"));
                        }
                        if (cfProperties.containsKey("output_zip_file")) {
                            zc.setOutputFile(cfProperties.getProperty("output_zip_file"));
                            System.out.println("Location of output file set to " + cfProperties.getProperty("output_zip_file"));
                        }
                        if (cfProperties.containsKey("audio")) {
                            zc.setWithAudio(cfProperties.getProperty("audio"));
                            System.out.println("Should contain audio set to " + cfProperties.getProperty("audio"));
                        }
                    }
                    cf2strcorpusfunctions.add(zc);
                    break;
                case "handlepidregistration":
                    HandlePidRegistration hppr = new HandlePidRegistration();
                    if (cfProperties != null) {
                        // Pass on the configuration parameter
                        if (cfProperties.containsKey("user")) {
                            hppr.setUser(cfProperties.getProperty("user"));
                            System.out.println("User set to " + cfProperties.getProperty("user"));
                        }
                        if (cfProperties.containsKey("pass")) {
                            hppr.setPass(cfProperties.getProperty("pass"));
                            System.out.println("Password set to " + cfProperties.getProperty("pass").replaceAll(".", "*"));
                            //System.out.println("Password set to " + cfProperties.getProperty("pass"));
                        }
                        if (cfProperties.containsKey("prefix")) {
                            hppr.setHandlePrefix(cfProperties.getProperty("prefix"));
                            System.out.println("Prefix set to " + cfProperties.getProperty("prefix"));
                        }
                    }
                    cf2strcorpusfunctions.add(hppr);
                    break;
                case "removeunlinkedfiles":
                    RemoveUnlinkedFiles ruf = new RemoveUnlinkedFiles();                    
                    cf2strcorpusfunctions.add(ruf);
                    break;
                case "scorehtml":
                    ScoreHTML shtml = new ScoreHTML();
                    if (cfProperties != null) {
                        if (cfProperties.containsKey(corpusname)) {
                            shtml.setCorpusName(cfProperties.getProperty(corpusname));
                            System.out.println("Corpus name set to " + cfProperties.getProperty(corpusname));
                        }
                    }
                    cf2strcorpusfunctions.add(shtml);
                    break;
                case "hscorehtml":
                    HScoreHTML hshtml = new HScoreHTML();
                    cf2strcorpusfunctions.add(hshtml);
                    break;
                case "corpushtml":
                    CorpusHTML chtml = new CorpusHTML();
                    cf2strcorpusfunctions.add(chtml);
                    break;
                case "listhtml":
                    ListHTML lhtml = new ListHTML();
                    if (cfProperties != null) {
                        // Pass on the configuration parameter
                        if (cfProperties.containsKey(segmentation)) {
                            lhtml.setSegmentation(cfProperties.getProperty(segmentation));
                            System.out.println("Segmentation set to " + cfProperties.getProperty(segmentation));
                        }
                        if (cfProperties.containsKey(corpusname)) {
                            lhtml.setCorpusName(cfProperties.getProperty(corpusname));
                            System.out.println("Corpus name set to " + cfProperties.getProperty(corpusname));
                        }
                        if (cfProperties.containsKey(fsm)) {
                            lhtml.setExternalFSM(cfProperties.getProperty(fsm));
                            System.out.println("External FSM path set to " + cfProperties.getProperty(fsm));
                        }
                    }
                    cf2strcorpusfunctions.add(lhtml);
                    break;
                case "exbeventlinebreakschecker":
                    ExbEventLinebreaksChecker elb = new ExbEventLinebreaksChecker();
                    cf2strcorpusfunctions.add(elb);
                    break;
                case "maketimelineconsistent":
                    ExbMakeTimelineConsistent emtc = new ExbMakeTimelineConsistent();
                    if (cfProperties != null) {
                        // Pass on the configuration parameter
                        if (cfProperties.containsKey("interpolate")) {
                            emtc.setInterpolateTimeline(cfProperties.getProperty("interpolate"));
                            System.out.println("FixWhitespace set to " + cfProperties.getProperty("interpolate"));
                        }
                    }
                    cf2strcorpusfunctions.add(emtc);
                    break;
                case "exbstructurechecker":
                    ExbStructureChecker esc = new ExbStructureChecker();
                    cf2strcorpusfunctions.add(esc);
                    break;
                case "exbsegmentationchecker":
                    ExbSegmentationChecker eseg = new ExbSegmentationChecker();
                    if (cfProperties != null) {
                        // Pass on the configuration parameter
                        if (cfProperties.containsKey(segmentation)) {
                            eseg.setSegmentation(cfProperties.getProperty(segmentation));
                            System.out.println("Segmentation set to " + cfProperties.getProperty(segmentation));
                        }
                        if (cfProperties.containsKey(fsm)) {
                            eseg.setExternalFSM(cfProperties.getProperty(fsm));
                            System.out.println("External FSM path set to " + cfProperties.getProperty(fsm));
                        }
                    }
                    cf2strcorpusfunctions.add(eseg);
                    break;
                case "exbsegmenter":
                    ExbSegmentationChecker esegr = new ExbSegmentationChecker();
                    if (cfProperties != null) {
                        // Pass on the configuration parameter
                        if (cfProperties.containsKey(segmentation)) {
                            esegr.setSegmentation(cfProperties.getProperty(segmentation));
                            System.out.println("Segmentation set to " + cfProperties.getProperty(segmentation));
                        }
                        if (cfProperties.containsKey(fsm)) {
                            esegr.setExternalFSM(cfProperties.getProperty(fsm));
                            System.out.println("External FSM path set to " + cfProperties.getProperty(fsm));
                        }
                    }
                    cf2strcorpusfunctions.add(esegr);
                    break;
                case "calculateannotatedtime":
                    ExbCalculateAnnotatedTime cat = new ExbCalculateAnnotatedTime();
                    cf2strcorpusfunctions.add(cat);
                    break;
                case "addcsvmetadatatocoma":
                    AddCSVMetadataToComa acmtc = new AddCSVMetadataToComa();
                    if (cfProperties != null) {
                        // Pass on the configuration parameter
                        if (cfProperties.containsKey("csv")) {
                            acmtc.setCSVFilePath(cfProperties.getProperty("csv"));
                            System.out.println("CSV file path set to " + cfProperties.getProperty("csv"));
                        }
                        if (cfProperties.containsKey("speaker")) {
                            acmtc.setSpeakerOrCommunication(cfProperties.getProperty("speaker"));
                            System.out.println("CSV file set for " + cfProperties.getProperty("speaker"));
                        }
                    }
                    cf2strcorpusfunctions.add(acmtc);
                    break;
                case "removeemptyevents":
                    RemoveEmptyEvents ree = new RemoveEmptyEvents();
                    cf2strcorpusfunctions.add(ree);
                    break;
                case "comatieroverviewcreator":
                    ComaTierOverviewCreator ctoc = new ComaTierOverviewCreator();
                    cf2strcorpusfunctions.add(ctoc);
                    break;
                case "generaltransformer":
                    GeneralTransformer gt = new GeneralTransformer();
                    if (cfProperties != null) {
                        if (cfProperties.containsKey("coma")) {
                            gt.setComa(cfProperties.getProperty("coma"));
                            System.out.println("Run on Coma set to " + cfProperties.getProperty("coma"));
                        }
                        if (cfProperties.containsKey("exb")) {
                            gt.setExb(cfProperties.getProperty("exb"));
                            System.out.println("Run on exb set to " + cfProperties.getProperty("exb"));
                        }
                        if (cfProperties.containsKey("exs")) {
                            gt.setExs(cfProperties.getProperty("exs"));
                            System.out.println("Run on exs set to " + cfProperties.getProperty("exs"));
                        }
                        if (cfProperties.containsKey("xsl")) {
                            gt.setPathToXSL(cfProperties.getProperty("xsl"));
                            System.out.println("Path to XSL set to " + cfProperties.getProperty("xsl"));
                        }
                        if (cfProperties.containsKey("overwritefiles")) {
                            gt.setOverwriteFiles(cfProperties.getProperty("overwritefiles"));
                            System.out.println("overwritefiles set to " + cfProperties.getProperty("overwritefiles"));
                        }
                    }
                    cf2strcorpusfunctions.add(gt);
                    break;
                case "exbmp3next2wavadder":
                    ExbMP3Next2WavAdder emn2wa = new ExbMP3Next2WavAdder();
                    cf2strcorpusfunctions.add(emn2wa);
                    break;
                case "exbreftierchecker":
                    ExbRefTierChecker ertc = new ExbRefTierChecker();
                    cf2strcorpusfunctions.add(ertc);
                    break;
                case "exbscriptmixchecker":
                    ExbScriptMixChecker esmc = new ExbScriptMixChecker();
                    cf2strcorpusfunctions.add(esmc);
                    break;
                case "duplicatetiercontentchecker":
                    DuplicateTierContentChecker duplc = new DuplicateTierContentChecker();
                    cf2strcorpusfunctions.add(duplc);
                    if (cfProperties != null) {
                        // Pass on the configuration parameter
                        if (cfProperties.containsKey("tiers")) {
                            duplc.setTierNames(cfProperties.getProperty("tiers"));
                            System.out.println("Tier names set to " + cfProperties.getProperty("tiers"));
                        }
                    }
                    break;
                case "languagetoolchecker":
                    LanguageToolChecker ltc = new LanguageToolChecker();
                    if (cfProperties != null) {
                        // Pass on the configuration parameter
                        if (cfProperties.containsKey(spelllang)) {
                            ltc.setLanguage(cfProperties.getProperty(spelllang));
                            System.out.println("Language set to " + cfProperties.getProperty(spelllang));
                        }
                        if (cfProperties.containsKey("tier")) {
                            ltc.setTierToCheck(cfProperties.getProperty("tier"));
                            System.out.println("Tier to check set to " + cfProperties.getProperty("tier"));
                        }
                    }
                    cf2strcorpusfunctions.add(ltc);
                    break;
                case "vikusviewer":
                    VikusViewer vv = new VikusViewer();
                    cf2strcorpusfunctions.add(vv);
                    break;
                case "exbeventtokenizationchecker":
                    ExbEventTokenizationChecker eetc = new ExbEventTokenizationChecker();
                    cf2strcorpusfunctions.add(eetc);
                    if (cfProperties != null) {
                        // Pass on the configuration parameter
                        if (cfProperties.containsKey("tokensTier")) {
                            eetc.setTokensTierName(cfProperties.getProperty("tokens"));
                            System.out.println("Tokens tier name set to " + cfProperties.getProperty("token"));
                        }
                        if (cfProperties.containsKey("annotationTiers")) {
                            eetc.setTierNames(cfProperties.getProperty("annotation"));
                            System.out.println("Tier names set to " + cfProperties.getProperty("tiers"));
                        }
                    }
                    break;
                case "exbtimestampschecker":
                    ExbTimestampsChecker extc = new ExbTimestampsChecker();
                    cf2strcorpusfunctions.add(extc);
                    break;
                case "exbforbiddensymbolschecker":
                    ExbForbiddenSymbolsChecker efsc = new ExbForbiddenSymbolsChecker();
                    cf2strcorpusfunctions.add(efsc);
                    if (cfProperties != null) {
                        if (cfProperties.containsKey("tiers")) {
                            efsc.setTierNames(cfProperties.getProperty("tiers"));
                            System.out.println("The tiers to check are set to " + cfProperties.getProperty("tiers"));
                        }
                    }
                    break;
                case "elanpunctuationchecker":
                    ElanPunctuationChecker epc = new ElanPunctuationChecker();
                    cf2strcorpusfunctions.add(epc);
                    break;
                case "flextextpunctuationchecker":
                    FlextextPunctuationChecker fpc = new FlextextPunctuationChecker();
                    cf2strcorpusfunctions.add(fpc);
                    break;
                case "exbseparatetiersfordifferentspeakers":
                    ExbSeparateTiersForDifferentSpeakers estfds = new ExbSeparateTiersForDifferentSpeakers();
                    cf2strcorpusfunctions.add(estfds);
                    break;
                default:
                    report.addCritical("CommandlineFunctionality", "Function String \"" + function + "\" is not recognized");
            }
        }
        return cf2strcorpusfunctions;
    }

    //run the chosen functions on the chosen corpus data
    Report runChosencorpusfunctions() {
        //it's an unordered Collection of corpus data
        if (isCollection) {
            for (CorpusFunction function : corpusfunctions) {
                if (fixing) {
                    report.merge(runCorpusFunction(cdc, function, true));
                } else {
                    report.merge(runCorpusFunction(cdc, function));
                }
            }
            //Congrats - It's a corpus!
        } else if (isCorpus) {
            for (CorpusFunction function : corpusfunctions) {
                if (fixing) {
                    report.merge(runCorpusFunction(corpus, function, true));
                } else {
                    report.merge(runCorpusFunction(corpus, function));
                }
            }
            //must be a single file then
        } else {
            for (CorpusFunction function : corpusfunctions) {
                if (fixing) {
                    report.merge(runCorpusFunction(corpusData, function, true));
                } else {
                    report.merge(runCorpusFunction(corpusData, function));
                }
            }
        }

        return report;
    }
    //run multiple functions on a corpus, that means all the files in the corpus
    //the function can run on

    public Report runCorpusFunctions(Corpus c, Collection<CorpusFunction> cfc) {
        Report report = new Report();
        for (CorpusFunction cf : cfc) {
            Report newReport = runCorpusFunction(c, cf);
            report.merge(newReport);
        }
        return report;
    }

    //run multiple functions on the set corpus, that means all the files in the corpus
    //the function can run on
    public Report runCorpusFunctions(Collection<CorpusFunction> cfc) {
        return runCorpusFunctions(corpus, cfc);
    }

    //run one function on a corpus, that means all the files in the corpus
    //the funciton can run on
    public Report runCorpusFunction(Corpus c, CorpusFunction cf) {
        return runCorpusFunction(c, cf, false);
    }

    //run one function on a corpus, that means all the files in the corpus
    //the funciton can run on
    public Report runCorpusFunction(Corpus c, CorpusFunction cf, boolean fix) {
        return runCorpusFunction(c.getCorpusData(),cf,fix);
    }

    //run one function on a corpus, that means all the files in the corpus
    //the function can run on
    public Report runCorpusFunction(CorpusFunction cf) {
        return runCorpusFunction(corpus, cf, false);
    }

    //run one function on a corpus, that means all the files in the corpus
    //the funciton can run on
    public Report runCorpusFunction(Collection<CorpusData> cdc, CorpusFunction cf, boolean fix) {
        Report report = new Report();
        //find out on which objects this corpus function can run
        //choose those from the corpus
        //and run the checks on those files recursively
        Collection<Class<? extends CorpusData>> usableTypes = null;
        try {
            usableTypes = cf.getIsUsableFor();
            //if the corpus files are an instance
            //of the class cl, run the function
            for (CorpusData cd : cdc) {
                if (usableTypes.contains(cd.getClass())) {
                    Report newReport = runCorpusFunction(cd, cf, fix);
                    report.merge(newReport);
                }
            }
        } catch (ClassNotFoundException e) {
            report.addException(e,"usable classes not found");
        }

        return report;
    }

    //run one function on a corpus, that means all the files in the corpus
    //the funciton can run on
    public Report runCorpusFunction(Collection<CorpusData> cdc, CorpusFunction cf) {
        return runCorpusFunction(cdc, cf, false);
    }

    public Report runCorpusFunction(CorpusData cd, CorpusFunction cf) {
        return cf.execute(cd);
    }

    public Report runCorpusFunction(CorpusData cd, CorpusFunction cf, boolean fix) {
        return cf.execute(cd, fix);
    }

    public static Report runCorpusFunctions(CorpusData cd, Collection<CorpusFunction> cfc) {
        Report report = new Report();
        for (CorpusFunction cf : cfc) {
            Report newReport = (cf.execute(cd));
            report.merge(newReport);
        }
        return report;
    }

    public static Report runCorpusFunctions(CorpusData cd, Collection<CorpusFunction> cfc, boolean fix) {
        Report report = new Report();
        for (CorpusFunction cf : cfc) {
            Report newReport = (cf.execute(cd, fix));
            report.merge(newReport);
        }
        return report;
    }

    //TODO
    //to save individual corpusparameters in a file
    //and maybe also save the functions todos there
    public void readParameters() {
        //read the XML file as variables
    }

    public void setCorpusData(CorpusData corpusData) {
        this.corpusData = corpusData;
    }

    public void setChosencorpusfunctions(Collection<String> chosencorpusfunctions) {
        CorpusMagician.chosencorpusfunctions = chosencorpusfunctions;
    }

    public Corpus getCorpus() {
        return corpus;
    }

    public CorpusData getCorpusData() {
        return corpusData;
    }

    public Collection<String> getChosencorpusfunctions() {
        return chosencorpusfunctions;
    }

    public static void createReports() throws IOException, TransformerException, ParserConfigurationException, UnsupportedEncodingException, SAXException, XPathExpressionException, JDOMException {
        System.out.println(report.getFullReports());
        String reportOutput;
        for (URL reportlocation : reportlocations) {
            if (reportlocation.getFile().endsWith("html")) {
            if (iserrorsonly) {
                //ToDo
                //reportOutput = ReportItem.generateDataTableHTML(report.getErrorStatistics(basedirectory), report.getSummaryLines());
                reportOutput = ReportItem.generateDataTableHTML(report.getErrorStatistics(), report.getSummaryLines());
            } else {
                reportOutput = ReportItem.generateDataTableHTML(report.getRawStatistics(), report.getSummaryLines());
            }
           } else if (reportlocation.getFile().endsWith("csv")) {
            if (iserrorsonly) {
                reportOutput = ReportItem.GenerateCSV(report.getErrorStatistics(), report.getSummaryLines());
            } else {
            reportOutput = ReportItem.GenerateCSV(report.getRawStatistics(), report.getSummaryLines());
            }
            } else {
            //reportOutput = report.getSummaryLines() + "\n" + report.getErrorReports();
            reportOutput = report.getSummaryLines() + "\n" + report.getFullReports();
            }
            String absoluteReport = reportOutput;
            if (absoluteReport != null && basedirectory != null && absoluteReport.contains(basedirectory.toString())) {
                absoluteReport = reportOutput.replaceAll(basedirectory.toString(), "");
            }
            if (absoluteReport != null) {
                cio.write(absoluteReport, reportlocation);
            }
            
        }
        //create the error list file
        if (!nocurationfolder) {
            System.out.println("Basedirectory is " + basedirectory);
            System.out.println("BasedirectoryPath is " + basedirectory.getPath());
            URL errorlistlocation = new URL(basedirectory + "curation/CorpusServices_Errors.xml");
            URL fixJsonlocation = new URL(basedirectory + "curation/fixes.json");
            File curationFolder = new File((new URL(basedirectory + "curation").getFile()));
            if (!curationFolder.exists()) {
                //the curation folder it not there and needs to be created
                curationFolder.mkdirs();
            }
            Document exmaErrorList = TypeConverter.W3cDocument2JdomDocument(ExmaErrorList.createFullErrorList());
            String exmaErrorListString = TypeConverter.JdomDocument2String(exmaErrorList);
            if (exmaErrorListString != null && basedirectory != null && exmaErrorListString.contains(basedirectory.getPath())) {
                exmaErrorListString = exmaErrorListString.replaceAll(basedirectory.getPath(), "../");
            }
            if (exmaErrorListString != null) {
                exmaErrorListString = pp.indent(exmaErrorListString, "event");
                cio.write(exmaErrorListString, errorlistlocation);
                System.out.println("Wrote ErrorList at " + errorlistlocation);
            }
            if (isfixesjson) {
                String fixJson = "";
                if (isCorpus) {
                    fixJson = report.getFixJson(corpus);
                } else {
                    fixJson = report.getFixJson();
                }
                if (fixJson != null) {
                    cio.write(fixJson, fixJsonlocation);
                    System.out.println("Wrote JSON file for fixes at " + fixJsonlocation);
                }
            }
        }
    }

    public static void readCommandLineOptions() throws MalformedURLException {
        String urlstring = cmd.getOptionValue("input");
        fixing = cmd.hasOption("f");
        iserrorsonly = cmd.hasOption("e");
        isfixesjson = cmd.hasOption("j");
        nocurationfolder = cmd.hasOption("n");
        if (urlstring.startsWith("file://")) {
            inputurl = new URL(urlstring);
        } else {
            inputurl = Paths.get(urlstring).toAbsolutePath().normalize().toUri().toURL();
        }
        //now the place where Report should end up
        //also allow normal filepaths and convert them
         String[] reportstring = cmd.getOptionValues("output");
        for (String o : reportstring) {
            if (o.startsWith("file://")) {
                URL out = new URL(o);
                reportlocations.add(out);
            } else {
                URL s = Paths.get(o).toUri().toURL();
                reportlocations.add(s);
            }
        }
        //now add the functionsstrings to array
        String[] corpusfunctionarray = cmd.getOptionValues("c");
        for (String cf : corpusfunctionarray) {
            CorpusMagician.chosencorpusfunctions.add(cf);
        }
        System.out.println(CorpusMagician.chosencorpusfunctions.toString());
    }

    private static void createCommandLineOptions(String[] args) throws FileNotFoundException, IOException {
        Options options = new Options();

        Option input = new Option("i", "input", true, "input file path (coma file for corpus, folder or other file for unstructured data)");
        input.setRequired(true);
        input.setArgName("FILE PATH");
        options.addOption(input);

        //Set option o to take one and more arguments
        Option output = new Option("o", "output", true, "output file");
        output.setArgs(Option.UNLIMITED_VALUES);
        output.setRequired(true);
        output.setValueSeparator(',');
        output.setArgName("FILE PATH");
        options.addOption(output);

        Option corpusfunction = new Option("c", "corpusfunction", true, "corpus function");
        // Set option c to take 1 to oo arguments
        corpusfunction.setArgs(Option.UNLIMITED_VALUES);
        corpusfunction.setArgName("CORPUS FUNCTION");
        corpusfunction.setRequired(true);
        corpusfunction.setValueSeparator(',');
        options.addOption(corpusfunction);

        /*
         Option speed = new Option("s", "speed", false, "faster but more heap space");
         speed.setRequired(false);
         options.addOption(speed);
         */
        Option propertyOption = Option.builder("p")
                .longOpt("property")
                .argName("property=value")
                .hasArgs()
                .valueSeparator()
                .numberOfArgs(2)
                .desc("use value for given properties")
                .build();

        options.addOption(propertyOption);

        Option fix = new Option("f", "fix", false, "fixes problems automatically");
        fix.setRequired(false);
        options.addOption(fix);

        Option help = new Option("h", "help", false, "display help");
        fix.setRequired(false);
        options.addOption(help);

        Option errorsonly = new Option("e", "errorsonly", false, "output only errors");
        fix.setRequired(false);
        options.addOption(errorsonly);

        Option fixesjson = new Option("j", "fixesjson", false, "output json file for fixes");
        fix.setRequired(false);
        options.addOption(fixesjson);

        Option settingsfile = new Option("s", "settingsfile", true, "settings file path");
        settingsfile.setRequired(false);
        settingsfile.setArgName("FILE PATH");
        options.addOption(settingsfile);
        
        Option nocuration = new Option("n", "nocuration", false, "do not create creation folder and exma-error file");
        fix.setRequired(false);
        options.addOption(nocuration);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null);

        String header = "Specify a corpus folder or file and a function to be applied\n\n";
        //String footer = "\nthe available functions are:\n" + getAllExistingCFsAsString() + "\n\nPlease report issues at https://lab.multilingua.uni-hamburg.de/redmine/projects/corpus-services/issues";
        String footerverbose = "\nthe available functions are:\n" + getAllExistingCFsAsString() + "\n\nDescriptions of the available functions follow:\n\n";
        String desc;
        String hasfix;
        String usable;
        for (CorpusFunction cf : getAllExistingCFsAsCFs()) {
            desc = cf.getFunction() + ":   " + cf.getDescription();
            usable = "\nThe function can be used on:\n";
            try {
                for (Class cl : cf.getIsUsableFor()) {
                    usable += cl.getSimpleName() + " ";
                }
            }
            catch (ClassNotFoundException e) {
                report.addException(e, "usable classes not found");
            }
            hasfix = "\nThe function has a fixing option: " + cf.getCanFix().toString();
            footerverbose += desc + hasfix + usable + "\n\n";
            usable = "";
        }
        footerverbose += "\n\nPlease report issues at https://lab.multilingua.uni-hamburg.de/redmine/projects/corpus-services/issues";
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("hzsk-corpus-services", header, options, footerverbose, true);
            System.exit(1);
        }
        
        if (cmd.hasOption("n") && cmd.hasOption("j")) {
            System.out.println("Options n and j are not allowed at the same time");
            formatter.printHelp("hzsk-corpus-services", header, options, footerverbose, true);
            System.exit(1);
        }

        //TODO
        //in reality this never works because there will be an error since the required parameters are missing - but that returns the help as well....
        if (cmd.hasOption("h")) {
            // automatically generate the help statement
            formatter.printHelp("hzsk-corpus-services", header, options, footerverbose, true);
            System.exit(1);
        }

        if (cmd.hasOption("p")) {
            if (cmd.hasOption("s")) {
                System.out.println("Options s and p for parameters are not allowed at the same time!!");
                formatter.printHelp("hzsk-corpus-services", header, options, footerverbose, true);
                System.exit(1);
            } else {
                cfProperties = cmd.getOptionProperties("p");
            }
        } else {
            if (cmd.hasOption("s")) {
                //read filepath
                settingsfilepath = cmd.getOptionValue("s");
            } else {
                //default
                settingsfilepath = "settings.param";
            }
            //also need to allow for not findind the xml settings file here!
            if (new File(settingsfilepath).exists()) {
                FileInputStream test = new FileInputStream(settingsfilepath);
                cfProperties.loadFromXML(test);
                System.out.println("Properties are: " + cfProperties);
            } else {
                System.out.println("No parameters loaded.");
            }
        }

        //we can save the properties if the input was not from an settings.xml
        //cfProperties.storeToXML() 
        //add function to read properties from file! Needs to be a key value list though not xml
        //Reads a property list (key and element pairs) from the input
        //Need to use 
//     * byte stream. The input stream is in a simple line-oriented
//     * format as specified in
//     * {@link #load(java.io.Reader) load(Reader)} and is assumed to use
//     * the ISO 8859-1 character encoding; that is each byte is one Latin1
//     * character. Characters not in Latin1, and certain special characters,
//     * are represented in keys and elements using Unicode escapes as defined in
//     * section 3.3 of

        /*
         String inputFilePath = cmd.getOptionValue("input");
         String outputFilePath = cmd.getOptionValue("output");

         System.out.println(inputFilePath);
         System.out.println(outputFilePath);
         */
    }

}
