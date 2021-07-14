package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import net.sf.saxon.trans.SymbolicName;
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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
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

    private Namespace tableNamespace =
            Namespace.getNamespace("table","urn:oasis:names:tc:opendocument:xmlns:table:1.0") ;
    private Namespace textNamespace =
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

    String refcoFileName;

    Document refcoDoc ;

    RefcoCriteria criteria = new RefcoCriteria() ;

    ArrayList<String> isoList = new ArrayList<>() ;

    Corpus refcoCorpus ;

    public RefcoChecker() {
        this(false);
    }

    public RefcoChecker(boolean hasfixingoption) {
        super(hasfixingoption);
        BufferedReader bReader = new BufferedReader(new
                InputStreamReader(getClass().getResourceAsStream("/iso-639-3.tab")));
            String line;
            try {
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
        return refcoCorpusCheck();
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report stats = new Report();
        // Get all usable formats for the checker
        Collection<Class<? extends CorpusData>> usableFormats = this.getIsUsableFor();
        // Get all usable files from the corpus
        Collection<CorpusData> usableFiles = c.getCorpusData().stream().filter((cd) -> usableFormats.contains(cd.getClass())).collect(Collectors.toList());
        refcoCorpus = new Corpus(usableFiles) ;
        stats.merge(refCoGenericCheck());
        // Apply function for each of the supported file
        for (CorpusData cdata : usableFiles) {
            stats.merge(function(cdata, fix));
        }
        return stats;
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
            CorpusIO cio = new CorpusIO();
            try {
                Logger.getLogger(RefcoChecker.class.toString()).log(Level.INFO, "Reading corpus");
                URL url = Paths.get(args[1]).toAbsolutePath().normalize().toUri().toURL();
                Corpus corpus = new Corpus(cio.read(url));
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
        for (Element node : (List<Element>) XPath.newInstance("//table:table-cell[@table:number-columns-repeated]")
                .selectNodes(document)) {
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
            for (Element row : (List<Element>) sessionTable.getChildren("table-row",tableNamespace)) {
                List<Element> columns = row.getChildren("table-cell",tableNamespace);
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
            for (Element row : (List<Element>) tierTable.getChildren("table-row",tableNamespace)) {
                List<Element> columns = row.getChildren("table-cell", tableNamespace);
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
            for (Element row : (List<Element>) transcriptionTable.getChildren("table-row",tableNamespace)) {
                List<Element> columns = row.getChildren("table-cell", tableNamespace);
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
            for (Element row : (List<Element>) glossesTable.getChildren("table-row",tableNamespace)) {
                List<Element> columns = row.getChildren("table-cell", tableNamespace);
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
            for (Element row : (List<Element>) punctuationsTable.getChildren("table-row",tableNamespace)) {
                List<Element> columns = row.getChildren("table-cell", tableNamespace);
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
                report.addCritical(function, "Session file name is empty");
            else {
                // Each cell can contain several files. Split the files and check for each one if it exists
                List<String> filenames = new ArrayList<>(
                    Arrays.asList(s.fileName.split(valueSeparator)));
                for (String f : filenames) {
                    boolean fileExists = new File(f).exists();
                    // If it is an ELAN file, it can/should be in the annotation folder
                    if (f.toLowerCase().endsWith("eaf"))
                        fileExists = fileExists || new File("./Annotations/" + f).exists() || new File("./annotations/" + f).exists();
                    // if it is a wav file, it can should be in the recordings folder
                    else if (f.toLowerCase().endsWith("wav")) {
                        fileExists = fileExists || new File("./Recordings/" + f).exists() || new File("./recordings/" + f).exists();
                    }
                    if (!fileExists)
                        report.addCritical(function,"File does not exist: " + s.fileName);
                }
            }
            if (s.speakerName.isEmpty())
                report.addCritical(function,"Speaker name is empty");
            if (s.speakerAge.isEmpty())
                report.addWarning(function, "Speaker age is empty");
            else if (!s.speakerAge.matches("~?\\d{1,3}"))
                report.addWarning(function,"Speaker age does not match schema: " + s.speakerAge);
            if (s.speakerGender.isEmpty())
                report.addWarning(function,"Speaker gender is empty");
            if (s.recordingLocation.isEmpty())
                report.addCritical(function,"Recording location is empty");
            if (s.recordingDate.isEmpty())
                report.addCritical(function,"Recording date is empty");
            else {
                // Check date by trying to parse it
                SimpleDateFormat df = new SimpleDateFormat("yyyy-mm-dd");
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
                report.addWarning(function,"Tier function is empty");
            if (t.segmentationStrategy.isEmpty())
                report.addWarning(function,"Segmentation strategy is empty");
            if (t.languages.isEmpty())
                report.addCritical(function,"Tier languages is empty");
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
                report.addWarning(function,"Morpheme distinction is empty");
        }
        // Check all transcription graphemes
        for (Transcription t : criteria.transcriptions) {
            if (t.grapheme.isEmpty())
                report.addCritical(function,"Grapheme is empty");
            if (t.linguisticValue.isEmpty())
                report.addCritical(function,"Linguistic value is empty");
            if (t.linguisticConvention.isEmpty())
                report.addWarning(function,"Linguistic convention is empty");
        }
        // Check all glosses
        for (Gloss g : criteria.glosses) {
            if (g.gloss.isEmpty())
                report.addCritical(function,"Gloss is empty");
            if (g.meaning.isEmpty())
                report.addCritical(function,"Gloss meaning is empty");
            // We skip comments assuming it is optional
            if (g.tiers.isEmpty())
                report.addCritical(function,"Tiers is empty");
            else {
                 // Each cell can contain several tiers. Split the tiers and check for each one if it is a
                // valid tier defined in AnnotationTiers
                ArrayList<String> tierList = new ArrayList<>(Arrays.asList(g.tiers.split(valueSeparator)));
                for (String t : tierList) {
                    if (criteria.tiers.stream().filter((tt) -> tt.tierName.equals(t)).toArray().length == 0)
                        report.addCritical(function, "Gloss tier not defined in AnnotationTiers: " + t);
                }
            }
        }
        // Check all punctuation
        for (Punctuation p : criteria.punctuations) {
            if (p.character.isEmpty())
                report.addCritical(function,"Punctuation character is empty");
            if (p.meaning.isEmpty())
                report.addCritical(function,"Character meaning is empty");
            // We skip comments assuming it is optional
            if (p.tiers.isEmpty())
                report.addCritical(function,"Tiers is empty");
            else {
                 // Each cell can contain several tiers. Split the tiers and check for each one if it is a
                // valid tier defined in AnnotationTiers
                ArrayList<String> tierList = new ArrayList<>(Arrays.asList(p.tiers.split(valueSeparator)));
                for (String t : tierList) {
                    if (criteria.tiers.stream().filter((tt) -> tt.tierName.equals(t)).toArray().length == 0)
                        report.addCritical(function, "Gloss tier not defined in AnnotationTiers: " + t);
                }
            }
        }
        return report ;
    }

    private Report refcoCorpusCheck() {
        Report report = new Report() ;

        return report ;
    }

    private boolean checkLanguage(String lang) {
        // ISO code
        if (lang.length() == 3) {
            return isoList.contains(lang);
        }
        else if (lang.matches("\\w{4}\\d{4}")) {
            try {
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
            HttpURLConnection con = (HttpURLConnection) u.openConnection();
            con.setRequestMethod("GET");
            return con.getResponseCode() == 200;
        }
        catch (IOException e) {
            return false ;
        }
    }

    private List<Text> getTextsInTier(String tier) throws JDOMException {
        return XPath.newInstance(
                String.format("//TIER[@LINGUISTIC_TYPE_REF=\"%s\"]//ANNOTATION_VALUE/text()",tier))
                .selectNodes(((ELANData) cd).getJdom());
    }

    private int countWordsInTier(String tier) {
        int count = 0 ;
        for (CorpusData cd : refcoCorpus.getCorpusData()) {
            if (cd.getClass() == ELANData.class) {
                try {
                    // Get all texts from given annotation tier
                    List<Text> texts = getTextsInTier(tier);
                    for (Text t : texts) {
                        // Word separation simply on spaces
                        count += t.getText().split(" ").length;
                    }
                }
                catch (JDOMException e){

                }
            }
        }
        return count ;
    }

    private int countTranscribedWords() {
        return countWordsInTier("Transcription") ;
    }

    private int countAnnotatedWords() {
        // TODO this seems to be problematic
        return countWordsInTier("Morphologie") ;
    }
}
