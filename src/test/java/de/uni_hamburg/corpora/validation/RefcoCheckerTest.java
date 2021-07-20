/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.junit.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Unit test for the RefcoChecker class
 *
 * @author bba1792 Dr. Herbert Lange
 * @version 20210720
 */
public class RefcoCheckerTest {

    private Document ODSDOM ;

    private Document ELANDOM ;

    private final String cellXPath =
            "//table:table-row[table:table-cell[text:p=\"%s\"]]/table:table-cell[position()=%d]/text:p";

    public RefcoCheckerTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        try {
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
            ODSDOM = new SAXBuilder().build(new StringReader(ODSXML));
            ELANDOM = new SAXBuilder().build(new StringReader(ELANXML)) ;
        } catch (JDOMException | IOException e) {
            fail("Unexpected exception: " + e + "\ncaused by " + e.getCause()) ;
        }
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testConstructor() {
        RefcoChecker rc = new RefcoChecker();
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
    /**
     * Test of getDescription method, of class RefcoChecker
     */
    @Test
    public void testGetDescription() {
        RefcoChecker rc = new RefcoChecker();
        String description = rc.getDescription();
        assertNotNull("Description is not null", description);
    }

    /**
     * Test of check method, of class RefcoChecker.
     */
    @Test
    public void testCheck() throws Exception {
        // TODO
        // requires input files
    }


    /**
     * Test of getIsUsableFor method, of class RefcoChecker.
     */
    @Test
    public void testGetIsUsableFor() {
        System.out.println("getIsUsableFor");
        RefcoChecker instance = new RefcoChecker();
        try {
            Collection<Class<? extends CorpusData>> result = instance.getIsUsableFor();
            //no null object here
            assertNotNull("The class list is not null", result);
            assertTrue("Checker is usable for ELAN", result.contains(ELANData.class));
        }
        catch (ClassNotFoundException e) {
            fail("Unexpected exception: " + e + "\ncaused by " + e.getCause()) ;
        }
    }

    /**
     * Test of setRefcoFileName method, of class RefcoChecker.
     */
    @Test
    public void testSetRefcoFileName() {
        // TODO
        // requires input file
    }

    @Test
    public void testExpandTableCells() {
        RefcoChecker rc = new RefcoChecker() ;
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

    /**
     * Test of getCellText method, of class RefcoChecker.
     */
    @Test
    public void testGetCellText() {
        try {
            RefcoChecker rc = new RefcoChecker() ;
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

    /**
     * Test of safeGetText method, of class RefcoChecker.
     */
    @Test
    public void testSafeGetText() {
        RefcoChecker rc = new RefcoChecker();
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

    /**
     * Test of getTextInRow method, of class RefcoChecker.
     */
    @Test
    public void testGetTextInRow() {
        RefcoChecker rc = new RefcoChecker() ;
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

    /**
     * Test of getInformationNotes method, of class RefcoChecker.
     */
    @Test
    public void testGetInformationNotes() {
        RefcoChecker rc = new RefcoChecker() ;
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

    /**
     * Test of readRefcoCriteria method, of class RefcoChecker.
     */
    @Test
    public void testReadRefcoCriteria() {
        // TODO
        // Requires input files
    }

    /**
     * Test of refcoGenericCheck method, of class RefcoChecker.
     */
    @Test
    public void testRefcoGenericCheck() {
        // TODO
        // Requires input files
    }

    /**
     * Test of refcoCorpusCheck method, of class RefcoChecker.
     */
    @Test
    public void testRefcoCorpusCheck() {
        // TODO
        // Requires input files
    }

    /**
     * Test of checkTranscription method, of class RefcoChecker.
     */
    @Test
    public void testCheckTranscription() {
        // TODO
        // Requires input files
    }

    /*
    checkMorphology
    checkMorphologyGloss
    */

    /**
     * Test of checkTranscriptionText method, of class RefcoChecker.
     */
    @Test
    public void testCheckTranscriptionText() {
        RefcoChecker rc = new RefcoChecker();
        try {
            Method checkTranscriptionText = rc.getClass().getDeclaredMethod("checkTranscriptionText", CorpusData.class, List.class, Set.class, Set.class);
            checkTranscriptionText.setAccessible(true);
            ELANData corpusData = new ELANData();
            Field urlField = corpusData.getClass().getDeclaredField("url") ;
            urlField.setAccessible(true);
            urlField.set(corpusData,new URL("file://./test.eaf"));
            //corpusData.setJdom(ELANDOM);
            List<Text> transcriptionText = rc.getTextsInTierByType(ELANDOM, "Transcription");
            Set<Character> allCharacters = new HashSet<>(rc.getChars("GO abdeghik,lm-noqrstuvy=?"));
            Set<Character> moreThan80percent = new HashSet<>(rc.getChars(" ,-=?GOabdeghklmnoqr"));
            Set<Character> lessThan40percent = new HashSet<>(rc.getChars(" ,-=?a"));
            Report r = (Report) checkTranscriptionText.invoke(rc,corpusData,transcriptionText,allCharacters,
                    new HashSet<String>());
            assertEquals("All characters are matched", 1, r.getRawStatistics().stream()
                    .filter(ReportItem::isGood).count());
            assertEquals("No warnings or errors", 0, r.getRawStatistics().stream()
                    .filter(ReportItem::isBad).count());
            r = (Report) checkTranscriptionText.invoke(rc,corpusData,transcriptionText,moreThan80percent,
                    new HashSet<String>());
            assertEquals("All characters are matched", 1, r.getRawStatistics().stream()
                    .filter(ReportItem::isGood).count());
            // "Bad" messages that are not about tokens containing invalid characters
            List<ReportItem> relevantMessages = r.getRawStatistics().stream().filter((ri) -> ri.isBad() &&
                    !ri.toString().contains("Transcription token contains invalid characters")).collect(Collectors.toList());
            assertEquals("No relevant warnings or errors", 0, relevantMessages.size());
            r = (Report) checkTranscriptionText.invoke(rc,corpusData,transcriptionText,lessThan40percent,
                    new HashSet<String>());
            assertEquals("All characters are matched", 0, r.getRawStatistics().stream()
                    .filter(ReportItem::isGood).count());
            // "Bad" messages that are not about tokens containing invalid characters
            relevantMessages = r.getRawStatistics().stream().filter((ri) -> ri.isBad() &&
                    !ri.toString().contains("Transcription token contains invalid characters")).collect(Collectors.toList());
            assertEquals("One relevant warning or error", 1, relevantMessages.size());
            assertTrue("Error is about too many mismatches", relevantMessages.get(0).toString().contains("Less than 50 percent of transcription characters are valid"));
        } catch (NoSuchMethodException | MalformedURLException | IllegalAccessException | NoSuchFieldException | JDOMException | InvocationTargetException e) {
            fail("Unexpected exception: " + e + "\ncaused by " + e.getCause()) ;
        }
    }

    /**
     * Test of checkLanguage method, of class RefcoChecker.
     */
    @Test
    public void testCheckLanguage() {
        RefcoChecker rc = new RefcoChecker();
        // Neither ISO-639-3 nor Glottolog
        assertFalse("Invalid language foo", rc.checkLanguage("foobar"));
        // Potential ISO-639-3
        assertFalse("Invalid language foo", rc.checkLanguage("foo"));
        assertTrue("Valid language elo", rc.checkLanguage("elo"));
        // Potential Glottolog
        assertFalse("Invalid language abcd1234", rc.checkLanguage("abcd1234"));
        assertTrue("Valid language nisv1234", rc.checkLanguage("nisv1234"));
    }

    /**
     * Test of checkUrl method, of class RefcoChecker.
     */
    @Test
    public void testCheckUrl() {
        assertTrue("Url known to work", RefcoChecker.checkUrl("https://www.uni-hamburg.de/"));
        assertFalse("Valid Url known to fail", RefcoChecker.checkUrl("https://www.uni-hamburg.de/error"));
        assertFalse("Invalid Url", RefcoChecker.checkUrl("foobar"));
    }

    /**
     * Test of getTextsInTierByType method, of class RefcoChecker.
     */
    @Test
    public void testGetTextsInTierByType() {
        try {
            RefcoChecker rc = new RefcoChecker();
            String transcriptionText = rc.getTextsInTierByType(ELANDOM,"Transcription").stream()
                    .map(Text::getText).reduce(String::concat).get();
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

     /**
     * Test of getTextsInTierByID method, of class RefcoChecker.
     */
    @Test
    public void testGetTextsInTierByID() {
        try {
            RefcoChecker rc = new RefcoChecker();
            String transcriptionText = rc.getTextsInTierByID(ELANDOM,"Aven").stream()
                    .map(Text::getText).reduce(String::concat).get();
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

    /**
     * Test of countWordsInTierByType method, of class RefcoChecker.
     */
    @Test
    public void testCountWordsInTierByType() {
        try {
            RefcoChecker rc = new RefcoChecker();
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

    /**
     * Test of countTranscribedWords method, of class RefcoChecker.
     */
    @Test
    public void testCountTranscribedWords() {
        try {
            RefcoChecker rc = new RefcoChecker();
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

    /**
     * Test of listToParamList method, of class RefcoChecker.
     */
    @Test
    public void testListToParamList() {
        RefcoChecker rc = new RefcoChecker();
        String[] ss = { "x","1",".","a" } ;
        // Get first list from plain array
        ArrayList<String> l = new ArrayList<>(Arrays.asList(ss)) ;
        assertEquals("List as long as the plain array", ss.length,l.size());
        // Generate a Object list by means of streams
        List tmp = Arrays.stream(l.toArray()).collect(Collectors.toList());
        assertEquals("New list as long as the old one", l.size(),tmp.size());
        List<String> ll = (List<String>) rc.listToParamList(String.class, tmp);
        assertEquals("Final list as long as the original one", l.size(), ll.size());
        // Check the presence of all original elements in the new list
        for (String s : l) {
            assertTrue("The element of the original list is also in the new list", ll.contains(s));
        }
    }

    /**
     * Test of getChar method, of class RefcoChecker.
     */
    @Test
    public void testGetChars() {
        String s = "abc" ;
        String c = "." ;
        RefcoChecker rc = new RefcoChecker();
        assertEquals("For a short string the char is the same as the string", new Character('.'),
                rc.getChars(c).get(0));
        for (int i = 0; i < s.length(); i++) {
            assertEquals("For a longer string the characters match",
                    new Character(s.charAt(i)), rc.getChars(s).get(i));
            }
    }
}
