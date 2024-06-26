package de.uni_hamburg.corpora.validation.quest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Sets;
import com.google.common.primitives.Chars;
import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.quest.*;
import de.uni_hamburg.corpora.validation.Checker;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.*;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathFactory;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;
import org.xml.sax.SAXException;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.SchemaOutputResolver;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.customProperties.HyperSchemaFactoryWrapper;

/**
 * The checker for Refco set of criteria.
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240405
 */
public class RefcoChecker extends Checker implements CorpusFunction {

    private final String REFCO_CHECKER_VERSION="20221114";

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
    private final Set<String> glossSeparator = new HashSet<>(); //new HashSet(Arrays.asList("-", "=")); // "[-=;:\\\\>()
    // <~\\[\\]]+"
    // Separator to split tier ids in tier names and speakers
    private String tierSpeakerSeparator = "@";

    // Placeholder used in the corpus documentation to denote a space character
    private final String spacePlaceholder = "[ ]";

    // The XML namespace for table elements in ODS files
    private final Namespace tableNamespace =
            Namespace.getNamespace("table","urn:oasis:names:tc:opendocument:xmlns:table:1.0") ;
    // The XML namespace for text elements in ODS files
    private final Namespace textNamespace =
            Namespace.getNamespace("text","urn:oasis:names:tc:opendocument:xmlns:text:1.0") ;

    // The hand-picked list of languages acceptable for translation tiers
    private final List<String> validTranslationLanguages = Arrays.asList("mandarin chinese", "english", "french", "german"
            , "indonesian", "portuguese", "russian", "spanish");

    // The hand-picked list of acceptable tier functions
    private final List<String> validTierFunctions = Arrays.asList("transcription", "reference", "note",
            "part-of-speech", "morpheme gloss", "morpheme glossing", "morpheme segmentation", "free translation");

    // The set of all undocumented languages we encountered. To skip duplicate warnings
    private final Set<String> knownLanguages = new HashSet<>();

    // Flag if locations should be skipped
    private boolean skipLocations = false;
    // Flag if we want segment and time in the location
    private final boolean detailedLocations = false;
    // Flag if we have a dictionary to check lexical glosses
    private boolean hasDict = false;
    // Automaton for lexeme checking in gloss tiers
    DictionaryAutomaton dict;

    /**
     * The filename of the RefCo spreadsheet
     */
    private String refcoFileName;
    private String refcoShortName;
    private boolean refcoFileLoaded = false;

    /**
     * The XML DOM of the RefCo spreadsheet
     */
    private Document refcoDoc ;
    private final XPathFactory xpathFactory = new JaxenXPathFactory();

    public RefcoCriteria getCriteria() {
        return criteria;
    }

    /**
     * The criteria extracted from the Refco spreadsheet
     */
    private final RefcoCriteria criteria = new RefcoCriteria() ;

    /**
     * The list of ISO-639-3 language codes
     */
    private final ArrayList<String> isoList = new ArrayList<>() ;

    /**
     * The corpus with all usable files
     */
    private Corpus refcoCorpus ;

    /**
     *  The frequency list of all transcription tokens in the corpus
     */
    private final FrequencyList tokenFreq = new FrequencyList();

    /**
     * The frequency list of all segmented annotation/morphology glosses in the corpus
     */
    private final FrequencyList morphemeFreq = new FrequencyList();

    /**
     * The frequency list of all segmented lexical glosses in the corpus
     */
    private final FrequencyList lexicalFreq = new FrequencyList();

    /**
     * The frequency list of all non-segmented annotation/morphology glosses in the corpus
     */
    private final FrequencyList glossFreq = new FrequencyList();

    /**
     * The frequency list of all non-segmentable gloss tokens
     */
    private final FrequencyList missingGlossFreq = new FrequencyList();

    /**
     * The frequency list of all non-matchable lexeme tokens
     */
    private final FrequencyList missingLexicalFreq = new FrequencyList();

    /**
     * Collection of approximately matched lexemes to be disambiguated
     */
    Map<String,List<String>> approximateMatches = new HashMap<>();

    /**
     * The global report, will be filled by the constructor and the function applied to the complete corpus
     */
    private final Report report = new Report();

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
        if (properties.containsKey("get-schema")) {
            try {
                System.out.println(deriveXMLSpecification());
                System.out.println();
                System.out.println(deriveJSONSpecification());
                System.exit(0);
            } catch (Exception e) {
                report.addCritical(getFunction(),e, "Exception when deriving schemas");
            }
        }
        if (properties.containsKey("skip-locations") && properties.getProperty("skip-locations")
                .equalsIgnoreCase("true")) {
            skipLocations = true;
        }
        if (properties.containsKey("detailed-locations") && properties.getProperty("detailed-locations")
                .equalsIgnoreCase("true")) {
            skipLocations = true;
        }
        if (properties.containsKey("dict")) {
            logger.info("Load dict " + properties.getProperty("dict"));
            hasDict = true;
            try {
                URL resourceUrl = this.getClass().getClassLoader().getResource(properties.getProperty("dict"));
                if (resourceUrl != null) {
                    dict = new DictionaryAutomaton(resourceUrl.openStream());
                } else {
                    dict = new DictionaryAutomaton(new File(properties.getProperty("dict")));
                }
            } catch (IOException e) {
                logger.info("Error loading dictionary " + properties.getProperty("dict"));
            }

        }
    }

    /**
     * Returns the global report used e.g. for report items in the constructor
     * @return the global report
     */
    public Report getReport() { return report; }

    /**
     * Gives the description of what the checker does
     * @return the description of the checker
     */
    @Override
    public String getDescription() {
        return "Checks the RefCo criteria for a corpus. Requires a RefCo corpus documentation spreadsheet.";
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
        if (refcoFileLoaded)
            report.merge(refcoCorpusCheck(cd));
        return report;
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
        if (props.containsKey("refco-file")) {
            String path = "";
            if (!props.containsKey("refco-config-path-absolute") || !props.getProperty("refco-config-path-absolute")
                    .equalsIgnoreCase("true"))
                path = c.getBaseDirectory().getPath();
            // Load config and add potential problems to log
            if (new File(path + props.getProperty("refco-file")).exists())
                report.merge(setRefcoFile(path + props.getProperty("refco-file")));
            else {
                report.addCritical(getFunction(), "Missing corpus documentation file " +
                        path + props.getProperty("refco-file"));
            }
        }
        else {
            report.addCritical(getFunction(),"Missing corpus documentation file property");
        }
        if (refcoFileLoaded) {
            report.addNote(getFunction(),"Report created by RefCo checker version " + REFCO_CHECKER_VERSION +
                    " based on documentation following RefCo " + criteria.getRefcoVersion().getInformation() +
                    " specification version");
            System.out.println("... running the corpus function");
            // Set the RefCo corpus
            setRefcoCorpus(c);
            // Run the generic tests and merge their reports into the current report
            // but flag allows skipping it
            if (!props.containsKey("skip-documentation-check")
                    || !props.getProperty("skip-documentation-check").equalsIgnoreCase("true"))
                report.merge(refcoDocumentationCheck());
            // Apply function for each of the supported file. Again merge the reports
            for (CorpusData cdata : c.getCorpusData()) {
                function(cdata, fix);
            }
            // Check for morpheme glosses that never occurred in the complete corpus
            for (Map.Entry<String, Integer> e : morphemeFreq.getMap().entrySet()) {
                if (e.getValue() == 0)
                    report.addWarning(getFunction(), ReportItem.newParamMap(
                            new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                            new Object[]{getFunction(), refcoShortName,
                                    "Corpus data: Morpheme gloss never encountered in corpus: " + e.getKey(),
                                    "Check for potential errors or remove gloss from documentation"}));
            }
            if (!missingGlossFreq.isEmpty())
                report.addNote(getFunction(),"Corpus data: Morpheme glosses missing from documentations:\n" +
                        missingGlossFreq);
            if (!morphemeFreq.isEmpty() && !lexicalFreq.isEmpty() && props.containsKey("gloss-stats") &&
                    props.getProperty("gloss-stats").equalsIgnoreCase("true")) {
                report.addNote(getFunction(), "Corpus data: Morphological glosses encountered in the corpus:\n" +
                        morphemeFreq);
                report.addNote(getFunction(), "Corpus data: Lexical glosses encountered in the corpus:\n" +
                        lexicalFreq);
            }
        }
        // In any case, just return the report
        logger.info("Corpus checks done");
        return report ;
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
            ObjectMapper mapper = new ObjectMapper();
            // Allows serialization even when getters are missing
            mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            mapper.configure(SerializationFeature.INDENT_OUTPUT,true);
            try {
                Report report = new Report();
                // Read the corpus
                Logger.getLogger(RefcoChecker.class.toString()).log(Level.INFO, "Reading corpus");
                CorpusIO cio = new CorpusIO();
                // Handle relative paths
                URL url = Paths.get(args[1]).toAbsolutePath().normalize().toUri().toURL();
                // Create the corpus
                Corpus corpus = new Corpus(rc.criteria.getCorpusTitle(), url, cio.read(url, report));
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
                //ObjectMapper mapper = new ObjectMapper();
                // Allows serialization even when getters are missing
                mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
                mapper.configure(SerializationFeature.INDENT_OUTPUT,true);
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
                try (ZipFile f = new ZipFile(new File(refcoFileName))) {
                    ZipEntry e = f.getEntry("content.xml");
                    if (e == null) {
                        report.addCritical(getFunction(), ReportItem.newParamMap(
                                new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                                new Object[]{getFunction(), refcoShortName, "General: ODS file invalid",
                                        //"does not contain content.xml",
                                        "Check spreadsheet file and only use proper ODS files"}));
                    } else {
                        SAXBuilder builder = new SAXBuilder();
                        refcoDoc = builder.build(f.getInputStream(e));
                        // Or we could use the proper API instead but that requires additional dependencies
                        // refcoDoc = builder.build(new StringReader(OdfSpreadsheetDocument.loadDocument(refcoFileName).getContentDom().toString()));
                    }
                }
            }
            else {
                 report.addCritical(getFunction(),ReportItem.newParamMap(
                            new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                            new Object[]{getFunction(),refcoShortName,
                                    "General: Spreadsheet is neither an ODS nor FODS file",
                                    "Only use proper (F)ODS files"})) ;
            }
            // expand compressed cells
            expandTableCells(refcoDoc);
            removeEmptyCells(refcoDoc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Pattern filenamePattern = Pattern.compile("CorpusDocumentation_([\\w\\d]{8})_(\\w+)_(\\w+).f?ods");
        Matcher filenameMatcher = filenamePattern.matcher(refcoShortName);
        if (!filenameMatcher.matches()) {
            report.addWarning(getFunction(), ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(), refcoShortName, "General: Filename does not match schema " +
                            "CorpusDocumentation_<Glottocode>_<Creator-Name>_<Corpus-Name>.ods/.fods: " + refcoShortName,
                            "Rename documentation file"}));
        }
        else {
            // Check glottocode in file name is valid
            String langCode = filenameMatcher.group(1);
            if (!checkLanguage(langCode)) {
                report.addWarning(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),refcoShortName,
                                "General: Language given in filename not valid Glottocode: "+ langCode,
                                "Check that language is valid Glottocode"}));
            }
        }
        // Mark refco file as loaded
        refcoFileLoaded = true;
        // Extract the criteria from the XML
        report.merge(readRefcoCriteria(refcoDoc));
        return report;
    }

    /** Create a new corpus from only these files, keeping the original corpus name and base directory
     * This corpus is among others used to check the existence of referenced files
     */
    private void setRefcoCorpus(Corpus c) throws MalformedURLException, JexmaraldaException, SAXException {
        // Get all usable formats for the checker
        Collection<Class<? extends CorpusData>> usableFormats = this.getIsUsableFor();
        // Get all usable files from the corpus, i.e. the ones whose format is included in usableFormats
        Collection<CorpusData> usableFiles = c.getCorpusData().stream().filter((cd) -> usableFormats.contains(cd.getClass())).collect(Collectors.toList());
        refcoCorpus = new Corpus(c.getCorpusName(), c.getBaseDirectory(), usableFiles);
    }
    
    /**
     * Functions that expands compressed cells by replacing them by several blank cells. ODS files use some kind of
     * run-length compression for multiple blank cells in a row which is problematic for the parser.
     *
     * @param document the document in which the cells should be expanded
     */
    private void expandTableCells(Document document) {
        // Find all cells that have the attribute number-columns-repeated
        for (Element node :
                new XPathBuilder<>("//table:table-cell[@table:number-columns-repeated]", Filters.element()).compileWith(xpathFactory).evaluate(document)) {
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
                Element e = node.clone();
                e.removeAttribute("number-columns-repeated", tableNamespace);
                e.setAttribute("ignore","true");
                replacement.add(e);
            }
            // Replace the original cell by the sequence of new ones
            node.getParentElement().setContent(node.getParentElement().indexOf(node),replacement);
        }
        // Expand rows as well
        for (Element node :
                new XPathBuilder<>("//table:table-row[@table:number-rows-repeated]", Filters.element()).compileWith(xpathFactory).evaluate(document)) {
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
                Element e = node.clone();
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
     */
    private void removeEmptyCells(Document document) {
        boolean deleted = true;
        while (deleted) {
            deleted = false;
            for (Element node :
                    new XPathBuilder<>("//table:table-cell[not(text:p) and " +
                            "position() = last()]", Filters.element()).compileWith(xpathFactory).evaluate(document)) {
                node.detach();
                deleted = true;
            }
            for (Element node :
                    new XPathBuilder<>("//table:table-row[not" +
                            "(table:table-cell) and position() = last()]", Filters.element()).compileWith(xpathFactory).evaluate(document)) {
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
            //return element.getText();
            return XMLTools.showAllText(element).trim();
    }

    /**
     * Function that gets the text from a certain cell in a row given its index
     *
     * @param path the xpath expression to find the cell
     * @param e the root element
     * @param title the text contained in the first cell
     * @param pos the cell in the row
     * @return either the text contained or an empty string
     */
    private String getTextInRow(String path, Element e, String title, int pos) {
        if (path == null || e == null)
            return "" ;
        else {
            Element cell =
                    new XPathBuilder<>(String.format(path, title, pos), Filters.element()).compileWith(xpathFactory).evaluateFirst(e);
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
    private RefcoCriteria.InformationNotes getInformationNotes(String path, Element root, String title) throws JDOMException {
        return new RefcoCriteria.InformationNotes(getTextInRow(path, root, title, 2),getTextInRow(path, root, title, 3));
    }

    /**
     * Method that reads the XML data from the spreadsheet into a java data structure. Expects a clean table, i.e.
     * expanded and removed empty cells
     * @param refcoDoc the spreadsheet document
     * @return a report containing potential problems when reading the documentation file
     */
    private Report readRefcoCriteria(Document refcoDoc) {
        Report report = new Report();
        try {

            // Read Overview tab
            Element overviewTable =
                    new XPathBuilder<>("//table:table[@table:name='Overview']", Filters.element()).compileWith(xpathFactory).evaluateFirst(refcoDoc);
            String cellXPath =
                    "//table:table-row[table:table-cell[text:p=\"%s\"]]/table:table-cell[position()=%d]/text:p";
            criteria.setCorpusTitle(getCellText(cellXPath, overviewTable,  "Corpus Title"));
            criteria.setSubjectLanguages(getCellText(cellXPath, overviewTable, "Subject Language(s)"));
            criteria.setArchive(getCellText(cellXPath, overviewTable, "Archive"));
            criteria.setPersistentId(getCellText(cellXPath, overviewTable, "Corpus Persistent Identifier"));
            criteria.setAnnotationLicense(getCellText(cellXPath, overviewTable, "Annotation Files Licence"));
            criteria.setRecordingLicense(getCellText(cellXPath, overviewTable, "Recording Files Licence"));
            criteria.setCreatorName(getCellText(cellXPath, overviewTable, "Corpus Creator Name"));
            criteria.setCreatorContact(getCellText(cellXPath, overviewTable, "Corpus Creator Contact"));
            criteria.setCreatorInstitution(getCellText(cellXPath, overviewTable, "Corpus Creator Institution"));
            criteria.setRefcoVersion(getInformationNotes(cellXPath, overviewTable, "Corpus Documentation's Version"));
            criteria.setNumberSessions(getInformationNotes(cellXPath, overviewTable, "Number of sessions"));
            criteria.setNumberTranscribedWords(getInformationNotes(cellXPath, overviewTable, "Total number of transcribed words"));
            criteria.setNumberAnnotatedWords(getInformationNotes(cellXPath, overviewTable, "Total number of morphologically analyzed words"));
            // Read CorpusComposition tab
            Element sessionTable =
                    new XPathBuilder<>("//table:table[@table:name='CorpusComposition']", Filters.element()).compileWith(xpathFactory).evaluateFirst(refcoDoc);
            if (sessionTable == null)
                report.addCritical(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),refcoShortName,"Corpus documentation: CorpusComposition table not found",
                                "Add table CorpusComposition to corpus documentation"}));
            else {
                boolean missingData = false;
                List<Element> rowList = sessionTable.getChildren("table-row", tableNamespace);
                for (Element row : rowList) {
                    List<Element> columns = row.getChildren("table-cell", tableNamespace);
                    if (columns.size() > 7 && !safeGetText(columns.get(0).getChild("p", textNamespace)).isEmpty()
                            && !safeGetText(columns.get(0).getChild("p", textNamespace)).startsWith("Session")) {
                        RefcoCriteria.Session session = new RefcoCriteria.Session();
                        session.setSessionName(safeGetText(columns.get(0).getChild("p", textNamespace)));
                        session.setFileNames(safeGetText(columns.get(1).getChild("p", textNamespace)));
                        session.setSpeakerNames(safeGetText(columns.get(2).getChild("p", textNamespace)));
                        session.setSpeakerAges(safeGetText(columns.get(3).getChild("p", textNamespace)));
                        session.setSpeakerGender(safeGetText(columns.get(4).getChild("p", textNamespace)));
                        session.setRecordingLocation(safeGetText(columns.get(5).getChild("p", textNamespace)));
                        session.setRecordingDate(safeGetText(columns.get(6).getChild("p", textNamespace)));
                        session.setGenre(safeGetText(columns.get(7).getChild("p", textNamespace)));
                        // Age group was a custom column
                        // session.ageGroup = safeGetText(columns.get(8).getChild("p", textNamespace));
                        criteria.sessions.add(session);
                    } else if (!columns.isEmpty() && !safeGetText(columns.get(0).getChild("p", textNamespace)).startsWith(
                            "Session")) {
                        missingData = true;
                    }
                }
                if (missingData || rowList.size() <= 1) {
                    report.addCritical(getFunction(),ReportItem.newParamMap(
                            new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                            new Object[]{getFunction(), refcoShortName,"Corpus documentation: Wrong number of columns or " +
                                    "missing data in CorpusComposition table",
                                    "Check number of columns and presence of data in all cells"}));
                }
            }
            // Read AnnotationTiers tab
            Element tierTable =
                    new XPathBuilder<>("//table:table[@table:name='AnnotationTiers']", Filters.element()).compileWith(xpathFactory).evaluateFirst(refcoDoc);
            if (tierTable == null)
                report.addCritical(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), refcoShortName,
                                "Corpus documentation: AnnotationTiers table not found",
                                "Add table AnnotationTiers to corpus documentation"}));
            else {
                boolean missingData = false;
                List<Element> rowList = tierTable.getChildren("table-row", tableNamespace);
                for (Element row : rowList) {
                    List<Element> columns = row.getChildren("table-cell", tableNamespace);
                    if (columns.size() > 3 && !safeGetText(columns.get(0).getChild("p", textNamespace)).isEmpty()
                            && !safeGetText(columns.get(0).getChild("p", textNamespace)).equals("Names")) {
                        RefcoCriteria.Tier tier = new RefcoCriteria.Tier();
                        tier.setTierName(safeGetText(columns.get(0).getChild("p", textNamespace)).trim());
                        tier.setTierFunctions(Arrays.stream(safeGetText(columns.get(1).getChild("p", textNamespace))
                                .split(valueSeparator)).map(String::toLowerCase).collect(Collectors.toList()));
                        tier.setSegmentationStrategy(safeGetText(columns.get(2).getChild("p", textNamespace)));
                        tier.setLanguages(safeGetText(columns.get(3).getChild("p", textNamespace)));
                        criteria.getTiers().add(tier);
                    }  else if (!columns.isEmpty() && !safeGetText(columns.get(0).getChild("p", textNamespace)).startsWith(
                            "Name")) {
                        missingData = true;
                    }
                }
                if (missingData || rowList.size() <= 1)
                    report.addCritical(getFunction(),ReportItem.newParamMap(
                            new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                            new Object[]{getFunction(), refcoShortName,"Corpus documentation: Wrong number of columns or " +
                                    "missing data in AnnotationTiers table", "Check number of columns and presence of" +
                                    " data in all cells"}));
            }
            // Read Transcription tab
            Element transcriptionTable =
                    new XPathBuilder<>("//table:table[@table:name='Transcription']", Filters.element()).compileWith(xpathFactory).evaluateFirst(refcoDoc);
            if (transcriptionTable == null)
                report.addCritical(getFunction(),
                        ReportItem.newParamMap(
                                new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                                new Object[]{getFunction(),refcoShortName,"Corpus documentation: Transcription table not found",
                                        "Add table Transcription to corpus documentation"}));
            else {
                boolean missingData = false;
                List<Element> rowList = transcriptionTable.getChildren("table-row", tableNamespace);
                for (Element row : rowList) {
                    List<Element> columns = row.getChildren("table-cell", tableNamespace);
                    if (columns.size() > 2 && !safeGetText(columns.get(0).getChild("p", textNamespace)).isEmpty()
                            && !safeGetText(columns.get(0).getChild("p", textNamespace)).equals("Graphemes")) {
                        RefcoCriteria.Transcription transcription = new RefcoCriteria.Transcription();
                        transcription.setGrapheme(safeGetText(columns.get(0).getChild("p", textNamespace)));
                        transcription.setLinguisticValue(safeGetText(columns.get(1).getChild("p", textNamespace)));
                        transcription.setLinguisticConvention(safeGetText(columns.get(2).getChild("p", textNamespace)));
                        criteria.getTranscriptions().add(transcription);
                    } else if (!columns.isEmpty() && !safeGetText(columns.get(0).getChild("p", textNamespace)).startsWith(
                            "Grapheme")) {
                        missingData = true;
                    }
                }
                if (missingData || rowList.size() <= 1)
                    report.addCritical(getFunction(),ReportItem.newParamMap(
                            new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                            new Object[]{getFunction(), refcoShortName,"Corpus documentation: Wrong number of columns or " +
                                    "missing data in Transcription table",
                                    "Check number of columns and presence of data in all cells"}));
            }
            // Read Glosses tab
            Element glossesTable =
                    new XPathBuilder<>("//table:table[starts-with(@table:name,'Gloss')]", Filters.element()).compileWith(xpathFactory).evaluateFirst(refcoDoc);
            if (glossesTable == null)
                report.addCritical(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),refcoShortName,"Glosses table not found",
                                "Add table Glosses to corpus documentation"}));
            else {
                boolean missingData = false;
                List<Element> rowList = glossesTable.getChildren("table-row", tableNamespace);
                for (Element row : rowList) {
                    List<Element> columns = row.getChildren("table-cell", tableNamespace);
                    if (columns.size() > 3 && !safeGetText(columns.get(0).getChild("p", textNamespace)).isEmpty()
                            && !safeGetText(columns.get(0).getChild("p", textNamespace)).equals("Abbreviations")) {
                        RefcoCriteria.Gloss gloss = new RefcoCriteria.Gloss();
                        gloss.setGloss(safeGetText(columns.get(0).getChild("p", textNamespace)).replace("\\s+", ""));
                        // Keep track of the documented glosses
                        morphemeFreq.put(gloss.getGloss());
                        gloss.setMeaning(safeGetText(columns.get(1).getChild("p", textNamespace)));
                        gloss.setComments(safeGetText(columns.get(2).getChild("p", textNamespace)));
                        gloss.setTiers(safeGetText(columns.get(3).getChild("p", textNamespace)));
                        criteria.getGlosses().add(gloss);
                    } else if (!columns.isEmpty() && !safeGetText(columns.get(0).getChild("p", textNamespace)).startsWith(
                            "Abbreviation")) {
                        missingData = true;
                    }
                }
                if (missingData || rowList.size() <= 1)
                    report.addCritical(getFunction(),ReportItem.newParamMap(
                            new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                            new Object[]{getFunction(),refcoShortName,"Corpus documentation: Wrong number of columns or " +
                                    "missing data in Glosses table",
                                    "Check number of columns and presence of data in all cells"}));
            }
            // Read Punctuation tab
            Element punctuationsTable =
                    new XPathBuilder<>("//table:table[@table:name='Punctuations']", Filters.element()).compileWith(xpathFactory).evaluateFirst(refcoDoc);
            if (punctuationsTable == null)
                report.addCritical(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),refcoShortName,"Punctuation table not found",
                                "Add table Punctuation to corpus documentation"}));
            else {
                boolean missingData = false;
                List<Element> rowList = punctuationsTable.getChildren("table-row", tableNamespace);
                for (Element row : rowList) {
                    List<Element> columns = row.getChildren("table-cell", tableNamespace);
                    if (columns.size() > 4 && !safeGetText(columns.get(0).getChild("p", textNamespace)).isEmpty()
                            && !safeGetText(columns.get(0).getChild("p", textNamespace)).startsWith("Character")) {
                        RefcoCriteria.Punctuation punctuation = new RefcoCriteria.Punctuation();
                        punctuation.setCharacter(safeGetText(columns.get(0).getChild("p", textNamespace)));
                        if (punctuation.getCharacter().equals(spacePlaceholder))
                            punctuation.setCharacter(" ");
                        punctuation.setMeaning(safeGetText(columns.get(1).getChild("p", textNamespace)));
                        punctuation.setComments(safeGetText(columns.get(2).getChild("p", textNamespace)));
                        punctuation.setTiers(safeGetText(columns.get(3).getChild("p", textNamespace)));
                        punctuation.setFunction(safeGetText(columns.get(4).getChild("p", textNamespace)));
                        // Add gloss separator
                        if (punctuation.getFunction().equalsIgnoreCase("morpheme break"))
                            glossSeparator.add(punctuation.getCharacter());
                        criteria.getPunctuations().add(punctuation);
                    }
                    else if (!columns.isEmpty() && !safeGetText(columns.get(0).getChild("p", textNamespace)).startsWith(
                            "Character")) {
                        missingData = true;
                    }
                }
                 if (missingData || rowList.size() <= 1) {
                     report.addCritical(getFunction(), ReportItem.newParamMap(
                             new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                             new Object[]{getFunction(), refcoShortName, "Corpus documentation: Wrong number of columns or " +
                                     "missing data in Punctuation table",
                                     "Check number of columns and presence of data in all cells"}));
                 }
            }
        } catch (JDOMException | NullPointerException exception) {
            report.addCritical(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.Exception},
                    new Object[]{getFunction(),refcoShortName,"Corpus documentation: Unexpected exception",exception}));
        }
        return report;
    }

    /**
     * Function that performs generic checks on the RefCo documentation stored in the Checker object (using
     * setRefcoFile)
     *
     * @return the detailed report of all checks performed
     */
    private Report refcoGenericCheck() {
        Report report = new Report() ;
        if (criteria.getCorpusTitle() == null || criteria.getCorpusTitle().isEmpty())
            report.addCritical(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),refcoShortName,"Overview: Corpus title is empty", "Add a corpus title"}));
        if (criteria.getSubjectLanguages() == null || criteria.getSubjectLanguages().isEmpty())
            report.addCritical(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(),refcoShortName,"Overview: Subject languages is empty",
                        "Add a subject language"}));
        else {
            // Each cell can contain several languages. Split the languages and check for each one if it is a
            // valid language code
            // TODO check if that is the intended way to separate languages
            ArrayList<String> subjectLangList = new ArrayList<>(Arrays.asList(criteria.getSubjectLanguages()
                    .split(valueSeparator)));
            for (String l : subjectLangList) {
                if (!checkLanguage(l)) {
                    report.addWarning(getFunction(),ReportItem.newParamMap(
                            new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                            new Object[]{getFunction(),refcoShortName,"Overview: Language is neither a Glottolog, a ISO-639-3 " +
                                    "language code nor otherwise known: " + l, "Use a valid language code"}));
                }
            }
        }
        if (criteria.getArchive() == null || criteria.getArchive().isEmpty())
            report.addCritical(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(),refcoShortName,"Overview: Archive name is empty", "Add an archive name"}));
        if (criteria.getPersistentId() == null || criteria.getPersistentId().isEmpty())
            report.addCritical(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(),refcoShortName,"Overview: Persistent identifier is empty",
                            "Add a persistent identifier"}));
        else if (!checkUrl(criteria.getPersistentId())){
            report.addCritical(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(),refcoShortName,"Overview: Persistent identifier not a valid or working" +
                            " URL",
                            "Use a valid URL as the persistent identifier and check that it works properly, i.e. " +
                                    "refers to an accessible resource"}));
        }
        if (criteria.getAnnotationLicense() == null || criteria.getAnnotationLicense().isEmpty())
            report.addCritical(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(),refcoShortName,"Overview: Annotation license is empty",
                            "Add annotation license"}));
        if (criteria.getRecordingLicense() == null || criteria.getRecordingLicense().isEmpty())
            report.addWarning(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(),refcoShortName,"Overview: Recording license is empty",
                            "Add recording license"}));
        if (criteria.getCreatorName() == null || criteria.getCreatorName().isEmpty())
            report.addCritical(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(),refcoShortName,"Overview: Creator name is empty","Add creator name"}));
        if (criteria.getCreatorContact() == null || criteria.getCreatorContact().isEmpty())
            report.addCritical(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(),refcoShortName,"Overview: Creator contact is empty","Add Creator contact"}));
        if (criteria.getCreatorInstitution() == null || criteria.getCreatorInstitution().isEmpty())
            report.addWarning(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(),refcoShortName,"Overview: Creator institution is empty",
                            "Add creator institution"}));
        if (criteria.getRefcoVersion().getInformation() == null || criteria.getRefcoVersion().getInformation().isEmpty())
            report.addCritical(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(),refcoShortName,"Overview: RefCo version is empty","Add RefCo version"}));
        else {
            try {
                // Check the number by trying to parse it
                Integer.parseInt(criteria.getRefcoVersion().getInformation());
                // TODO do something with this number
            }
            catch (NumberFormatException e) {
                report.addCritical(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),refcoShortName,"Overview: Refco version is not a number",
                                "Check the RefCo version number"}));
            }
        }
        if (criteria.getNumberSessions().getInformation() == null || criteria.getNumberSessions().getInformation().isEmpty())
            report.addWarning(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(),refcoShortName,"Overview: Number of sessions is empty",
                            "Add number of sessions"}));
        else {
            try {
                // Check the number by trying to parse it
                int i = Integer.parseInt(criteria.getNumberSessions().getInformation());
                // Compare it to the number of rows in the CorpusComposition table
                if (i != criteria.getSessions().size())
                    report.addWarning(getFunction(),ReportItem.newParamMap(
                            new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                            new Object[]{getFunction(),refcoShortName,"Corpus composition: Number of sessions does not " +
                                    "match number of sessions, expected " + i + " and found: " + criteria.sessions.size(),
                            "Compare the number given in Overview to the number of sessions in Corpus composition"}));
            }
            catch (NumberFormatException e) {
                report.addWarning(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),refcoShortName,"Overview: Number of sessions is not a number",
                                "Check the number of sessions"}));
            }
        }
        if (criteria.getNumberTranscribedWords().getInformation() == null || criteria.getNumberTranscribedWords().getInformation().isEmpty())
            report.addWarning(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(),refcoShortName,"Overview: Number of transcribed words is empty",
                            "Add number of transcribed words count"}));
        else {
            try {
                // Check the number by trying to parse it
                int i = Integer.parseInt(criteria.getNumberTranscribedWords().getInformation());
                // Compare it to our own count
                int c = countTranscribedWords();
                if (i==0 || c == 0 || 0.8 > (float)c/i || (float)c/i > 1.2)
                    report.addWarning(getFunction(),ReportItem.newParamMap(
                            new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                            new Object[]{getFunction(),refcoShortName,"Overview: Transcription word count is either 0" +
                                    " or more than 20 percent off. Counted " + c + " expected " + i,
                                    "Correct the word count"}));
            }
            catch (JDOMException e) {
                report.addCritical(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),refcoShortName,"Overview: Exception encountered when counting " +
                                "transcribed words"}));
            }
            catch (NumberFormatException e) {
                report.addWarning(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),refcoShortName,"Overview: Number of transcribed words is not a number",
                                "Check the word count and make sure it is given as a proper number"
                        }));
            }
        }
        if (criteria.getNumberAnnotatedWords().getInformation() == null || criteria.getNumberAnnotatedWords().getInformation().isEmpty())
            report.addWarning(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(),refcoShortName,"Overview: Number of annotated words is empty",
                        "Add the number of annotated words"}));
        else {
            try {
                // Check the number by trying to parse it
                int documentedWords = Integer.parseInt(criteria.getNumberAnnotatedWords().getInformation());
                try {
                    int countedWords = countAnnotatedWords();
                    if (documentedWords==0 || countedWords == 0 || 0.8 > (float)countedWords/documentedWords ||
                            (float)countedWords/documentedWords > 1.2)
                        report.addWarning(getFunction(),ReportItem.newParamMap(
                                new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                                new Object[]{getFunction(),refcoShortName,"Overview: Annotation word count is either " +
                                        "0 or more than 20 percent off. Counted " + countedWords + " expected " +
                                        documentedWords, "Correct the word count"}));
                }
                catch (JDOMException e) {
                    report.addCritical(getFunction(),ReportItem.newParamMap(
                            new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.Exception},
                        new Object[]{getFunction(),refcoShortName,"Exception when counting annotated words",e}));
                }
            }
            catch (NumberFormatException e) {
                report.addWarning(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),refcoShortName,"Number of annotated words is not a number",
                                "Check and fix the number of annotated words"}));
            }
        }
        return report;
    }

    /**
     * Function that performs session checks on the RefCo documentation stored in the Checker object (using
     * setRefcoFile)
     *
     * @return the detailed report of all checks performed
     */
    private Report refcoSessionCheck() {
        Report report = new Report();
        // First get all files included in the corpus
        HashSet<URI> allFiles = new HashSet<>();
        try {
            allFiles.addAll(FileTools.listFiles(Paths.get(refcoCorpus.getBaseDirectory().toURI())));
            allFiles.addAll(FileTools.listFiles(Paths.get(new URL(refcoCorpus.getBaseDirectory() + "/../Annotations/").toURI())));
            allFiles.addAll(FileTools.listFiles(Paths.get(new URL(refcoCorpus.getBaseDirectory() + "/../annotations/").toURI())));
            allFiles.addAll(FileTools.listFiles(Paths.get(new URL(refcoCorpus.getBaseDirectory() + "/../Recordings/").toURI())));
            allFiles.addAll(FileTools.listFiles(Paths.get(new URL(refcoCorpus.getBaseDirectory() + "/../recordings/").toURI())));
        }
        catch (Exception e) {
            report.addCritical(getFunction(), ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.Exception},
                    new Object[]{getFunction(), refcoShortName, "Corpus composition: exception when listing all " +
                            "files",
                            e}));
        }
        // Remove curation files and the documentation itself
        allFiles = (HashSet<URI>) allFiles.stream().filter((f) ->
                        !f.toString().contains("/curation/") && !f.toString().contains("CorpusDocumentation") &&
                                !new File(f).isDirectory()
        ).collect(Collectors.toSet());
        Set<URI> documentedFiles = new HashSet<>();
        // Check each of the sessions
        for (RefcoCriteria.Session s : criteria.sessions) {
            if (s.getSessionName() == null || s.getSessionName().isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),refcoShortName,"Session name is empty","Add session name"}));
            if (s.getFileNames() == null || s.getFileNames().isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),refcoShortName,"Session file names are empty: " + s.getSessionName(),
                                "Add all relevant file names for session"}));
            else {
                // Each cell can contain several files. Split the files and check for each one if it exists
                ArrayList<String> filenames = new ArrayList<>(
                    Arrays.asList(s.getFileNames().split(valueSeparator)));
                for (String f : filenames) {
                    try {
                        // Collect all candidate URIs, even for files in other directories
                        List<URI> uris = getUris(f);
                        // Copy all file URIs of files in respective folders
                        documentedFiles.addAll(uris);
                    }
                    catch (MalformedURLException | URISyntaxException e) {
                        report.addCritical(getFunction(),ReportItem.newParamMap(
                                new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.Exception},
                                new Object[]{getFunction(),refcoShortName,"Exception encountered checking file name: "+ f,e}));
                    }
                }
            }
            if (s.getSpeakerNames() == null || s.getSpeakerNames().isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),refcoShortName,"Corpus composition: Speaker name is empty",
                                "Add speaker name"}));
            if (s.getSpeakerAges() == null || s.getSpeakerAges().isEmpty())
                report.addWarning(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),refcoShortName,
                                "Corpus composition: Speaker age is empty: " + s.getSpeakerNames(),"Add speaker age"}));
            else if (!Arrays.stream(s.getSpeakerAges().split(valueSeparator)).allMatch(a -> a.equals("na") || a.matches("~?\\d{1,3}"))) {
                report.addWarning(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), refcoShortName, "Corpus composition: Speaker age does not match " +
                                "schema: " + s.getSpeakerAges(), "Check and fix speaker age"}));
            }
            if (s.getSpeakerGender() == null || s.getSpeakerGender().isEmpty())
                report.addWarning(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),refcoShortName,
                                "Corpus composition: Speaker gender is empty: " + s.getSpeakerNames(),"Add speaker gender"}));
            if (s.getRecordingLocation() == null || s.getRecordingLocation().isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),refcoShortName,
                                "Corpus composition: Recording location is empty: " + s.getSpeakerNames(),
                                "Add recording location"}));
            if (s.getRecordingDate() == null || s.getRecordingDate().isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),refcoShortName,
                                "Corpus composition: Recording date is empty: " + s.getSpeakerNames(),
                                "Add recording date"}));
            else {
                // Check date by trying to parse it
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    Date date = df.parse(s.getRecordingDate());
                    // Java is very lax when parsing dates, so we have to compare the parsed date to the original one
                    if (!df.format(date).equals(s.getRecordingDate()))
                        throw new IllegalArgumentException("Parsed date does not match given date");
                } catch (ParseException | IllegalArgumentException e) {
                    report.addWarning(getFunction(),ReportItem.newParamMap(
                            new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                            new Object[]{getFunction(),refcoShortName,"Corpus composition: Recording date in invalid format. Expected yyyy-mm-dd, got: "
                                    + s.getRecordingDate(), "Check and fix recording date"}));
                }
            }
            if (s.getGenre() == null || s.getGenre().isEmpty())
                report.addWarning(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),refcoShortName,"Corpus composition: Genre is empty", "Add genre"}));
        }
        // Check the documented files against the files found
        FileListChecker fileListChecker = new FileListChecker(documentedFiles,allFiles,new Properties());
        try {
            report.merge(fileListChecker.function((Corpus) null, false));
        }
        catch (JexmaraldaException | XPathExpressionException | SAXException | TransformerException | FSMException | JDOMException | ClassNotFoundException | ParserConfigurationException | IOException | URISyntaxException | NoSuchAlgorithmException e) {
            report.addCritical(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.Exception},
                    new Object[]{getFunction(),refcoShortName,
                            "Exception encountered when checking file documentation",
                            e}));
        }
        return report;
    }

    private List<URI> getUris(String f) throws URISyntaxException, MalformedURLException {
        List<URI> uris = new ArrayList<>();
        // Corpus files are in the annotation folders
        if (f.toLowerCase().endsWith("eaf")) {
            if (new File(new URL(refcoCorpus.getBaseDirectory() + "/Annotations/").toURI()).exists())
                uris.add(new URL(refcoCorpus.getBaseDirectory() + "/Annotations/" + f).toURI().normalize());
            else if (new File(new URL(refcoCorpus.getBaseDirectory() + "/annotations/").toURI()).exists())
                uris.add(new URL(refcoCorpus.getBaseDirectory() + "/annotations/" + f).toURI().normalize());
            else if (new File(new URL(refcoCorpus.getBaseDirectory() + "/../Annotations/").toURI()).exists())
                uris.add(new URL(refcoCorpus.getBaseDirectory() + "/../Annotations/" + f).toURI().normalize());
            else if (new File(new URL(refcoCorpus.getBaseDirectory() + "/../annotations/").toURI()).exists())
                uris.add(new URL(refcoCorpus.getBaseDirectory() + "/../annotations/" + f).toURI().normalize());
            else
                uris.add(new URL(refcoCorpus.getBaseDirectory() + "/" + f).toURI().normalize());
        }
        // Audio recordings are in the Recordings folder
        else if (f.toLowerCase().endsWith("wav") || f.toLowerCase().endsWith("mp3")) {
            if (new File(new URL(refcoCorpus.getBaseDirectory() + "/Recordings/").toURI()).exists())
                uris.add(new URL(refcoCorpus.getBaseDirectory() + "/Recordings/" + f).toURI().normalize());
            else if (new File(new URL(refcoCorpus.getBaseDirectory() + "/recordings/").toURI()).exists())
                uris.add(new URL(refcoCorpus.getBaseDirectory() + "/recordings/" + f).toURI().normalize());
            else if (new File(new URL(refcoCorpus.getBaseDirectory() + "/../Recordings/").toURI()).exists())
                uris.add(new URL(refcoCorpus.getBaseDirectory() + "/../Recordings/" + f).toURI().normalize());
            else if (new File(new URL(refcoCorpus.getBaseDirectory() + "/../recordings/").toURI()).exists())
                uris.add(new URL(refcoCorpus.getBaseDirectory() + "/../recordings/" + f).toURI().normalize());
            else
                uris.add(new URL(refcoCorpus.getBaseDirectory() + "/" + f).toURI().normalize());
        }
        // All other files are metadata
        else {
            if (new File(new URL(refcoCorpus.getBaseDirectory() + "/Metadata/").toURI()).exists())
                uris.add(new URL(refcoCorpus.getBaseDirectory() + "/Metadata/" + f).toURI().normalize());
            else if (new File(new URL(refcoCorpus.getBaseDirectory() + "/metadata/").toURI()).exists())
                uris.add(new URL(refcoCorpus.getBaseDirectory() + "/metadata/" + f).toURI().normalize());
            else if (new File(new URL(refcoCorpus.getBaseDirectory() + "/../Metadata/").toURI()).exists())
                uris.add(new URL(refcoCorpus.getBaseDirectory() + "/../Metadata/" + f).toURI().normalize());
            else if (new File(new URL(refcoCorpus.getBaseDirectory() + "/../metadata/").toURI()).exists())
                uris.add(new URL(refcoCorpus.getBaseDirectory() + "/../metadata/" + f).toURI().normalize());
            else
                uris.add(new URL(refcoCorpus.getBaseDirectory() + "/" + f).toURI().normalize());
        }
        return uris;
    }

    /**
     * Function that performs tier checks on the RefCo documentation stored in the Checker object (using setRefcoFile)
     *
     * @return the detailed report of all checks performed
     */
    private Report refcoTierCheck() {
        Report report = new Report();
        // Check all tiers
        // Get all tiers from the corpus
        Map<String, Set<String>> allTiers;
        // First we have to see if we have a tier speaker separator character
        // If we have a tier speaker separator we can update the field
        tierSpeakerSeparator = criteria.getPunctuations().stream()
                .filter((c) -> c.getFunction().equalsIgnoreCase("convention for associating a speaker to a tier"))
                .map(RefcoCriteria.Punctuation::getCharacter).findAny().orElse(tierSpeakerSeparator);
        try {
            // Get all the tier names from the ELAN files
            allTiers = getTierIDs();
        } catch (Exception e) {
            report.addCritical(getFunction(), ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.Exception},
                    new Object[]{getFunction(), refcoShortName, "Corpus data: exception when extracting all tiers",
                            e}));
            allTiers = new HashMap<>();
        }
        // Make a deep copy of the tier map
        Map<String,Set<String>> remainingTiers = allTiers.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, (e) -> new HashSet<>(e.getValue())));
        for (Map.Entry<String,Set<String>> tier : allTiers.entrySet()) {
            String tierName = tier.getKey();
            // logger.info("Looking at " + tierName);
            if (tierName.contains(tierSpeakerSeparator)) {
                for (String fileName : tier.getValue()) {
                    // Get all speakers for all files in which the tier appears
                    for (String speaker : criteria.sessions.stream()
                            .filter((s) -> Arrays.asList(s.getFileNames().split(",\\s*")).contains(fileName))
                            .map((s) -> Arrays.asList(s.getSpeakerNames().split(",\\s*")))
                            .collect(ArrayList<String>::new, ArrayList::addAll, ArrayList::addAll)) {
                        // If we have a matching speaker for a file we remove the file from the tier list
                        if (tierName.endsWith(tierSpeakerSeparator + speaker)) {
                            remainingTiers.get(tierName).remove(fileName);
                        }
                    }
                }
            }
            else if (criteria.getTiers().stream().anyMatch((t) -> t.getTierName().equals(tierName))) {
                remainingTiers.remove(tierName);
            }
        }
        for (RefcoCriteria.Tier t : criteria.getTiers()) {
//            // If we have a tier speaker separator we also have to check the combination of tier names with speakers
//            if (t.tierName.contains(tierSpeakerSeparator)) {
//                for (String fileName :
//                        allTiers.keySet().stream().filter((tn) -> tn.startsWith(t.tierName))
//                                .map(allTiers::get).collect(ArrayList<String>::new, ArrayList::addAll,
//                                        ArrayList::addAll)) {
//                    for (String newTier :
//                            findSpeakers(fileName).stream().map((s) -> t.tierName + tierSpeakerSeparator + s)
//                                    .collect(Collectors.toList())) {
//                        allTiers.remove(newTier);
//                    }
//                }
//            }
//            else {
//                allTiers.remove(t.tierName);
//            }
            if (t.getTierName() == null || t.getTierName().isEmpty())
                report.addCritical(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), refcoShortName, "Annotation Tiers: tier name is empty",
                                "Add tier name"}));
            if (t.getTierFunctions() == null || t.getTierFunctions().isEmpty())
                report.addCritical(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), refcoShortName,
                                "Annotation Tiers: tier function is empty: " + t.getTierName(), "Add tier function"}));
            else if (t.getTierFunctions().stream().filter(validTierFunctions::contains).collect(Collectors.toSet()).isEmpty()) {
                report.addWarning(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), refcoShortName,
                                "Annotation Tiers: potential custom tier detected:\n" + t.getTierName() + " with tier " +
                                        "function " + t.getTierFunctions(),
                                "Check if custom tier function is intended or change tier function"}));
            }
            if (t.getSegmentationStrategy() == null || t.getSegmentationStrategy().isEmpty())
                report.addWarning(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), refcoShortName,
                                "Annotation Tiers: segmentation strategy is empty: " + t.getTierName(),
                                "Add segmentation strategy"}));
            if (t.getLanguages() == null || t.getLanguages().isEmpty())
                report.addCritical(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), refcoShortName,
                                "Annotation Tiers: tier languages is empty: " + t.getTierName(), "Add tier language"}));
            else {
                // Each cell can contain several languages. Split the languages and check for each one if it is a
                // valid language code
                // TODO check if that is the intended way to separate languages
                ArrayList<String> tierLangList = new ArrayList<>(Arrays.asList(t.getLanguages().split(valueSeparator)));
                for (String l : tierLangList) {
                    if (!checkLanguage(l)) {
                        report.addWarning(getFunction(), ReportItem.newParamMap(
                                new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                                new Object[]{getFunction(), refcoShortName, "Annotation Tiers: language is neither a " +
                                        "Glottolog, a ISO-639-3 language code nor otherwise known:\n" + l,
                                        "Use a valid language code"}));
                    }
                }
            }
        }
        // If we have some tiers left and at least one of them has files left we have to report it
        if (!remainingTiers.isEmpty() && remainingTiers.entrySet().stream().anyMatch((e) -> !e.getValue().isEmpty())) {
            report.addWarning(getFunction(), ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(), refcoShortName, "Tiers are not documented:\n" +
                            remainingTiers.entrySet().stream()
                                    .filter((e) -> !e.getValue().isEmpty())
                                    .map((e) -> e.getKey() + ": " +
                                    String.join(",", e.getValue()))
                                    .collect(Collectors.joining(",\n")),
                            "Add documentation for all tiers"}));
        }
        return report;
    }

    /**
     * Function that performs transcription checks on the RefCo documentation stored in the Checker object (using
     * setRefcoFile)
     *
     * @return the detailed report of all checks performed
     */
    private Report refcoTranscriptionCheck() {
        Report report = new Report();
        // Check all transcription graphemes
        for (RefcoCriteria.Transcription t : criteria.getTranscriptions()) {
            if (t.getGrapheme() == null || t.getGrapheme().isEmpty())
                report.addCritical(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), refcoShortName, "Grapheme is empty", "Add grapheme"}));
            if (t.getLinguisticValue() == null || t.getLinguisticValue().isEmpty())
                report.addCritical(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), refcoShortName, "Grapheme linguistic value is empty: " + t.getGrapheme()
                                , "Add linguistic value"}));
            if (t.getLinguisticConvention() == null || t.getLinguisticConvention().isEmpty())
                report.addWarning(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), refcoShortName,
                                "Grapheme linguistic convention is empty: " + t.getGrapheme(),
                                "Add linguistic convention"}));
        }
        return report;
    }

    /**
     * Function that performs gloss checks on the RefCo documentation stored in the Checker object (using setRefcoFile)
     *
     * @return the detailed report of all checks performed
     */
    private Report refcoGlossCheck() {
        Report report = new Report();
        // Check all glosses
        for (RefcoCriteria.Gloss g : criteria.getGlosses()) {
            if (g.getGloss() == null || g.getGloss().isEmpty())
                report.addCritical(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), refcoShortName, "Glosses: Gloss is empty",
                                "Add gloss abbreviations"}));
            // Check if we can split the gloss but only if it is a grammatical morpheme (i.e. does not contain
            // lower-case letters)
//            else if (g.gloss.split("[" + String.join("", glossSeparator) + "]").length > 1
            else if (Arrays.stream(g.getGloss().split("")).map(glossSeparator::contains).reduce(Boolean::logicalOr).orElse(false)
                    && !g.getGloss().matches(".*[a-z].*"))
                report.addWarning(getFunction(),
                        ReportItem.newParamMap(
                                new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                                new Object[]{getFunction(), refcoShortName,
                                        "Glosses: Gloss contains separating character:\n" + g.getGloss() + " contains one of " +
                                                glossSeparator,
                                        "Document the gloss parts separately"}));
            if (g.getMeaning() == null || g.getMeaning().isEmpty())
                report.addCritical(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), refcoShortName, "Glosses: Gloss meaning is empty: " + g.getGloss(),
                                "Add a gloss definition to the corpus documentation"}));
            // We skip comments assuming it is optional
            if (g.getTiers() == null || g.getTiers().isEmpty())
                report.addCritical(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), refcoShortName, "Corpus data: Gloss tiers are empty: " + g.getGloss(),
                                "Check that all gloss tiers are documented as such"}));
                // If the tiers is not "all", check if its valid tiers
            else if (!g.getTiers().equalsIgnoreCase("all")) {
                // Each cell can contain several tiers. Split the tiers and check for each one if it is a
                // valid tier defined in AnnotationTiers
                ArrayList<String> tierList = new ArrayList<>(Arrays.asList(g.getTiers().split(valueSeparator)));
                for (String t : tierList) {
                    // At least one of the tiers has to be defined in the AnnotationTiers table, otherwise report error
                    if (criteria.getTiers().stream().filter((tt) -> tt.getTierName().equals(t) || tt.getTierFunctions().contains(t)).toArray().length == 0)
                        report.addCritical(getFunction(), ReportItem.newParamMap(
                                new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                                new Object[]{getFunction(), refcoShortName,
                                        "Glosses: Gloss tier not defined in Annotation Tiers: " + t,
                                        "Add documentation for tier"}));
                }
            }
        }
        return report;
    }

    /**
     * Function that performs punctuation checks on the RefCo documentation stored in the Checker object (using
     * setRefcoFile)
     *
     * @return the detailed report of all checks performed
     */
    private Report refcoPunctuationCheck() {
        Report report = new Report();
        // Check all punctuation
        for (RefcoCriteria.Punctuation p : criteria.getPunctuations()) {
            if (p.getCharacter() == null || p.getCharacter().isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),refcoShortName,"Punctuation: Grapheme is empty",
                                "Add punctuation grapheme"}));
            if (p.getMeaning() == null || p.getMeaning().isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),refcoShortName,
                                "Punctuation: meaning is empty for grapheme: " + p.getCharacter(), "Add grapheme meaning"}));
            // We skip comments assuming it is optional
            if (p.getTiers() == null || p.getTiers().isEmpty())
                report.addCritical(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(),refcoShortName,
                                "Punctuation: tiers is empty for grapheme: " + p.getCharacter(),
                                "Add valid tiers for punctuation"}));
            // If the tiers is not "all", check if its valid tiers
            else if (!p.getTiers().equalsIgnoreCase("all")) {
                // Each cell can contain several tiers. Split the tiers and check for each one if it is a
                // valid tier defined in AnnotationTiers
                ArrayList<String> tierList = new ArrayList<>(Arrays.asList(p.getTiers().split(valueSeparator)));
                for (String tierName : tierList) {
                    // At least one of the tiers has to be defined in the AnnotationTiers table, otherwise report error
                    boolean isDocumented = false;
                    for (RefcoCriteria.Tier tier : criteria.getTiers()) {
                        if (tier.getTierName().equalsIgnoreCase(tierName) ||
                                tier.getTierFunctions().contains(tierName.toLowerCase())) {
                            isDocumented = true;
                            break;
                        }
                    }
                    if (!isDocumented) {
                        report.addCritical(getFunction(), ReportItem.newParamMap(
                                new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                                new Object[]{getFunction(), refcoShortName, "Punctuation: tier not defined in " +
                                        "Annotation Tiers: " + tierName,
                                        "Add documentation for tier"}));
                    }
                }
            }
            if (p.getFunction() == null || p.getFunction().isEmpty()) {
                report.addCritical(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), refcoShortName,
                                "Punctuation: function is empty for grapheme: " + p.getCharacter(),
                                "Add valid function for punctuation"}));
            }
        }
        return report ;
    }

    /**
     * Function that performs checks on the RefCo documentation stored in the Checker object (using setRefcoFile)
     *
     * @return the detailed report of all checks performed
     */
    private Report refcoDocumentationCheck() {
        Report report = new Report();
        logger.info("Generic checks");
        report.merge(refcoGenericCheck());
        logger.info("Session checks");
        report.merge(refcoSessionCheck());
        logger.info("Tier checks");
        report.merge(refcoTierCheck());
        logger.info("Transcription checks");
        report.merge(refcoTranscriptionCheck());
        logger.info("Gloss checks");
        report.merge(refcoGlossCheck());
        logger.info("Punctuation checks");
        report.merge(refcoPunctuationCheck());
        return report;
    }

    /**
     * function to find all transcription tier names
     * @param file the file to contain the tiers
     * @return list of transcription tiers
     */
    private ArrayList<String> findTranscriptionTiers(String file) {
        // Add tiers containing transcription in the tier function
        // as well as the ones with morpheme segmentation
        return findTiersByFunction(file,Arrays.asList("transcription", "morpheme segmentation"));
    }

    /**
     * function to find all gloss tier names
     * @param file the file to contain the tiers
     * @return list of gloss tiers
     */
    private ArrayList<String> findGlossTiers(String file) {
        return findTiersByFunction(file,Arrays.asList("morpheme gloss","morpheme glossing"));
    }

    /**
     * function to find all tiers based on their function
     * @param file the file to contain the tiers
     * @param functions list of tier functions
     * @return list of tiers matching the functions
     */
    private ArrayList<String> findTiersByFunction(String file, List<String> functions) {
        ArrayList<String> foundTiers = new ArrayList<>();
        // Get all documented speakers for the file
        List<String> speakers = findSpeakers(file);
        Map<String,Set<String>> allTiers = getTierIDs();
        for (RefcoCriteria.Tier t: criteria.getTiers()) {
//            if (t.tierFunctions.contains() ||
//                    (t.tierFunctions.contains())) {
            if (functions.stream().anyMatch((f) -> t.getTierFunctions().stream().anyMatch((tf) -> tf.contains(f)))) {
                // Check if the tier is present in the file
                if (allTiers.containsKey(t.getTierName()) && allTiers.get(t.getTierName()).contains(file)) {
                    foundTiers.add(t.getTierName());
                }
                // We try to combine tier names with speakers and check if the tier is present in the file
                for (String speaker : speakers) {
                    String tierName = t.getTierName() + tierSpeakerSeparator + speaker;
                    if (allTiers.containsKey(tierName) && allTiers.get(tierName).contains(file))
                        foundTiers.add(tierName);
                }
            }
        }
        logger.info("TIERS FOUND " + String.join(",", foundTiers) +
                " FOR FUNCTIONS " + String.join(",", functions) + " AND " +
                "SPEAKERS " + String.join(",", speakers) + " IN FILE " +
                file
        );
        return foundTiers;
    }

    /**
     * Function to find all documented speakers for a file
     * @param fileName the file for which the speakers are documented
     * @return list of speakers
     */
    public List<String> findSpeakers(String fileName) {
        return criteria.sessions.stream().filter((s) -> s.getFileNames().contains(fileName))
                .map((s) -> Arrays.asList(s.getSpeakerNames().split(",\\s*")))
                .collect(ArrayList::new, List::addAll, List::addAll);
    }
    /**
     * function to check the transcription text based on valid chunks and glosses
     *
     * @param cd the corpus document used to correctly assign the log messages
     * @param tier the relevant tier
     * @param text the extracted text from the transcription tiers
     * @param chunks the valid character sequences (graphemes/punctuations)
     * @param glosses the documented glosses
     * @return the check report
     */
    private Report checkTranscriptionText(CorpusData cd, String tier, List<Text> text, List<String> chunks,
                                          Set<String> glosses) {
        // Create a string for all the characters in the automaton
        String dictAlphabet = "";
        if (!chunks.isEmpty()) {
            dictAlphabet = "(" +
                    chunks.stream().map(Pattern::quote).collect(Collectors.joining("|")) +
                    ")";
        }
        Report report = new Report();
        // All the characters that are valid
        int matched = 0;
        // All invalid characters in the text
        int missing = 0 ;
        // Indicator if a word contains missing characters
        boolean mismatch ;
        // Object used to segment string
        StringSegmentation sm = new StringSegmentation();
        for (Text t : text) {
            // Tokenize text
            for (String token : t.getText().split(tokenSeparator)) {
                // Check if token either is a gloss or each character is in the valid characters
                mismatch = false ;
                // Update frequency list
                tokenFreq.put(token);
                if (glosses.contains(token)){
                    // Add the length of the gloss to matched
                    matched += token.length() ;
                }
                // Token is not one of the glosses
                else {
                    // Check if we can segment the token using the chunks and glosses
                    if (sm.segmentWord(token,
                            new ArrayList<>(Sets.union(new HashSet<>(chunks), glosses)))) {
                        matched += token.length();
                    }
                    else {
                        missing += token.length();
                        mismatch = true ;
                    }
                }
                // Only accept non-morphological glosses, i.e. glosses that are not only uppercase letters
                //else if (token.matches(".*[a-z].*")){

//                // It is neither recognized by the automaton nor a non-morphological gloss
//                else {
//                    missing += token.length();
//                    mismatch = true;
//                }
                if (mismatch && !token.isEmpty()) {
//                    try {
//                        if (skipLocations) {
//                            report.addWarning(getFunction(), ReportItem.newParamMap(new String[]{"function", "filename",
//                                            "description", "howtoFix"},
//                                    new Object[]{getFunction(), cd.getFilename(), "Corpus data: Transcription token contains " +
//                                            "invalid character(s):\n" + token + " containing: [" +
//                                            token.replaceAll(dictAlphabet, "") + "]",
//                                            "Add all transcription characters to the documentation"}));
//                        }
//                        else {
//                            List<CorpusData.Location> locations = getLocations((ELANData) cd, Collections.singletonList(tier), token);
//                            for (CorpusData.Location l : locations) {
//                                report.addWarning(getFunction(), ReportItem.newParamMap(new String[]{"function", "filename",
//                                                "description", "tier", "segment", "howtoFix"},
//                                        new Object[]{getFunction(), cd.getFilename(), "Corpus data: Transcription token contains " +
//                                                "invalid character(s):\n" + token + " containing: [" +
//                                                UnicodeTools.padCombining(token.replaceAll(dictAlphabet, "")) + "]",
//                                                l.tier, l.segment, "Add all transcription characters to the documentation"}));
//                            }
//                        }
//                    } catch (Exception e) {
//                        report.addCritical(getFunction(), ReportItem.newParamMap(new String[]{"function", "filename",
//                                        "description", "exception"},
//                                new Object[]{getFunction(), cd.getFilename(), "Corpus data: Exception when trying to " +
//                                        "locate token " + token, e}));
//                    }
                    addWarningWithLocation(cd,tier,token,"Transcription token contains " +
                                            "invalid character(s):\n" + token + " containing: [" +
                                            token.replaceAll(dictAlphabet, "") + "]",
                                            "Add all transcription characters to the documentation");
                }
            }
        }
        float percentValid = (float)matched/(matched+missing) ;
        if (percentValid < (transcriptionCharactersValid / 100.0)) {
            report.addCritical(getFunction(), ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(), cd.getFilename(),
                            "Corpus data: Less than " + transcriptionCharactersValid + " percent of transcription " +
                                    "characters are valid  in tier " + tier+ ".\nValid: " + matched + " Invalid: " + missing + " " +
                                    "Percentage: " +
                                    Math.round(percentValid * 1000)/10.0,
                    "Add documentation for all graphemes and punctuation marks used in transcription"}));
        }
        else {
            if (Math.round(percentValid * 1000)/10.0 == 100)
                report.addCorrect(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), cd.getFilename(),
                                "Corpus data: All characters are valid in tier " + tier ,"Documentation cannot be improved"}));
            else
                report.addCorrect(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), cd.getFilename(),
                                "Corpus data: More than " + transcriptionCharactersValid + " percent of transcription " +
                                        "characters are valid in tier " + tier + ".\nValid: " + matched + " Invalid: " + missing + " " +
                                        "Percentage: " +
                                        Math.round(percentValid * 1000)/10.0,"Documentation can be improved but no fix necessary"}));
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
        ArrayList<String> transcriptionTiers = findTranscriptionTiers(cd.getFilename());
        logger.info("Checking transcription tiers: " + String.join(",", transcriptionTiers));
        // Check if we actually have relevant tiers
        if (transcriptionTiers.isEmpty()) {
            report.addCritical(getFunction(), ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(), cd.getFilename(), "Corpus data: No transcription tiers found",
                            "Check that all transcription tiers are documented"}));
            return report;
        }
        // Check each transcription separately
        for (String tierId : transcriptionTiers) {
            // Get all transcription graphemes
            List<String> validTranscriptionCharacters = new ArrayList<>(criteria.getTranscriptions().size());
            for (RefcoCriteria.Transcription t : criteria.getTranscriptions()) {
                // Add all of the grapheme's characters
                // validTranscriptionCharacters.addAll(getChars(t.grapheme));
                validTranscriptionCharacters.add(t.getGrapheme());
            }
            // and punctuation characters
            for (RefcoCriteria.Punctuation p : criteria.getPunctuations()) {
                if (p.getTiers().equals("all"))
                    // Add all of the punctuation's characters
                    // validTranscriptionCharacters.addAll(getChars(p.character));
                    validTranscriptionCharacters.add(p.getCharacter());
                // does that work properly? seems so
                else if ((Arrays.asList(p.getTiers().split(valueSeparator)).contains(tierId)) ||
                        Arrays.stream(p.getTiers().split(valueSeparator))
                                .anyMatch((t) -> tierId.startsWith(t + tierSpeakerSeparator))) {
                    // Add all the punctuation's characters
                    validTranscriptionCharacters.add(p.getCharacter());
                }
            }
            // Also get all glosses valid for the transcription tiers
            Set<String> validGlosses = new HashSet<>();
            for (RefcoCriteria.Gloss g : criteria.getGlosses()) {
                if (g.getTiers().equals("all"))
                    validGlosses.add(g.getGloss());
                // else if (Arrays.asList(g.tiers.split(valueSeparator)).contains(tierId)) {
                // TODO does this work properly
                else if (Arrays.stream(g.getTiers().split(valueSeparator))
                        .anyMatch((t) -> tierId.startsWith(t + tierSpeakerSeparator) || tierId.equalsIgnoreCase(t))) {
                    validGlosses.add(g.getGloss());
                }
            }
            // Get the text from all transcription tiers
            List<Text> transcriptionText = getTextsInTierByID(content, tierId);
            // Check if one of the relevant variables is empty and, if yes, skip the transcription test
            if (validTranscriptionCharacters.isEmpty()) {
                report.addCritical(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), cd.getFilename(), "Transcription/Punctuation: No valid transcription " +
                                "characters (graphemes/punctuation) defined for tier: " + tierId,
                                "Define all graphemes and punctuation characters used in the corpus"}));
                return report;
            }
            if (containsTier((ELANData) cd,tierId)) {
                if (transcriptionText.isEmpty()) {
                    report.addCritical(getFunction(), ReportItem.newParamMap(
                            new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                            new Object[]{getFunction(), cd.getFilename(), "Corpus data: No transcribed text found in " +
                                    "tier: " + tierId,
                                    "Check that all documented transcription tiers exist in the corpus"}));
                    return report;
                }
                report.merge(checkTranscriptionText(cd, tierId, transcriptionText, validTranscriptionCharacters,
                        validGlosses));
            }
            // Problematic
//            else {
//                report.addCritical(getFunction(), ReportItem.newParamMap(new String[]{"function", "filename", "description",
//                                "howtoFix"},
//                        new Object[]{getFunction(), cd.getFilename(), "Corpus data: Missing transcription tier: " +
//                                tierId, "Check that all documented transcription tiers exist in the corpus"}));
//            }
        }
        return report;
    }

    /**
     * Function that checks the morphology tier for documented glosses
     * @param cd the corpus document used to correctly assign the log messages
     * @param tier the relevant tier
     * @param text the extracted text from all morphology tiers
     * @param glosses all documented glosses
     * @return the detailed report of the checks
     */
    private Report checkMorphologyGloss(CorpusData cd, String tier, List<Text> text, HashSet<String> glosses,
                                        HashSet<String> punctuation) {
        Report report = new Report() ;
        // Copy the glosses
        List<String> chunks = glosses.stream().collect(Collectors.toList());
        // Add punctuation
        chunks.addAll(punctuation);
        // Add digits and separator
        chunks.addAll(Arrays.asList("1","2","3"));
        // All the tokens that are valid
        int matched = 0;
        // All invalid tokens in the text
        int missing = 0 ;
        // Object used to segment string
        StringSegmentation sm = new StringSegmentation();
        for (Text t : text) {
            // Tokenize text
            for (String token : t.getText().split(tokenSeparator)) {
                glossFreq.put(token);
                ArrayList<String> segments = new ArrayList<>();
                // if we can split the token we do that
                if (!glossSeparator.isEmpty()) {
                    String splitRegex = "[" + String.join("",
                            glossSeparator.stream().map(Pattern::quote).collect(Collectors.toSet()))
                            + "]";
                    segments.addAll(Arrays.asList(token.split(splitRegex)));
                }
                // Otherwise, we take the whole token
                else
                    segments.add(token);
                // Check all the segments
                for (String s : segments) {
                    if (sm.segmentWord(s,chunks)) {
                        List<String> segmented = sm.getSegments();
                        // TODO this is hacky
                        // Remove e.g. digits and punctuation
                        segmented.removeIf((sgm) -> sgm.length() == 1);
                        morphemeFreq.putAll(segmented);
                        matched++;
                    }
                    else if (s.codePoints().anyMatch(Character::isLowerCase)) {
                        // TODO which punctuations marks are allowed here
                        if (Pattern.compile("[\\p{IsAlphabetic}_-]+").matcher(s).matches()) {
                            // Check if we have a dictionary
                            if (hasDict) {
                                // TODO what to do on underscore, currently replace by space
                                for (String tmpSegment : s.split("_")) {
                                    // Check if word is in dictionary
                                    if (dict.match(tmpSegment)) {
                                        // report.addNote(getFunction(),"Found word " + tmpSegment + " in dictionary");
                                        lexicalFreq.put(tmpSegment);
                                        matched++;
                                    } else {
                                        missing++;
                                        // Keep track of the non-matched lexeme
                                        missingLexicalFreq.put(tmpSegment);
                                        // Check if we have any approximate matches
                                        List<String> candidates = UniversalLevenshteinAutomatonK1.matchDictionary(tmpSegment, dict);
                                        // Also add all candidates if we pad the segment
                                        // candidates.addAll(UniversalLevenshteinAutomatonK1.matchDictionary(tmpSegment + "#", dict));
                                        // report.addNote(getFunction(),"Failed to find word " + tmpSegment + " in dictionary: " + candidates);
                                        int lenThreshold = 4;
                                        if (!candidates.isEmpty() && tmpSegment.length() >= lenThreshold) {
                                            approximateMatches.put(tmpSegment, candidates);
                                        }
                                    }
                                }
                            }
                            else {
                                lexicalFreq.put(s);
                                matched++;
                            }
                        }
                        else {
                            addWarningWithLocation(cd, tier, s,
                                    "Unexpected characters in lexical " +
                                            "gloss " + s + ": " + s.replaceAll("[\\p{IsAlphabetic}_-]]",""),
                                    "Check and fix your gloss tier"
                                    );
//                            report.addWarning(getFunction(), ReportItem.newParamMap(new String[]{"function", "filename",
//                                            "description"},
//                                    new Object[]{getFunction(), cd.getFilename(), "Unexpected characters in lexical " +
//                                            "gloss " + s + ": " + s.replaceAll("[\\p{IsAlphabetic}_-]]","")}));
                        }

                    }
                    else if (!s.isEmpty()){
                        missingGlossFreq.put(s);
                        missing++;
                        // This leads to large amount of warnings
                        try {
                            if (skipLocations) {
                                report.addWarning(getFunction(), ReportItem.newParamMap(
                                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                                        new Object[]{getFunction(), cd.getFilename(),
                                                "Invalid morpheme in token: " + s + " in " + token,
                                                "Document gloss properly or check for typo"
                                        }));
                            }
                            else {
                                for (CorpusData.Location l : getLocations((ELANData) cd, Collections.singletonList(tier), token)) {
                                    report.addWarning(getFunction(), ReportItem.newParamMap(
                                            new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.Tier, ReportItem.Field.Segment},
                                            new Object[]{getFunction(), cd.getFilename(),
                                                    "Invalid morpheme in token: " + s + " in " + token,
                                                    "Document gloss properly or check for typo",
                                                    l.tier, l.segment
                                            }));
                                }
                            }
                        } catch (Exception e) {
                            report.addCritical(getFunction(), ReportItem.newParamMap(
                                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.Exception},
                                    new Object[]{getFunction(), cd.getFilename(), "Corpus data: Exception when trying to " +
                                            "locate token " + token,
                                            e}));
                        }
                    }
                }
            }
        }
        // Check if we can disambiguate the approximate matches
        if (!approximateMatches.isEmpty()) {
            for (String lexeme : approximateMatches.keySet()) {
                // int lexemeCount = missingLexicalFreq.get(lexeme);
                int maxCount = -1;
                String maxCandidate = null;
                for (String candidate : approximateMatches.get(lexeme)) {
                    if (lexicalFreq.contains(candidate)) {
                        if (lexicalFreq.get(candidate) > maxCount) {
                            maxCount = lexicalFreq.get(candidate);
                            maxCandidate = candidate;
                        }
                    }
                }
                // TODO what is the threshold
                // int threshold = 1;
                // if (maxCount >= threshold * lexemeCount && maxCandidate != null) {
                if (maxCandidate != null) {
                    addWarningWithLocation(cd,tier,lexeme,
                            "Potential typo detected in " + lexeme + ": best candidate " + maxCandidate + " with count " + maxCount,
                            "Check if this actually is a typo and fix if necessary"
                    );
//                        try {
//                            if (skipLocations) {
//                                report.addWarning(getFunction(), ReportItem.newParamMap(new String[]{"function", "filename", "description",
//                                                "howtoFix"},
//                                        new Object[]{getFunction(), cd.getFilename(),
//                                                "Potential typo detected in " + lexeme + ": best candidate " + maxCandidate +
//                                                " with count " + maxCount,
//                                                "Check if this actually is a typo and fix if necessary"
//                                        }));
//                            }
//                            else {
//                                for (CorpusData.Location l : getLocations((ELANData) cd, Collections.singletonList(tier), lexeme)) {
//                                    report.addWarning(getFunction(), ReportItem.newParamMap(new String[]{"function", "filename", "description",
//                                                    "howtoFix", "tier", "segment"},
//                                            new Object[]{getFunction(), cd.getFilename(),
//                                                    "Potential typo detected in " + lexeme + ": best candidate " + maxCandidate +
//                                                    " with count " + maxCount,
//                                                    "Check if this actually is a typo and fix if necessary",
//                                                    l.tier, l.segment
//                                            }));
//                                }
//                            }
//                        } catch (Exception e) {
//                            report.addCritical(getFunction(), ReportItem.newParamMap(new String[]{"function", "filename",
//                                            "description", "exception"},
//                                    new Object[]{getFunction(), cd.getFilename(), "Corpus data: Exception when trying to " +
//                                            "locate token " + lexeme,
//                                            e}));
//                        }
                }
                else {
                    addWarningWithLocation(cd,tier,lexeme,
                            "Word not in the dictionary and no similar words found: " + lexeme,
                            "Check if this could be a typo and fix if necessary"
                    );
                }
            }
        }
        float percentValid = (float)matched/(matched+missing) ;
        if (percentValid < glossMorphemesValid / 100.0) {
            report.addWarning(getFunction(), ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                    new Object[]{getFunction(), cd.getFilename(),
                            "Corpus data: Less than " + glossMorphemesValid + " percent of tokens are" +
                                    " valid gloss morphemes in tier " + tier + ".\nValid: " + matched + " Invalid: " + missing +
                                    " Percentage valid: " + Math.round(percentValid * 1000) / 10.0,
                            "Improve the gloss documentation to cover more tokens"}));
        }
        else {
            if (Math.round(percentValid * 1000) / 10.0 == 100) {
                report.addCorrect(getFunction(), ReportItem.newParamMap(new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), cd.getFilename(),
                                "Corpus data: All tokens valid glosses in tier " + tier,
                                "Documentation cannot be improved"}));
            }
            else {
                report.addCorrect(getFunction(), ReportItem.newParamMap(new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), cd.getFilename(),
                                "Corpus data: More than " + glossMorphemesValid + " percent of tokens are " +
                                        "valid gloss morphemes in tier " + tier + ".\nValid: " + matched + " Invalid: " + missing +
                                        " Percentage valid: " + Math.round(percentValid * 1000) / 10.0,
                                "Documentation can be improved but no fix necessary"}));
            }
        }
        return report;
    }

    /**
     * Function that checks all morphology tiers in a corpus document
     * @param cd the corpus document
     * @return the detailed report of the checks
     * @throws JDOMException on XPath problems
     */
    private Report checkGloss(CorpusData cd) throws JDOMException {
        Report report = new Report();
        // Get the dom
        Document content = ((ELANData) cd).getJdom();
        // Get morphology tiers
        List<String> morphologyTiers = findGlossTiers(cd.getFilename());
        // Check if we actually have tiers
        if (morphologyTiers.isEmpty()) {
                report.addCritical(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), cd.getFilename(), "Corpus composition: No morphology tiers found",
                                "Add documentation for tiers of type morphology gloss"}));
                return report;
            }
        // For each morphology tier
        for (String tierId : morphologyTiers) {
            // Get all valid Glosses
            HashSet<String> validGlosses = new HashSet<>();
            for (RefcoCriteria.Gloss g : criteria.getGlosses()) {
                if (g.getTiers().equals("all"))
                    validGlosses.add(g.getGloss());
                else if (Arrays.stream(g.getTiers().split(valueSeparator))
                        .anyMatch(t -> tierId.startsWith(t + tierSpeakerSeparator) || tierId.equalsIgnoreCase(t))) {
                    validGlosses.add(g.getGloss());
                }
            }
            // Also get the valid punctuation characters
            HashSet<String> validPunctuation = new HashSet<>();
            for (RefcoCriteria.Punctuation p : criteria.getPunctuations()) {
                if (p.getTiers().equals("all"))
                    validPunctuation.add(p.getCharacter());
                else if (Arrays.stream(p.getTiers().split(valueSeparator))
                        .anyMatch(t -> tierId.startsWith(t + tierSpeakerSeparator) || tierId.equalsIgnoreCase(t))) {
                    validPunctuation.add(p.getCharacter());
                }
            }
            // Get the text from all morphology tiers
            List<Text> glossText = getTextsInTierByID(content, tierId);
            // Check if one of the relevant variables is empty and, if yes, skip the transcription test
            if (validGlosses.isEmpty()) {
                report.addWarning(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), cd.getFilename(), "No valid glosses defined in tier " + tierId,
                                "Add documentation for all gloss morphemes"}));
                return report;
            }
            if (glossText.isEmpty()) {
                report.addCritical(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), cd.getFilename(), "No annotated text found in tier: " + tierId,
                                "Check the tier documentation to make sure that your morphology tiers are covered"}));
                return report;
            }
            report.merge(checkMorphologyGloss(cd,tierId,glossText,validGlosses,validPunctuation));
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
        logger.info("Corpus check for file: " + cd.getFilename());
        Report report = new Report() ;
        // Check for ELAN data
        if (cd instanceof ELANData) {
            // Check the transcription
            // but parameter allows skipping
            if (!props.containsKey("skip-transcription-check") ||
                    !((String) props.get("skip-transcription-check")).equalsIgnoreCase("true"))
                try {
                    logger.info("Transcription check");
                    report.merge(checkTranscription(cd));
                }
                catch (JDOMException e) {
                    report.addCritical(getFunction(),
                            ReportItem.newParamMap(
                                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.Exception},
                                    new Object[]{getFunction(), cd.getFilename(), "Exception encountered when reading Transcription " +
                                            "tier", e}));
                }
            // Check the morphology gloss
            // but parameter allows skipping
            if (!props.containsKey("skip-gloss-check") ||
                    !((String) props.get("skip-gloss-check")).equalsIgnoreCase("true"))
                try {
                    logger.info("Gloss check");
                    report.merge(checkGloss(cd));
                }
                catch (JDOMException e) {
                    report.addCritical(getFunction(),
                            ReportItem.newParamMap(
                                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.Exception},
                                    new Object[]{getFunction(), cd.getFilename(),
                                            "Exception encountered when reading Morphology tier", e}));
                }
        }
        else {
            report.addCritical(getFunction(), ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description},
                    new Object[]{getFunction(), refcoShortName, "Not supported corpus type: " + cd.getClass().getName()}));
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
        // We encountered an unknown language before
        if (knownLanguages.contains(lang.toLowerCase())) {
            return true;
        }
        // Check if the language is in the list of known translation languages
        if (validTranslationLanguages.stream().map((tl) -> tl.contains(lang.toLowerCase())).reduce(Boolean::logicalOr)
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
            knownLanguages.add(lang.toLowerCase());
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
            String path = String.format("//TIER[@LINGUISTIC_TYPE_REF=\"%s\"]//ANNOTATION_VALUE/text()", tier);
            return new XPathBuilder<>(path, Filters.text()).compileWith(xpathFactory).evaluate(d);
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
            return new XPathBuilder<>(String.format("//TIER[@TIER_ID=\"%s\"]//ANNOTATION_VALUE/text()", tier),
                    Filters.text()).compileWith(xpathFactory).evaluate(d);
        }
    }

    /**
     * Function to count all words in certain tiers of the corpus stored in the Checker object. The tier is identified
     * by type
     *
     * @param tierFunction the tier function
     * @return the number of words encountered in the selected tiers of all documents in the corpus
     * @throws JDOMException on XPath problems
     */
    private int countWordsInTierByFunction(String tierFunction) throws JDOMException{
        int count = 0 ;
        List<String> tierList =
                criteria.getTiers().stream().filter((t) -> t.getTierFunctions()
                                .stream().anyMatch(f -> f.contains(tierFunction.toLowerCase())))
                        .map(RefcoCriteria.Tier::getTierName)
                        .toList();
        for (String tierName :
                tierList) {
            for (CorpusData cd : refcoCorpus.getCorpusData()) {
                List<String> speakerList = getDocumentedSpeakers(cd.getFilename());
                if (cd instanceof ELANData) {
                    // Get all texts from given annotation tier
                    List<Text> texts = new ArrayList<>();
                    if (speakerList.isEmpty()) {
                        texts.addAll(getTextsInTierByID(((ELANData) cd).getJdom(), tierName));
                    }
                    else {
                        for (String speaker : speakerList) {
                            texts.addAll(getTextsInTierByID(((ELANData) cd).getJdom(), tierName + tierSpeakerSeparator + speaker));
                        }
                    }
                    for (Text t : texts) {
                        // Word separation simply on spaces
                        count += t.getText().split(tokenSeparator).length;
                    }
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
        return countWordsInTierByFunction("transcription") ;
    }

    /**
     * Function that counts all words from annotation tiers, i.e. tiers of type morphologie, in the corpus stored in
     * the Checker object
     *
     * @return the number of transcribed words
     * @throws JDOMException on XPath problems
     */
    private int countAnnotatedWords() throws JDOMException {
        return countWordsInTierByFunction("Morpheme gloss") ;
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
        params.put("refco-config-path-absolute", "Flag that the path to the corpus documentation file" +
                " is absolute");
        params.put("skip-documentation-check", "Flag to skip the documentation check");
        params.put("skip-transcription-check", "Flag to skip the transcription check");
        params.put("skip-gloss-check", "Flag to skip the gloss check");
        params.put("gloss-stats", "Includes stats about all glosses");
        params.put("skip-locations", "Flag to skip determining the location of an error");
        params.put("detailed-locations", "Flag to include details such as segment and time slot in location (takes a " +
                "lot of time!)");
        params.put("dict", "Dictionary to be used to check lexemes in gloss tiers");
        return params;
    }

    /**
     * Simple function to print an xml element
     * @param e the element
     * @return the string representation
     */
    public static String showElement(Element e) {
        return new XMLOutputter().outputString(e);
    }

    /**
     * Extracts all tier ids  from the globalcorpus
     * @return A map from tier id to files in which it is defined
     */
    private Map<String, Set<String>> getTierIDs() {
        Map<String,Set<String>> allTiers = new HashMap<>();
        for (ELANData cd : refcoCorpus.getELANData()) {
            for (String tier_id :
                    new XPathBuilder<>("//TIER/@TIER_ID", Filters.attribute()).compileWith(xpathFactory)
                            .evaluate(cd.getJdom())
                            .stream().map(Attribute::getValue).toList()) {
                if (allTiers.containsKey(tier_id) && allTiers.get(tier_id) != null) {
                    allTiers.get(tier_id).add(cd.getFilename());
                }
                else
                    allTiers.put(tier_id,new HashSet<>(Collections.singleton(cd.getFilename())));
            }
        }
        return allTiers;
    }

    /**
     * Gives the location of a text token in a corpus document
     * @param cd the corpus document
     * @param token the token to be looked up
     * @throws JDOMException on problems with the xpath expressions
     * @return the list of all location consisting of a tier and a segment
     */
    private List<CorpusData.Location> getLocations(ELANData cd, List<String> validTiers, String token) throws JDOMException {
        List<CorpusData.Location> locations = new ArrayList<>();
        if (token == null || token.isEmpty() || validTiers == null || validTiers.isEmpty())
            return locations;
        String normalizedToken = token.replaceAll("\"", "'");
        List<Element> tiers =
                new XPathBuilder<>(String.format("/ANNOTATION_DOCUMENT/TIER[contains(string(.),\"%s\")]",
                                normalizedToken), Filters.element()).compileWith(xpathFactory)
                        .evaluate(cd.getJdom());
        for (Element tier : tiers.stream().filter((t) ->
                validTiers.contains(t.getAttributeValue("TIER_ID"))).toList()) {
            Attribute tier_id = tier.getAttribute("TIER_ID");
            assert tier_id != null : "Tier id is null";
            // TODO finding the segment and time takes too long
            if (detailedLocations) {
                Element annotation_segment = null;
                // All elements are ANNOTATION tags here
                for (Element e : tier.getChildren()) {
                    if (e.getChild("ALIGNABLE_ANNOTATION") != null)
                        annotation_segment = e.getChild("ALIGNABLE_ANNOTATION");
                    else if (e.getChild("REF_ANNOTATION") != null)
                        annotation_segment = e.getChild("REF_ANNOTATION");
                    assert annotation_segment != null : "Annotation segment is null";
                    if (XMLTools.showAllText(annotation_segment).contains(normalizedToken)) {
                        String annotation_id = annotation_segment.getAttributeValue("ANNOTATION_ID");
                        if (!annotation_segment.getName().equals("ALIGNABLE_ANNOTATION")) {
                            if (annotation_segment.getName().equals("REF_ANNOTATION")) {
                                // Resolve reference first
                                annotation_segment =
                                        new XPathBuilder<>(String.format("//ALIGNABLE_ANNOTATION[@ANNOTATION_ID=\"%s\"]",
                                                annotation_segment.getAttributeValue("ANNOTATION_REF")), Filters.element()).compileWith(xpathFactory)
                                                .evaluateFirst(tier);
                                assert annotation_segment != null : "Annotation segment is null after resolving reference";
                            } else {
                                locations.add(new CorpusData.Location("Tier:" + tier_id.getValue(),
                                        "Segment:" + annotation_segment.getAttributeValue("ANNOTATION_ID=")));
                            }
                        }
                        Attribute start_ref = annotation_segment.getAttribute("TIME_SLOT_REF1");
                        Attribute end_ref = annotation_segment.getAttribute("TIME_SLOT_REF2");
                        assert start_ref != null : "Start ref is null";
                        assert end_ref != null : "End ref is null";
                        Attribute start_time =
                                new XPathBuilder<>(String.format("//TIME_SLOT[@TIME_SLOT_ID=\"%s\"]/@TIME_VALUE",
                                        start_ref.getValue()), Filters.attribute()).compileWith(xpathFactory)
                                        .evaluateFirst(cd.getJdom());
                        assert start_time != null : "Start time is null";
                        Attribute end_time =
                                new XPathBuilder<>(String.format("//TIME_SLOT[@TIME_SLOT_ID=\"%s\"]/@TIME_VALUE",
                                                end_ref.getValue()), Filters.attribute()).compileWith(xpathFactory)
                                        .evaluateFirst(cd.getJdom());
                        assert end_time != null : "End time is null";
                        locations.add(new CorpusData.Location("Tier:" + tier_id.getValue(),
                                "Segment:" + annotation_id + ", Time:" +
                                        DurationFormatUtils.formatDuration(start_time.getIntValue(), "mm:ss.SSSS") + "-" +
                                        DurationFormatUtils.formatDuration(end_time.getIntValue(), "mm:ss.SSSS"))
                        );
                    }
                }
            }
            else {
                locations.add(new CorpusData.Location("Tier:" + tier_id.getValue(), ""));
            }
        }
        if (locations.isEmpty())
            return Collections.singletonList(new CorpusData.Location("Unknown", ""));
        else
            return locations;
    }

    /**
     * Checks if a tier exists in a corpus file
     * @param cd the corpus data of an elan file
     * @param tierId the tier
     * @return true if tier exists and false otherwise
     */
    public boolean containsTier(ELANData cd, String tierId) {
        // Check if node list for tier is empty
        try {
            return !new XPathBuilder<>(String.format("//TIER[@TIER_ID=\"%s\"]", tierId), Filters.text()).compileWith(xpathFactory)
                            .evaluate(cd.getJdom()).isEmpty();
        }
        // Exception also means that tier does not exist
        catch (Exception e) {
            return false;
        }
    }

    public String deriveXMLSpecification() throws JAXBException, IOException {
        JAXBContext ctx = JAXBContext.newInstance(RefcoCriteria.class);
        StringWriter sw = new StringWriter();
        ctx.generateSchema(new SchemaOutputResolver() {
            @Override
            public Result createOutput(String namespaceUri, String suggestedFileName) {
                StreamResult result = new StreamResult(sw);
                result.setSystemId("StringWriter");
                return result;
            }
        });
        return sw.toString();
    }

    private String deriveJSONSpecification() throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        HyperSchemaFactoryWrapper schemaVisitor = new HyperSchemaFactoryWrapper();
        om.acceptJsonFormatVisitor(RefcoCriteria.class, schemaVisitor);
        JsonSchema jsonSchema = schemaVisitor.finalSchema();
        return om.writerWithDefaultPrettyPrinter().writeValueAsString(jsonSchema);
    }

    /***
     * Gets the list of all documented speakers
     * @return the list of documented speakers
     */
    public List<String> getDocumentedSpeakers() {
        return getDocumentedSpeakers("any");
    }
    /***
     * Gets the list of all documented speakers
     * @param fileName the name of the file the speakers should be documented for
     * @return the list of documented speakers
     */
    public List<String> getDocumentedSpeakers(String fileName) {
        List<String> speakers = new ArrayList<>();
        if (criteria.getSessions() != null) {
            for (RefcoCriteria.Session s : criteria.getSessions()) {
                if (fileName.equals("any") || Arrays.stream(s.fileNames.split(valueSeparator)).anyMatch(m -> m.contains(fileName))) {
                    speakers.addAll(Arrays.asList(s.speakerNames.split(valueSeparator)));
                }
            }
        }
        return speakers;
    }

    /***
     * Gets the list of all documented files
     * @return the list of documented files
     */
    public List<String> getDocumentedFiles() {
        List<String> files = new ArrayList<>();
        if (criteria.getSessions() != null) {
            for (RefcoCriteria.Session s : criteria.getSessions()) {
                files.addAll(Arrays.asList(s.fileNames.split(valueSeparator)));
            }
        }
        return files;
    }

    /***
     * Adds a warning to the report and optionally adds a location
     * @param cd the current corpus data
     * @param tier the current tier
     * @param item the item to be located
     * @param message the warning message
     * @param hint the hint
     */
    private void addWarningWithLocation(CorpusData cd, String tier, String item, String message, String hint) {
        try {
            if (skipLocations) {
                report.addWarning(getFunction(), ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.HowToFix},
                        new Object[]{getFunction(), cd.getFilename(),
                                message,
                                hint
                        }));
            }
            else {
                for (CorpusData.Location l : getLocations((ELANData) cd, Collections.singletonList(tier), item)) {
                    report.addWarning(getFunction(), ReportItem.newParamMap(
                            new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.Tier, ReportItem.Field.Segment},
                            new Object[]{getFunction(), cd.getFilename(),
                                    message,
                                    hint,
                                    l.tier, l.segment
                            }));
                }
            }
        } catch (Exception e) {
            report.addCritical(getFunction(), ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.Exception},
                    new Object[]{getFunction(), cd.getFilename(), "Corpus data: Exception when trying to " +
                            "locate token " + item,
                            e}));
        }
    }
}
