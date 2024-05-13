package de.uni_hamburg.corpora;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Last updated
 * @author Herbert Lange
 * @version 20240510
 * Test cases for all methods defined in ReportItem class.
 */
public class ReportItemTest {
    Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    public ReportItemTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
    }

    /**
     * Generic tests for constructors.
     */
    @Test
    public void testConstructor() {
        logger.info("Run constructor test");
        Field fileName ;
        // Test constructor without parameters
        ReportItem ri = new ReportItem() ;
        assertEquals("Unknown function", ri.getFunction(), "No parameter - function should be unknown");
        assertEquals("Totally unknown error", ri.getWhat(), "No parameter - error should be unknown");
        assertEquals("No known fixes", ri.getHowto(), "No parameter - howto fix should be unknown");
        assertEquals("", ri.getStackTrace(), "No parameter - no stack trace without exception");
        assertEquals("", ri.getLocalisedMessage(), "No parameter - no message without exception");
        assertEquals(ReportItem.Severity.CRITICAL, ri.getSeverity(), "No parameter - severity should be critical");
        // access private filename
        try {
            fileName = ri.getClass().getDeclaredField("filename") ;
            fileName.setAccessible(true);
            assertNull(fileName.get(ri), "No parameter - filename should be null");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("No parameter - cannot access filename");
        }
        // Test constructor with the two parameters: severity and error
        ri = new ReportItem(ReportItem.Severity.IFIXEDITFORYOU, "Error to be fixed");
        assertEquals("Unknown function", ri.getFunction(), "Parameters severity and what - function should be unknown");
        assertEquals("Error to be fixed", ri.getWhat(), "Parameters severity and what - error should be what we want");
        assertEquals("No known fixes", ri.getHowto(), "Parameters severity and what - howto fix should be unknown");
        assertEquals("", ri.getStackTrace(), "Parameters severity and what - no stack trace without exception");
        assertEquals("", ri.getLocalisedMessage(), "Parameters severity and what - no message without exception");
        assertEquals(ReportItem.Severity.IFIXEDITFORYOU, ri.getSeverity(), "Parameters severity and what - severity should be IFIXEDITTFORYOU");
        // access private filename
        try {
            fileName = ri.getClass().getDeclaredField("filename") ;
            fileName.setAccessible(true);
            assertNull(fileName.get(ri), "Parameters severity and what - filename should be null");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Parameters severity and what - cannot access filename");
        }
        // Test constructor with three parameters: severity, exception and error
        ri = new ReportItem(ReportItem.Severity.CORRECT,new RuntimeException("No error here"), "Everything correct");
        assertEquals("Unknown function", ri.getFunction(), "Parameters severity, exception and what - function should be unknown");
        assertEquals("Everything correct", ri.getWhat(), "Parameters severity, exception and what - error should be what we want");
        assertEquals("No known fixes", ri.getHowto(), "Parameters severity, exception and what - howto fix should be unknown");
        assertTrue(ri.getStackTrace().startsWith("java.lang.RuntimeException: No error here"), "Parameters severity, exception and what - stack trace for the exception");
        assertEquals("No error here", ri.getLocalisedMessage(), "Parameters severity, exception and what - message for the exception");
        assertEquals(ReportItem.Severity.CORRECT, ri.getSeverity(), "Parameters severity, exception and what - severity should be CORRECT");
        // access private filename
        try {
            fileName = ri.getClass().getDeclaredField("filename") ;
            fileName.setAccessible(true);
            assertNull(fileName.get(ri), "Parameters severity, exception and what - filename should be null");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Parameters severity, exception and what - cannot access filename");
        }
        // Test constructor with four parameters: severity, exception, filename and error
        ri = new ReportItem(ReportItem.Severity.UNKNOWN,new RuntimeException("No idea what's wrong"), "some_file.txt", "No idea");
        assertEquals("Unknown function", ri.getFunction(), "Parameters severity, exception, filename and what - function should be unknown");
        assertEquals("No idea", ri.getWhat(), "Parameters severity, exception, filename and what - error should be what we want");
        assertEquals("No known fixes", ri.getHowto(), "Parameters severity, exception, filename and what - howto fix should be unknown");
        assertTrue(ri.getStackTrace().startsWith("java.lang.RuntimeException: No idea what's wrong"), "Parameters severity, exception, filename and what - stack trace for the exception");
        assertEquals("No idea what's wrong", ri.getLocalisedMessage(), "Parameters severity, exception, filename and what - message for the exception");
        assertEquals(ReportItem.Severity.UNKNOWN, ri.getSeverity(), "Parameters severity, exception, filename and what - severity should be UNKNOWN");
        // access private filename
        try {
            fileName = ri.getClass().getDeclaredField("filename") ;
            fileName.setAccessible(true);
            assertEquals("some_file.txt", fileName.get(ri), "Parameters severity, exception, filename and what - filename should be some_file.txt");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Parameters severity, exception, filename and what - cannot access filename");
        }
        // Test constructor with four parameters: severity, filename, error and function
        ri = new ReportItem(ReportItem.Severity.NOTE, "some_other_file.txt", "Just a note", "fixing_function");
        assertEquals("fixing_function", ri.getFunction(), "Parameters severity, filename, error and function - function should be fixing_function");
        assertEquals("Just a note", ri.getWhat(), "Parameters severity, filename, error and function - error should be what we want");
        assertEquals("No known fixes", ri.getHowto(), "Parameters severity, filename, error and function - howto fix should be unknown");
        assertEquals("" , ri.getStackTrace(), "Parameters severity, filename, error and function - no stack trace without exception");
        assertEquals("", ri.getLocalisedMessage(), "Parameters severity, filename, error and function - no message without exception");
        assertEquals(ReportItem.Severity.NOTE, ri.getSeverity(), "Parameters severity, filename, error and function - severity should be NOTE");
        // access private filename
        try {
            fileName = ri.getClass().getDeclaredField("filename") ;
            fileName.setAccessible(true);
            assertEquals("some_other_file.txt", fileName.get(ri), "Parameters severity, filename, error and function - filename should be some_other_file.txt");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Parameters severity, filename, error and function - cannot access filename");
        }
        // Test constructor with three parameters: severity, saxparseexception and error
        ri = new ReportItem(ReportItem.Severity.MISSING,new SAXParseException("Something's missing", "public_id", "system_id",23,42), "Something's missing");
        assertEquals("Unknown function", ri.getFunction(), "Parameters severity, saxparseexception and error - function should be unknown");
        assertEquals("Something's missing", ri.getWhat(), "Parameters severity, saxparseexception and error - error should be what we want");
        assertEquals("No known fixes", ri.getHowto(), "Parameters severity, saxparseexception and error - howto fix should be unknown");
        assertTrue(ri.getStackTrace().startsWith("org.xml.sax.SAXParseExceptionpublicId: public_id; systemId: system_id; lineNumber: 23; columnNumber: 42; Something's missing"), "Parameters severity, saxparseexception and error - stack trace for the  exception");
        assertEquals("Something's missing", ri.getLocalisedMessage(), "Parameters severity, saxparseexception and error - message for the exception");
        assertEquals(ReportItem.Severity.MISSING, ri.getSeverity(), "Parameters severity, saxparseexception and error - severity should be MISSING");
        // access private filename
        try {
            fileName = ri.getClass().getDeclaredField("filename") ;
            fileName.setAccessible(true);
            assertEquals("system_id", fileName.get(ri), "Parameters severity, saxparseexception and error - filename should be system_id");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Parameters severity, saxparseexception and error - cannot access filename");
        }
         // Test constructor with five parameters: severity, filename, error, function, howto
        ri = new ReportItem(ReportItem.Severity.CRITICAL, "another_filename.txt", "That's bad", "another_function", "This will fix it");
        assertEquals("another_function", ri.getFunction(), "Parameters severity, filename, error, function and howto - function should be another_function");
        assertEquals("That's bad", ri.getWhat(), "Parameters severity, filename, error, function and howto - error should be what we want");
        assertEquals("This will fix it", ri.getHowto(), "Parameters severity, filename, error, function and howto - howto fix should be what we want");
        assertEquals("", ri.getStackTrace(), "Parameters severity, filename, error, function and howto - no stack trace without exception");
        assertEquals("", ri.getLocalisedMessage(), "Parameters severity, filename, error, function and howto - no message without exception");
        assertEquals(ReportItem.Severity.CRITICAL, ri.getSeverity(), "Parameters severity, filename, error, function and howto - severity should be CRITICAL");
        // access private filename
        try {
            fileName = ri.getClass().getDeclaredField("filename") ;
            fileName.setAccessible(true);
            assertEquals("another_filename.txt", fileName.get(ri), "Parameters severity, filename, error, function and howto - filename should be system_id");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Parameters severity, filename, error, function and howto - cannot access filename");
        }

    }


    /**
     * Test cases for the constructors with null as values for exceptions.
     */
    @Test
    public void testNullException() {
        ReportItem ri = new ReportItem();
        try {
            Field exception = ri.getClass().getDeclaredField("e");
            exception.setAccessible(true);
            assertNull(exception.get(ri), "First null exception");
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Error accessing field");
        }
        ri = new ReportItem(ReportItem.Severity.CRITICAL,(Throwable) null, "some what");
        try {
            Field exception = ri.getClass().getDeclaredField("e");
            exception.setAccessible(true);
            assertNull(exception.get(ri), "Second null exception");
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Error accessing field");
        }
        ri = new ReportItem(ReportItem.Severity.CRITICAL, (Throwable) null, "some what");
        try {
            Field exception = ri.getClass().getDeclaredField("e");
            exception.setAccessible(true);
            assertNull(exception.get(ri), "Third null exception");
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Error accessing field");
        }
    }

    @Test
    public void testIsGood() {
        logger.info("Run isGood test");
        // Set of all acceptable items
        Set<ReportItem.Severity> goodItems = new HashSet<>(Arrays.asList(ReportItem.Severity.CORRECT,
                ReportItem.Severity.NOTE, ReportItem.Severity.IFIXEDITFORYOU));
        // Check all possible values if they meet the expectation
        for (ReportItem.Severity s : ReportItem.Severity.values()) {
            ReportItem ri = new ReportItem(s, "Test isGood");
            assertEquals(goodItems.contains(s), ri.isGood(), "Good items match isGood");
        }
    }

    @Test
    public void testIsBad() {
        logger.info("Run isBad test");
        // Set of all acceptable items
        Set<ReportItem.Severity> badItems = new HashSet<>(Arrays.asList(ReportItem.Severity.WARNING, ReportItem.Severity.CRITICAL, ReportItem.Severity.MISSING, ReportItem.Severity.UNKNOWN));
        // Check all possible values if they meet the expectation
        for (ReportItem.Severity s : ReportItem.Severity.values()) {
            ReportItem ri = new ReportItem(s, "Test isBad");
            assertEquals(badItems.contains(s), ri.isBad(), "Bad items match isBad");
        }
    }

    @Test
    public void testIsSevere() {
        logger.info("Run isSevere test");
        // Set of all acceptable items
        Set<ReportItem.Severity> severeItems = new HashSet<>(Arrays.asList(ReportItem.Severity.CRITICAL, ReportItem.Severity.MISSING, ReportItem.Severity.UNKNOWN));
        // Check all possible values if they meet the expectation
        for (ReportItem.Severity s : ReportItem.Severity.values()) {
            ReportItem ri = new ReportItem(s, "Test isSevere");
            assertEquals(severeItems.contains(s), ri.isSevere(), "Severe items match isSevere");
        }
    }

    @Test
    public void testIsFix() {
        logger.info("Run isFix test");
        // Set of all acceptable items
        Set<ReportItem.Severity> fixItems = new HashSet<>(Collections.singletonList(ReportItem.Severity.IFIXEDITFORYOU));
        // Check all possible values if they meet the expectation
        for (ReportItem.Severity s : ReportItem.Severity.values()) {
            ReportItem ri = new ReportItem(s, "Test isFix");
            assertEquals(fixItems.contains(s), ri.isFix(), "Fix items match isFix");
        }
    }

    @Test
    public void testGetLocation() {
        logger.info("Run getLocation test");
        ReportItem ri = new ReportItem();
        assertEquals("", ri.getLocation(), "No location gives empty string");
        ri = new ReportItem(ReportItem.Severity.NOTE, "some_file.txt", "", "");
        assertEquals("some_file.txt", ri.getLocation(), "A sax error gives the location");
        ri = new ReportItem(ReportItem.Severity.CRITICAL,new SAXParseException("", "", "some_file.xml",23,42), "");
        assertEquals("some_file.xml:23.42", ri.getLocation(), "A sax error gives the location");
    }

    @Test
    public void testGetWhat() {
        logger.info("Run getWhat test");
        ReportItem ri = new ReportItem();
        assertEquals("Totally unknown error", ri.getWhat(), "No what");
        ri = new ReportItem(ReportItem.Severity.NOTE, "");
        assertEquals("", ri.getWhat(), "Empty what");
        ri = new ReportItem(ReportItem.Severity.NOTE, "what1");
        assertEquals("what1", ri.getWhat(), "First what");
        ri = new ReportItem(ReportItem.Severity.NOTE,new Exception(), "what2");
        assertEquals("what2", ri.getWhat(), "Second what");
        ri = new ReportItem(ReportItem.Severity.NOTE,new Exception(), "", "what3");
        assertEquals("what3", ri.getWhat(), "Third what");
        ri = new ReportItem(ReportItem.Severity.NOTE, "", "what4", "");
        assertEquals("what4", ri.getWhat(), "Fourth what");
        ri = new ReportItem(ReportItem.Severity.NOTE,new SAXParseException("",null), "what5");
        assertEquals("what5", ri.getWhat(), "Fifth what");
        ri = new ReportItem(ReportItem.Severity.NOTE, "", "what6", "", "");
        assertEquals("what6", ri.getWhat(), "Sixth what");
    }

    @Test
    public void testGetHowto() {
        logger.info("Run getHowto test");
        ReportItem ri = new ReportItem();
        assertEquals("No known fixes", ri.getHowto(), "No howto");
        ri = new ReportItem(ReportItem.Severity.WARNING, "some_file.txt", "some error", "someFunction", "");
        assertEquals("", ri.getHowto(), "Empty howto");
        ri = new ReportItem(ReportItem.Severity.WARNING, "some_file.txt", "some error", "someFunction", "how to fix it");
        assertEquals("how to fix it", ri.getHowto(), "Some howto");
    }

    @Test
    public void testGetFunction() {
        logger.info("Run getFunction test");
        ReportItem ri = new ReportItem();
        assertEquals("Unknown function", ri.getFunction(), "No function");
        ri = new ReportItem(ReportItem.Severity.WARNING, "some_file.txt", "some error", "");
        assertEquals("", ri.getFunction(), "Empty function");
        ri = new ReportItem(ReportItem.Severity.WARNING, "some_file.txt", "some error", "someFunction1");
        assertEquals("someFunction1", ri.getFunction(), "First function");
        ri = new ReportItem(ReportItem.Severity.WARNING, "some_file.txt", "some error", "someFunction2", "some howto");
        assertEquals("someFunction2", ri.getFunction(), "Second function");
    }

    @Test
    public void testGetLocalisedMessage() {
        logger.info("Run getLocalisedMessage test");
        ReportItem ri = new ReportItem();
        assertEquals("", ri.getLocalisedMessage(), "No message");
        ri = new ReportItem(ReportItem.Severity.CRITICAL,(Throwable) null, "some what");
        assertEquals("", ri.getLocalisedMessage(), "Null exception");
        ri = new ReportItem(ReportItem.Severity.CRITICAL, (Throwable) null, "some what");
        assertEquals("", ri.getLocalisedMessage(), "Null SAX exception");
        ri = new ReportItem(ReportItem.Severity.CRITICAL,new RuntimeException("Some error occurred"), "some what");
        assertEquals("Some error occurred", ri.getLocalisedMessage(), "First exception");
        ri = new ReportItem(ReportItem.Severity.CRITICAL, new RuntimeException("Some other error occurred"), "some what");
        assertEquals("Some other error occurred", ri.getLocalisedMessage(), "Second exception");
        ri = new ReportItem(ReportItem.Severity.CRITICAL,new SAXParseException("Some SAX error occurred", "public_id", "system_id", 23, 24), "some what");
        assertEquals("Some SAX error occurred", ri.getLocalisedMessage(), "SAX exception");
    }

    @Test
    public void testGetSummary() {
        logger.info("Run getSummary test");
        ReportItem ri = new ReportItem();
        assertEquals("    Totally unknown error", ri.getSummary(), "Empty item");
        ri = new ReportItem(ReportItem.Severity.CRITICAL, "some what");
        assertEquals("    some what", ri.getSummary(), "Simple message");
        ri = new ReportItem(ReportItem.Severity.CRITICAL, "some_file.txt", "some what", "someFunction");
        assertEquals("    some_file.txt: some what", ri.getSummary(), "With filename");
        ri = new ReportItem(ReportItem.Severity.CRITICAL,new SAXParseException("Some SAX error occurred", "public_id", "system_id", 23, 24), "some other what");
        assertEquals("    system_id:23.24: some other what", ri.getSummary(), "SAX exception");
    }

    @Test
    public void testToString() {
        logger.info("Run toString test");
        ReportItem ri = new ReportItem() ;
        assertEquals(": Totally unknown error. No known fixes. ", ri.toString(), "Empty item");
        ri = new ReportItem(ReportItem.Severity.NOTE, "some what");
        assertEquals(": some what. No known fixes. ", ri.toString(), "Basic item");
        ri = new ReportItem(ReportItem.Severity.NOTE,new SAXParseException("XML parse exception", "public_id", "system_id",23,42), "some XML problem");
        assertTrue(ri.toString().startsWith("system_id:23.42: some XML problem. No known fixes. XML parse exception\n" +
                "org.xml.sax.SAXParseException"), "Item with XML exception");
    }

    @Test
    public void testGetStackTrace() {
        logger.info("Run getStackTrace test");
        ReportItem ri = new ReportItem() ;
        assertEquals("", ri.getStackTrace(), "Empty item");
        ri = new ReportItem(ReportItem.Severity.NOTE,new RuntimeException("Runtime error"), "some runtime problem");
        assertTrue(ri.getStackTrace().startsWith("java.lang.RuntimeException: Runtime error"), "Item with runtime exception");
        ri = new ReportItem(ReportItem.Severity.NOTE,new SAXParseException("XML parse exception", "public_id", "system_id",23,42), "some XML problem");
        assertTrue(ri.getStackTrace().startsWith("org.xml.sax.SAXParseExceptionpublicId: public_id; systemId: system_id; lineNumber: 23; columnNumber: 42; XML parse exception"), "Item with XML exception");
    }

    @Test
    public void testGeneratePlainText() {
        // logger.info("Run generatePlainText test");
        // TODO
    }

    @Test
    public void testGenerateSummary() {
        logger.info("Run generateSummary test") ;
        ArrayList<ReportItem> items = new ArrayList<>() ;
        items.add(new ReportItem()) ;
        String summary = ReportItem.generateSummary(items) ;
        assertEquals("Total of 1 messages: 1 critical errors, 0 warnings, 0 notes and 0 others.",summary, "Short summary") ;
        items = new ArrayList<>() ;
        items.add(new ReportItem(ReportItem.Severity.CRITICAL, "critical")) ;
        items.add(new ReportItem(ReportItem.Severity.WARNING, "warning")) ;
        items.add(new ReportItem(ReportItem.Severity.NOTE, "note")) ;
        items.add(new ReportItem(ReportItem.Severity.MISSING, "missing")) ;
        items.add(new ReportItem(ReportItem.Severity.CORRECT, "correct")) ;
        items.add(new ReportItem(ReportItem.Severity.IFIXEDITFORYOU, "ifixeditforyou")) ;
        items.add(new ReportItem(ReportItem.Severity.UNKNOWN, "unknown")) ;
        summary = ReportItem.generateSummary(items) ;
        assertEquals("Total of 7 messages: 2 critical errors, 1 warnings, 1 notes and 3 others.",summary, "Longer summary") ;

    }

    @Test
    public void testGenerateHTML() {
        // logger.info("Run generateHTML test");
        // TODO
    }

    @Test
    public void testGenerateDataTableHTML() {
        // logger.info("Run generateDataTableHTML test");
        // TODO
    }
}
