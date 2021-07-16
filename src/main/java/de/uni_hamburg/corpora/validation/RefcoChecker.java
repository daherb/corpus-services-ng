package de.uni_hamburg.corpora.validation;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.primitives.Chars;
import de.uni_hamburg.corpora.*;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author bba1792 Dr. Herbert Lange
 * @version 20210713
 * The checker for Refco set of criteria.
 */
public class RefcoChecker extends Checker implements CorpusFunction {

    private final Logger logger = Logger.getLogger(this.getClass().toString());

    // Separator used to separate multiple values in a cell
    private final String valueSeparator = "\\s*[,;:]\\s*" ;
    // Separator used to separate words/token
    private final String tokenSeparator = "\\s+" ;

    private final Namespace tableNamespace =
            Namespace.getNamespace("table","urn:oasis:names:tc:opendocument:xmlns:table:1.0") ;
    private final Namespace textNamespace =
            Namespace.getNamespace("text","urn:oasis:names:tc:opendocument:xmlns:text:1.0") ;

    class InformationNotes {
        String information ;
        String notes;
    }
    class Session {
        String sessionName ;
        String fileName ;
        String speakerName ;
        String speakerAge ;
        String speakerGender ;
        String recordingLocation ;
        String recordingDate ;
        String genre ; // is this a controlled vocabulary?
        String ageGroup ; // is this a controlled vocabulary?
    }
    class Tier {
        String tierName ;
        String tierFunction ;
        String segmentationStrategy ;
        String languages ;
        String morphemeDistinction;
    }

    class Transcription {
        String grapheme ;
        String linguisticValue ;
        String linguisticConvention ;
    }

    class Gloss {
        String gloss ;
        String meaning ;
        String comments ;
        String tiers ;
    }

    class Punctuation {
        String character ;
        String meaning ;
        String comments ;
        String tiers ;
    }

    class RefcoCriteria {
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
        InformationNotes translationLanguages ; // TODO: Is this the proper interpretation?
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

    }

    // The filename of the RefCo spreadsheet
    String refcoFileName;

    // The XML DOM of the RefCo spreadsheet
    Document refcoDoc ;

    // The criteria extracted from the Refco spreadsheet
    RefcoCriteria criteria = new RefcoCriteria() ;

    // The list of ISO-639-3 language codes
    ArrayList<String> isoList = new ArrayList<>() ;

    // The complete corpus
    Corpus refcoCorpus ;

    // The frequency list of all transcription tokens in the corpus
    HashMap<String,Integer> tokenFreq = new HashMap<>();

    // The frequency list of all annotation/morphology glosses in the corpus
    HashMap<String,Integer> glossFreq = new HashMap<>();

    public RefcoChecker() {
        this(false);
    }

    public RefcoChecker(boolean hasfixingoption) {
        super(hasfixingoption);
        try {
            InputStream isoStream = getClass().getResourceAsStream("/iso-639-3.tab") ;
            // If the resource is missing try to load it as a file instead
            if (isoStream == null)
                isoStream = new FileInputStream("iso-639-3.tab");
            BufferedReader bReader = new BufferedReader(new
                    InputStreamReader(getClass().getResourceAsStream("/iso-639-3.tab")));
            String line;
            while ((line = bReader.readLine()) != null) {
                    // Skip the header
                    if (!line.startsWith("Id"))
                        isoList.add(line.split("\t")[0]) ;
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
    }

    @Override
    public String getDescription() {
        return "checks the RefCo criteria for a corpus. Requires a filled RefCo spreadsheet.";
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        return refcoCorpusCheck(cd);
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        // Get all usable formats for the checker
        Collection<Class<? extends CorpusData>> usableFormats = this.getIsUsableFor();
        // Get all usable files from the corpus
        Collection<CorpusData> usableFiles = c.getCorpusData().stream().filter((cd) -> usableFormats.contains(cd.getClass())).collect(Collectors.toList());
        refcoCorpus = new Corpus(c.getCorpusName(), c.getBaseDirectory(), usableFiles) ;
        report.merge(refCoGenericCheck());
        // Initialize frequency list for glosses
        for (Gloss gloss : criteria.glosses) {
            glossFreq.put(gloss.gloss,0);
        }
        // Apply function for each of the supported file
        for (CorpusData cdata : usableFiles) {
            report.merge(function(cdata, fix));
        }
        // Check for glosses that never occurred
        for (Map.Entry<String,Integer> e : glossFreq.entrySet()) {
            if (e.getValue() == 0)
                report.addWarning(function,  "Gloss never encountered in corpus: " + e.getKey()) ;
        }
        return report;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() throws ClassNotFoundException {
        HashSet<Class<? extends CorpusData>> classes = new HashSet<>();
        classes.add(ELANData.class);
        return classes ;
    }

    public static void main(String[] args) {
        RefcoChecker rc = new RefcoChecker();
        if (args.length < 2) {
            System.out.println("Usage: RefcoChecker RefcoFile CorpusDirectory");
        }
        else {
            Logger.getLogger(RefcoChecker.class.toString()).log(Level.INFO, "Loading RefCo file");
            rc.setRefcoFileName(args[0]);
            // Safe the Refco criteria to file
            // Generate pretty-printed json
            ObjectMapper mapper = new ObjectMapper();
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
            }
            CorpusIO cio = new CorpusIO();
            try {
                Logger.getLogger(RefcoChecker.class.toString()).log(Level.INFO, "Reading corpus");
                URL url = Paths.get(args[1]).toAbsolutePath().normalize().toUri().toURL();
                Corpus corpus = new Corpus("RefCo", url, cio.read(url));
                Logger.getLogger(RefcoChecker.class.toString()).log(Level.INFO, "Running tests");
                Report report = rc.function(corpus,false);
                Logger.getLogger(RefcoChecker.class.toString()).log(Level.INFO, "Writing report");
                String html = ReportItem.generateDataTableHTML(report.getRawStatistics(),report.getSummaryLines());
                FileWriter fw = new FileWriter("/tmp/refco.html") ;
                fw.write(html) ;
                fw.close() ;
                Logger.getLogger(RefcoChecker.class.toString()).log(Level.INFO, "Done") ;
            } catch (URISyntaxException | IOException | SAXException | JexmaraldaException | ClassNotFoundException | XPathExpressionException | NoSuchAlgorithmException | ParserConfigurationException | JDOMException | FSMException | TransformerException e) {
                e.printStackTrace() ;
            }
        }
    }

    /**
     * Sets the spreadsheet as XML data
     *
     * @param fileName the spreadsheet file name
     */
    public void setRefcoFileName(String fileName) {
        refcoFileName = fileName ;
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        readRefcoCriteria(refcoDoc);
    }

    /**
     * ods file use some kind of run-length compression for multiple blank cells in a row which is problematic for our parser.
     * this functions expands compressed cells
     * @param document the document in which the cells should be expanded
     * @throws JDOMException if the xpath expression is invalid
     */
    private void expandTableCells(Document document) throws JDOMException {
        for (Element node : listToParamList(Element.class, XPath.newInstance("//table:table-cell[@table:number-columns-repeated]")
                .selectNodes(document))) {
            ArrayList<Element> replacement = new ArrayList<>();
            for (int i = 0; i < Integer.parseInt(node.getAttribute("number-columns-repeated", tableNamespace).getValue());
                 i++){
                Element e = (Element) node.clone();
                e.removeAttribute("number-columns-repeated", tableNamespace);
                replacement.add(e);
            }
            node.getParentElement().setContent(node.getParentElement().indexOf(node),replacement);
        }
    }
    /**
     * Gets the value for a cell given by its title, i.e. returns the text of the second cell of a row where
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
     * Safely returns the text from an Element returning an empty string if the element does not exist
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
     * Gets the text from a certain cell in a row given its index
     *
     * @param path the xpath expression to find the cell
     * @param e the root element
     * @param title the text contained in the first cell
     * @param pos the cell in the row
     * @return either the text contained or an empty string
     * @throws JDOMException if the xpath expression is invalid
     */
    private String getTextInRow(String path, Element e, String title, int pos) throws JDOMException {
        Element cell = (Element) XPath.newInstance(String.format(path, title, pos))
                    .selectSingleNode(e);
        return safeGetText(cell);
    }

    /**
     * Gets a pair, the information and the associated notes, from a row
     *
     * @param path the xpath expression to find the cells
     * @param root the root
     * @param title the text contained in the first cell
     * @return an object representing both the information and the associated notes
     * @throws JDOMException if the xpath expression is invalid
     */
    private InformationNotes getInformationNotes(String path, Element root, String title) throws JDOMException {
        InformationNotes version = new InformationNotes();
        version.information = getTextInRow(path, root, title, 2);
        version.notes = getTextInRow(path, root, title, 3);
        return version;
    }

    /**
     * Reads the XML data from the spreadsheet into a java data structure
     * @param refcoDoc the spreadsheet document
     */
    private void readRefcoCriteria(Document refcoDoc) {
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
            for (Element row : listToParamList(Element.class, sessionTable.getChildren("table-row",tableNamespace))) {
                List<Element> columns = listToParamList(Element.class, row.getChildren("table-cell",tableNamespace)) ;
                if (columns.size() > 1 && !safeGetText(columns.get(0).getChild("p", textNamespace)).isEmpty()
                        && !safeGetText(columns.get(0).getChild("p", textNamespace)).equals("Sessions")) {
                    Session session = new Session();
                    session.sessionName = safeGetText(columns.get(0).getChild("p", textNamespace)) ;
                    session.fileName = safeGetText(columns.get(1).getChild("p", textNamespace)) ;
                    session.speakerName = safeGetText(columns.get(2).getChild("p", textNamespace)) ;
                    session.speakerAge = safeGetText(columns.get(3).getChild("p", textNamespace)) ;
                    session.speakerGender = safeGetText(columns.get(4).getChild("p", textNamespace)) ;
                    session.recordingLocation = safeGetText(columns.get(5).getChild("p", textNamespace)) ;
                    session.recordingDate = safeGetText(columns.get(6).getChild("p", textNamespace)) ;
                    session.genre = safeGetText(columns.get(7).getChild("p", textNamespace)) ;
                    session.ageGroup = safeGetText(columns.get(8).getChild("p", textNamespace)) ;
                    criteria.sessions.add(session);
                }
            }
            // Read AnnotationTiers tab
            Element tierTable = (Element) XPath.newInstance("//table:table[@table:name='AnnotationTiers']").selectSingleNode(refcoDoc);
            for (Element row : listToParamList(Element.class, tierTable.getChildren("table-row",tableNamespace))) {
                List<Element> columns = listToParamList(Element.class, row.getChildren("table-cell", tableNamespace)) ;
                if (columns.size() > 1 && !safeGetText(columns.get(0).getChild("p", textNamespace)).isEmpty()
                        && !safeGetText(columns.get(0).getChild("p", textNamespace)).equals("Names")) {
                    Tier tier = new Tier();
                    tier.tierName = safeGetText(columns.get(0).getChild("p", textNamespace)) ;
                    tier.tierFunction = safeGetText(columns.get(1).getChild("p", textNamespace)) ;
                    tier.segmentationStrategy = safeGetText(columns.get(2).getChild("p", textNamespace)) ;
                    tier.languages = safeGetText(columns.get(3).getChild("p", textNamespace)) ;
                    tier.morphemeDistinction = safeGetText(columns.get(4).getChild("p", textNamespace)) ;
                    criteria.tiers.add(tier);
                }
            }
            // Read Transcription tab
            Element transcriptionTable = (Element) XPath.newInstance("//table:table[@table:name='Transcription']").selectSingleNode(refcoDoc);
            for (Element row : listToParamList(Element.class, transcriptionTable.getChildren("table-row",tableNamespace))) {
                List<Element> columns = listToParamList(Element.class, row.getChildren("table-cell", tableNamespace));
                if (columns.size() > 1 && !safeGetText(columns.get(0).getChild("p", textNamespace)).isEmpty()
                        && !safeGetText(columns.get(0).getChild("p", textNamespace)).equals("Graphemes")) {
                    Transcription transcription = new Transcription();
                    transcription.grapheme = safeGetText(columns.get(0).getChild("p", textNamespace)) ;
                    transcription.linguisticValue = safeGetText(columns.get(1).getChild("p", textNamespace)) ;
                    transcription.linguisticConvention = safeGetText(columns.get(2).getChild("p", textNamespace)) ;
                    criteria.transcriptions.add(transcription);
                }
            }
            // Read Glosses tab
            Element glossesTable = (Element) XPath.newInstance("//table:table[@table:name='Glosses']").selectSingleNode(refcoDoc);
            for (Element row : listToParamList(Element.class, glossesTable.getChildren("table-row",tableNamespace))) {
                List<Element> columns = listToParamList(Element.class, row.getChildren("table-cell", tableNamespace));
                if (columns.size() > 1&& !safeGetText(columns.get(0).getChild("p", textNamespace)).isEmpty()
                        && !safeGetText(columns.get(0).getChild("p", textNamespace)).equals("Glosses")) {
                    Gloss gloss = new Gloss();
                    gloss.gloss = safeGetText(columns.get(0).getChild("p", textNamespace)) ;
                    gloss.meaning = safeGetText(columns.get(1).getChild("p", textNamespace)) ;
                    gloss.comments = safeGetText(columns.get(2).getChild("p", textNamespace)) ;
                    gloss.tiers = safeGetText(columns.get(3).getChild("p", textNamespace)) ;
                    criteria.glosses.add(gloss);
                }
            }
            // Read Punctuation tab
            Element punctuationsTable = (Element) XPath.newInstance("//table:table[@table:name='Punctuations']").selectSingleNode(refcoDoc);
            for (Element row : listToParamList(Element.class, punctuationsTable.getChildren("table-row",tableNamespace))) {
                List<Element> columns = listToParamList(Element.class, row.getChildren("table-cell", tableNamespace)) ;
                if (columns.size() > 1 && !safeGetText(columns.get(0).getChild("p", textNamespace)).isEmpty()
                        && !safeGetText(columns.get(0).getChild("p", textNamespace)).equals("Characters")) {
                    Punctuation punctuation = new Punctuation();
                    punctuation.character = safeGetText(columns.get(0).getChild("p", textNamespace)) ;
                    punctuation.meaning = safeGetText(columns.get(1).getChild("p", textNamespace)) ;
                    punctuation.comments = safeGetText(columns.get(2).getChild("p", textNamespace)) ;
                    punctuation.tiers = safeGetText(columns.get(3).getChild("p", textNamespace)) ;
                    criteria.punctuations.add(punctuation) ;
                }
            }
            //System.out.println(criteria.subjectLanguages.stream().reduce((s1,s2) -> s1 + "/" + s2));
        } catch (JDOMException | NullPointerException jdomException) {
            jdomException.printStackTrace();
        }
    }

    private Report refCoGenericCheck() {
        Report report = new Report() ;
        if (!refcoFileName.matches("\\d{8}_\\w+_RefCo-Report.f?ods"))
            report.addWarning(function,"Filename does not match schema yyyymmdd_CorpusName_RefCo-Report.ods/.fods: " + refcoFileName);
        if (criteria.corpusTitle.isEmpty())
            report.addCritical(function, "Corpus title is empty");
        if (criteria.subjectLanguages.isEmpty())
            report.addCritical(function, "Subject languages is empty");
        else {
            // Each cell can contain several languages. Split the languages and check for each one if it is a
            // valid language code
            // TODO check if that is the intended way to separate languages
            ArrayList<String> subjectLangList = new ArrayList<>(Arrays.asList(criteria.subjectLanguages
                    .split(valueSeparator)));
            for (String l : subjectLangList) {
                if (!checkLanguage(l)) {
                    report.addWarning(function, "Language is neither a Glottolog nor a ISO-639-3 language code: " + l);
                }
            }
        }
        if (criteria.archive.isEmpty())
            report.addWarning(function,"Archive name is empty");
        if (criteria.persistentId.isEmpty())
            report.addCritical(function,"Persistent identifier is empty");
        else if (!checkUrl(criteria.persistentId)){
            report.addCritical(function,"Invalid URL");
        }
        if (criteria.annotationLicense.isEmpty())
            report.addWarning(function,"Annotation license is empty");
        if (criteria.recordingLicense.isEmpty())
            report.addWarning(function,"Recording license is empty");
        if (criteria.creatorName.isEmpty())
            report.addCritical(function,"Creator name is empty");
        if (criteria.creatorContact.isEmpty())
            report.addCritical(function,"Creator contact is empty");
        if (criteria.creatorInstitution.isEmpty())
            report.addWarning(function,"Creator institution is empty");
        if (criteria.refcoVersion.information.isEmpty())
            report.addCritical(function, "RefCo version is empty");
        else {
            try {
                // Check the number by trying to parse it
                Integer.parseInt(criteria.refcoVersion.information);
                // TODO do something with this number
            }
            catch (NumberFormatException e) {
                report.addCritical(function, "Refco version is not a number");
            }
        }
        if (criteria.numberSessions.information.isEmpty())
            report.addWarning(function, "Number of sessions is empty");
        else {
            try {
                // Check the number by trying to parse it
                int i = Integer.parseInt(criteria.numberSessions.information);
                // Compare it to the number of rows in the CorpusComposition table
                if (i != criteria.sessions.size())
                    report.addWarning(function, "Number of sessions does not match number of sessions in " +
                            "CorpusComposition");
            }
            catch (NumberFormatException e) {
                report.addWarning(function, "Number of sessions is not a number");
            }
        }
        if (criteria.numberTranscribedWords.information.isEmpty())
            report.addWarning(function, "Number of transcribed words is empty");
        else {
            try {
                // Check the number by trying to parse it
                int i = Integer.parseInt(criteria.numberTranscribedWords.information);
                // Compare it to our own count
                int c = countTranscribedWords();
                if (i==0 || c == 0 || 0.8 < (float)c/i || (float)c/i > 1.2)

                    report.addWarning(function, "Word count is either 0 or more than 20 percent off. Counted " + c +
                            " expected " + i);
            }
            catch (JDOMException e) {
                report.addCritical(function,e ,"Exception encountered when counting words");
            }
            catch (NumberFormatException e) {
                report.addWarning(function, "Number of transcribed words is not a number");
            }
        }
        if (criteria.numberAnnotatedWords.information.isEmpty())
            report.addWarning(function, "Number of annotated words is empty");
        else {
            try {
                // Check the number by trying to parse it
                Integer.parseInt(criteria.numberAnnotatedWords.information);
                // TODO do something with this number
                // The name of the annotation tier does not seem stable
            }
            catch (NumberFormatException e) {
                report.addWarning(function, "Number of annotated words is not a number");
            }
        }
        if (criteria.translationLanguages.information.isEmpty())
            report.addWarning(function,"Translation languages is empty");
        else {
            // Each cell can contain several languages. Split the languages and check for each one if it is a
            // valid language code
            // TODO check if that is the intended way to separate languages
            ArrayList<String> translationLangList = new ArrayList<>(
                    Arrays.asList(criteria.translationLanguages.information.split(valueSeparator)));
            for (String l : translationLangList) {
                if (!checkLanguage(l)) {
                    report.addWarning(function,"Language is neither a Glottolog nor a ISO-639-3 language code: " + l);
                }
            }
        }
        // Check each of the sessions
        for (Session s : criteria.sessions) {
            if (s.speakerName.isEmpty())
                report.addCritical(function, "Session name is empty");
            if (s.fileName.isEmpty())
                report.addCritical(function, "Session file name is empty: " + s.sessionName);
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
                            report.addCritical(function, e,"Exception encountered checking file name: "+ f);
                        }
                    // if it is a wav file, it can should be in the recordings folder
                    else if (f.toLowerCase().endsWith("wav")) {
                        try {
                            fileExists = fileExists
                                    || new File(new URL(refcoCorpus.getBaseDirectory() + "/Recordings/" + f).toURI()).exists()
                                    || new File(new URL(refcoCorpus.getBaseDirectory() + "/recordings/" + f).toURI()).exists();
                        }
                        catch (MalformedURLException | URISyntaxException e) {
                            report.addCritical(function, e,"Exception encountered checking file name: "+ f);
                        }
                    }
                    if (!fileExists)
                        report.addCritical(function,"File does not exist: " + f);
                }
            }
            if (s.speakerName.isEmpty())
                report.addCritical(function,"Speaker name is empty");
            if (s.speakerAge.isEmpty())
                report.addWarning(function, "Speaker age is empty: " + s.speakerName);
            else if (!s.speakerAge.matches("~?\\d{1,3}"))
                report.addWarning(function,"Speaker age does not match schema: " + s.speakerAge);
            if (s.speakerGender.isEmpty())
                report.addWarning(function,"Speaker gender is empty: " + s.speakerName);
            if (s.recordingLocation.isEmpty())
                report.addCritical(function,"Recording location is empty: " + s.speakerName);
            if (s.recordingDate.isEmpty())
                report.addCritical(function,"Recording date is empty: " + s.speakerName);
            else {
                // Check date by trying to parse it
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    df.parse(s.recordingDate);
                } catch (ParseException e) {
                    report.addWarning(function,"Recording date in invalid format. Expected yyyy-mm-dd, got: " + s.recordingDate);
                }
            }
            if (s.genre.isEmpty())
                report.addWarning(function,"Genre is empty");
            if (s.ageGroup.isEmpty())
                report.addWarning(function, "Age group is empty");
        }
        // Check all tiers
        for (Tier t : criteria.tiers) {
            if (t.tierName.isEmpty())
                report.addCritical(function, "Tier name is empty");
            if (t.tierFunction.isEmpty())
                report.addWarning(function,"Tier function is empty: " + t.tierName);
            if (t.segmentationStrategy.isEmpty())
                report.addWarning(function,"Segmentation strategy is empty: " + t.tierName);
            if (t.languages.isEmpty())
                report.addCritical(function,"Tier languages is empty: " + t.tierName);
            else {
                // Each cell can contain several languages. Split the languages and check for each one if it is a
                // valid language code
                // TODO check if that is the intended way to separate languages
                ArrayList<String> tierLangList = new ArrayList<>(Arrays.asList(t.languages.split(valueSeparator)));
                for (String l : tierLangList) {
                    if (!checkLanguage(l)) {
                        report.addWarning(function, "Language is neither a Glottolog nor a ISO-639-3 language code: " + l);
                    }
                }
            }
            if (t.morphemeDistinction.isEmpty())
                report.addWarning(function,"Morpheme distinction is empty: " + t.tierName);
        }
        // Check all transcription graphemes
        for (Transcription t : criteria.transcriptions) {
            if (t.grapheme.isEmpty())
                report.addCritical(function,"Grapheme is empty");
            if (t.grapheme.length() != 1)
                report.addCritical(function,"Grapheme is not a single character: " + t.grapheme);
            if (t.linguisticValue.isEmpty())
                report.addCritical(function,"Grapheme linguistic value is empty: " + t.grapheme);
            if (t.linguisticConvention.isEmpty())
                report.addWarning(function,"Grapheme linguistic convention is empty: " + t.grapheme);
        }
        // Check all glosses
        for (Gloss g : criteria.glosses) {
            if (g.gloss.isEmpty())
                report.addCritical(function,"Gloss is empty");
            if (g.meaning.isEmpty())
                report.addCritical(function,"Gloss meaning is empty: " + g.gloss);
            // We skip comments assuming it is optional
            if (g.tiers.isEmpty())
                report.addCritical(function,"Gloss tiers is empty: " + g.gloss);
            // If the tiers is not "all", check if its valid tiers
            else if (!g.tiers.equalsIgnoreCase("all")) {
                // Each cell can contain several tiers. Split the tiers and check for each one if it is a
                // valid tier defined in AnnotationTiers
                ArrayList<String> tierList = new ArrayList<>(Arrays.asList(g.tiers.split(valueSeparator)));
                for (String t : tierList) {
                    // At least one of the tiers has to be defined in the AnnotationTiers table, otherwise report error
                    if (criteria.tiers.stream().filter((tt) -> tt.tierName.equals(t) || tt.tierFunction.contains(t)).toArray().length == 0)
                        report.addCritical(function, "Gloss tier not defined in AnnotationTiers: " + t);
                }
            }
        }
        // Check all punctuation
        for (Punctuation p : criteria.punctuations) {
            if (p.character.isEmpty())
                report.addCritical(function,"Punctuation character is empty");
            if (p.character.length() != 1)
                report.addCritical(function,"Punctuation is not a single character: " + p.character);
            if (p.meaning.isEmpty())
                report.addCritical(function,"Punctuation meaning is empty: " + p.character);
            // We skip comments assuming it is optional
            if (p.tiers.isEmpty())
                report.addCritical(function,"Punctuation tiers is empty: " + p.character);
            // If the tiers is not "all", check if its valid tiers
            else if (!p.tiers.equalsIgnoreCase("all")) {
                 // Each cell can contain several tiers. Split the tiers and check for each one if it is a
                // valid tier defined in AnnotationTiers
                ArrayList<String> tierList = new ArrayList<>(Arrays.asList(p.tiers.split(valueSeparator)));
                for (String t : tierList) {
                    // At least one of the tiers has to be defined in the AnnotationTiers table, otherwise report error
                    if (criteria.tiers.stream().filter((tt) -> tt.tierName.equals(t) || tt.tierFunction.equals(t)).toArray().length == 0)
                        report.addCritical(function, "Punctuation tier not defined in AnnotationTiers: " + t);
                }
            }
        }
        return report ;
    }

    private Report refcoCorpusCheck(CorpusData cd) {
        Report report = new Report() ;
        // Check for ELAN data
        if (cd instanceof ELANData) {
            // Check the transcription
            try {
                report.merge(checkTranscription(cd));
            }
            catch (JDOMException e) {
                report.addCritical(function, cd,"Exception encountered when reading Transcription tier: " + e);
            }
            // Check the morphology
            try {
                report.merge(checkMorphology(cd));
            }
            catch (JDOMException e) {
                report.addCritical(function, cd,"Exception encountered when reading Transcription tier: " + e);
            }
        }
        else {
            report.addCritical(function, cd,  "Not supported corpus type: " + cd.getClass().getName());
        }
        return report ;
    }

    private Report checkTranscription(CorpusData cd) throws JDOMException {
        Report report = new Report();
        // Get the dom
        Document content = ((ELANData) cd).getJdom();
        // Get all transcription tiers
        ArrayList<String> transcriptionTiers = new ArrayList<>();
        transcriptionTiers.add("Transcription") ; // Add default tier function for transcription
        for (Tier t: criteria.tiers) {
            // Also add all tiers that have transcription as a function
            if (t.tierFunction.equalsIgnoreCase("transcription")) {
                transcriptionTiers.add(t.tierName);
            }
        }
        // Get all transcription graphemes
        Set<Character> validTranscriptionCharacters = new HashSet<>(criteria.transcriptions.size()) ;
        for (Transcription t : criteria.transcriptions) {
            // Add all of the grapheme's characters
            Chars.asList(t.grapheme.toCharArray());
            validTranscriptionCharacters.addAll(getChars(t.grapheme));
        }
        // and punctuation characters
        for (Punctuation p : criteria.punctuations) {
            if (p.tiers.equals("all"))
                // Add all of the punctuation's characters
                validTranscriptionCharacters.addAll(getChars(p.character));
            else {
                for (String t : new ArrayList<>(Arrays.asList(p.tiers.split(valueSeparator)))) {
                    if (transcriptionTiers.contains(t)) {
                        // Add all of the punctuation's characters
                        validTranscriptionCharacters.addAll(getChars(p.character));
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
            report.addCritical(function, cd,  "No transcription tiers found");
            return report;
        }
        if (validTranscriptionCharacters.isEmpty()) {
            report.addWarning(function, cd, "No valid transcription characters (graphemes/punctuation) defined");
            return report;
        }
        if (transcriptionText.isEmpty()) {
            report.addCritical(function, cd,  "No transcribed text found in one of the expected tiers: " + transcriptionTiers.stream().reduce((s1,s2) -> s1 + "," + s2).get());
            return report;
        }
        report.merge(checkTranscriptionText(cd,transcriptionText,validTranscriptionCharacters,validGlosses));
        return report;
    }

    private Report checkMorphology(CorpusData cd) throws JDOMException {
        Report report = new Report();
        // Get the dom
        Document content = ((ELANData) cd).getJdom();
        // Get morphology tiers TODO that is very preliminary
        List<String> morphologyTiers = criteria.tiers.stream()
                .filter((t) -> t.tierName.toLowerCase().startsWith("morpho")
                        || t.tierFunction.toLowerCase().startsWith("morpho"))
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
        // Get the text from all transcription tiers
        List<Text> glossText = new ArrayList<>();
        for (String t : morphologyTiers) {
            glossText.addAll(getTextsInTierByID(content, t));
        }
        // Check if one of the relevant variables is empty and, if yes, skip the transcription test
        if (morphologyTiers.isEmpty()) {
            report.addCritical(function, cd,  "No morphology tiers found");
            return report;
        }
        if (validGlosses.isEmpty()) {
            report.addWarning(function, cd, "No valid glosses defined");
            return report;
        }
        if (glossText.isEmpty()) {
            report.addCritical(function, cd,  "No annotated text found in one of the expected tiers: " + morphologyTiers.stream().reduce((s1,s2) -> s1 + "," + s2).get());
            return report;
        }
        report.merge(checkMorphologyGloss(cd,glossText,validGlosses));
        return report ;
    }

    private Report checkMorphologyGloss(CorpusData cd, List<Text> text, HashSet<String> glosses) {
        Report report = new Report() ;

        // All the tokens that are valid
        int matched = 0;
        // All invalid tokens in the text
        int missing = 0 ;
        // Indicator if a word contains missing characters
        boolean mismatch ;
        for (Text t : text) {
            // Tokenize text
            for (String token : t.getText().split(tokenSeparator)) {
                // Check if token is a gloss
                if (!glosses.contains(token)) {
                    missing += 1 ;
                    // report.addWarning(function,cd,"Invalid token: " + token);
                }
                else {
                    glossFreq.compute(token,(k,v) -> (v == null) ? 1 : v + 1);
                    matched += 1 ;
                }
            }
        }
        float percentValid = (float)matched/(matched+missing) ;
        if (percentValid < 0.2)
            report.addCritical(function, cd,  "Less than 20 percent of tokens are valid glosses. " +
                    "Valid: " + matched + " Invalid: " + missing + " Percentage vald: " +
                    Math.round(percentValid*100)) ;
        else
            report.addNote(function,cd,"More than 20 percent of tokens are valid glosses. " +
                    "Valid: " + matched + " Invalid: " + missing + " Percentage valid: " +
                    Math.round(percentValid*100)) ;
        return report ;
    }

    private Report checkTranscriptionText(CorpusData cd, List<Text> text, Set<Character> chars,
                                          Set<String> glosses) {
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
                    for (char c : token.toCharArray()) {
                        if (chars.contains(c))
                            matched += 1;
                        else {
                            missing += 1;
                            mismatch = true ;
                        }
                    }
                }
                else {
                    // Add the length of the gloss to matched
                    matched += token.length() ;
                }
                if (mismatch)
                    report.addWarning(function,cd,"Transcription token contains invalid characters: " + token);
            }
        }
        float percentValid = (float)matched/(matched+missing) ;
        if (percentValid < 0.5)
            report.addCritical(function, cd,  "Less than 50 percent of transcription characters are valid. " +
                    "Valid: " + matched + " Invalid: " + missing + " Percentage: " +
                    Math.round(percentValid*100)) ;
        else
            report.addNote(function,cd,"More than 50 percent of transcription characters are valid. " +
                    "Valid: " + matched + " Invalid: " + missing + " Percentage: " +
                    Math.round(percentValid*100));
        return report ;
    }

    private boolean checkLanguage(String lang) {
        // ISO code
        if (lang.length() == 3) {
            return isoList.contains(lang);
        }
        // Glottolog
        else if (lang.matches("\\w{4}\\d{4}")) {
            try {
                // Look the languoid up online
                URL url = new URL("https://glottolog.org/resource/languoid/id/" + lang);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                return con.getResponseCode() == 200;
            }
            catch (IOException e) {
                return false ;
            }
        }
        else
            return false ;
    }

    private boolean checkUrl(String url) {
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

    private List<Text> getTextsInTierByType(Document d, String tier) throws JDOMException {
        List texts = XPath.newInstance(
                String.format("//TIER[@LINGUISTIC_TYPE_REF=\"%s\"]//ANNOTATION_VALUE/text()",tier))
                .selectNodes(d);
        return listToParamList(Text.class, texts) ;
    }

    private List<Text> getTextsInTierByID(Document d, String tier) throws JDOMException {
        List texts = XPath.newInstance(
                String.format("//TIER[@TIER_ID=\"%s\"]//ANNOTATION_VALUE/text()",tier))
                .selectNodes(d);
        return listToParamList(Text.class, texts) ;
    }

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

    private int countTranscribedWords() throws JDOMException {
        return countWordsInTierByType("Transcription") ;
    }

    private int countAnnotatedWords() throws JDOMException {
        // TODO this seems to be problematic
        return countWordsInTierByType("Morphologie") ;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> listToParamList(Class<? extends T> p, List l) {
        ArrayList<T> nl = new ArrayList<>(l.size()) ;
        for (Object o : l) {
            if (o.getClass() == p)
                try {
                    nl.add((T) o);
                }
                catch (ClassCastException e) {
                    e.printStackTrace();
                }
        }
        return nl;
    }

    private List<Character> getChars(String s) {
        return Chars.asList(s.toCharArray());
    }
}
