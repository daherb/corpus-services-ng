/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.junit.*;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Unit test for the RefcoChecker class
 *
 * @author bba1792 Dr. Herbert Lange
 * @version 20210720
 */
public class RefcoCheckerTest {

    Logger logger = Logger.getLogger(this.getClass().getName());

    // The refco corpus documentation file in the resources folder
    private static String resourcePath = "src/test/java/de/uni_hamburg/corpora/validation/quest/resources/";
    private static File refcoODS = new File(resourcePath + "CorpusDocumentation_nisv1234_JocelynAznar_Nisvai.ods");
    private static File refcoFODS = new File(resourcePath + "CorpusDocumentation_nisv1234_JocelynAznar_Nisvai.fods");
    private static File refcoEAF = new File(resourcePath + "T1_15-12-2013_Levetbao_Aven_Waet-Masta_1089.eaf");

    private static RefcoChecker rc;

    private Document ODSDOM ;

    private Document ELANDOM ;

    private final String cellXPath =
            "//table:table-row[table:table-cell[text:p=\"%s\"]]/table:table-cell[position()=%d]/text:p";


    public RefcoCheckerTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws NoSuchMethodException, IOException, JexmaraldaException, URISyntaxException, ClassNotFoundException, SAXException, InvocationTargetException, IllegalAccessException {
        //ClassInfo.printMethods(RefcoChecker.class);
        Properties props = new Properties();
        props.setProperty("refco-file", refcoODS.toString());
        rc = new RefcoChecker(props);
        Method setRefcoCorpusMethod = rc.getClass().getDeclaredMethod("setRefcoCorpus", Corpus.class);
        setRefcoCorpusMethod.setAccessible(true);
        // Read corpus
        CorpusIO cio = new CorpusIO();
        URL corpusUrl = new File(resourcePath).toURI().toURL();
        setRefcoCorpusMethod.invoke(rc, new Corpus("refcoTest", corpusUrl,
                cio.read(corpusUrl)));
    }

    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        /* try {
            String ODSXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<office:document xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\""
                    + " xmlns:calcext=\"urn:org:documentfoundation:names:experimental:calc:xmlns:calcext:1.0\""
                    + " xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\""
                    + " xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\">"
                    + "<table:table-row table:style-name=\"ro1\">"
                    + "<table:table-cell table:style-name=\"ce21\" office:value-type=\"string\" calcext:value-type=\"string\">"
                    + "<text:p>Translation language(s)</text:p>"
                    + "</table:table-cell>"
                    + "<table:table-cell table:style-name=\"ce26\" office:value-type=\"string\" calcext:value-type=\"string\">"
                    + "<text:p>French</text:p>"
                    + "</table:table-cell>"
                    + "<table:table-cell table:style-name=\"ce21\" office:value-type=\"string\" calcext:value-type=\"string\">"
                    + "<text:p>Some notes</text:p></table:table-cell>"
                    + "<table:table-cell table:style-name=\"ce20\" table:number-columns-repeated=\"4\"/>"
                    + "<table:table-cell table:number-columns-repeated=\"1017\"/>"
                    + "</table:table-row>"
                    + "</office:document>";
            String ELANXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<ANNOTATION_DOCUMENT AUTHOR=\"Jocelyn Aznar\""
                    + " DATE=\"2021-07-09T14:34:06+01:00\" FORMAT=\"3.0\" VERSION=\"3.0\""
                    + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.mpi.nl/tools/elan/EAFv3.0.xsd\">"
                    + "<TIER ANNOTATOR=\"Jocelyn Aznar\" LINGUISTIC_TYPE_REF=\"Transcription\""
                    + " PARTICIPANT=\"Aven\" TIER_ID=\"Aven\">"
                    + "<ANNOTATION>"
                    + "<ALIGNABLE_ANNOTATION ANNOTATION_ID=\"a1\""
                    + " TIME_SLOT_REF1=\"ts1\" TIME_SLOT_REF2=\"ts2\">"
                    + "<ANNOTATION_VALUE>Ga=vu moq?</ANNOTATION_VALUE>"
                    + "</ALIGNABLE_ANNOTATION>"
                    + "</ANNOTATION>"
                    + "<ANNOTATION>"
                    + "<ALIGNABLE_ANNOTATION ANNOTATION_ID=\"a2\""
                    + " TIME_SLOT_REF1=\"ts3\" TIME_SLOT_REF2=\"ts4\">"
                    + "<ANNOTATION_VALUE>Ok, na=tog na=kal nstori avyn-i, visgy-n nahre ari,</ANNOTATION_VALUE>"
                    + "</ALIGNABLE_ANNOTATION>"
                    + "</ANNOTATION>"
                    + "<ANNOTATION>"
                    + "<ALIGNABLE_ANNOTATION ANNOTATION_ID=\"a4\""
                    + " TIME_SLOT_REF1=\"ts5\" TIME_SLOT_REF2=\"ts6\">"
                    + "<ANNOTATION_VALUE>avyn dara=sob roh qarmao,</ANNOTATION_VALUE>"
                    + "</ALIGNABLE_ANNOTATION>"
                    + "</ANNOTATION>"
                    + "</TIER>"
                    + "<TIER ANNOTATOR=\"Jocelyn Aznar\" LINGUISTIC_TYPE_REF=\"Morphologie\""
                    + " PARENT_REF=\"Aven\" PARTICIPANT=\"Aven\" TIER_ID=\"Morphologie\">"
                    + "<ANNOTATION>"
                    + "<REF_ANNOTATION ANNOTATION_ID=\"a222\" ANNOTATION_REF=\"a1\">"
                    + "<ANNOTATION_VALUE>3SG=bon ASP.F</ANNOTATION_VALUE>"
                    + "</REF_ANNOTATION>"
                    + "</ANNOTATION>"
                    + "<ANNOTATION>"
                    + "<REF_ANNOTATION ANNOTATION_ID=\"a223\" ANNOTATION_REF=\"a2\">"
                    + "<ANNOTATION_VALUE>ok 1SG=vouloir 1SG=parler stori DEF-INTR OBJ-PI.3SG enfant PL</ANNOTATION_VALUE>"
                    + "</REF_ANNOTATION>"
                    + "</ANNOTATION>"
                    + "<ANNOTATION>"
                    + "<REF_ANNOTATION ANNOTATION_ID=\"a225\" ANNOTATION_REF=\"a4\">"
                    + "<ANNOTATION_VALUE>COO.DEF 1INCL=assoir ASP.T aujourd'hui</ANNOTATION_VALUE>"
                    + "</REF_ANNOTATION>"
                    + "</ANNOTATION>"
                    + "</TIER>"
                    + "</ANNOTATION_DOCUMENT>" ;
            RefcoChecker rc = new RefcoChecker(new Properties());
//            ODSDOM = new SAXBuilder().build(new StringReader(ODSXML));
            ELANDOM = new SAXBuilder().build(new StringReader(ELANXML)) ;
            props = new Properties();
        } catch (JDOMException | IOException e) {
            fail("Unexpected exception: " + e + "\ncaused by " + e.getCause()) ;
        }*/
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Checks if the corpus documentation file which we need for the tests exists.
     * This is a meta test that makes sure that the other tests can be run successfully
     */
    @Test
    public void corpusDocumentationExists() {
        assertTrue("The refco documentation ods spreadsheet does not exist",
                refcoODS.exists());
        assertTrue("The refco documentation fods spreadsheet does not exist",
                refcoFODS.exists());
    }

    /**
     * Test the constructor
     *
     * The constructor does:
     * - read a list of iso language codes, either from resource or from file
     * - load the refco-file given in the properties or produce an error in the reports
     */
    @Test
    public void testConstructor() throws NoSuchFieldException, IllegalAccessException {
        // Start with empty properties
        Properties props = new Properties();
        // Create object with empty properties
        RefcoChecker rc = new RefcoChecker(props);
        // In any case, the iso list should be loaded properly
        Field isoListField = rc.getClass().getDeclaredField("isoList");
        isoListField.setAccessible(true);
        List<String> isoList = (List<String>) isoListField.get(rc);
        assertNotNull("The ISO list is null", isoList);
        assertFalse("The ISO list is empty", isoList.isEmpty());
        // We check that we actually have an error because we couldn't load the file
        assertEquals("Unexpected error number",
                1, rc.getReport().getErrorStatistics().size());
        assertEquals("Unxpected error message",": Missing corpus documentation file property. No " +
                "known fixes. ", rc.getReport().getErrorStatistics().get(0).toString());
        assertEquals("Not all report items are error items", rc.getReport().getErrorStatistics().size(),
                rc.getReport().getRawStatistics().size());
        // We check with proper refco file
        props.setProperty("refco-file", refcoODS.toString());
        rc = new RefcoChecker(props);
        assertEquals("Unexpected errors", 0, rc.getReport().getErrorStatistics().size());
        assertEquals("Unexpected report items", 0, rc.getReport().getRawStatistics().size());
        // Now we check if we get the same criteria if we use either the ODS or the FODS file
        RefcoChecker.RefcoCriteria criteriaODS = rc.getCriteria();
        props.setProperty("refco-file", refcoFODS.toString());
        rc = new RefcoChecker(props);
        RefcoChecker.RefcoCriteria criteriaFODS = rc.getCriteria();
        assertEquals("The criteria are not the same", criteriaODS,criteriaFODS);
    }

    /**
     * Test for "public Report getReport()"
     */
    @Test
    public void getReportTest() {
        RefcoChecker rc = new RefcoChecker(new Properties());
        Report report = rc.getReport();
        assertNotNull("Report is null", report);
        assertFalse("Report is empty", report.getRawStatistics().isEmpty());
        assertEquals("Unexpected report",
                "All reports\nRefcoChecker:\n: Missing corpus documentation file property. No known fixes. \n",
                report.getFullReports());
    }

    /**
     *  Test for "public String getDescription()"
     */
    @Test
    public void getDescriptionTest() {
        RefcoChecker rc = new RefcoChecker(new Properties());
        String description = rc.getDescription();
        assertNotNull("Description is null", description);
        assertFalse("Description is empty", description.isEmpty());
        assertEquals("Unexpected description",
                "checks the RefCo criteria for a corpus. Requires a RefCo corpus documentation spreadsheet.",
                description);
    }

//public Report function(CorpusData arg0,Boolean arg1)
//public Report function(Corpus arg0,Boolean arg1)

    /**
     * Test for "public Collection<Class<? extends CorpusData>> getIsUsableFor()"
     */
    @Test
    public void getIsUsableForTest() {
        RefcoChecker rc = new RefcoChecker(new Properties());
        Collection<Class<? extends CorpusData>> usableFor = rc.getIsUsableFor();
        assertNotNull("UsableFor is null", usableFor);
        assertFalse("UsableFor is empty", usableFor.isEmpty());
        // Usable for ELAN
        assertTrue("Unexpected UsableFor ELAN",usableFor.contains(ELANData.class));
    }

//public static void main(String[] arg0)


    /**
     * Test for "public Report setRefcoFile(String arg0)"
     */
    @Test
    public void setRefcoFileTest() throws NoSuchFieldException, IllegalAccessException, IOException {
        RefcoChecker rc = new RefcoChecker(new Properties());
        Report report = rc.setRefcoFile(refcoODS.toString());
        // Prepare access to the fields using reflections
        Field refcoFileNameField = rc.getClass().getDeclaredField("refcoFileName");
        Field refcoShortNameField = rc.getClass().getDeclaredField("refcoShortName");
        Field refcoDocField = rc.getClass().getDeclaredField("refcoDoc");
        refcoFileNameField.setAccessible(true);
        refcoShortNameField.setAccessible(true);
        refcoDocField.setAccessible(true);
        // Access the fields
        String refcoFileName = (String) refcoFileNameField.get(rc);
        String refcoShortName = (String) refcoShortNameField.get(rc);
        Document refcoDoc = (Document) refcoDocField.get(rc);
        // All fields are not null
        assertNotNull("refcoFileName is null", refcoFileName);
        assertNotNull("refcoShortName is null", refcoShortName);
        assertNotNull("refcoDoc is null", refcoDoc);
        // Strings are not empty
        assertFalse("refcoFileName is empty", refcoFileName.isEmpty());
        assertFalse("refcoShortName is empty", refcoShortName.isEmpty());
        // Values are expected
        assertEquals("Unexpected refcoFileName", refcoFileName, refcoODS.toString());
        assertEquals("Unexpected refcoShortName", refcoShortName, refcoODS.getName());
        assertEquals("Unexpected root in refcoDoc", "document-content", refcoDoc.getRootElement().getName());
        // Error if file has wrong extension
        // Copy to new file
        File wrongSuffixFile = new File(refcoODS.toString() + ".foo");
        Files.copy(refcoODS.toPath(),wrongSuffixFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        report = rc.setRefcoFile(wrongSuffixFile.toString());
        // Cleanup
        assertTrue("Problem removing temp file", wrongSuffixFile.delete());
        assertTrue("Expected critical item about OBS/FOBS missing", report.getErrorReports().contains(
                "General: Spreadsheet is neither an ODS nor FODS file"));
        // Warning if the file name does not match the schema
        // Copy to new file
        File wrongSchemaFile = new File(resourcePath + "foobar.ods");
        Files.copy(refcoODS.toPath(),wrongSchemaFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        report = rc.setRefcoFile(wrongSchemaFile.toString());
        // Cleanup
        assertTrue("Problem removing temp file", wrongSchemaFile.delete());
        assertTrue("Expected warning item about filename not matching schema missing",
                report.getWarningReports().contains(
                "General: Filename does not match schema"));
        // Warning if the file name does not match the schema
        // Copy to new file
        File wrongLangFile = new File(resourcePath + "CorpusDocumentation_foob1234_JocelynAznar_Nisvai.ods");
        Files.copy(refcoODS.toPath(),wrongLangFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        report = rc.setRefcoFile(wrongLangFile.toString());
        // Cleanup
        assertTrue("Problem removing temp file", wrongLangFile.delete());
        assertTrue("Expected warning item about invalid language missing", report.getWarningReports().contains(
                "General: Language given in filename not valid Glottocode"));
    }

    /**
     * Test for "private void expandTableCells(Document arg0)"
     */
    @Test
    public void expandTableCellsTest() throws NoSuchMethodException, IOException, JDOMException,
            InvocationTargetException, IllegalAccessException {
        RefcoChecker rc = new RefcoChecker(new Properties());
        SAXBuilder saxBuilder = new SAXBuilder();
        Document spreadsheet = saxBuilder.build(refcoFODS);
        // Get all expandable row and column nodes
        List nodes =
                XPath.newInstance("//table:table-cell[@table:number-columns-repeated]").selectNodes(spreadsheet);
        nodes.addAll(XPath.newInstance("//table:table-row[@table:number-rows-repeated]")
                .selectNodes(spreadsheet));
        assertFalse("No expandable nodes", nodes.isEmpty());
        // Prepare call using reflections
        Method expandTableCellsMethod = rc.getClass().getDeclaredMethod("expandTableCells", Document.class);
        expandTableCellsMethod.setAccessible(true);
        // Call method
        expandTableCellsMethod.invoke(rc, spreadsheet);
        nodes = XPath.newInstance("//table:table-cell[@table:number-columns-repeated]")
                .selectNodes(spreadsheet);
        nodes.addAll(XPath.newInstance("//table:table-row[@table:number-rows-repeated]")
                .selectNodes(spreadsheet));
        assertTrue("Still expandable nodes after expansion", nodes.isEmpty());
    }

    /**
     * Test for "private void removeEmptyCells(Document arg0)"
     */
    @Test
    public void removeEmptyCellsTest() throws IOException, JDOMException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        RefcoChecker rc = new RefcoChecker(new Properties());
        SAXBuilder saxBuilder = new SAXBuilder();
        Document spreadsheet = saxBuilder.build(refcoFODS);
        // Get all removable nodes
        List nodes =
                XPath.newInstance("//table:table-cell[not(text:p) and position() = last()]").selectNodes(spreadsheet);
        assertFalse("No empty nodes", nodes.isEmpty());
        // Prepare call using reflections
        Method removeEmptyCellsMethod = rc.getClass().getDeclaredMethod("removeEmptyCells", Document.class);
        removeEmptyCellsMethod.setAccessible(true);
        // Call method
        removeEmptyCellsMethod.invoke(rc, spreadsheet);
        nodes =
                XPath.newInstance("//table:table-cell[not(text:p) and position() = last()]").selectNodes(spreadsheet);
        assertTrue("Still empty nodes after removal", nodes.isEmpty());
    }

    /**
     * Test for "private String getCellText(String arg0,Element arg1,String arg2)"
     */
    @Test
    public void getCellTextTest() throws NoSuchMethodException, IOException, JDOMException, InvocationTargetException
            , IllegalAccessException {
        RefcoChecker rc = new RefcoChecker(new Properties());
        SAXBuilder saxBuilder = new SAXBuilder();
        Document spreadsheet = saxBuilder.build(refcoFODS);
        // Prepare call using reflections
        Method getCellTextMethod = rc.getClass().getDeclaredMethod("getCellText", String.class, Element.class, String.class);
        getCellTextMethod.setAccessible(true);
        // Call method
        String corpusTitle = (String) getCellTextMethod.invoke(rc,
                "//table:table-row[table:table-cell[text:p=\"%s\"]]/table:table-cell[position()=%d]/text:p",
                spreadsheet.getRootElement(), "Corpus Title");
        assertNotNull("Text is null", corpusTitle);
        assertFalse("Text is not empty", corpusTitle.isEmpty());
        assertEquals("Unexpected text", "Corpus de narrations nisvaies", corpusTitle);
        String wrong_title = (String) getCellTextMethod.invoke(rc,
                "//table:table-row[table:table-cell[text:p=\"%s\"]]/table:table-cell[position()=%d]/text:p",
                spreadsheet.getRootElement(), "Wrong Title");
        assertTrue("Text is empty",wrong_title.isEmpty());
    }

    /**
     * Test for "private String safeGetText(Element arg0)"
     */
    @Test
    public void safeGetTextTest() throws IOException, JDOMException, NoSuchMethodException, InvocationTargetException
            , IllegalAccessException {
        RefcoChecker rc = new RefcoChecker(new Properties());
        SAXBuilder saxBuilder = new SAXBuilder();
        Document spreadsheet = saxBuilder.build(refcoFODS);
        // Prepare call using reflections
        Method safeGetTextMethod = rc.getClass().getDeclaredMethod("safeGetText", Element.class);
        safeGetTextMethod.setAccessible(true);
        // Call method
        Element element =
                (Element) XPath.newInstance("//table:table-cell[text:p=\"Corpus Title\"]/text:p").selectSingleNode(spreadsheet);
        // Check existing node with text
        String text = (String) safeGetTextMethod.invoke(rc,element);
        assertNotNull("Text is null for existing node",text);
        assertFalse("Text is empty for existing node", text.isEmpty());
        assertEquals("Unexpected text for existing node","Corpus Title", text);
        // Check node without text
        element =
                (Element) XPath.newInstance("//foobar").selectSingleNode(spreadsheet);
        text = (String) safeGetTextMethod.invoke(rc,element);
        assertNotNull("Text is null for non-existent node",text);
        assertTrue("Text is not empty for non-existent node", text.isEmpty());
        // Check non-existent node
        element =
                (Element) XPath.newInstance("//table:table-cell[not(text:p)]").selectSingleNode(spreadsheet);
        text = (String) safeGetTextMethod.invoke(rc,element);
        assertNotNull("Text is null for node without text",text);
        assertTrue("Text is not empty for node without text", text.isEmpty());
        // Check null node
        text = (String) safeGetTextMethod.invoke(rc,(Element) null);
        assertNotNull("Text is null for null element", text);
        assertEquals("Unexpected text for null element", "", text);
    }

    /**
     * Test for "private String getTextInRow(String arg0,Element arg1,String arg2,int arg3)"
     */
    @Test
    public void getTextInRowTest() throws IOException, JDOMException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        RefcoChecker rc = new RefcoChecker(new Properties());
        SAXBuilder saxBuilder = new SAXBuilder();
        Document spreadsheet = saxBuilder.build(refcoFODS);
        // Prepare call using reflections
        Method getTextInRowMethod = rc.getClass().getDeclaredMethod("getTextInRow",
                String.class, Element.class, String.class, int.class);
        getTextInRowMethod.setAccessible(true);
        // With null path
        String text = (String) getTextInRowMethod.invoke(rc,(String) null, spreadsheet.getRootElement(),"foo",0);
        assertNotNull("Text is null for null path", text);
        assertTrue("Text is not empty for null path", text.isEmpty());
        // Try on a real row
        Element table =
                (Element) XPath.newInstance("//table:table[@table:name='AnnotationTiers']").selectSingleNode(spreadsheet);
        String cellXPath =
                "//table:table-row[table:table-cell[text:p=\"%s\"]]/table:table-cell[position()=%d]/text:p";
        text = (String) getTextInRowMethod.invoke(rc,cellXPath, table, "Morphologie",-1);
        assertNotNull("Text is null for negative index", text);
        assertTrue("Text is not empty for negative index", text.isEmpty());
        text = (String) getTextInRowMethod.invoke(rc,cellXPath, table, "Morphologie",5);
        assertNotNull("Text is null for valid index", text);
        assertFalse("Text is empty for valid index", text.isEmpty());
        assertEquals("Unexpected text for valid index", "UpperGramLowerLex", text);
        text = (String) getTextInRowMethod.invoke(rc,cellXPath, table, "Morphologie",20);
        assertNotNull("Text is null for index out of bounds", text);
        assertTrue("Text is empty for valid bounds", text.isEmpty());
    }

    /**
     * Test for "private RefcoChecker$InformationNotes getInformationNotes(String arg0,Element arg1,String arg2)"
     */
    @Test
    public void checkGetInformationNotes() throws IOException, JDOMException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        RefcoChecker rc = new RefcoChecker(new Properties());
        SAXBuilder saxBuilder = new SAXBuilder();
        Document spreadsheet = saxBuilder.build(refcoFODS);
        // Prepare call using reflections
        Method getInformationNotesMethod = rc.getClass().getDeclaredMethod("getInformationNotes",
                String.class, Element.class, String.class);
        getInformationNotesMethod.setAccessible(true);
        Element overviewTable =
                (Element) XPath.newInstance("//table:table[@table:name='Overview']").selectSingleNode(spreadsheet);
        String cellXPath =
                "//table:table-row[table:table-cell[text:p=\"%s\"]]/table:table-cell[position()=%d]/text:p";
        RefcoChecker.InformationNotes notes = (RefcoChecker.InformationNotes) getInformationNotesMethod
                .invoke(rc,(Object) null, overviewTable,"Corpus Documentation's Version");
        assertNotNull("Information notes are null for null path",notes);
        assertTrue("Both parts are empty for null path", notes.notes.isEmpty() &&
                notes.information.isEmpty());
        notes = (RefcoChecker.InformationNotes) getInformationNotesMethod
                .invoke(rc,cellXPath, (Object) null,"Corpus Documentation's Version");
        assertNotNull("Information notes are null for null element",notes);
        assertTrue("Both parts are empty for null element", notes.notes.isEmpty() &&
                notes.information.isEmpty());
        notes = (RefcoChecker.InformationNotes) getInformationNotesMethod
                .invoke(rc,cellXPath, overviewTable,"Corpus Documentation's Version");
        assertNotNull("Information notes are null for existing element",notes);
        assertEquals("Unexpected notes for existing element", "Current RefCo version", notes.notes);
        assertEquals("Unexpected information for existing element", "2", notes.information);

    }

    /**
     * Helper to check the error log to not be null or empty and contain exactly one error item
     * @param item the item that was modified
     * @param report the error report
     */
    private void checkReport(String item, Report report) {
        assertNotNull("Report is null for " + item, report);
        assertFalse("Report is empty for " + item, report.getRawStatistics().isEmpty());
        if (report.getRawStatistics().size() > 1)
            logger.info(report.getFullReports());
        assertEquals("Unexpected number of report items for " + item, 1,
                report.getRawStatistics().size());

    }

    /**
     * Test for "private Report readRefcoCriteria(Document arg0)"
     */
    @Test
    public void readRefcoCriteriaTest() throws IOException, JDOMException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        RefcoChecker rc = new RefcoChecker(new Properties());
        SAXBuilder saxBuilder = new SAXBuilder();
        Document spreadsheet = saxBuilder.build(refcoFODS);
        // Cleanup table
        Method expandTableCellsMethod = rc.getClass().getDeclaredMethod("expandTableCells", Document.class);
        expandTableCellsMethod.setAccessible(true);
        expandTableCellsMethod.invoke(rc,spreadsheet);
        Method removeEmptyCellsMethod = rc.getClass().getDeclaredMethod("removeEmptyCells", Document.class);
        removeEmptyCellsMethod.setAccessible(true);
        removeEmptyCellsMethod.invoke(rc,spreadsheet);
        // Prepare call using reflections
        Method readRefcoCriteriaMethod = rc.getClass().getDeclaredMethod("readRefcoCriteria", Document.class);
        readRefcoCriteriaMethod.setAccessible(true);
        // Empty document
        Report report = (Report) readRefcoCriteriaMethod.invoke(rc,new Document(new Element("root")));
        assertNotNull("Report for empty document is null", report);
        assertFalse("Report for empty document is empty", report.getRawStatistics().isEmpty());
        // Real document
        report = (Report) readRefcoCriteriaMethod.invoke(rc,spreadsheet);
        assertNotNull("Report for corpus documentation is null", report);
        assertTrue("Report for corpus documentation is not empty", report.getRawStatistics().isEmpty());
        assertNotNull("Criteria for corpus documentation is null", rc.getCriteria());
    }

    /**
     * Test for "private Report refcoGenericCheck()"
     */
    @Test
    public void refcoGenericCheckTest() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        Method refcoGenericCheckMethod = rc.getClass().getDeclaredMethod("refcoGenericCheck");
        refcoGenericCheckMethod.setAccessible(true);
        // Valid corpus documentation
        Report report = (Report) refcoGenericCheckMethod.invoke(rc);
        assertNotNull("Report is null", report);
        assertTrue("Report is not empty", report.getRawStatistics().isEmpty());
        // Check for all string fields for errors if null or empty
        for (Field f : rc.getCriteria().getClass().getDeclaredFields()) {
            if (f.getType() == String.class) {
                String orig = (String) f.get(rc.getCriteria());
                f.set(rc.getCriteria(),(String) null);
                report = (Report) refcoGenericCheckMethod.invoke(rc);
                checkReport("null " + f.getName(), report);
                f.set(rc.getCriteria(),"");
                report = (Report) refcoGenericCheckMethod.invoke(rc);
                checkReport("empty " + f.getName(), report);
                f.set(rc.getCriteria(),orig);
            }
            else if (f.getType() == RefcoChecker.InformationNotes.class) {
                RefcoChecker.InformationNotes orig = (RefcoChecker.InformationNotes) f.get(rc.getCriteria());
                f.set(rc.getCriteria(),new RefcoChecker.InformationNotes((String) null,orig.notes));
                report = (Report) refcoGenericCheckMethod.invoke(rc);
                checkReport("null "+ f.getName() + "information", report);
                f.set(rc.getCriteria(),new RefcoChecker.InformationNotes("",orig.notes));
                report = (Report) refcoGenericCheckMethod.invoke(rc);
                checkReport("empty "+ f.getName() + "information", report);
                f.set(rc.getCriteria(),orig);
            }
        }
        // More in-depth tests
        // Check subject languages
        {
            String origSubjectLanguages = rc.getCriteria().subjectLanguages;
            rc.getCriteria().subjectLanguages = "ger";
            report = (Report) refcoGenericCheckMethod.invoke(rc);
            assertTrue("non-empty report for valid iso subject language code",
                    report.getRawStatistics().isEmpty());
            rc.getCriteria().subjectLanguages = "nisv1234";
            report = (Report) refcoGenericCheckMethod.invoke(rc);
            assertTrue("non-empty report for valid glottolog subject language code",
                    report.getRawStatistics().isEmpty());
            rc.getCriteria().subjectLanguages = "French";
            report = (Report) refcoGenericCheckMethod.invoke(rc);
            assertTrue("non-empty report for valid known subject language",
                    report.getRawStatistics().isEmpty());
            rc.getCriteria().subjectLanguages = "foo";
            report = (Report) refcoGenericCheckMethod.invoke(rc);
            checkReport("invalid iso subject language code", report);
            rc.getCriteria().subjectLanguages = "foob1234";
            report = (Report) refcoGenericCheckMethod.invoke(rc);
            checkReport("invalid glottolog subject language code", report);
            rc.getCriteria().subjectLanguages = "foobar_subject";
            report = (Report) refcoGenericCheckMethod.invoke(rc);
            checkReport("invalid subject language", report);
            rc.getCriteria().subjectLanguages = origSubjectLanguages;
        }
        // Check persistent identifier
        {
            String origPersistentIdentifier = rc.getCriteria().persistentId;
            rc.getCriteria().persistentId = "foo";
            report = (Report) refcoGenericCheckMethod.invoke(rc);
            checkReport("illegal persistent identifier", report);
            rc.getCriteria().persistentId = origPersistentIdentifier;
        }
        // Check refco version
        {
            String origRefcoVersion = rc.getCriteria().refcoVersion.information;
            rc.getCriteria().refcoVersion.information = "foo";
            report = (Report) refcoGenericCheckMethod.invoke(rc);
            checkReport("illegal refco version", report);
            rc.getCriteria().refcoVersion.information = origRefcoVersion;
        }
        // Check number of sessions
        {
            String origNumberSessions = rc.getCriteria().numberSessions.information;
            rc.getCriteria().numberSessions.information = "foo";
            report = (Report) refcoGenericCheckMethod.invoke(rc);
            checkReport("illegal number of sessions", report);
            rc.getCriteria().numberSessions.information = "42";
            report = (Report) refcoGenericCheckMethod.invoke(rc);
            checkReport("wrong number of sessions", report);
            rc.getCriteria().numberSessions.information = origNumberSessions;
        }
        // Check number of transcribed words
        {
            String origNumberTranscribedWords = rc.getCriteria().numberTranscribedWords.information;
            rc.getCriteria().numberTranscribedWords.information = "foo";
            report = (Report) refcoGenericCheckMethod.invoke(rc);
            checkReport("illegal number of transcribed words", report);
            rc.getCriteria().numberTranscribedWords.information = "42";
            report = (Report) refcoGenericCheckMethod.invoke(rc);
            checkReport("wrong number of transcribed words", report);
            rc.getCriteria().numberTranscribedWords.information = origNumberTranscribedWords;
        }
        // Check number of annotated words
        {
            String origNumberAnnotatedWords = rc.getCriteria().numberAnnotatedWords.information;
            rc.getCriteria().numberAnnotatedWords.information = "foo";
            report = (Report) refcoGenericCheckMethod.invoke(rc);
            checkReport("illegal number of annotated words", report);
            rc.getCriteria().numberAnnotatedWords.information = "42";
            report = (Report) refcoGenericCheckMethod.invoke(rc);
            checkReport("wrong number of annotated words", report);
            rc.getCriteria().numberAnnotatedWords.information = origNumberAnnotatedWords;
        }
        // Check translation languages
        {
            String origTranslationLanguages = rc.getCriteria().translationLanguages.information;
            rc.getCriteria().translationLanguages.information = "ger";
            report = (Report) refcoGenericCheckMethod.invoke(rc);
            assertTrue("non-empty report for valid iso translation language code",
                    report.getRawStatistics().isEmpty());
            rc.getCriteria().translationLanguages.information = "nisv1234";
            report = (Report) refcoGenericCheckMethod.invoke(rc);
            assertTrue("non-empty report for valid glottolog translation language code",
                    report.getRawStatistics().isEmpty());
            rc.getCriteria().translationLanguages.information = "French";
            report = (Report) refcoGenericCheckMethod.invoke(rc);
            assertTrue("non-empty report for valid known translation language",
                    report.getRawStatistics().isEmpty());
            rc.getCriteria().translationLanguages.information = "foo";
            report = (Report) refcoGenericCheckMethod.invoke(rc);
            checkReport("invalid iso translation language code", report);
            rc.getCriteria().translationLanguages.information = "foob1234";
            report = (Report) refcoGenericCheckMethod.invoke(rc);
            checkReport("invalid glottolog translation language code", report);
            rc.getCriteria().translationLanguages.information = "foobar_translation";
            report = (Report) refcoGenericCheckMethod.invoke(rc);
            checkReport("invalid translation language", report);
            rc.getCriteria().translationLanguages.information = origTranslationLanguages;
        }
    }

    /**
     *  Test for "private Report refcoSessionCheck()"
     */
    @Test
    public void refcoSessionCheckTest() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        Method refcoSessionCheckMethod = rc.getClass().getDeclaredMethod("refcoSessionCheck");
        refcoSessionCheckMethod.setAccessible(true);
        // Valid corpus documentation
        Report report = (Report) refcoSessionCheckMethod.invoke(rc);
        assertNotNull("Report is null", report);
        assertTrue("Report is not empty", report.getRawStatistics().isEmpty());
        // Check for all string fields for errors if null or empty
        for (RefcoChecker.Session s : rc.getCriteria().sessions) {
            for (Field f : s.getClass().getDeclaredFields()) {
                if (f.getType() == String.class && f.getName() != "fileName") {
                    String orig = (String) f.get(s);
                    f.set(s, (String) null);
                    report = (Report) refcoSessionCheckMethod.invoke(rc);
                    checkReport("null " + f.getName(), report);
                    f.set(s, "");
                    report = (Report) refcoSessionCheckMethod.invoke(rc);
                    checkReport("empty " + f.getName(), report);
                    f.set(s, orig);
                } else if (f.getType() == RefcoChecker.InformationNotes.class) {
                    RefcoChecker.InformationNotes orig = (RefcoChecker.InformationNotes) f.get(rc.getCriteria());
                    f.set(rc.getCriteria(), new RefcoChecker.InformationNotes((String) null, orig.notes));
                    report = (Report) refcoSessionCheckMethod.invoke(rc);
                    checkReport("null " + f.getName() + " information", report);
                    f.set(rc.getCriteria(), new RefcoChecker.InformationNotes("", orig.notes));
                    report = (Report) refcoSessionCheckMethod.invoke(rc);
                    checkReport("empty " + f.getName() + " information", report);
                    f.set(rc.getCriteria(), orig);
                }
            }
        }
        // More in-depth tests by modifying the first session
        RefcoChecker.Session s = rc.getCriteria().sessions.get(0);
        // Check file name
        {
            String origFileName = s.fileName;
            // Null file name
            s.fileName = null;
            report = (Report) refcoSessionCheckMethod.invoke(rc);
            assertEquals("Report does not contain the expected number of elements for null filename", 2,
                    report.getRawStatistics().size());
            assertTrue("Report does not contain the expected item for null filename",
                    report.getFullReports().contains("Session file names are empty: T1"));
            // Empty file name
            s.fileName = "";
            assertEquals("Report does not contain the expected number of elements for empty filename", 2,
                    report.getRawStatistics().size());
            assertTrue("Report does not contain the expected item for empty filename",
                    report.getFullReports().contains("Session file names are empty: T1"));
            // Just an invalid file name
            s.fileName = "foo";
            report = (Report) refcoSessionCheckMethod.invoke(rc);
            assertEquals("Report does not contain the expected number of elements for invalid filename", 2,
                    report.getRawStatistics().size());
            assertTrue("Report does not contain the expected item for invalid filename",
                    report.getFullReports().contains("File does not exist"));
            //checkReport("invalid file name", report);
            // The invalid file name followed by the original valid one
            s.fileName = "foo," + origFileName;
            report = (Report) refcoSessionCheckMethod.invoke(rc);
            checkReport("invalid and valid file name", report);
            // The original valid file name followed by an invalid one
            s.fileName = origFileName + ",1_15-12-2013_Levetbao_Aven_Waet-Masta_1089.eaf";
            report = (Report) refcoSessionCheckMethod.invoke(rc);
            checkReport("valid and invalid file name", report);
            s.fileName = origFileName;
        }
        // Check speaker age
        {
            String origSpeakerAge = s.speakerAge;
            s.speakerAge = "foo";
            report = (Report) refcoSessionCheckMethod.invoke(rc);
            checkReport("invalid speaker age", report);
            s.speakerAge = "123foo";
            report = (Report) refcoSessionCheckMethod.invoke(rc);
            checkReport("partially invalid speaker age", report);
            s.speakerAge = "1000";
            report = (Report) refcoSessionCheckMethod.invoke(rc);
            checkReport("speaker age out of bounds", report);
            s.speakerAge = origSpeakerAge;
        }
        // Check recording date
        {
            String origRecordingDate = s.recordingDate;
            s.recordingDate = "foo";
            report = (Report) refcoSessionCheckMethod.invoke(rc);
            checkReport("invalid recording date", report);
            s.recordingDate = "9999-99-99";
            report = (Report) refcoSessionCheckMethod.invoke(rc);
            checkReport("recording date out of bounds", report);
            s.recordingDate = origRecordingDate;
        }
    }

    /**
     *  Test for "private Report refcoTierCheck()"
     */
    @Test
    public void refcoTierCheckTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method refcoTierCheckMethod = rc.getClass().getDeclaredMethod("refcoTierCheck");
        refcoTierCheckMethod.setAccessible(true);
        // Valid corpus documentation
        Report report = (Report) refcoTierCheckMethod.invoke(rc);
        assertNotNull("Report is null", report);
        assertTrue("Report is not empty", report.getRawStatistics().isEmpty());
        // Check for all string fields for errors if null or empty
        for (RefcoChecker.Tier t : rc.getCriteria().tiers) {
            for (Field f : t.getClass().getDeclaredFields()) {
                if (f.getType() == String.class
                        // Tier name also triggers not all tiers documented
                        && !f.getName().equals("tierName")
                        // Morpheme distinction is optional for non-morphology tiers
                        && !f.getName().equals("morphemeDistinction")
                ) {
                    String orig = (String) f.get(t);
                    f.set(t, (String) null);
                    report = (Report) refcoTierCheckMethod.invoke(rc);
                    checkReport("null " + f.getName(), report);
                    f.set(t, "");
                    report = (Report) refcoTierCheckMethod.invoke(rc);
                    checkReport("empty " + f.getName(), report);
                    f.set(t, orig);
                }
            }
        }
        // More in-depth tests by modifying the first tier
        // Check tier function
        {
            // Backup original value
            ArrayList<String> origTierFunction =
                    (ArrayList<String>) rc.getCriteria().tiers.get(0).tierFunctions.stream().collect(Collectors.toList());
            // Null tier functions
            rc.getCriteria().tiers.get(0).tierFunctions = null;
            report = (Report) refcoTierCheckMethod.invoke(rc);
            checkReport("null tier functions", report);
            // Empty tier functions
            rc.getCriteria().tiers.get(0).tierFunctions = new ArrayList<>();
            report = (Report) refcoTierCheckMethod.invoke(rc);
            checkReport("empty tier functions", report);
            // Invalid tier function
            rc.getCriteria().tiers.get(0).tierFunctions.add("foobar_tier");
            report = (Report) refcoTierCheckMethod.invoke(rc);
            checkReport("invalid tier function", report);
            // Restore original
            rc.getCriteria().tiers.get(0).tierFunctions = origTierFunction.stream().collect(Collectors.toList());
        }
        // Check languages
        {
            // Backup original value
            String origLanguages = rc.getCriteria().tiers.get(0).languages;
            // Valid iso code
            rc.getCriteria().tiers.get(0).languages = "ger";
            report = (Report) refcoTierCheckMethod.invoke(rc);
            assertTrue("non-empty report for valid iso tier language code",
                    report.getRawStatistics().isEmpty());
            // Valid glottolog
            rc.getCriteria().tiers.get(0).languages = "nisv1234";
            report = (Report) refcoTierCheckMethod.invoke(rc);
            assertTrue("non-empty report for valid glottolog tier language code",
                    report.getRawStatistics().isEmpty());
            // Valid known language
            rc.getCriteria().tiers.get(0).languages = "French";
            report = (Report) refcoTierCheckMethod.invoke(rc);
            assertTrue("non-empty report for valid known tier language", report.getRawStatistics().isEmpty());
            // Invalid languages
            rc.getCriteria().tiers.get(0).languages = "foo";
            report = (Report) refcoTierCheckMethod.invoke(rc);
            checkReport("invalid iso tier language code", report);
            rc.getCriteria().tiers.get(0).languages = "foob1234";
            report = (Report) refcoTierCheckMethod.invoke(rc);
            checkReport("invalid glottolog tier language code", report);
            rc.getCriteria().tiers.get(0).languages = "foobar";
            report = (Report) refcoTierCheckMethod.invoke(rc);
            checkReport("invalid tier language", report);
            // Restore original
            rc.getCriteria().tiers.get(0).languages = origLanguages;

        }
        // Check morpheme distinction
        {
            // Backup original value
            ArrayList<String> origTierFunction =
                    (ArrayList<String>) rc.getCriteria().tiers.get(0).tierFunctions.stream().collect(Collectors.toList());
            String origMorphemeDistinction = rc.getCriteria().tiers.get(0).morphemeDistinction;
            // make sure it is a morpheme tier
            rc.getCriteria().tiers.get(0).tierFunctions = new ArrayList<>(Collections.singleton("morpheme gloss"));
            // null morpheme distinction
            rc.getCriteria().tiers.get(0).morphemeDistinction = null;
            report = (Report) refcoTierCheckMethod.invoke(rc);
            checkReport("null morpheme distinction", report);
            // empty morpheme distinction
            rc.getCriteria().tiers.get(0).morphemeDistinction = "";
            report = (Report) refcoTierCheckMethod.invoke(rc);
            checkReport("null morpheme distinction", report);
            // Restore origingal
            rc.getCriteria().tiers.get(0).tierFunctions = origTierFunction.stream().collect(Collectors.toList());
            rc.getCriteria().tiers.get(0).morphemeDistinction = origMorphemeDistinction;
        }
        // Check all tiers
        {
            ArrayList<RefcoChecker.Tier> origTiers = (ArrayList<RefcoChecker.Tier>)
                    rc.getCriteria().tiers.stream().collect(Collectors.toList());
            rc.getCriteria().tiers.get(0).tierName = null ;
            report = (Report) refcoTierCheckMethod.invoke(rc);
            assertEquals("null tier name does not trigger two errors", 2, report.getRawStatistics().size());
            assertTrue("errors do not contain empty tier name", report.getFullReports().contains("tier name is empty"));
            assertTrue("errors do not contain undocumented tier", report.getFullReports()
                    .contains("Tiers are not documented"));
            rc.getCriteria().tiers.get(0).tierName = "" ;
            report = (Report) refcoTierCheckMethod.invoke(rc);
            assertEquals("empty tier name does not trigger two errors", 2, report.getRawStatistics().size());
            assertTrue("errors do not contain empty tier name for empty tier name", report.getFullReports().contains("tier name is empty"));
            assertTrue("errors do not contain undocumented tier for empty tier name", report.getFullReports()
                    .contains("Tiers are not documented"));
            rc.getCriteria().tiers.remove(0);
            report = (Report) refcoTierCheckMethod.invoke(rc);
            checkReport("missing tier",report);
            assertEquals("", 1, report.getRawStatistics().size());
        }
    }

    /**
     *  Test for "private Report refcoTranscriptionCheck()"
     */
    @Test
    public void refcoTranscriptionCheckTest() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        Method refcoTranscriptionCheckMethod = rc.getClass().getDeclaredMethod("refcoTranscriptionCheck");
        refcoTranscriptionCheckMethod.setAccessible(true);
        // Valid corpus documentation
        Report report = (Report) refcoTranscriptionCheckMethod.invoke(rc);
        assertNotNull("Report is null", report);
        assertTrue("Report is not empty", report.getRawStatistics().isEmpty());
        // Check for all string fields for errors if null or empty
        for (RefcoChecker.Transcription t : rc.getCriteria().transcriptions) {
            for (Field f : t.getClass().getDeclaredFields()) {
                if (f.getType() == String.class) {
                    String orig = (String) f.get(t);
                    f.set(t, (String) null);
                    report = (Report) refcoTranscriptionCheckMethod.invoke(rc);
                    checkReport("null " + f.getName(), report);
                    f.set(t, "");
                    report = (Report) refcoTranscriptionCheckMethod.invoke(rc);
                    checkReport("empty " + f.getName(), report);
                    f.set(t, orig);
                }
            }
        }
    }

    /**
     *  Test for "private Report refcoGlossCheck()"
     */
    @Test
    public void refcoGlossCheckTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method refcoGlossCheckMethod = rc.getClass().getDeclaredMethod("refcoGlossCheck");
        refcoGlossCheckMethod.setAccessible(true);
        // Valid corpus documentation
        Report report = (Report) refcoGlossCheckMethod.invoke(rc);
        assertNotNull("Report is null", report);
        assertTrue("Report is not empty", report.getRawStatistics().isEmpty());
        // Check for all string fields for errors if null or empty
        for (RefcoChecker.Gloss g : rc.getCriteria().glosses) {
            for (Field f : g.getClass().getDeclaredFields()) {
                if (f.getType() == String.class
                        // Comments are optional
                        && !f.getName().equals("comments")) {
                    String orig = (String) f.get(g);
                    f.set(g, (String) null);
                    report = (Report) refcoGlossCheckMethod.invoke(rc);
                    checkReport("null " + f.getName(), report);
                    f.set(g, "");
                    report = (Report) refcoGlossCheckMethod.invoke(rc);
                    checkReport("empty " + f.getName(), report);
                    f.set(g, orig);
                }
            }
        }
        // More in-depth tests
        RefcoChecker.Gloss g = rc.getCriteria().glosses.get(0);
        // Gloss format
        {
            // Backup original
            String origGloss = rc.getCriteria().glosses.get(0).gloss;
            g.gloss = "[hors-texte]";
            report = (Report) refcoGlossCheckMethod.invoke(rc);
            assertEquals("non-empty report for lower case gloss token", 0,
                    report.getRawStatistics().size());
            g.gloss = "FOO-BAR";
            report = (Report) refcoGlossCheckMethod.invoke(rc);
            checkReport("invalid gloss containing separator", report);
            // Restore backup
            rc.getCriteria().glosses.get(0).gloss = origGloss;
        }
        // Valid tiers
        {
            // Backup original
            String origTiers = rc.getCriteria().glosses.get(0).tiers;
            g.tiers = "Morphologie, Aven";
            report = (Report) refcoGlossCheckMethod.invoke(rc);
            assertEquals("non-empty report for valid tiers",0, report.getRawStatistics().size());
            g.tiers = "all";
            report = (Report) refcoGlossCheckMethod.invoke(rc);
            assertEquals("non-empty report for \"all\" tiers",0, report.getRawStatistics().size());
            g.tiers = "Morphologie, foobar";
            report = (Report) refcoGlossCheckMethod.invoke(rc);
            checkReport("invalid tiers for gloss", report);
            // Restore backup
            rc.getCriteria().glosses.get(0).tiers = origTiers;
        }
    }

    /**
     *  Test for "private Report refcoPunctuationCheck()"
     */
    @Test
    public void refcoPunctuationCheckTest() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        Method refcoPunctuationCheckMethod = rc.getClass().getDeclaredMethod("refcoPunctuationCheck");
        refcoPunctuationCheckMethod.setAccessible(true);
        // Valid corpus documentation
        Report report = (Report) refcoPunctuationCheckMethod.invoke(rc);
        assertNotNull("Report is null", report);
        assertTrue("Report is not empty", report.getRawStatistics().isEmpty());
        // Check for all string fields for errors if null or empty
        for (RefcoChecker.Punctuation g : rc.getCriteria().punctuations) {
            for (Field f : g.getClass().getDeclaredFields()) {
                if (f.getType() == String.class
                        // Comments are optional
                        && !f.getName().equals("comments")) {
                    String orig = (String) f.get(g);
                    f.set(g, (String) null);
                    report = (Report) refcoPunctuationCheckMethod.invoke(rc);
                    checkReport("null " + f.getName(), report);
                    f.set(g, "");
                    report = (Report) refcoPunctuationCheckMethod.invoke(rc);
                    checkReport("empty " + f.getName(), report);
                    f.set(g, orig);
                }
            }
        }
        // More in-depth tests
        RefcoChecker.Punctuation p = rc.getCriteria().punctuations.get(0);
        // Valid tiers
        {
            // Backup original
            String origTiers = rc.getCriteria().punctuations.get(0).tiers;
            p.tiers = "Morphologie, Aven";
            report = (Report) refcoPunctuationCheckMethod.invoke(rc);
            assertEquals("non-empty report for valid tiers",0, report.getRawStatistics().size());
            p.tiers = "all";
            report = (Report) refcoPunctuationCheckMethod.invoke(rc);
            assertEquals("non-empty report for \"all\" tiers",0, report.getRawStatistics().size());
            p.tiers = "Morphologie, foobar";
            report = (Report) refcoPunctuationCheckMethod.invoke(rc);
            checkReport("invalid tiers for punctuation", report);
            // Restore backup
            rc.getCriteria().punctuations.get(0).tiers = origTiers;
        }
    }

    /**
     *  Test for "private Report refcoDocumentationCheck()"
     *
     *  Only check for no errors in the report because all other corner cases are already tested above
     */
    @Test
    public void refcoDocumentationCheckTest() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        Method refcoDocumentationCheckMethod = rc.getClass().getDeclaredMethod("refcoDocumentationCheck");
        refcoDocumentationCheckMethod.setAccessible(true);
        // Valid corpus documentation
        Report report = (Report) refcoDocumentationCheckMethod.invoke(rc);
        assertNotNull("Report is null", report);
        assertTrue("Report is not empty", report.getRawStatistics().isEmpty());
    }

    /**
     * Test for "private ArrayList<String> findTranscriptionTiers()"
     */
    @Test
    public void findTranscriptionTiersTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method findTranscriptionTiersMethod = rc.getClass().getDeclaredMethod("findTranscriptionTiers");
        findTranscriptionTiersMethod.setAccessible(true);
        ArrayList<String> tiers = (ArrayList<String>) findTranscriptionTiersMethod.invoke(rc);
        assertFalse("Tier list is empty", tiers.isEmpty());
        assertArrayEquals("Unexpected transcription tiers", new Object[] {"Aven"},
                tiers.toArray());
    }

    /**
     * Test for "private Report checkTranscriptionText(CorpusData arg0,List<Text> arg1,List<String> arg2,Set<String> arg3)"
     */
    @Test
    public void checkTranscriptionTextTest() throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, JDOMException {
        Method checkTranscriptionTextMethod = rc.getClass().getDeclaredMethod("checkTranscriptionText",
                CorpusData.class, String.class, List.class, List.class, Set.class);
        checkTranscriptionTextMethod.setAccessible(true);
        Field refcoCorpusField = rc.getClass().getDeclaredField("refcoCorpus");
        refcoCorpusField.setAccessible(true);
        Corpus refcoCorpus = (Corpus) refcoCorpusField.get(rc);
        // Collect valid data
        // Get the first corpus data file from the corpus
        CorpusData cd = refcoCorpus.getCorpusData().stream().collect(Collectors.toList()).get(0);
        // Get some transcription text and add some test tokens
        List<Text> text = Collections.singletonList(new Text(
                    rc.getTextsInTierByType(((ELANData) cd).getJdom(), "Transcription").get(0).getText()
                            + " BAR [hors-texte]"));
        List<String> chunks = Arrays.asList(new String[]{"Ga", "=", "v", "u", "moq", "?", "B", "AR"});
        Set<String> glosses = new HashSet<>(Arrays.asList(new String[]{"[hors-texte]"}));
        Report report;
        // a working example
        {
            report = (Report) checkTranscriptionTextMethod.invoke(rc, cd, "Transcription", text, chunks, glosses);
            assertEquals("Report does not contain exactly one item", 1,
                    report.getRawStatistics().size());
            assertTrue("Report does not contain the positive notice",
                    report.getFullReports().contains("More than 99 percent of transcription characters are valid"));
        }
        // Warning about non-matched word because all-caps glosses are ignored
        {
            chunks = Arrays.asList(new String[]{"Ga", "=", "v", "u", "moq", "?"});
            glosses = new HashSet<>(Arrays.asList(new String[]{"BAR", "[hors-texte]"}));
            report = (Report) checkTranscriptionTextMethod.invoke(rc, cd, "Transcription", text, chunks, glosses);
            // Only keep warnings in report
            List<ReportItem> warningStatistics =
                    report.getRawStatistics().stream().filter((i) -> (i.getSeverity() == ReportItem.Severity.WARNING))
                            .collect(Collectors.toList());
            String warningReport = warningStatistics.stream().map((i) -> i.toString())
                    .collect(Collectors.joining("\n"));
            assertEquals("Report does not contain one warning", 1, warningStatistics.size());
            assertEquals("Report contains errors", 0, report.getErrorStatistics().size());
            assertTrue("Report does not contain the expected message",
                    warningReport.contains("Transcription token contains invalid character(s)")
                            && warningReport.contains("BAR containing: [BAR]"));
        }
        // Trigger an exception with a broken DOM
        {
            Document origJdom = ((ELANData) cd).getJdom();
            ((ELANData) cd).setJdom(new Document());
            report = (Report) checkTranscriptionTextMethod.invoke(rc, cd, "Transcription", text, chunks, glosses);
            assertEquals("Report contains errors", 1, report.getErrorStatistics().size());
            assertTrue("Report does not contain the expected message", report.getErrorReports()
                    .contains("Exception when trying"));
            ((ELANData) cd).setJdom(origJdom);
        }
        // Valid characters above threshold
        {
         text = Collections.singletonList(new Text(
                    "a"));
            chunks = Arrays.asList(new String[]{"a"});
            glosses = new HashSet<>();
            report = (Report) checkTranscriptionTextMethod.invoke(rc, cd, "Transcription", text, chunks, glosses);
            assertNotEquals("Report is empty for all valid characters", 0,
                    report.getRawStatistics().size());
            assertTrue("Report does not contain expected item for all valid characters",
                    report.getRawStatistics().get(0).toString().contains("More than 99 percent of transcription " +
                            "characters are valid"));
        }
        // Valid characters below threshold
        {
            text = Collections.singletonList(new Text(
                    "a b"));
            chunks = Arrays.asList(new String[]{"a"});
            glosses = new HashSet<>();
            report = (Report) checkTranscriptionTextMethod.invoke(rc, cd, "Transcription", text, chunks, glosses);
            assertNotEquals("Report is empty for too many invalid characters", 0,
                    report.getRawStatistics().size());
            assertTrue("Report does not contain expected item for too many invalid characters",
                    report.getRawStatistics().get(1).toString().contains("Less than 99 percent of transcription " +
                            "characters are valid"));
        }
    }

    /**
     * Test for "private Report checkTranscription(CorpusData arg0)"
     */
    @Test
    public void checkTranscriptionTest() throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, JDOMException {
        Method checkTranscriptionMethod = rc.getClass().getDeclaredMethod("checkTranscription", CorpusData.class);
        checkTranscriptionMethod.setAccessible(true);
        Field refcoCorpusField = rc.getClass().getDeclaredField("refcoCorpus");
        refcoCorpusField.setAccessible(true);
        Corpus refcoCorpus = (Corpus) refcoCorpusField.get(rc);
        CorpusData cd = refcoCorpus.getCorpusData().stream().collect(Collectors.toList()).get(0);
        // Get all transcription tiers from the documentation
        Method  findTranscriptionTiersMethod = rc.getClass().getDeclaredMethod("findTranscriptionTiers");
        findTranscriptionTiersMethod.setAccessible(true);
        ArrayList<String> transcriptionTiers = (ArrayList<String>) findTranscriptionTiersMethod.invoke(rc);
        Report report;
        // Success case
        {
            report = (Report) checkTranscriptionMethod.invoke(rc, cd);
            assertEquals("Report not only contains one item for success", 1,
                    report.getRawStatistics().size());
            assertTrue("Report does not contain the expected item",
                    report.getRawStatistics().stream().collect(Collectors.toList()).get(0).toString()
                            .contains("More than 99 percent of transcription characters are valid"));
        }
        // No documented transcription tier
        {
            // Backup tiers
            ArrayList<RefcoChecker.Tier> origTiers = (ArrayList<RefcoChecker.Tier>)
                    rc.getCriteria().tiers.stream().collect(Collectors.toList());
            rc.getCriteria().tiers =
                    (ArrayList<RefcoChecker.Tier>) rc.getCriteria().tiers.stream().filter((t) ->
                                    !transcriptionTiers.contains(t.tierName))
                            .collect(Collectors.toList());
            report = (Report) checkTranscriptionMethod.invoke(rc, cd);
            assertEquals("Report is empty for no documented transcription tiers", 1,
                    report.getRawStatistics().size());
            assertTrue("Report does not contain expected item for no documented transcription tiers",
                    report.getRawStatistics().get(0).toString().contains("No transcription tiers found"));
            // Restore original
            rc.getCriteria().tiers = origTiers;
        }
        // No defined transcription characters
        {
            ArrayList<RefcoChecker.Transcription> origTranscription =
                    (ArrayList<RefcoChecker.Transcription>) rc.getCriteria().transcriptions.stream().collect(Collectors.toList());
            ArrayList<RefcoChecker.Punctuation> origPunctuation =
                    (ArrayList<RefcoChecker.Punctuation>) rc.getCriteria().punctuations.stream().collect(Collectors.toList());
            rc.getCriteria().transcriptions = new ArrayList<>();
            rc.getCriteria().punctuations = (ArrayList<RefcoChecker.Punctuation>) rc.getCriteria().punctuations.stream()
                    .filter((p) -> Arrays.stream(p.tiers.split(",\\s+"))
                            .filter((t) -> !transcriptionTiers.contains(t)).collect(Collectors.toList()).isEmpty())
                    .collect(Collectors.toList());
            report = (Report) checkTranscriptionMethod.invoke(rc, cd);
            assertEquals("Report is empty for no documented transcription characters", 1,
                    report.getRawStatistics().size());
            assertTrue("Report does not contain expected item for no documented transcription characters",
                    report.getRawStatistics().get(0).toString().contains("No valid transcription " +
                            "characters (graphemes/punctuation) defined"));
            // Restore original
            rc.getCriteria().transcriptions = origTranscription ;
            rc.getCriteria().punctuations = origPunctuation ;
        }
        // TODO
//        // Missing transcription tier
//        {
//            Document origJdom = (Document) ((ELANData) cd).getJdom().clone();
//            // Remove transcription tier
//            for (Element e :
//                    (List<Element>) XPath.newInstance("//TIER[@TIER_ID='Aven']").selectNodes(((ELANData) cd).getJdom())) {
//                e.getParent().removeContent(e);
//            }
//            assertTrue(XPath.newInstance("//TIER[@TIER_ID='Aven']").selectNodes(((ELANData) cd).getJdom()).isEmpty());
//            assertFalse(XPath.newInstance("//TIER[@TIER_ID='Aven']").selectNodes(origJdom).isEmpty());
//            report = (Report) checkTranscriptionMethod.invoke(rc, cd);
//            assertEquals("Rerport is empty for missing transcription tier", 1,
//                    report.getRawStatistics().size());
//            assertTrue("Report does not contain the expected message for missing transcription tier",
//                    report.getRawStatistics().get(0).toString()
//                            .contains("Missing transcription tier: Aven."));
//            ((ELANData) cd).setJdom(origJdom);
//        }
    }

    /**
     * Test for "private Report checkMorphologyGloss(CorpusData arg0,java.util.List<Text> arg1,HashSet<String> arg2)"
     */
    @Test
    public void checkMorphologyGlossTest() throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException, JDOMException, InvocationTargetException {
        Method checkMorphologyGlossMethod = rc.getClass().getDeclaredMethod("checkMorphologyGloss", CorpusData.class, String.class, List.class, HashSet.class);
        checkMorphologyGlossMethod.setAccessible(true);
        Field refcoCorpusField = rc.getClass().getDeclaredField("refcoCorpus");
        refcoCorpusField.setAccessible(true);
        Corpus refcoCorpus = (Corpus) refcoCorpusField.get(rc);
        CorpusData cd = refcoCorpus.getCorpusData().stream().collect(Collectors.toList()).get(0);
        // Get some gloss text and add some test tokens
        List<Text> text = Collections.singletonList(new Text(
                    rc.getTextsInTierByType(((ELANData) cd).getJdom(), "Morphologie").get(0).getText()
                            + " BAR [hors-texte]"));
        Report report = (Report) checkMorphologyGlossMethod.invoke(rc,cd,"Morphologie", text,new HashSet<>());
        //assertTrue(report.getRawStatistics().isEmpty());
        //logger.info(report.getFullReports());
        // TODO ???
    }

    // [...]


    /**
     * Test for "private Report refcoCorpusCheck(CorpusData arg0)"
     */
    @Test
    public void refcoCorpusCheckTest() throws NoSuchMethodException, NoSuchFieldException, InvocationTargetException, IllegalAccessException {
        Method refcoCorpusCheckMethod = rc.getClass().getDeclaredMethod("refcoCorpusCheck", CorpusData.class);
        refcoCorpusCheckMethod.setAccessible(true);
        Field refcoCorpusField = rc.getClass().getDeclaredField("refcoCorpus");
        refcoCorpusField.setAccessible(true);
        Corpus refcoCorpus = (Corpus) refcoCorpusField.get(rc);
        //refcoCorpusCheckMethod.invoke(rc,refcoCorpus.getCorpusData());
        // TODO
    }


    /**
     * Test for "public boolean checkLanguage(String arg0)"
     */
    @Test
    public void checkLanguageTest() {}

    /**
     * Test for "public static boolean checkUrl(String arg0)"
     */
    @Test
    public void checkUrlTest() {}

    /**
     * Test for "List<Text> getTextsInTierByType(Document arg0,String arg1)"
     */
    @Test
    public void getTextsInTierByTypeTest() {}

    /**
     * Test for "public List<Text> getTextsInTierByID(Document arg0,String arg1)"
     */
    @Test
    public void getTextsInTierByIDTest() {}

    /**
     * Test for "private int countWordsInTierByType(String arg0)"
     */
    @Test
    public void countWordsInTierByTypeTest() {}

    /**
     * Test for "private int countTranscribedWords()"
     */
    @Test
    public void countTranscribedWordsTest() {}

    /**
     * Test for "private int countAnnotatedWords()"
     */
    @Test
    public void countAnnotatedWordsTest() {}

    /**
     * Test for "public List<T> listToParamList(Class<? extends T> arg0,List arg1)"
     */
    @Test
    public void listToParamListTest() {}

    /**
     * Test for "public static String showElement(Element arg0)"
     */
    @Test
    public void showElementTest() {}

    /**
     * Test for "public static String showAllText(Element arg0)"
     */
    @Test
    public void showAllTextTest() {}

    /**
     * Test for "private Map<String, Set<String>> getTierIDs()"
     */
    @Test
    public void getTierIDsTest() {}

//private static Object lambda$getTierIDs$13(Object arg0)
//private static Object lambda$showAllText$12(Object arg0)
//private static String lambda$countWordsInTierByType$11(RefcoChecker$Tier arg0)
//private static boolean lambda$countWordsInTierByType$10(String arg0,RefcoChecker$Tier arg1)
//private static Boolean lambda$checkLanguage$9(String arg0,String arg1)
//private static Integer lambda$checkTranscriptionText$8(String arg0,Integer arg1)
//private static Integer lambda$checkMorphologyGloss$7(String arg0,Integer arg1)
//private static Integer lambda$checkMorphologyGloss$6(String arg0,Integer arg1)
//private static String lambda$checkMorphology$5(RefcoChecker$Tier arg0)
//private static boolean lambda$checkMorphology$4(RefcoChecker$Tier arg0)
//private static boolean lambda$refcoGenericCheck$3(String arg0,RefcoChecker$Tier arg1)
//private static String lambda$refcoGenericCheck$2(Map arg0,String arg1)
//private static String lambda$refcoGenericCheck$1(String arg0,String arg1)
//private static boolean lambda$function$0(Collection arg0,CorpusData arg1)


    /**
     * Test for "private Report checkMorphology(CorpusData arg0)"
     */
    @Test
    public void checkMorphologyTest() {}

    /**
     * Test for "public RefcoChecker$RefcoCriteria getCriteria()"
     */
    @Test
    public void getCriteriaTest() {}


    /**
     * Test for "public List<Character> getChars(String arg0)"
     */
    @Test
    public void getCharsTest() {}

//    /**
//     * Test for "RefcoChecker$Location getLocation(ELANData arg0,String arg1)"
//     */
//    @Test
//    public void getLocationTest() throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException, InvocationTargetException {
//        Method getLocationMethod = rc.getClass().getDeclaredMethod("getLocation", ELANData.class, String.class);
//        getLocationMethod.setAccessible(true);
//        Field refcoCorpusField = rc.getClass().getDeclaredField("refcoCorpus");
//        refcoCorpusField.setAccessible(true);
//        Corpus refcoCorpus = (Corpus) refcoCorpusField.get(rc);
//        ELANData cd = (ELANData) refcoCorpus.getCorpusData().stream().collect(Collectors.toList()).get(0);
//        // Null token
//        {
//            RefcoChecker.Location location = (RefcoChecker.Location) getLocationMethod.invoke(rc, cd, (String) null);
//            assertEquals("Location for empty token is unknown",
//                    new RefcoChecker.Location("unknown",""), location);
//        }
//        // Empty token
//        {
//            RefcoChecker.Location location = (RefcoChecker.Location) getLocationMethod.invoke(rc, cd, "");
//            assertEquals("Location for empty token is unknown",
//                    new RefcoChecker.Location("unknown",""), location);
//        }
//        // Invalid token
//        {
//            RefcoChecker.Location location = (RefcoChecker.Location) getLocationMethod.invoke(rc, cd, "foobar");
//            assertEquals("Location for invalid token is unknown",
//                    new RefcoChecker.Location("unknown",""), location);
//        }
//        // Token in first tier
//        {
//            RefcoChecker.Location location = (RefcoChecker.Location) getLocationMethod.invoke(rc, cd, "Ga=vu");
//            assertEquals("Unexpected location for token in first tier",
//                    new RefcoChecker.Location("Tier:Aven","Segment:a1, Time:00:01.738-00:02.300"), location);
//        }
//        // Token in morphology tier
//        {
//            RefcoChecker.Location location = (RefcoChecker.Location) getLocationMethod.invoke(rc, cd, "ARBRE=DEI.P");
//            assertEquals("Unexpected location for token in third tier",
//                    new RefcoChecker.Location("Tier:Aven","Segment:a226, Time:00:11.800-00:12.695"), location);
//        }
//    }

     /**
     * Test for "List<RefcoChecker$Location> getLocations(ELANData arg0,String arg1)"
     */
    @Test
    public void getLocationsTest() throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException,
            InvocationTargetException {
        // TODO still work in progress
        Method getLocationsMethod = rc.getClass().getDeclaredMethod("getLocations", ELANData.class, List.class, String.class);
        getLocationsMethod.setAccessible(true);
        Field refcoCorpusField = rc.getClass().getDeclaredField("refcoCorpus");
        refcoCorpusField.setAccessible(true);
        Corpus refcoCorpus = (Corpus) refcoCorpusField.get(rc);
        ELANData cd = (ELANData) refcoCorpus.getCorpusData().stream().collect(Collectors.toList()).get(0);
        List<String> tierList = new ArrayList<>(Arrays.asList(new String[]{"Aven"}));
        // Null token
        {
            List<RefcoChecker.Location> locations = (List<RefcoChecker.Location>)
                    getLocationsMethod.invoke(rc,cd,tierList,(String) null);
            assertTrue("Locations not empty for null token", locations.isEmpty());
        }
        // Empty token
        {
            List<RefcoChecker.Location> locations = (List<RefcoChecker.Location>)
                    getLocationsMethod.invoke(rc,cd,tierList,"");
            assertTrue("Locations not empty for empty token", locations.isEmpty());
        }
        // Null tier list
        {
            List<RefcoChecker.Location> locations = (List<RefcoChecker.Location>)
                    getLocationsMethod.invoke(rc, cd, null, "Ga=vu");
            assertTrue("Locations not empty for null tier list", locations.isEmpty());
        }
        // Empty tier list
        {
            List<RefcoChecker.Location> locations = (List<RefcoChecker.Location>)
                    getLocationsMethod.invoke(rc, cd, new ArrayList<>(), "Ga=vu");
            assertTrue("Locations not empty for empty tier list", locations.isEmpty());
        }
        // Invalid token in a valid tier
        {
            List<RefcoChecker.Location> locations = (List<RefcoChecker.Location>)
                    getLocationsMethod.invoke(rc, cd, tierList, "foobar");
            assertEquals("Locations not empty for invalid token in valid tier",
                    Collections.singletonList(new RefcoChecker.Location("Unknown", "")), locations);
        }
        // [hors-texte] in Transcription tier
        {
            List<RefcoChecker.Location> expectedLocations = Collections.singletonList(
                            new RefcoChecker.Location("Tier:Aven", "Segment:a1, Time:00:01.738-00:02.300"));
            List<RefcoChecker.Location> locations = (List<RefcoChecker.Location>)
                    getLocationsMethod.invoke(rc, cd, tierList, "[hors-texte]");
            assertEquals("Unexpected locations for [hors-texte] in Transcription tier", expectedLocations, locations);
        }
        // [hors-texte] in Morphology tier
        {
            List<RefcoChecker.Location> expectedLocations = Collections.singletonList(
                    new RefcoChecker.Location("Tier:Morphologie", "Segment:a225, Time:00:10.361-00:11.607"));
            tierList = new ArrayList<>(Arrays.asList(new String[]{"Morphologie"}));
            List<RefcoChecker.Location> locations = (List<RefcoChecker.Location>)
                    getLocationsMethod.invoke(rc, cd, tierList, "[hors-texte]");
            assertEquals("Unexpected locations for [hors-texte] in Morphology tier", expectedLocations, locations);
        }
        // [hors-texte] in both tiers
        {
             List<RefcoChecker.Location> expectedLocations = Arrays.asList(new RefcoChecker.Location[]{
                             new RefcoChecker.Location("Tier:Aven", "Segment:a1, Time:00:01.738-00:02.300"),
                             new RefcoChecker.Location("Tier:Morphologie", "Segment:a225, Time:00:10.361-00:11.607")
                     });
            tierList.add("Aven");
            List<RefcoChecker.Location> locations = (List<RefcoChecker.Location>)
                    getLocationsMethod.invoke(rc, cd, tierList, "[hors-texte]");
            assertEquals("Unexpected locations for [hors-texte] in both tier", expectedLocations, locations);
        }
        // Legal token with several occurrences
        {
            List<RefcoChecker.Location> expectedLocations = new ArrayList<>(Arrays.asList(new RefcoChecker.Location[]{
                    new RefcoChecker.Location("Tier:Aven","Segment:a1, Time:00:01.738-00:02.300"),
                    new RefcoChecker.Location("Tier:Aven","Segment:a125, Time:06:22.285-06:25.179")
            }));
            List<RefcoChecker.Location> locations = (List<RefcoChecker.Location>)
                    getLocationsMethod.invoke(rc, cd, tierList, "Ga=vu");
            assertEquals("Unexpected locations for legal token with several occurrences", expectedLocations, locations);
        }
    }

    /**
     * Test for "public Map<String, String> getParameters()"
     */
    @Test
    public void getParametersTest() {}


    /*@Test
    public void testConstructor() {
        RefcoChecker rc = new RefcoChecker(props);
        try {
            Field isoListField = rc.getClass().getDeclaredField("isoList");
            isoListField.setAccessible(true);
            List<String> isoList = (List<String>) isoListField.get(rc);
            assertNotNull("The list is not null", isoList);
            assertNotEquals("The list is not empty", 0 , isoList.size());

        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Unexpected exception: " + e + "\ncaused by " + e.getCause()) ;
        }

    }
    *//**
     * Test of getDescription method, of class RefcoChecker
     *//*
    @Test
    public void testGetDescription() {
        RefcoChecker rc = new RefcoChecker(props);
        String description = rc.getDescription();
        assertNotNull("Description is not null", description);
    }

    *//**
     * Test of check method, of class RefcoChecker.
     *//*
    @Test
    public void testCheck() throws Exception {
        // TODO
        // requires input files
    }


    *//**
     * Test of getIsUsableFor method, of class RefcoChecker.
     *//*
    @Test
    public void testGetIsUsableFor() {
        System.out.println("getIsUsableFor");
        RefcoChecker instance = new RefcoChecker(props);
        Collection<Class<? extends CorpusData>> result = instance.getIsUsableFor();
        //no null object here
        assertNotNull("The class list is not null", result);
        assertTrue("Checker is usable for ELAN", result.contains(ELANData.class));
    }

    *//**
     * Test of setRefcoFileName method, of class RefcoChecker.
     *//*
    @Test
    public void testSetRefcoFileName() {
        // TODO
        // requires input file
    }

    @Test
    public void testExpandTableCells() {
        RefcoChecker rc = new RefcoChecker(props) ;
        try {
            Document expanded = (Document) ODSDOM.clone();
            // Get private method and make it accessible/public
            Method expandMethod = rc.getClass().getDeclaredMethod("expandTableCells", Document.class);
            expandMethod.setAccessible(true);
            XMLOutputter outputter = new XMLOutputter();
            assertEquals("Before expanding the two xml documents are equal",outputter.outputString(ODSDOM),outputter.outputString(expanded));
            expandMethod.invoke(rc,expanded);
            assertNotEquals("The expanded document is not the same as the original",outputter.outputString(ODSDOM),outputter.outputString(expanded));
            assertEquals("The non-expanded document has 5 cells", 5, XPath.newInstance("//table:table-cell").selectNodes(ODSDOM).size());
            assertEquals("The expanded document has 8 cells", 8, XPath.newInstance("//table:table-cell").selectNodes(expanded).size());
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | JDOMException e) {
            fail("Unexpected exception: " + e + "\ncaused by " + e.getCause()) ;
        }
    }

    *//**
     * Test of getCellText method, of class RefcoChecker.
     *//*
    @Test
    public void testGetCellText() {
        try {
            RefcoChecker rc = new RefcoChecker(props) ;
            // Get private method and make it accessible/public
            Method getCellText = rc.getClass()
                    .getDeclaredMethod("getCellText", String.class, Element.class, String.class);
            getCellText.setAccessible(true);
            String text = (String) getCellText.invoke(rc,cellXPath, ODSDOM.getRootElement(), "Translation language(s)");
            assertEquals("The cell text for \"Translation language(s)\" is \"French\"","French", text);
            text = (String) getCellText.invoke(rc,cellXPath, ODSDOM.getRootElement(), "Invalid value");
            assertEquals("The cell text for \"Invalid value\" is empty","", text);
            Element nullElement = null ;
            text = (String) getCellText.invoke(rc, cellXPath, nullElement, "Random value");
            assertEquals("The cell text for null element is empty","", text);
            try {
                getCellText.invoke(rc, "", ODSDOM.getRootElement(), "Invalid value");
                fail("An exception should have occurred");
            }
            catch (Exception e) {
                assertEquals("A JDOM exception is caused by an empty XPath", JDOMException.class, e.getCause().getClass());
            }
            try {
                getCellText.invoke(rc, "/foo/", ODSDOM.getRootElement(), "Invalid value");
                fail("An exception should have occurred");
            }
            catch (Exception e) {
                assertEquals("A JDOM exception is caused by an invalid XPath", JDOMException.class, e.getCause().getClass());
            }
            text = (String) getCellText.invoke(rc, "//foo", ODSDOM.getRootElement(), "Invalid value");
            assertEquals("The cell text for an XPath that does not return any element is empty", "", text);
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            fail("Unexpected exception: " + e + "\ncaused by " + e.getCause()) ;
        }

    }

    *//**
     * Test of safeGetText method, of class RefcoChecker.
     *//*
    @Test
    public void testSafeGetText() {
        RefcoChecker rc = new RefcoChecker(props);
        try {
            // Get private method and make it accessible/public
            Method safeGetText = rc.getClass().getDeclaredMethod("safeGetText", Element.class);
            safeGetText.setAccessible(true);
            Element textElement = null ;
            String text = (String) safeGetText.invoke(rc,textElement);
            assertEquals("Element is null", "", text) ;
            textElement = new Element("test");
            text = (String) safeGetText.invoke(rc,textElement);
            assertEquals("Element with no text", "", text) ;
            textElement.setText("Test text");
            text = (String) safeGetText.invoke(rc,textElement);
            assertEquals("Element with test text", "Test text", text) ;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            fail("Unexpected exception: " + e + "\ncaused by " + e.getCause()) ;
        }
    }

    *//**
     * Test of getTextInRow method, of class RefcoChecker.
     *//*
    @Test
    public void testGetTextInRow() {
        RefcoChecker rc = new RefcoChecker(props) ;
        try {
            // Get private method and make it accessible/public
            Method getTextInRow = rc.getClass().getDeclaredMethod("getTextInRow", String.class, Element.class,
                    String.class, int.class) ;
            getTextInRow.setAccessible(true) ;
            // The cases with invalid XPaths or null elements are tested above in testGetCellText
            String text = (String) getTextInRow.invoke(rc,cellXPath, ODSDOM.getRootElement(),"Translation language(s)",2);
            assertEquals("The text value next to the cell \"Translation language(s)\"", "French", text);
            text = (String) getTextInRow.invoke(rc,cellXPath, ODSDOM.getRootElement(),"Translation language(s)",1);
            assertEquals("The text value of the cell \"Translation language(s)\"", "Translation language(s)", text);
            text = (String) getTextInRow.invoke(rc,cellXPath, ODSDOM.getRootElement(),"Translation language(s)",3);
            assertEquals("The text value of the next cell", "Some notes", text);
            text = (String) getTextInRow.invoke(rc,cellXPath, ODSDOM.getRootElement(),"Translation language(s)",4);
            assertEquals("The text value of a blank cell is empty", "", text);
            text = (String) getTextInRow.invoke(rc,cellXPath, ODSDOM.getRootElement(),"Translation language(s)",256);
            assertEquals("The text value of a non-existent cell is empty", "", text);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            fail("Unexpected exception: " + e + "\ncaused by " + e.getCause()) ;
        }
    }

    *//**
     * Test of getInformationNotes method, of class RefcoChecker.
     *//*
    @Test
    public void testGetInformationNotes() {
        RefcoChecker rc = new RefcoChecker(props) ;
        try {
            // Get private method and make it accessible/public
            Method getTextInRow = rc.getClass().getDeclaredMethod("getInformationNotes", String.class, Element.class, String.class) ;
            getTextInRow.setAccessible(true) ;
            // The cases with invalid XPaths or null elements are tested above in testGetCellText
            RefcoChecker.InformationNotes infoNotes = (RefcoChecker.InformationNotes)
                    getTextInRow.invoke(rc,cellXPath, ODSDOM.getRootElement(),"Translation language(s)");
            assertEquals("The information for the row containing \"Translation language(s)\"",
                    "French", infoNotes.information);
            assertEquals("The notes for the row containing \"Translation language(s)\"",
                    "Some notes", infoNotes.notes);
            infoNotes = (RefcoChecker.InformationNotes)
                    getTextInRow.invoke(rc,cellXPath, ODSDOM.getRootElement(),"Non-existent row");
            assertEquals("The information for a non-existent row are empty", "", infoNotes.information);
            assertEquals("The notes for a non-existent row are empty", "", infoNotes.notes);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            fail("Unexpected exception: " + e + "\ncaused by " + e.getCause()) ;
        }
    }

    *//**
     * Test of readRefcoCriteria method, of class RefcoChecker.
     *//*
    @Test
    public void testReadRefcoCriteria() {
        // TODO
        // Requires input files
    }

    *//**
     * Test of refcoGenericCheck method, of class RefcoChecker.
     *//*
    @Test
    public void testRefcoGenericCheck() {
        // TODO
        // Requires input files
    }

    *//**
     * Test of refcoCorpusCheck method, of class RefcoChecker.
     *//*
    @Test
    public void testRefcoCorpusCheck() {
        // TODO
        // Requires input files
    }

    *//**
     * Test of checkTranscription method, of class RefcoChecker.
     *//*
    @Test
    public void testCheckTranscription() {
        // TODO
        // Requires input files
    }

    *//*
    checkMorphology
    checkMorphologyGloss
    *//*

    *//**
     * Test of checkTranscriptionText method, of class RefcoChecker.
     *//*
    @Test
    public void testCheckTranscriptionText() {
        RefcoChecker rc = new RefcoChecker(props);
        try {
            Method checkTranscriptionText = rc.getClass().getDeclaredMethod("checkTranscriptionText", CorpusData.class, List.class, List.class, Set.class);
            checkTranscriptionText.setAccessible(true);
            ELANData corpusData = new ELANData();
            Field urlField = corpusData.getClass().getDeclaredField("url") ;
            urlField.setAccessible(true);
            urlField.set(corpusData,new URL("file://./test.eaf"));
            //corpusData.setJdom(ELANDOM);
            List<Text> transcriptionText = rc.getTextsInTierByType(ELANDOM, "Transcription");
            List<String> allCharacters = Arrays.asList("avyn", "stor",
                    "moq", "tog", "kal", "roh", "hre", "sob", "mao", "vis",
                    "Ga", "vu", "na", "Ok", "ar", "gy",
                    "=", "?", ",", "-", "i", "n", ",", "d", "a", "q", "r", "o", "h");

            List<String> moreThan80percent = Arrays.asList("avyn", "stor",
                    "moq", "tog", "kal", "roh", "hre", "sob", "mao", "vis",
                    "Ga", "vu", "na", // "Ok", "ar", "gy",
                    "=", "?", "-", "i", "n", ",", "d", "a", "q", "r", "o", "h");
            List<String> lessThan40percent = Arrays.asList("avyn", "stor",
                    "moq", "tog", "kal", // "roh", "hre", "sob", "mao", "vis",
                    "Ga", "vu", "na", // "Ok", "ar", "gy",
                    "=", "?", "-", "i" //"n", ",", "d", "a", "q", "r", "o", "h"
            );
            Report r = (Report) checkTranscriptionText.invoke(rc,corpusData,transcriptionText,allCharacters,
                    new HashSet<String>());
            Field transcriptionLimitField = rc.getClass().getDeclaredField("transcriptionCharactersValid");
            transcriptionLimitField.setAccessible(true);
            int transcriptionLimit = transcriptionLimitField.getInt(rc);
            assertTrue("Sufficient characters are matched for all valid characters", ReportItem.generatePlainText(r.getRawStatistics(), true)
                    .contains("More than " + transcriptionLimit + " percent of transcription characters are valid"));
            assertEquals("No warnings or errors", 0, r.getRawStatistics().stream()
                    .filter(ReportItem::isBad).count());
            r = (Report) checkTranscriptionText.invoke(rc,corpusData,transcriptionText,moreThan80percent,
                    new HashSet<String>());
            assertTrue("Sufficient characters matched for 80 percent valid characters", ReportItem.generatePlainText(r.getRawStatistics(), true)
                    .contains("More than " + transcriptionLimit + " percent of transcription characters are valid"));
            // "Bad" messages that are not about tokens containing invalid characters
            List<ReportItem> relevantMessages = r.getRawStatistics().stream().filter((ri) -> ri.isBad() &&
                    !ri.toString().contains("Transcription token contains invalid characters")).collect(Collectors.toList());
            assertEquals("No relevant warnings or errors", 0, relevantMessages.size());
            r = (Report) checkTranscriptionText.invoke(rc,corpusData,transcriptionText,lessThan40percent,
                    new HashSet<String>());
            // DEBUG
            //logger.log(Level.INFO,ReportItem.generatePlainText(r.getRawStatistics(),true));
            assertFalse("Sufficient characters matched for 40 percent valid characters", ReportItem.generatePlainText(r.getRawStatistics(), true)
                    .contains("More than 50 percent of transcription characters are valid"));
            // "Bad" messages that are not about tokens containing invalid characters
            relevantMessages = r.getRawStatistics().stream().filter((ri) -> ri.isBad() &&
                    !ri.toString().contains("Transcription token contains invalid characters")).collect(Collectors.toList());
            assertEquals("One relevant warning or error", 1, relevantMessages.size());
            assertTrue("Error is about too many mismatches", relevantMessages.get(0).toString().contains("Less than 50 percent of transcription characters are valid"));
        } catch (NoSuchMethodException | MalformedURLException | IllegalAccessException | NoSuchFieldException | JDOMException | InvocationTargetException e) {
            fail("Unexpected exception: " + e + "\ncaused by " + e.getCause()) ;
        }
    }

    *//**
     * Test of checkLanguage method, of class RefcoChecker.
     *//*
    @Test
    public void testCheckLanguage() {
        RefcoChecker rc = new RefcoChecker(props);
        // Neither ISO-639-3 nor Glottolog
        assertFalse("Invalid language foo", rc.checkLanguage("foobar"));
        // Potential ISO-639-3
        assertFalse("Invalid language foo", rc.checkLanguage("foo"));
        assertTrue("Valid language elo", rc.checkLanguage("elo"));
        // Potential Glottolog
        assertFalse("Invalid language abcd1234", rc.checkLanguage("abcd1234"));
        assertTrue("Valid language nisv1234", rc.checkLanguage("nisv1234"));
    }

    *//**
     * Test of checkUrl method, of class RefcoChecker.
     *//*
    @Test
    public void testCheckUrl() {
        assertTrue("Url known to work", RefcoChecker.checkUrl("https://www.uni-hamburg.de/"));
        assertFalse("Valid Url known to fail", RefcoChecker.checkUrl("https://www.uni-hamburg.de/error"));
        assertFalse("Invalid Url", RefcoChecker.checkUrl("foobar"));
    }

    *//**
     * Test of getTextsInTierByType method, of class RefcoChecker.
     *//*
    @Test
    public void testGetTextsInTierByType() {
        try {
            RefcoChecker rc = new RefcoChecker(props);
            String transcriptionText = rc.getTextsInTierByType(ELANDOM,"Transcription").stream()
                    .map(Text::getText).reduce(String::concat).orElse(null);
            assertNotEquals("The text is not null", null, transcriptionText);
            assertEquals("The expected text",
                    "Ga=vu moq?Ok, na=tog na=kal nstori avyn-i, visgy-n nahre ari,avyn dara=sob roh qarmao,",
                    transcriptionText);
            assertTrue("Non-existent tier gives empty list", rc.getTextsInTierByType(ELANDOM,"Foobar").isEmpty());
            assertTrue("Any tier in empty document gives empty list", rc.getTextsInTierByType(new Document(new Element("root")),"Foobar").isEmpty());
            assertTrue("Any tier in null document gives empty list", rc.getTextsInTierByType(null,"Foobar").isEmpty());
        } catch (JDOMException e) {
             fail("Unexpected exception: " + e + "\ncaused by " + e.getCause()) ;
        }
    }

     *//**
     * Test of getTextsInTierByID method, of class RefcoChecker.
     *//*
    @Test
    public void testGetTextsInTierByID() {
        try {
            RefcoChecker rc = new RefcoChecker(props);
            String transcriptionText = rc.getTextsInTierByID(ELANDOM,"Aven").stream()
                    .map(Text::getText).reduce(String::concat).orElse(null);
            assertNotEquals("The text is not null", null, transcriptionText);
            assertEquals("The expected text",
                    "Ga=vu moq?Ok, na=tog na=kal nstori avyn-i, visgy-n nahre ari,avyn dara=sob roh qarmao,",
                    transcriptionText);
            assertTrue("Non-existent tier gives empty list", rc.getTextsInTierByID(ELANDOM,"Foobar").isEmpty());
            assertTrue("Any tier in empty document gives empty list", rc.getTextsInTierByID(new Document(new Element("root")),"Foobar").isEmpty());
            assertTrue("Any tier in null document gives empty list", rc.getTextsInTierByID(null,"Foobar").isEmpty());
        } catch (JDOMException e) {
             fail("Unexpected exception: " + e + "\ncaused by " + e.getCause()) ;
        }
    }

    *//**
     * Test of countWordsInTierByType method, of class RefcoChecker.
     *//*
    @Test
    public void testCountWordsInTierByType() {
        try {
            RefcoChecker rc = new RefcoChecker(props);
            // Preparation: Update the internal corpus of the Checker object to set it to the ELANDOM
            Field refcoCorpusField = rc.getClass().getDeclaredField("refcoCorpus");
            refcoCorpusField.setAccessible(true);
            ELANData corpusData = new ELANData();
            Field jdomField = corpusData.getClass().getDeclaredField("jdom") ;
            jdomField.setAccessible(true);
            jdomField.set(corpusData,ELANDOM);
            Field parentUrlField = corpusData.getClass().getDeclaredField("parenturl") ;
            parentUrlField.setAccessible(true);
            parentUrlField.set(corpusData,new URL("file://./test.eaf"));
            refcoCorpusField.set(rc,new Corpus("ElanText",new URL("file://./"),Collections.singleton(corpusData)));
            // Test the method
            Method countWordsInTierByType = rc.getClass().getDeclaredMethod("countWordsInTierByType", String.class);
            countWordsInTierByType.setAccessible(true);
            int wordCount = (int) countWordsInTierByType.invoke(rc,"Morphologie");
            assertEquals("The expected count for a valid tier",14, wordCount);
            wordCount = (int) countWordsInTierByType.invoke(rc,"Foobar");
            assertEquals("For an invalid tier the word count is zero",0, wordCount);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | NoSuchFieldException | MalformedURLException | SAXException | JexmaraldaException e) {
             fail("Unexpected exception: " + e + "\ncaused by " + e.getCause()) ;
        }
    }

    *//**
     * Test of countTranscribedWords method, of class RefcoChecker.
     *//*
    @Test
    public void testCountTranscribedWords() {
        try {
            RefcoChecker rc = new RefcoChecker(props);
            // Preparation: Update the internal corpus of the Checker object to set it to the ELANDOM
            Field refcoCorpusField = rc.getClass().getDeclaredField("refcoCorpus");
            refcoCorpusField.setAccessible(true);
            ELANData corpusData = new ELANData();
            Field jdomField = corpusData.getClass().getDeclaredField("jdom") ;
            jdomField.setAccessible(true);
            jdomField.set(corpusData,ELANDOM);
            Field parentUrlField = corpusData.getClass().getDeclaredField("parenturl") ;
            parentUrlField.setAccessible(true);
            parentUrlField.set(corpusData,new URL("file://./test.eaf"));
            refcoCorpusField.set(rc,new Corpus("ElanText",new URL("file://./"),Collections.singleton(corpusData)));
            // Test the method
            Method countTranscribedWords = rc.getClass().getDeclaredMethod("countTranscribedWords");
            countTranscribedWords.setAccessible(true);
            int wordCount = (int) countTranscribedWords.invoke(rc);
            assertEquals("The expected count for a valid tier",14, wordCount);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | NoSuchFieldException | MalformedURLException | SAXException | JexmaraldaException e) {
             fail("Unexpected exception: " + e + "\ncaused by " + e.getCause()) ;
        }
    }

    *//**
     * Test of listToParamList method, of class RefcoChecker.
     *//*
    @Test
    public void testListToParamList() {
        RefcoChecker rc = new RefcoChecker(props);
        String[] ss = { "x","1",".","a" } ;
        // Get first list from plain array
        ArrayList<String> l = new ArrayList<>(Arrays.asList(ss)) ;
        assertEquals("List as long as the plain array", ss.length,l.size());
        // Generate a Object list by means of streams
        List tmp = Arrays.stream(l.toArray()).collect(Collectors.toList());
        assertEquals("New list as long as the old one", l.size(),tmp.size());
        List<String> ll = rc.listToParamList(String.class, tmp);
        assertEquals("Final list as long as the original one", l.size(), ll.size());
        // Check the presence of all original elements in the new list
        for (String s : l) {
            assertTrue("The element of the original list is also in the new list", ll.contains(s));
        }
    }

    *//**
     * Test of getChar method, of class RefcoChecker.
     *//*
    @Test
    public void testGetChars() {
        String s = "abc" ;
        String c = "." ;
        RefcoChecker rc = new RefcoChecker(props);
        assertEquals("For a short string the char is the same as the string", new Character('.'),
                rc.getChars(c).get(0));
        for (int i = 0; i < s.length(); i++) {
            assertEquals("For a longer string the characters match",
                    new Character(s.charAt(i)), rc.getChars(s).get(i));
            }
    }*/
}
