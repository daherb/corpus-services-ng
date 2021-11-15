package de.uni_hamburg.corpora.validation.quest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.primitives.Chars;
import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.quest.DictionaryAutomaton;
import de.uni_hamburg.corpora.utilities.quest.UniversalLevenshteinAutomatonK1;
import de.uni_hamburg.corpora.validation.Checker;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.ini4j.InvalidFileFormatException;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author bba1792 Dr. Herbert Lange
 * @version 20210929
 * The checker for Refco set of criteria.
 */
public class RefcoChecker extends Checker implements CorpusFunction {

    // The local logger that can be used for debugging
    private final Logger logger = Logger.getLogger(this.getClass().toString());

    // Percentage of characters in transcription tokens to be valid
    private static final int transcriptionCharactersValid = 99;
    // Percentage of gloss token morphemes to be valid
    private static final int glossMorphemesValid = 70;
    // Separator used to separate multiple values in a cell
    private final String valueSeparator = "\\s*[,;:]\\s*" ;
    // Separator used to separate words/token
    private final String tokenSeparator = "\\s+" ;
    // Separator used to separate morpheme glosses (see https://www.eva.mpg.de/lingua/pdf/Glossing-Rules.pdf)
    private final String glossSeparator = "[-=]"; // "[-=;:\\\\>()<~\\[\\]]+" ;

    // The XML namespace for table elements in ODS files
    private final Namespace tableNamespace =
            Namespace.getNamespace("table","urn:oasis:names:tc:opendocument:xmlns:table:1.0") ;
    // The XML namespace for text elements in ODS files
    private final Namespace textNamespace =
            Namespace.getNamespace("text","urn:oasis:names:tc:opendocument:xmlns:text:1.0") ;

    // The hand-picked list of languages acceptable for translation tiers
    private final List<String> translationLanguages = Arrays.asList("mandarin chinese", "english", "french", "german"
            , "indonesian", "portuguese", "russian", "spanish");

    // The hand-picked list of acceptable tier functions
    private final List<String> tierFunctions = Arrays.asList("Transcription", "Reference", "Note", "Part-of-speech",
            "Morpheme gloss", "Morpheme segmentation", "Free Translation");

    /**
     * A pair of information with potentially associated notes, used e.g. in the Overview table
     */
    static class InformationNotes {
        public InformationNotes(String information, String notes) {
            this.information = information;
            this.notes = notes;
        }

        String information ;
        String notes;
    }

    /**
     * Representation of information in the CorpusComposition table consisting of e.g.
     * speaker information, location and date
     */
    static class Session {
        String sessionName ;
        String fileName ;
        String speakerName ;
        String speakerAge ;
        String speakerGender ;
        String recordingLocation ;
        String recordingDate ;
        String genre ; // is this a controlled vocabulary?
//        String ageGroup ; // is this a controlled vocabulary?
    }


    /**
     * Representation of information in the AnnotationTiers table consisting of e.g.
     * tier functions and languages
     */
    static class Tier {
        String tierName ;
        String tierFunction ;
        String segmentationStrategy ;
        String languages ;
        String morphemeDistinction;
    }

    /**
     * Representation of information in the Transcriptions table consisting of e.g.
     * the list of valid graphemes used in transcription tiers
     */
    static class Transcription {
        String grapheme ;
        String linguisticValue ;
        String linguisticConvention ;
    }

    /**
     * Representation of information in the Glosses table consisting of e.g.
     * list of expected glosses and the tiers they are valid in
     */
    static class Gloss {
        String gloss ;
        String meaning ;
        String comments ;
        String tiers ;
    }

    /**
     * Representation of information in the Punctuation table consisting of e.g.
     * valid punctuation characters and the tiers they are valid in
     */
    static class Punctuation {
        String character ;
        String meaning ;
        String comments ;
        String tiers ;
    }

    /**
     * Representation of the complete information defined in the RefCo spreadsheet
     */
    public static class RefcoCriteria {
        // Tab: Overview
        // Corpus information
        String corpusTitle ;
        String subjectLanguages ;
        String archive ;
        String persistentId ; // should be an url to either a doi or handle
        String annotationLicense ;
        String recordingLicense ;
        String creatorName ;
        String creatorContact ; // usually mail address
        String creatorInstitution ;
        // Certification information
        InformationNotes refcoVersion ;
        // Quantitative Summary
        InformationNotes numberSessions ;
        InformationNotes numberTranscribedWords ;
        InformationNotes numberAnnotatedWords ;
        // Annotation Strategies
        // All languages are in a single cell
        InformationNotes translationLanguages ;
        // Tab: Corpus Compositions
        ArrayList<Session> sessions = new ArrayList<>() ;
        // Tab: Annotation Tiers
        ArrayList<Tier> tiers = new ArrayList<>() ;
        // Tab: Transcriptions
        ArrayList<Transcription> transcriptions = new ArrayList<>() ;
        // Tab: Glosses
        ArrayList<Gloss> glosses = new ArrayList<>() ;
        // Tab: Punctuation
        ArrayList<Punctuation> punctuations = new ArrayList<>() ;

        public String getCorpusTitle() {
            return corpusTitle;
        }

        public String getSubjectLanguages() {
            return subjectLanguages;
        }

        public String getArchive() {
            return archive;
        }

        public String getPersistentId() {
            return persistentId;
        }

        public String getAnnotationLicense() {
            return annotationLicense;
        }

        public String getRecordingLicense() {
            return recordingLicense;
        }

        public String getCreatorName() {
            return creatorName;
        }

        public String getCreatorContact() {
            return creatorContact;
        }

        public String getCreatorInstitution() {
            return creatorInstitution;
        }

        public InformationNotes getRefcoVersion() {
            return refcoVersion;
        }

        public InformationNotes getNumberSessions() {
            return numberSessions;
        }

        public InformationNotes getNumberTranscribedWords() {
            return numberTranscribedWords;
        }

        public InformationNotes getNumberAnnotatedWords() {
            return numberAnnotatedWords;
        }

        public InformationNotes getTranslationLanguages() {
            return translationLanguages;
        }

        public ArrayList<Session> getSessions() {
            return sessions;
        }

        public ArrayList<Tier> getTiers() {
            return tiers;
        }

        public ArrayList<Transcription> getTranscriptions() {
            return transcriptions;
        }

        public ArrayList<Gloss> getGlosses() {
            return glosses;
        }

        public ArrayList<Punctuation> getPunctuations() {
            return punctuations;
        }
    }

    /**
     * The filename of the RefCo spreadsheet
     */
    String refcoFileName;
    String refcoShortName;

    /**
     * The XML DOM of the RefCo spreadsheet
     */
    Document refcoDoc ;

    public RefcoCriteria getCriteria() {
        return criteria;
    }

    /**
     * The criteria extracted from the Refco spreadsheet
     */
    RefcoCriteria criteria = new RefcoCriteria() ;

    /**
     * The list of ISO-639-3 language codes
     */
    ArrayList<String> isoList = new ArrayList<>() ;

    /**
     * The corpus with all usable files
     */
    Corpus refcoCorpus ;

    /**
     *  The frequency list of all transcription tokens in the corpus
     */
    HashMap<String,Integer> tokenFreq = new HashMap<>();

    /**
     * The frequency list of all segmented annotation/morphology glosses in the corpus
     */
    HashMap<String,Integer> morphemeFreq = new HashMap<>();

    /**
     * The frequency list of all non-segmented annotation/morphology glosses in the corpus
     */
    HashMap<String,Integer> glossFreq = new HashMap<>();

    /**
     * The global report, will be filled by the constructor and the function applied to the complete corpus
     */
    Report report = new Report();

    /**
     * Default constructor with fixing option as parameter
     * @param properties global properties
     */
    public RefcoChecker(Properties properties) {
        // Call the inherited constructor
        super(false, properties);
        // Read the list of ISO-639-3 language codes
        try {
            // In a jar this would be one of the resources
            InputStream isoStream = getClass().getResourceAsStream("/iso-639-3.tab") ;
            // If the resource is missing try to load it as a file instead
            if (isoStream == null)
                isoStream = new FileInputStream("src/main/java/de/uni_hamburg/corpora/validation/resources/iso-639-3.tab");
            BufferedReader bReader = new BufferedReader(new
                    InputStreamReader(isoStream));
            String line;
            while ((line = bReader.readLine()) != null) {
                    // Skip the header
                    if (!line.startsWith("Id"))
                        isoList.add(line.split("\t")[0]) ;
                }
            }
            catch (IOException e) {
                logger.log(Level.SEVERE,"Unable to load ISO-639-3 language list", e);
            }
        if (properties.containsKey("refco-file")) {
            // Load config and add potential problems to log
            report.merge(setRefcoFile(properties.getProperty("refco-file")));
        }
        else {
            report.addCritical(getFunction(),"Missing corpus documentation file property");
        }
    }

    /**
     * Gives the description of what the checker does
     * @return the description of the checker
     */
    @Override
    public String getDescription() {
        return "checks the RefCo criteria for a corpus. Requires a filled RefCo spreadsheet.";
    }

    /**
     * The checker function for one corpus document
     * @param cd the corpus doecument
     * @param fix the choice of the fixing option
     * @return the report with detailed check results
     * @throws NoSuchAlgorithmException inherited
     * @throws ClassNotFoundException inherited
     * @throws FSMException inherited
     * @throws URISyntaxException inherited
     * @throws SAXException inherited
     * @throws IOException inherited
     * @throws ParserConfigurationException inherited
     * @throws JexmaraldaException inherited
     * @throws TransformerException inherited
     * @throws XPathExpressionException inherited
     * @throws JDOMException inherited
     */
    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        return refcoCorpusCheck(cd);
    }

    /**
     * The checker function for a complete corpus, i.e. a set of corpus documents
     * @param c the corpus
     * @param fix the choice of the fixing option
     * @return the report with detailed check results
     * @throws NoSuchAlgorithmException inherited
     * @throws ClassNotFoundException inherited
     * @throws FSMException inherited
     * @throws URISyntaxException inherited
     * @throws SAXException inherited
     * @throws IOException inherited
     * @throws ParserConfigurationException inherited
     * @throws JexmaraldaException inherited
     * @throws TransformerException inherited
     * @throws XPathExpressionException inherited
     * @throws JDOMException inherited
     */
    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        // Create the current report
        //Report report = new Report();
        // Get all usable formats for the checker
        Collection<Class<? extends CorpusData>> usableFormats = this.getIsUsableFor();
        // Get all usable files from the corpus, i.e. the ones whose format is included in usableFormats
        Collection<CorpusData> usableFiles = c.getCorpusData().stream().filter((cd) -> usableFormats.contains(cd.getClass())).collect(Collectors.toList());
        // Create a new corpus from only these files, keeping the original corpus name and base directory
        // This corpus is among others used to check the existence of referenced files
        refcoCorpus = new Corpus(c.getCorpusName(), c.getBaseDirectory(), usableFiles) ;
        // Initialize frequency list for glosses
        for (Gloss gloss : criteria.glosses) {
            morphemeFreq.put(gloss.gloss,0);
        }
        // Run the generic tests and merge their reports into the current report
        logger.info("Merge generic");
        report.merge(refcoGenericCheck());
        // Apply function for each of the supported file. Again merge the reports
        for (CorpusData cdata : usableFiles) {
            report.merge(function(cdata, fix));
        }
        // Check for morpheme glosses that never occurred in the complete corpus
        for (Map.Entry<String,Integer> e : morphemeFreq.entrySet()) {
            if (e.getValue() == 0)
                report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename","description",
                                "howtoFix"},
                        new Object[]{getFunction(), refcoShortName,
                                "Corpus data: Morpheme gloss never encountered in corpus: " + e.getKey(),
                                "Check for potential errors or remove gloss from documentation"}));
        }
        // Check all gloss tokens (not-segmented) for rare ones very similar to quite common ones, i.e. tokens with
        // Levenshtein difference 1 with a higher frequency count
        /*DictionaryAutomaton glossDictionary =
                new DictionaryAutomaton(new ArrayList<>(glossFreq.keySet()));
        logger.info("Doing the fancy experiment");
        for (String gloss : glossFreq.keySet()) {
            if (glossFreq.get(gloss) < 10) {
                for (String similar : UniversalLevenshteinAutomatonK1.matchDictionary(gloss, glossDictionary)) {
                    if (glossFreq.get(similar) > 100)
                        report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename","description",
                                        "howtoFix"},
                                new Object[]{getFunction(), refcoShortName,
                                        "EXPERIMENTAL Corpus data: Similar gloss found that is more common: " + similar +
                                                " with count " + glossFreq.get(similar) + " instead of " + gloss +
                                                " with count " + glossFreq.get(gloss),
                                        "Check for typo"}));
                }
            }
        }
        // Same for transcription
        DictionaryAutomaton tokenDictionary =
                new DictionaryAutomaton(new ArrayList<>(tokenFreq.keySet()));
        for (String token : tokenFreq.keySet()) {
            if (tokenFreq.get(token) < 10 && token.length() > 5) {
                for (String similar : UniversalLevenshteinAutomatonK1.matchDictionary(token, tokenDictionary)) {
                    if (tokenFreq.get(similar) > 100)
                        report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename","description",
                                        "howtoFix"},
                                new Object[]{getFunction(), refcoShortName,
                                        "EXPERIMENTAL Corpus data: Similar gloss found that is more common: " + similar +
                                                " with count " + tokenFreq.get(similar) + " instead of " + token + " " +
                                                "with count " + tokenFreq.get(token),
                                        "Check for typo"}));
                }
            }
        }*/
        return report;
    }

    /**
     * Function to retrieve the collection of all classes the checker is suitable for
     *
     * @return a collection of classes the checker is suitable for
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        // Only ELAN is supported
        return Collections.singleton(ELANData.class) ;
    }

    /**
     * The main function to run the RefCo checker as an independent application
     *
     * @param args the first argument being the refco spreadsheet and the second one being the corpus directory
     */
    public static void main(String[] args) {
        // Check the number of arguments and report the usage if parameters are missing
        if (args.length < 3) {
            System.out.println("Usage: RefcoChecker RefcoFile CorpusDirectory ReportFile");
            System.out.println("\tRefcoFile       : the RefCo spreadsheet");
            System.out.println("\tCorpusDirectory : the path to the corpus");
            System.out.println("\tReportFile      : the output file containing the report as a HTML page");
        }
        else {
            Properties props = new Properties();
            // Set the filename of the RefCo spreadsheet
            props.setProperty("refco-file",args[0]);
            Logger.getLogger(RefcoChecker.class.toString()).log(Level.INFO,
                    "Loading RefCo file");
            // Create the checker. This also triggers the parser for the spreadsheet
            // and initializes the criteria field
            RefcoChecker rc = new RefcoChecker(props);
            // Safe the Refco criteria to file
            // Generate pretty-printed json using an object mapper
            // DEBUG write criteria to json file
            /*ObjectMapper mapper = new ObjectMapper();
            // Allows serialization even when getters are missing
            mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            mapper.configure(SerializationFeature.INDENT_OUTPUT,true);
            try {
                FileWriter fw = new FileWriter("/tmp/refco.json") ;
                fw.write(mapper.writeValueAsString(rc.criteria));
                fw.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }*/
            try {
                Report report = new Report();
                // Read the corpus
                Logger.getLogger(RefcoChecker.class.toString()).log(Level.INFO, "Reading corpus");
                CorpusIO cio = new CorpusIO();
                // Handle relative paths
                URL url = Paths.get(args[1]).toAbsolutePath().normalize().toUri().toURL();
                // Create the corpus
                Corpus corpus = new Corpus(rc.criteria.corpusTitle, url, cio.read(url));
                // Run the tests on the corpus
                Logger.getLogger(RefcoChecker.class.toString()).log(Level.INFO, "Running tests");
                report.merge(rc.function(corpus,false));
                // When the tests are done write the report to file
                Logger.getLogger(RefcoChecker.class.toString()).log(Level.INFO, "Writing report");
                String html = ReportItem.generateDataTableHTML(report.getRawStatistics(),report.getSummaryLines());
                FileWriter fw = new FileWriter(args[2]) ;
                fw.write(html) ;
                fw.close() ;
                Logger.getLogger(RefcoChecker.class.toString()).log(Level.INFO, "Done") ;
                ObjectMapper mapper = new ObjectMapper();
                // Allows serialization even when getters are missing
                mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
                mapper.configure(SerializationFeature.INDENT_OUTPUT,true);
                try {
                    fw = new FileWriter("/tmp/glosses.json") ;
                    fw.write(mapper.writeValueAsString(rc.glossFreq));
                    fw.close();
                    fw = new FileWriter("/tmp/gloss-morphemes.json") ;
                    fw.write(mapper.writeValueAsString(rc.morphemeFreq));
                    fw.close();
                }

                catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (URISyntaxException | IOException | SAXException | JexmaraldaException | ClassNotFoundException | XPathExpressionException | NoSuchAlgorithmException | ParserConfigurationException | JDOMException | FSMException | TransformerException e) {
                e.printStackTrace() ;
            }
        }
    }

    /**
     * Function to store the spreadsheet as XML data
     *
     * @param fileName the spreadsheet file name
     * @return a report containing potential problems when reading the documentation file
     */
    public Report setRefcoFile(String fileName) {
        // Save the file name
        refcoFileName = fileName ;
        refcoShortName = new File(fileName).getName();
        // New report
        Report report = new Report();
        // Extract XML from spreadsheet file
        try {
            // Plain XML file
            if (refcoFileName.toLowerCase().endsWith("fods")) {
                SAXBuilder builder = new SAXBuilder();
                refcoDoc = builder.build(Paths.get(refcoFileName).toAbsolutePath().normalize().toUri().toURL()) ;

            }
            // ODS file is basically a ZIP file containing XMLs but we use a proper API to access it
            else if (refcoFileName.toLowerCase().endsWith("ods")) {
                // ODS files basically are ZIP file containing XMLs
                ZipFile f = new ZipFile(new File(refcoFileName));
                ZipEntry e = f.getEntry("content.xml");
                if (e == null) {
                    throw new InvalidFileFormatException(refcoFileName + " does not contain content.xml") ;
                }
                SAXBuilder builder = new SAXBuilder();
                refcoDoc = builder.build(f.getInputStream(e));
                // Or we could use the proper API instead but that requires additional dependencies
                // refcoDoc = builder.build(new StringReader(OdfSpreadsheetDocument.loadDocument(refcoFileName).getContentDom().toString()));
            }
            else {
                throw new InvalidFileFormatException(refcoFileName + " is neither an ods nor fods file") ;
            }
            // expand compressed cells
            expandTableCells(refcoDoc);
            removeEmptyCells(refcoDoc);
        } catch (Exception e) {
            e.printStackTrace();
        }
         if (!refcoShortName.matches("\\d{8}_\\w+_RefCo-Report.f?ods")) {
            report.addWarning(getFunction(), ReportItem.newParamMap(new String[]{"function", "filename", "description",
                            "howtoFix"},
                    new Object[]{getFunction(), refcoShortName, "General: Filename does not match schema " +
                            "yyyymmdd_CorpusName_RefCo-Report.ods/.fods: " + refcoShortName,
                            "Rename documentation file"}));
        }
        else {
            // Check date in file name is valid
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
            try {
                df.parse(refcoShortName.substring(0,8));
            }
            catch (ParseException e) {
                report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                        "exception", "howtoFix"},
                        new Object[]{getFunction(),refcoShortName,
                                "General: Date given in filename not valid: "+ refcoShortName.substring(0,8), e,
                                "Check that date is following format YYYYMMDD (ISO 8601)"}));
            }
        }
        // Extract the criteria from the XML
        report.merge(readRefcoCriteria(refcoDoc));
        return report;
    }

    /**
     * Functions that expands compressed cells by replacing them by several blank cells. ODS files use some kind of
     * run-length compression for multiple blank cells in a row which is problematic for the parser.
     *
     * @param document the document in which the cells should be expanded
     * @throws JDOMException if the XPath expression is invalid
     */
    private void expandTableCells(Document document) throws JDOMException {
        // Find all cells that have the attribute number-columns-repeated
        for (Element node : listToParamList(Element.class, XPath.newInstance("//table:table-cell[@table:number-columns-repeated]")
                .selectNodes(document))) {
            // Generate as many blank cells as neede
            ArrayList<Element> replacement = new ArrayList<>();
            int colCount = 0;
            try {
                colCount = Integer.parseInt(node.getAttribute("number-columns-repeated", tableNamespace).getValue());
                // Do not expand too many cells
                if (colCount > 1000)
                    colCount = 1 ;
            }
            catch (NumberFormatException e) {
                logger.log(Level.SEVERE,"Error parsing number",e);
            }
            for (int i = 0; i < colCount; i++){
                Element e = (Element) node.clone();
                e.removeAttribute("number-columns-repeated", tableNamespace);
                e.setAttribute("ignore","true");
                replacement.add(e);
            }
            // Replace the original cell by the sequence of new ones
            node.getParentElement().setContent(node.getParentElement().indexOf(node),replacement);
        }
        // Expand rows as well
        for (Element node : listToParamList(Element.class, XPath.newInstance("//table:table-row[@table:number-rows-repeated]")
                .selectNodes(document))) {
            // Generate as many blank cells as neede
            ArrayList<Element> replacement = new ArrayList<>();
            int rowCount = 0;
            try {
                rowCount = Integer.parseInt(node.getAttribute("number-rows-repeated", tableNamespace).getValue());
                // Do not expand too many cells
                if (rowCount > 1000)
                    rowCount = 1 ;
            }
            catch (NumberFormatException e) {
                logger.log(Level.SEVERE,"Error parsing number",e);
            }
            for (int i = 0; i < rowCount; i++){
                Element e = (Element) node.clone();
                e.removeAttribute("number-rows-repeated", tableNamespace);
                e.setAttribute("ignore","true");
                replacement.add(e);
            }
            // Replace the original cell by the sequence of new ones
            node.getParentElement().setContent(node.getParentElement().indexOf(node),replacement);
        }
    }

    /**
     * Removes both table cells without text content (i.e. without p-tag as children) and rows without table cells
     * @param document the document to be modified
     * @throws JDOMException in case the XPath expressions fail
     */
    private void removeEmptyCells(Document document) throws JDOMException {
        boolean deleted = true;
        while (deleted) {
            deleted = false;
            for (Element node : listToParamList(Element.class, XPath.newInstance("//table:table-cell[not(text:p) and " +
                            "position() = last()]")
                    .selectNodes(document))) {
                node.detach();
                deleted = true;
            }
            for (Element node : listToParamList(Element.class, XPath.newInstance("//table:table-row[not" +
                            "(table:table-cell) and position() = last()]")
                    .selectNodes(document))) {
                node.detach();
                deleted = true;
            }
        }
    }

    /**
     * Function that gets the value for a cell given by its title, i.e. returns the text of the second cell of a row where
     * the text of the first cell matches the title. If the cell does not exist, an empty string is returned
     *
     * @param path the xpath expression to find the cell
     * @param root the root node
     * @param title the title, i.e. the text of the first cell in the row
     * @return either the text contained or an empty string
     *
     */
    private String getCellText(String path, Element root, String title) throws JDOMException {
        return getTextInRow(path, root, title, 2);
    }

    /**
     * Function that safely returns the text from an Element returning an empty string if the element does not exist
     *
     * @param element the element to get the text from
     * @return either the text contained or an empty string
     */
    private String safeGetText(Element element) {
        if (element == null)
            return "";
        else
            return element.getText();
    }

    /**
     * Function that gets the text from a certain cell in a row given its index
     *
     * @param path the xpath expression to find the cell
     * @param e the root element
     * @param title the text contained in the first cell
     * @param pos the cell in the row
     * @return either the text contained or an empty string
     * @throws JDOMException if the xpath expression is invalid
     */
    private String getTextInRow(String path, Element e, String title, int pos) throws JDOMException {
        if (path == null || e == null)
            return "" ;
        else {
            Element cell = (Element) XPath.newInstance(String.format(path, title, pos))
                    .selectSingleNode(e);
            return safeGetText(cell);
        }
    }

    /**
     * Function that gets a pair, the information and the associated notes, from a row
     *
     * @param path the xpath expression to find the cells
     * @param root the root
     * @param title the text contained in the first cell
     * @return an object representing both the information and the associated notes
     * @throws JDOMException if the xpath expression is invalid
     */
    private InformationNotes getInformationNotes(String path, Element root, String title) throws JDOMException {
        return new InformationNotes(getTextInRow(path, root, title, 2),getTextInRow(path, root, title, 3));
    }

    /**
     * Method that reads the XML data from the spreadsheet into a java data structure
     * @param refcoDoc the spreadsheet document
     * @return a report containing potential problems when reading the documentation file
     */
    private Report readRefcoCriteria(Document refcoDoc) {
        Report report = new Report();
        try {

            // Read Overview tab
            Element overviewTable = (Element) XPath.newInstance("//table:table[@table:name='Overview']").selectSingleNode(refcoDoc);
            String cellXPath =
                    "//table:table-row[table:table-cell[text:p=\"%s\"]]/table:table-cell[position()=%d]/text:p";
            criteria.corpusTitle = getCellText(cellXPath, overviewTable,  "Corpus Title");
            criteria.subjectLanguages = getCellText(cellXPath, overviewTable,  "Subject Language(s)");
            criteria.archive = getCellText(cellXPath, overviewTable,  "Archive");
            criteria.persistentId = getCellText(cellXPath, overviewTable,  "Corpus Persistent Identifier") ;
            criteria.annotationLicense = getCellText(cellXPath, overviewTable,  "Annotation Files Licence");
            criteria.recordingLicense = getCellText(cellXPath, overviewTable,  "Recording Files Licence") ;
            criteria.creatorName = getCellText(cellXPath, overviewTable,  "Corpus Creator Name") ;
            criteria.creatorContact = getCellText(cellXPath, overviewTable,  "Corpus Creator Contact") ;
            criteria.creatorInstitution = getCellText(cellXPath, overviewTable,  "Corpus Creator Institution") ;
            criteria.refcoVersion = getInformationNotes(cellXPath, overviewTable, "Corpus Documentation's Version") ;
            criteria.numberSessions = getInformationNotes(cellXPath, overviewTable, "Number of sessions") ;
            criteria.numberTranscribedWords = getInformationNotes(cellXPath, overviewTable, "Total number of transcribed words");
            criteria.numberAnnotatedWords = getInformationNotes(cellXPath, overviewTable, "Total number of morphologically analyzed words");
            criteria.translationLanguages = getInformationNotes(cellXPath, overviewTable, "Translation language(s)");
            // Read CorpusComposition tab
            Element sessionTable = (Element) XPath.newInstance("//table:table[@table:name='CorpusComposition']").selectSingleNode(refcoDoc);
            if (sessionTable == null)
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                                "howtoFix"},
                        new Object[]{getFunction(),refcoShortName,"Corpus documentation: CorpusComposition table not found",
                                "Add table CorpusComposition to corpus documentation"}));
            else {
                boolean missingData = false;
                List<Element> rowList = listToParamList(Element.class, sessionTable.getChildren("table-row",
                        tableNamespace));
                for (Element row : rowList) {
                    List<Element> columns = listToParamList(Element.class, row.getChildren("table-cell", tableNamespace));
                    if (columns.size() > 7 && !safeGetText(columns.get(0).getChild("p", textNamespace)).isEmpty()
                            && !safeGetText(columns.get(0).getChild("p", textNamespace)).equals("Sessions")) {
                        Session session = new Session();
                        session.sessionName = safeGetText(columns.get(0).getChild("p", textNamespace));
                        session.fileName = safeGetText(columns.get(1).getChild("p", textNamespace));
                        session.speakerName = safeGetText(columns.get(2).getChild("p", textNamespace));
                        session.speakerAge = safeGetText(columns.get(3).getChild("p", textNamespace));
                        session.speakerGender = safeGetText(columns.get(4).getChild("p", textNamespace));
                        session.recordingLocation = safeGetText(columns.get(5).getChild("p", textNamespace));
                        session.recordingDate = safeGetText(columns.get(6).getChild("p", textNamespace));
                        session.genre = safeGetText(columns.get(7).getChild("p", textNamespace));
                        // Age group was a custom column
                        // session.ageGroup = safeGetText(columns.get(8).getChild("p", textNamespace));
                        criteria.sessions.add(session);
                    } else if (columns.size() > 0 && !safeGetText(columns.get(0).getChild("p", textNamespace)).equals(
                            "Sessions")) {
                        missingData = true;
                    }
                }
                if (missingData || rowList.size() <= 1) {
                    report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function", "filename",
                                    "description", "howtoFix"},
                            new Object[]{getFunction(), refcoShortName,"Corpus documentation: Wrong number of columns or " +
                                    "missing data in CorpusComposition table",
                                    "Check number of columns and presence of data in all cells"}));
                }
            }
            // Read AnnotationTiers tab
            Element tierTable = (Element) XPath.newInstance("//table:table[@table:name='AnnotationTiers']").selectSingleNode(refcoDoc);
            if (tierTable == null)
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function", "filename", "description"
                                , "howtoFix"},
                        new Object[]{getFunction(), refcoShortName,
                                "Corpus documentation: AnnotationTiers table not found",
                                "Add table AnnotationTiers to corpus documentation"}));
            else {
                boolean missingData = false;
                List<Element> rowList = listToParamList(Element.class, tierTable.getChildren("table-row",
                        tableNamespace));
                for (Element row : rowList) {
                    List<Element> columns = listToParamList(Element.class, row.getChildren("table-cell", tableNamespace));
                    if (columns.size() > 3 && !safeGetText(columns.get(0).getChild("p", textNamespace)).isEmpty()
                            && !safeGetText(columns.get(0).getChild("p", textNamespace)).equals("Names")) {
                        Tier tier = new Tier();
                        tier.tierName = safeGetText(columns.get(0).getChild("p", textNamespace)).trim();
                        tier.tierFunctions = Arrays.stream(safeGetText(columns.get(1).getChild("p", textNamespace))
                                .split(valueSeparator)).map(String::toLowerCase).collect(Collectors.toList());
                        tier.segmentationStrategy = safeGetText(columns.get(2).getChild("p", textNamespace));
                        tier.languages = safeGetText(columns.get(3).getChild("p", textNamespace));
                        // Morpheme distinction is optional
                        if (columns.size() > 4)
                            tier.morphemeDistinction = safeGetText(columns.get(4).getChild("p", textNamespace));
                        criteria.tiers.add(tier);
                    }  else if (columns.size() > 0 && !safeGetText(columns.get(0).getChild("p", textNamespace)).equals(
                            "Names")) {
                        missingData = true;
                    }
                }
                if (missingData || rowList.size() <= 1)
                    report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function", "filename",
                                    "description", "howtoFix"},
                            new Object[]{getFunction(), refcoShortName,"Corpus documentation: Wrong number of columns or " +
                                    "missing data in AnnotationTiers table", "Check number of columns and presence of" +
                                    " data in all cells"}));
            }
            // Read Transcription tab
            Element transcriptionTable = (Element) XPath.newInstance("//table:table[@table:name='Transcription']").selectSingleNode(refcoDoc);
            if (transcriptionTable == null)
                report.addCritical(getFunction(),
                        ReportItem.newParamMap(new String[]{"function","filename", "description", "howtoFix"},
                        new Object[]{getFunction(),refcoShortName,"Corpus documentation: Transcription table not found",
                                "Add table Transcription to corpus documentation"}));
            else {
                boolean missingData = false;
                List<Element> rowList = listToParamList(Element.class, transcriptionTable.getChildren("table-row",
                        tableNamespace));
                for (Element row : rowList) {
                    List<Element> columns = listToParamList(Element.class, row.getChildren("table-cell", tableNamespace));
                    if (columns.size() > 2 && !safeGetText(columns.get(0).getChild("p", textNamespace)).isEmpty()
                            && !safeGetText(columns.get(0).getChild("p", textNamespace)).equals("Graphemes")) {
                        Transcription transcription = new Transcription();
                        transcription.grapheme = safeGetText(columns.get(0).getChild("p", textNamespace));
                        transcription.linguisticValue = safeGetText(columns.get(1).getChild("p", textNamespace));
                        transcription.linguisticConvention = safeGetText(columns.get(2).getChild("p", textNamespace));
                        criteria.transcriptions.add(transcription);
                    } else if (columns.size() > 0 && !safeGetText(columns.get(0).getChild("p", textNamespace)).equals(
                            "Graphemes")) {
                        missingData = true;
                    }
                }
                if (missingData || rowList.size() <= 1)
                    report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename",
                                    "description", "howtoFix"},
                            new Object[]{getFunction(), refcoShortName,"Corpus documentation: Wrong number of columns or " +
                                    "missing data in Transcription table",
                                    "Check number of columns and presence of data in all cells"}));
            }
            // Read Glosses tab
            Element glossesTable =
                    (Element) XPath.newInstance("//table:table[starts-with(@table:name,'Gloss')]").selectSingleNode(refcoDoc);
            if (glossesTable == null)
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                                "howtoFix"},
                        new Object[]{getFunction(),refcoShortName,"Glosses table not found",
                                "Add table Glosses to corpus documentation"}));
            else {
                boolean missingData = false;
                List<Element> rowList = listToParamList(Element.class, glossesTable.getChildren("table-row",
                        tableNamespace));
                for (Element row : rowList) {
                    List<Element> columns = listToParamList(Element.class, row.getChildren("table-cell", tableNamespace));
                    if (columns.size() > 3 && !safeGetText(columns.get(0).getChild("p", textNamespace)).isEmpty()
                            && !safeGetText(columns.get(0).getChild("p", textNamespace)).equals("Abbreviations")) {
                        Gloss gloss = new Gloss();
                        gloss.gloss = safeGetText(columns.get(0).getChild("p", textNamespace)).replace("\\s+","");
                        gloss.meaning = safeGetText(columns.get(1).getChild("p", textNamespace));
                        gloss.comments = safeGetText(columns.get(2).getChild("p", textNamespace));
                        gloss.tiers = safeGetText(columns.get(3).getChild("p", textNamespace));
                        criteria.glosses.add(gloss);
                    } else if (columns.size() > 0 && !safeGetText(columns.get(0).getChild("p", textNamespace)).equals(
                            "Abbreviations")) {
                        missingData = true;
                    }
                }
                if (missingData || rowList.size() <= 1)
                    report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename",
                                    "description", "howtoFix"},
                            new Object[]{getFunction(),refcoShortName,"Corpus documentation: Wrong number of columns or " +
                                    "missing data in Glosses table",
                                    "Check number of columns and presence of data in all cells"}));
            }
            // Read Punctuation tab
            Element punctuationsTable = (Element) XPath.newInstance("//table:table[@table:name='Punctuations']").selectSingleNode(refcoDoc);
            if (punctuationsTable == null)
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                                "howtoFix"},
                        new Object[]{getFunction(),refcoShortName,"Punctuation table not found",
                                "Add table Punctuation to corpus documentation"}));
            else {
                boolean missingData = false;
                List<Element> rowList = listToParamList(Element.class, punctuationsTable.getChildren("table-row",
                        tableNamespace));
                for (Element row : rowList) {
                    List<Element> columns = listToParamList(Element.class, row.getChildren("table-cell", tableNamespace));
                    if (columns.size() > 3 && !safeGetText(columns.get(0).getChild("p", textNamespace)).isEmpty()
                            && !safeGetText(columns.get(0).getChild("p", textNamespace)).equals("Characters")) {
                        Punctuation punctuation = new Punctuation();
                        punctuation.character = safeGetText(columns.get(0).getChild("p", textNamespace));
                        punctuation.meaning = safeGetText(columns.get(1).getChild("p", textNamespace));
                        punctuation.comments = safeGetText(columns.get(2).getChild("p", textNamespace));
                        punctuation.tiers = safeGetText(columns.get(3).getChild("p", textNamespace));
                        // TODO Function is optional?
                        if (columns.size()>4)
                            punctuation.function = safeGetText(columns.get(4).getChild("p", textNamespace));
                        criteria.punctuations.add(punctuation);
                    } else if (columns.size() > 0 && !safeGetText(columns.get(0).getChild("p", textNamespace)).equals(
                            "Characters")) {
                        missingData = true;
                    }
                }
                 if (missingData || rowList.size() <= 1)
                     report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename",
                                     "description", "howtoFix"},
                             new Object[]{getFunction(),refcoShortName,"Corpus documentation: Wrong number of columns or " +
                                     "missing data in Punctuation table",
                                     "Check number of columns and presence of data in all cells"}));
            }
        } catch (JDOMException | NullPointerException exception) {
            report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                            "exception"},
                    new Object[]{getFunction(),refcoShortName,"Corpus documentation: Unexpected exception",exception}));
        }
        return report;
    }

    /**
     * Function that performs generic checks on the RefCo documentation stored in the Checker object (using setRefcoFile)
     *
     * @return the detailed report of all checks performed
     */
    private Report refcoGenericCheck() {
        logger.info("Generic checks");
        Report report = new Report() ;
        if (criteria.corpusTitle == null || criteria.corpusTitle.isEmpty())
            report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                            "howtoFix"},
                        new Object[]{getFunction(),refcoShortName,"Overview: Corpus title is empty", "Add a corpus title"}));
        if (criteria.subjectLanguages == null || criteria.subjectLanguages.isEmpty())
            report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                            "howtoFix"},
                        new Object[]{getFunction(),refcoShortName,"Overview: Subject languages is empty",
                                "Add a subject language"}));
        else {
            // Each cell can contain several languages. Split the languages and check for each one if it is a
            // valid language code
            // TODO check if that is the intended way to separate languages
            ArrayList<String> subjectLangList = new ArrayList<>(Arrays.asList(criteria.subjectLanguages
                    .split(valueSeparator)));
            for (String l : subjectLangList) {
                if (!checkLanguage(l)) {
                    report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename",
                                    "description", "howtoFix"},
                        new Object[]{getFunction(),refcoShortName,"Overview: Language is neither a Glottolog, a ISO-639-3 " +
                                "language code nor otherwise known: " + l, "Use a valid language code"}));
                }
            }
        }
        if (criteria.archive == null || criteria.archive.isEmpty())
            report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                            "howtoFix"},
                    new Object[]{getFunction(),refcoShortName,"Overview: Archive name is empty", "Add an archive name"}));
        if (criteria.persistentId == null || criteria.persistentId.isEmpty())
            report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                            "howtoFix"},
                    new Object[]{getFunction(),refcoShortName,"Overview: Persistent identifier is empty",
                            "Add a persistent identifier"}));
        else if (!checkUrl(criteria.persistentId)){
            report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                            "howtoFix"},
                    new Object[]{getFunction(),refcoShortName,"Overview: Persistent identifier not a URL",
                            "Use a valid URL as the persistent identifier"}));
        }
        if (criteria.annotationLicense == null || criteria.annotationLicense.isEmpty())
            report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                            "howtoFix"},
                    new Object[]{getFunction(),refcoShortName,"Overview: Annotation license is empty",
                            "Add annotation license"}));
        if (criteria.recordingLicense == null || criteria.recordingLicense.isEmpty())
            report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                            "howtoFix"},
                    new Object[]{getFunction(),refcoShortName,"Overview: Recording license is empty",
                            "Add recording license"}));
        if (criteria.creatorName == null || criteria.creatorName.isEmpty())
            report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                            "howtoFix"},
                    new Object[]{getFunction(),refcoShortName,"Overview: Creator name is empty","Add creator name"}));
        if (criteria.creatorContact == null || criteria.creatorContact.isEmpty())
            report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                            "howtoFix"},
                    new Object[]{getFunction(),refcoShortName,"Overview: Creator contact is empty","Add Creator contact"}));
        if (criteria.creatorInstitution == null || criteria.creatorInstitution.isEmpty())
            report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                            "howtoFix"},
                    new Object[]{getFunction(),refcoShortName,"Overview: Creator institution is empty",
                            "Add creator institution"}));
        if (criteria.refcoVersion.information == null || criteria.refcoVersion.information.isEmpty())
            report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                            "howtoFix"},
                    new Object[]{getFunction(),refcoShortName,"Overview: RefCo version is empty","Add RefCo version"}));
        else {
            try {
                // Check the number by trying to parse it
                Integer.parseInt(criteria.refcoVersion.information);
                // TODO do something with this number
            }
            catch (NumberFormatException e) {
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                                "howtoFix"},
                        new Object[]{getFunction(),refcoShortName,"Overview: Refco version is not a number",
                                "Check the RefCo version number"}));
            }
        }
        if (criteria.numberSessions.information == null || criteria.numberSessions.information.isEmpty())
            report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                            "howtoFix"},
                    new Object[]{getFunction(),refcoShortName,"Overview: Number of sessions is empty",
                            "Add number of sessions"}));
        else {
            try {
                // Check the number by trying to parse it
                int i = Integer.parseInt(criteria.numberSessions.information);
                // Compare it to the number of rows in the CorpusComposition table
                if (i != criteria.sessions.size())
                    report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename",
                                    "description", "howtoFix"},
                            new Object[]{getFunction(),refcoShortName,"Corpus composition: Number of sessions does not " +
                                    "match number of sessions, expected " + i + " and found: " + criteria.sessions.size(),
                            "Compare the number given in Overview to the number of sessions in Corpus composition"}));
            }
            catch (NumberFormatException e) {
                report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                                "howtoFix"},
                        new Object[]{getFunction(),refcoShortName,"Overview: Number of sessions is not a number",
                                "Check the number of sessions"}));
            }
        }
        if (criteria.numberTranscribedWords.information == null || criteria.numberTranscribedWords.information.isEmpty())
            report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                            "howtoFix"},
                    new Object[]{getFunction(),refcoShortName,"Overview: Number of transcribed words is empty",
                            "Add number of transcribed words count"}));
        else {
            try {
                // Check the number by trying to parse it
                int i = Integer.parseInt(criteria.numberTranscribedWords.information);
                // Compare it to our own count
                int c = countTranscribedWords();
                if (i==0 || c == 0 || 0.8 < (float)c/i || (float)c/i > 1.2)
                    report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename",
                                    "description", "howtoFix"},
                            new Object[]{getFunction(),refcoShortName,"Overview: Transcription word count is either 0" +
                                    " or more than 20 percent off. Counted " + c + " expected " + i,"Correct the word" +
                                    " count"}));
            }
            catch (JDOMException e) {
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,"Overview: Exception encountered when counting " +
                                "transcribed words"}));
            }
            catch (NumberFormatException e) {
                report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                                "howtoFix"},
                        new Object[]{getFunction(),refcoShortName,"Overview: Number of transcribed words is not a number",
                                "Check the word count and make sure it is given as a proper number"
                        }));
            }
        }
        if (criteria.numberAnnotatedWords.information == null || criteria.numberAnnotatedWords.information.isEmpty())
            report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                            "howtoFix"},
                        new Object[]{getFunction(),refcoShortName,"Overview: Number of annotated words is empty",
                                "Add the number of annotated words"}));
        else {
            try {
                // Check the number by trying to parse it
                Integer.parseInt(criteria.numberAnnotatedWords.information);
                // TODO do something with this number
                // The name of the annotation tier does not seem stable
            }
            catch (NumberFormatException e) {
                report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,"Number of annotated words is not a number"}));
            }
        }
        if (criteria.translationLanguages.information == null || criteria.translationLanguages.information.isEmpty())
            report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                    new Object[]{getFunction(),refcoShortName,"Translation languages is empty"}));
        else {
            // Each cell can contain several languages. Split the languages and check for each one if it is a
            // valid language code
            // TODO check if that is the intended way to separate languages
            ArrayList<String> translationLangList = new ArrayList<>(
                    Arrays.asList(criteria.translationLanguages.information.split(valueSeparator)));
            for (String l : translationLangList) {
                if (!checkLanguage(l)) {
                    report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,"Language is neither a Glottolog, a ISO-639-3 language code " +
                                "nor otherwise known: " + l}));
                }
            }
        }
        // Check each of the sessions
        for (Session s : criteria.sessions) {
            if (s.speakerName == null || s.speakerName.isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,"Session name is empty"}));
            if (s.fileName == null || s.fileName.isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,"Session file name is empty: " + s.sessionName}));
            else {
                // Each cell can contain several files. Split the files and check for each one if it exists
                ArrayList<String> filenames = new ArrayList<>(
                    Arrays.asList(s.fileName.split(valueSeparator)));
                for (String f : filenames) {
                    boolean fileExists = new File(f).exists();
                    // If it is an ELAN file, it can/should be in the annotation folder
                    if (f.toLowerCase().endsWith("eaf"))
                        try {
                            fileExists = fileExists
                                    || new File(new URL(refcoCorpus.getBaseDirectory() +"/Annotations/" + f).toURI()).exists()
                                    || new File(new URL(refcoCorpus.getBaseDirectory() + "/annotations/" + f).toURI()).exists();
                        }
                        catch (MalformedURLException | URISyntaxException e) {
                            report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename",
                                            "description","exception"},
                                    new Object[]{getFunction(),refcoShortName,
                                            "Exception encountered checking file name: "+ f, e}));
                        }
                    // if it is a wav file, it can should be in the recordings folder
                    else if (f.toLowerCase().endsWith("wav")) {
                        try {
                            fileExists = fileExists
                                    || new File(new URL(refcoCorpus.getBaseDirectory() + "/Recordings/" + f).toURI()).exists()
                                    || new File(new URL(refcoCorpus.getBaseDirectory() + "/recordings/" + f).toURI()).exists();
                        }
                        catch (MalformedURLException | URISyntaxException e) {
                            report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function", "filename",
                                            "description","exception"},
                                    new Object[]{getFunction(),refcoShortName,"Exception encountered checking file name: "+ f,e}));
                        }
                    }
                    if (!fileExists)
                        report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename",
                                        "description", "howtoFix"},
                                new Object[]{getFunction(),refcoShortName,"Corpus composition: File does not exist: " + f,
                                        "Check the file reference in the documentation and remove the reference to " +
                                                "the file if it is removed intentionally"}));
                }
            }
            if (s.speakerName == null || s.speakerName.isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,"Corpus composition: Speaker name is empty"}));
            if (s.speakerAge == null || s.speakerAge.isEmpty())
                report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,"Corpus composition: Speaker age is empty: " + s.speakerName}));
            else if (!s.speakerAge.matches("~?\\d{1,3}"))
                report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,"Corpus composition: Speaker age does not match schema: " + s.speakerAge}));
            if (s.speakerGender == null || s.speakerGender.isEmpty())
                report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,"Corpus composition: Speaker gender is empty: " + s.speakerName}));
            if (s.recordingLocation == null || s.recordingLocation.isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,"Corpus composition: Recording location is empty: " + s.speakerName}));
            if (s.recordingLocation == null || s.recordingDate.isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,"Corpus composition: Recording date is empty: " + s.speakerName}));
            else {
                // Check date by trying to parse it
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    df.parse(s.recordingDate);
                } catch (ParseException e) {
                    report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,"Corpus composition: Recording date in invalid format. Expected yyyy-mm-dd, got: "
                                + s.recordingDate}));
                }
            }
            if (s.genre == null || s.genre.isEmpty())
                report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,"Corpus composition: Genre is empty"}));
            // This is a custom column
//            if (s.ageGroup == null || s.ageGroup.isEmpty())
//                report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
//                        new Object[]{getFunction(),refcoShortName,"Age group is empty"}));
        }
        // Check all tiers
        for (Tier t : criteria.tiers) {
            if (t.tierName == null || t.tierName.isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,"Annotation Tiers: tier name is empty"}));
            if (t.tierFunction == null || t.tierFunction.isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename",
                                "description","howtoFix"},
                        new Object[]{getFunction(),refcoShortName,
                                "Annotation Tiers: tier function is empty: " + t.tierName,"Add tier function"}));
            if (t.segmentationStrategy == null || t.segmentationStrategy.isEmpty())
                report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,
                                "Annotation Tiers: segmentation strategy is empty: " + t.tierName}));
            if (t.languages == null || t.languages.isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,
                                "Annotation Tiers: tier languages is empty: " + t.tierName}));
            else {
                // Each cell can contain several languages. Split the languages and check for each one if it is a
                // valid language code
                // TODO check if that is the intended way to separate languages
                ArrayList<String> tierLangList = new ArrayList<>(Arrays.asList(t.languages.split(valueSeparator)));
                for (String l : tierLangList) {
                    if (!checkLanguage(l)) {
                        report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                                new Object[]{getFunction(),refcoShortName,"Annotation Tiers: language is neither a " +
                                        "Glottolog, a ISO-639-3 language code nor otherwise known: " + l}));
                    }
                }
            }
            if (t.morphemeDistinction == null || t.morphemeDistinction.isEmpty())
                report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename",
                                "description", "howtoFix"},
                        new Object[]{getFunction(),refcoShortName,"Morpheme distinction is empty: " + t.tierName,
                                "Add morpheme distinction"}));
        }
        // Check all transcription graphemes
        for (Transcription t : criteria.transcriptions) {
            if (t.grapheme == null || t.grapheme.isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,"Grapheme is empty"}));
            if (t.linguisticValue == null || t.linguisticValue.isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,"Grapheme linguistic value is empty: " + t.grapheme}));
            if (t.linguisticConvention == null || t.linguisticConvention.isEmpty())
                report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,
                                "Grapheme linguistic convention is empty: " + t.grapheme}));
        }
        // Check all glosses
        for (Gloss g : criteria.glosses) {
            if (g.gloss == null || g.gloss.isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,"Glosses: Gloss is empty"}));
            if (g.gloss.split(glossSeparator).length > 1)
                report.addWarning(getFunction(),
                        ReportItem.newParamMap(new String[]{"function","filename", "description", "howtoFix"},
                        new Object[]{getFunction(),refcoShortName,
                                "Glosses: Gloss contains separating character (" + glossSeparator + "): " + g.gloss,
                                "Document the gloss parts separately"}));
            if (g.meaning == null || g.meaning.isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                                "howtoFix"},
                        new Object[]{getFunction(),refcoShortName,"Glosses: Gloss meaning is empty: " + g.gloss,
                                "Add a gloss definition to the corpus documentation"}));
            // We skip comments assuming it is optional
            if (g.tiers == null || g.tiers.isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description",
                                "howtoFix"},
                        new Object[]{getFunction(),refcoShortName,"Corpus data: Gloss tiers are empty: " + g.gloss,
                                "Check that all gloss tiers are documented as such"}));
            // If the tiers is not "all", check if its valid tiers
            else if (!g.tiers.equalsIgnoreCase("all")) {
                // Each cell can contain several tiers. Split the tiers and check for each one if it is a
                // valid tier defined in AnnotationTiers
                ArrayList<String> tierList = new ArrayList<>(Arrays.asList(g.tiers.split(valueSeparator)));
                for (String t : tierList) {
                    // At least one of the tiers has to be defined in the AnnotationTiers table, otherwise report error
                    if (criteria.tiers.stream().filter((tt) -> tt.tierName.equals(t) || tt.tierFunction.contains(t)).toArray().length == 0)
                        report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                                new Object[]{getFunction(),refcoShortName,
                                        "Annotation tiers: Gloss tier not defined: " + t}));
                }
            }
        }
        // Check all punctuation
        for (Punctuation p : criteria.punctuations) {
            if (p.character == null || p.character.isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,"Punctuation: grapheme is empty"}));
            if (p.meaning == null || p.meaning.isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,
                                "Punctuation: meaning is empty for grapheme: " + p.character}));
            // We skip comments assuming it is optional
            if (p.tiers == null || p.tiers.isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                        new Object[]{getFunction(),refcoShortName,
                                "Punctuation: tiers is empty for grapheme: " + p.character}));
            // If the tiers is not "all", check if its valid tiers
            else if (!p.tiers.equalsIgnoreCase("all")) {
                 // Each cell can contain several tiers. Split the tiers and check for each one if it is a
                // valid tier defined in AnnotationTiers
                ArrayList<String> tierList = new ArrayList<>(Arrays.asList(p.tiers.split(valueSeparator)));
                for (String t : tierList) {
                    // At least one of the tiers has to be defined in the AnnotationTiers table, otherwise report error
                    if (criteria.tiers.stream().filter((tt) -> tt.tierName.equals(t) || tt.tierFunction.equals(t)).toArray().length == 0)
                        report.addCritical(getFunction(),ReportItem.newParamMap(new String[]{"function","filename", "description"},
                                new Object[]{getFunction(),refcoShortName,"Punctuation tier not defined in " +
                                        "AnnotationTiers: " + t}));
                }
            }
        }
        return report ;
    }

    /**
     * Function that checks a corpus document based on the RefCo documentation stored in the checker object (using setRefcoFile)
     *
     * @param cd the corpus document
     * @return the detailed report of the checks
     */
    private Report refcoCorpusCheck(CorpusData cd) {
        Report report = new Report() ;
        // Check for ELAN data
        if (cd instanceof ELANData) {
            // Check the transcription
            try {
                report.merge(checkTranscription(cd));
            }
            catch (JDOMException e) {
                report.addCritical(getFunction(),
                        ReportItem.newParamMap(new String[]{"function","filename","description", "exception"},
                        new Object[]{getFunction(), cd.getFilename(), "Exception encountered when reading Transcription " +
                                "tier", e}));
            }
            // Check the morphology
            try {
                report.merge(checkMorphology(cd));
            }
            catch (JDOMException e) {
                report.addCritical(getFunction(),
                        ReportItem.newParamMap(new String[]{"function","filename","description", "exception"},
                        new Object[]{getFunction(), cd.getFilename(),
                                "Exception encountered when reading Morphology tier", e}));
            }
        }
        else {
            report.addCritical(getFunction(), ReportItem.newParamMap(new String[]{"function","filename","description"},
                    new Object[]{getFunction(), refcoShortName, "Not supported corpus type: " + cd.getClass().getName()}));
        }
        return report ;
    }

    /**
     * Function that checks all transcription tiers in a corpus document
     *
     * @param cd the corpus document
     * @return the detailed report of the checks
     * @throws JDOMException on XPath problems
     */
    private Report checkTranscription(CorpusData cd) throws JDOMException {
        Report report = new Report();
        // Get the dom
        Document content = ((ELANData) cd).getJdom();
        // Get all transcription tiers
        ArrayList<String> transcriptionTiers = new ArrayList<>();
        transcriptionTiers.add("transcription") ; // Add default tier function for transcription
        for (Tier t: criteria.tiers) {
            // Also add all tiers that contain transcription in the tier function
            if (t.tierFunction.toLowerCase().contains("transcription")) {
                transcriptionTiers.add(t.tierName);
            }
        }
        // Get all transcription graphemes
        // Set<Character> validTranscriptionCharacters = new HashSet<>(criteria.transcriptions.size()) ;
        List<String> validTranscriptionCharacters = new ArrayList<>(criteria.transcriptions.size()) ;
        for (Transcription t : criteria.transcriptions) {
            // Add all of the grapheme's characters
            // validTranscriptionCharacters.addAll(getChars(t.grapheme));
            validTranscriptionCharacters.add(t.grapheme);
        }
        // and punctuation characters
        for (Punctuation p : criteria.punctuations) {
            if (p.tiers.equals("all"))
                // Add all of the punctuation's characters
                // validTranscriptionCharacters.addAll(getChars(p.character));
                validTranscriptionCharacters.add(p.character);
            else {
                for (String t : new ArrayList<>(Arrays.asList(p.tiers.split(valueSeparator)))) {
                    if (transcriptionTiers.contains(t.toLowerCase())) {
                        // Add all of the punctuation's characters
                        // validTranscriptionCharacters.addAll(getChars(p.character));
                        validTranscriptionCharacters.add(p.character);
                        break;
                    }
                }
            }
        }
        // Also get all glosses valid for the transcription tiers
        Set<String> validGlosses = new HashSet<>();
        for (Gloss g : criteria.glosses) {
            if (g.tiers.equals("all"))
                validGlosses.add(g.gloss);
            else {
                for (String t : new ArrayList<>(Arrays.asList(g.tiers.split(valueSeparator)))) {
                    if (transcriptionTiers.contains(t)) {
                        validGlosses.add(g.gloss);
                        break;
                    }
                }
            }
        }
        // Get the text from all transcription tiers
        List<Text> transcriptionText = new ArrayList<>();
        for (String t : transcriptionTiers) {
            transcriptionText.addAll(getTextsInTierByID(content, t));
        }
        // Check if one of the relevant variables is empty and, if yes, skip the transcription test
        if (transcriptionTiers.isEmpty()) {
            report.addCritical(getFunction(), ReportItem.newParamMap(new String[]{"function","filename","description",
                            "howtoFix"},
                            new Object[]{getFunction(), cd.getFilename(), "Corpus data: No transcription tiers found",
                                    "Check that all transcription tiers are documented"}));
            return report;
        }
        if (validTranscriptionCharacters.isEmpty()) {
            report.addCritical(getFunction(), ReportItem.newParamMap(new String[]{"function","filename","description",
                            "howtoFix"},
                    new Object[]{getFunction(), cd.getFilename(), "Transcription/Punctuation: No valid transcription " +
                            "characters " +
                            "(graphemes/punctuation) defined",
                            "Define all graphemes and punctuation characters used in the corpus"}));
            return report;
        }
        if (transcriptionText.isEmpty()) {
            report.addCritical(getFunction(), ReportItem.newParamMap(new String[]{"function","filename","description",
                            "howtoFix"},
                    new Object[]{getFunction(), cd.getFilename(), "Corpus data: No transcribed text found in one of the " +
                            "expected tiers: " +
                            String.join(", ", transcriptionTiers),
                            "Check that all documented transcription tiers exist in the corpus"}));
            return report;
        }
        report.merge(checkTranscriptionText(cd,transcriptionText,validTranscriptionCharacters,validGlosses));
        return report;
    }

    /**
     * Function that checks all morphology tiers in a corpus document
     * @param cd the corpus document
     * @return the detailed report of the checks
     * @throws JDOMException on XPath problems
     */
    private Report checkMorphology(CorpusData cd) throws JDOMException {
        Report report = new Report();
        // Get the dom
        Document content = ((ELANData) cd).getJdom();
        // Get morphology tiers TODO that is very hand-wavy
        List<String> morphologyTiers = criteria.tiers.stream()
                .filter((t) ->
                        t.tierFunction.toLowerCase().contains("morpho") ||
                        t.tierFunction.toLowerCase().contains("gloss")
                )
                .map((t) -> t.tierName).collect(Collectors.toList()) ;
        // Get all valid Glosses
        HashSet<String> validGlosses = new HashSet<>() ;
        for (Gloss g : criteria.glosses) {
            if (g.tiers.equals("all"))
                validGlosses.add(g.gloss) ;
            else {
                for (String t : new ArrayList<>(Arrays.asList(g.tiers.split(valueSeparator)))) {
                    if (morphologyTiers.contains(t)) {
                        validGlosses.add(g.gloss);
                        break;
                    }
                }
            }
        }
        // Get the text from all morphology tiers
        List<Text> glossText = new ArrayList<>();
        for (String t : morphologyTiers) {
            glossText.addAll(getTextsInTierByID(content, t));
        }
        // Check if one of the relevant variables is empty and, if yes, skip the transcription test
        if (morphologyTiers.isEmpty()) {
            report.addCritical(getFunction(), ReportItem.newParamMap(new String[]{"function","filename","description"
                    ,"howtoFix"},
                    new Object[]{getFunction(), cd.getFilename(), "Corpus composition: No morphology tiers found",
                            "Add documentation for tiers of type Morphology gloss"}));
            return report;
        }
        if (validGlosses.isEmpty()) {
            report.addWarning(getFunction(), ReportItem.newParamMap(new String[]{"function","filename","description"},
                            new Object[]{getFunction(), cd.getFilename(), "No valid glosses defined"}));
            return report;
        }
        if (glossText.isEmpty()) {
            report.addCritical(getFunction(), ReportItem.newParamMap(new String[]{"function","filename","description",
                    "howtoFix"},
                    new Object[]{getFunction(), cd.getFilename(), "No annotated text found in one of the expected tiers: " +
                    String.join(", ", morphologyTiers),
                            "Check the tier documentation to make sure that your morphology tiers are covered"}));
            return report;
        }
        report.merge(checkMorphologyGloss(cd,glossText,validGlosses));
        return report ;
    }

    /**
     * Function that checks the morphology tier for documented glosses
     * @param cd the corpus document used to correctly assign the log messages
     * @param text the extracted text from all morphology tiers
     * @param glosses all documented glosses
     * @return the detailed report of the checks
     */
    private Report checkMorphologyGloss(CorpusData cd, List<Text> text, HashSet<String> glosses) {
        Report report = new Report() ;

        // All the tokens that are valid
        int matched = 0;
        // All invalid tokens in the text
        int missing = 0 ;
        // Indicator if a word contains missing characters
        for (Text t : text) {
            // Tokenize text
            for (String token : t.getText().split(tokenSeparator)) {
                // Check if token is a gloss
                for (String morpheme : token.split(glossSeparator)) {
                    if (!glosses.contains(morpheme)) {
                        missing += 1;
                        // <></>his would lead to large amount of warnings
                        // report.addWarning(getFunction(),cd,"Invalid token: " + token);
                    } else {
                        matched += 1;
                    }
                    morphemeFreq.compute(morpheme,(k, v) -> (v == null) ? 1 : v + 1);
                }
                glossFreq.compute(token,(k, v) -> (v == null) ? 1 : v + 1);
            }
        }
        float percentValid = (float)matched/(matched+missing) ;
        if (percentValid < glossMorphemesValid / 100.0)
            report.addWarning(getFunction(), ReportItem.newParamMap(new String[]{"function","filename","description",
                            "howtoFix"},
                            new Object[]{getFunction(), cd.getFilename(),
                                    "Corpus data: Less than " + glossMorphemesValid + " percent of tokens are" +
                                            " valid gloss morphemes.\nValid: " + matched + " Invalid: " + missing +
                                            " Percentage valid: " + Math.round(percentValid*1000)/10.0,
                                    "Improve the gloss documentation to cover more tokens"}));
        else
            report.addCorrect(getFunction(),ReportItem.newParamMap(new String[]{"function", "filename", "description", "howtoFix"},
                    new Object[] {getFunction(),cd.getFilename(),
                            "Corpus data: More than " + glossMorphemesValid + " percent of tokens are " +
                                    "valid gloss morphemes.\nValid: " + matched + " Invalid: " + missing +
                                    " Percentage valid: " + Math.round(percentValid*1000)/10.0,
                            "Documentation can be improved but no fix necessary"}));
        return report;
    }

    /**
     * function to check the transcription text based on valid chunks and glosses
     *
     * @param cd the corpus document used to correctly assign the log messages
     * @param text the extracted text from the transcription tiers
     * @param chunks the valid character sequences (graphemes/punctuations)
     * @param glosses the documented glosses
     * @return the check report
     */
    // private Report checkTranscriptionText(CorpusData cd, List<Text> text, Set<Character> chunks,
    private Report checkTranscriptionText(CorpusData cd, List<Text> text, List<String> chunks,
                                          Set<String> glosses) {
        DictionaryAutomaton dict = new DictionaryAutomaton(chunks);
        Report report = new Report();
        // All the characters that are valid
        int matched = 0;
        // All invalid characters in the text
        int missing = 0 ;
        // Indicator if a word contains missing characters
        boolean mismatch ;
        for (Text t : text) {
            // Tokenize text
            for (String token : t.getText().split(tokenSeparator)) {
                // Check if token either is a gloss or each character is in the valid characters
                mismatch = false ;
                // Update frequency list
                tokenFreq.compute(token,(k,v) -> (v == null) ? 1 : v + 1);
                if (!glosses.contains(token)) {
                    /*for (char c : token.toCharArray()) {
                        if (chunks.contains(c))
                            matched += 1;
                        else {
                            missing += 1;
                            mismatch = true ;
                        }
                    }*/
                    // Check if we can segment the token using the chunks
                    if (dict.checkSegmentableWord(token))
                        matched += token.length();
                    else {
                        missing += token.length();
                        mismatch = true ;
                    }
                }
                else {
                    // Add the length of the gloss to matched
                    matched += token.length() ;
                }
                if (mismatch) {
                    report.addWarning(getFunction(), ReportItem.newParamMap(new String[]{"function","filename","description"},
                            new Object[]{getFunction(), cd.getFilename(), "Transcription token contains invalid characters: " +
                                    token}));
                }
            }
        }
        float percentValid = (float)matched/(matched+missing) ;
        if (percentValid < (transcriptionCharactersValid / 100.0)) {
            report.addCritical(getFunction(), ReportItem.newParamMap(new String[]{"function","filename","description"
                            ,"howtoFix"},
                    new Object[]{getFunction(), cd.getFilename(),
                            "Corpus data: Less than " + transcriptionCharactersValid + " percent of transcription " +
                                    "characters are valid.\nValid: " + matched + " Invalid: " + missing + " " +
                                    "Percentage: " +
                                    Math.round(percentValid * 1000)/10.0,
                    "Add documentation for all graphemes and punctuation marks used in transcription"}));
        }
        else {
            report.addCorrect(getFunction(), ReportItem.newParamMap(new String[]{"function","filename","description",
                            "howtoFix"},
                    new Object[]{getFunction(), cd.getFilename(),
                            "Corpus data: More than " + transcriptionCharactersValid + " percent of transcription " +
                                    "characters are valid.\nValid: " + matched + " Invalid: " + missing + " " +
                                    "Percentage: " +
                    Math.round(percentValid * 1000)/10.0,"Documentation can be improved but no fix necessary"}));
        }
        return report ;
    }

    /**
     * Function to check if a language identifier is either a valid Glottolog Languoid or an ISO-639-3 code
     *
     * @param lang the language identifier
     * @return if the identifier is valid
     */
    public boolean checkLanguage(String lang) {
        // Check if the language is in the list of known translation languages
        if (translationLanguages.stream().map((tl) -> tl.contains(lang.toLowerCase())).reduce(Boolean::logicalOr)
                .orElse(false))
            return true ;
        // ISO code
        else if (lang.length() == 3) {
            // Just check the list
            return isoList.contains(lang);
        }
        // Glottolog
        else if (lang.matches("\\w{4}\\d{4}")) {
            return checkUrl("https://glottolog.org/resource/languoid/id/" + lang);
        }
        else
            return false ;
    }

    /**
     * Function to call a URL and check the return code
     * @param url the web resource to be accessed
     * @return if the request was successful
     */
    public static boolean checkUrl(String url) {
        try {
            URL u = new URL(url);
            // Sleep 500 milliseconds before making a request to avoid problems with too many calls
            TimeUnit.MILLISECONDS.sleep(500);
            HttpURLConnection con = (HttpURLConnection) u.openConnection();
            con.setRequestMethod("GET");
            return con.getResponseCode() == 200;
        }
        catch (IOException | InterruptedException e) {
            return false ;
        }
    }

    /**
     * Function that gets all text from tiers identified by type
     * @param d the XML DOM of the corpus document
     * @param tier the tier type
     * @return the list of all text elements from the selected tiers
     * @throws JDOMException on XPath problems
     */
    public List<Text> getTextsInTierByType(Document d, String tier) throws JDOMException {
        if (d == null)
            return new ArrayList<>();
        else {
            List texts = XPath.newInstance(
                    String.format("//TIER[@LINGUISTIC_TYPE_REF=\"%s\"]//ANNOTATION_VALUE/text()", tier))
                    .selectNodes(d);
            return listToParamList(Text.class, texts);
        }
    }

    /**
     * Function that gets all text from tiers identified by the tier ID
     * @param d the XML DOM of the corpus document
     * @param tier the tier ID
     * @return the list of all text elements from the selected tiers
     * @throws JDOMException on XPath problems
     */
    public List<Text> getTextsInTierByID(Document d, String tier) throws JDOMException {
        if (d == null)
            return new ArrayList<>();
        else {
            List texts = XPath.newInstance(
                    String.format("//TIER[@TIER_ID=\"%s\"]//ANNOTATION_VALUE/text()", tier))
                    .selectNodes(d);
            return listToParamList(Text.class, texts);
        }
    }

    /**
     * Function to count all words in certain tiers of the corpus stored in the Checker object. The tier is identified
     * by type
     *
     * @param tier the tier type
     * @return the number of words encountered in the selected tiers of all documents in the corpus
     * @throws JDOMException on XPath problems
     */
    private int countWordsInTierByType(String tier) throws JDOMException{
        int count = 0 ;
        for (CorpusData cd : refcoCorpus.getCorpusData()) {
            if (cd instanceof ELANData) {
                // Get all texts from given annotation tier
                List<Text> texts = getTextsInTierByType(((ELANData) cd).getJdom(), tier);
                for (Text t : texts) {
                    // Word separation simply on spaces
                    count += t.getText().split(tokenSeparator).length;
                }
            }
        }
        return count ;
    }

    /**
     * Function that counts all words from transcription tiers, i.e. tiers of type transcription, in the corpus stored
     * in the Checker object
     *
     * @return the number of transcribed words
     * @throws JDOMException on XPath problems
     */
    private int countTranscribedWords() throws JDOMException {
        return countWordsInTierByType("Transcription") ;
    }

    /**
     * Function that counts all words from annotation tiers, i.e. tiers of type morphologie, in the corpus stored in
     * the Checker object
     *
     * @return the number of transcribed words
     * @throws JDOMException on XPath problems
     */
    private int countAnnotatedWords() throws JDOMException {
        return countWordsInTierByType("Morpheme gloss") ;
    }

    /**
     * Function to safely convert an un-parametrized list into a parametrized one
     *
     * @param p the parameter class extending the parameter type
     * @param l the un-parametrized list
     * @param <T> the parameter class
     * @return the parametrized list
     */
    @SuppressWarnings("unchecked")
    public   <T> List<T> listToParamList(Class<? extends T> p, List l) {
        ArrayList<T> nl = new ArrayList<>(l.size()) ;
        for (Object o : l) {
            if (o.getClass() == p)
                try {
                    nl.add((T) o);
                }
                catch (ClassCastException e) {
                    logger.log(Level.SEVERE,"Encountered exception", e);
                }
        }
        return nl;
    }

    /**
     * Function to convert a string to an list of characters
     * @param s the string
     * @return the list of characters
     */
    public List<Character> getChars(String s) {
        return Chars.asList(s.toCharArray());
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String,String> params = new HashMap<>();
        params.put("refco-file","The corpus documentation file as a ODS of FODS spreadsheet");
        return params;
    }


    /**
     * Class representing a location in a corpus given by a tier id and an annotation id
     */
    private static class Location {
        String tier;
        String segment;

        public Location(String tier, String segment) {
            this.tier = tier;
            this.segment = segment;
        }
    }

    /**
     * Gives the location of a text token in a corpus document
     * @param cd the corpus document
     * @param token the token to be looked up
     * @throws JDOMException on problems with the xpath expressions
     * @return the location consisting of a tier and a segment
     */
    private Location getLocation(ELANData cd, String token) throws JDOMException {
        String tier_id = ((Attribute) XPath.newInstance("//TIER/@TIER_ID[contains(string(..),'" + token + "')]")
                .selectSingleNode(cd.getJdom()))
                .getValue();
        String annotation_segment = ((Attribute) XPath.newInstance("//*[contains(text(),'" + token + "')]/" +
                "../@ANNOTATION_ID").selectSingleNode(cd.getJdom()))
                .getValue();
        int start_time = Integer.parseInt(((Attribute) XPath.newInstance("//TIME_SLOT[@TIME_SLOT_ID=" +
                "//ALIGNABLE_ANNOTATION[@ANNOTATION_ID='" + annotation_segment + "']/@TIME_SLOT_REF1]/@TIME_VALUE")
                .selectSingleNode(cd.getJdom())).getValue());
        int end_time = Integer.parseInt(((Attribute) XPath.newInstance("//TIME_SLOT[@TIME_SLOT_ID=" +
                "//ALIGNABLE_ANNOTATION[@ANNOTATION_ID='" + annotation_segment + "']/@TIME_SLOT_REF2]/@TIME_VALUE")
                .selectSingleNode(cd.getJdom())).getValue());
        return new Location("[Tier:" + tier_id + "]","[Segment:" + annotation_segment + ", Time:" +
                DurationFormatUtils.formatDuration(start_time,"mm:ss.SSSS") + "-" +
                DurationFormatUtils.formatDuration(end_time,"mm:ss.SSSS") + "]");
    }
}
