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
        assertEquals("Unknown function", ri.getFunction(),"No parameter - function should be unknown");
        assertEquals("Totally unknown error", ri.getWhat(), "No parameter - error should be unknown");
        assertEquals("No known fixes", ri.getHowto(), "No parameter - howto fix should be unknown");
        assertEquals("", ri.getStackTrace(),"No parameter - no stack trace without exception");
        assertEquals("", ri.getLocalisedMessage(),"No parameter - no message without exception");
        assertEquals(ReportItem.Severity.CRITICAL, ri.getSeverity(), "No parameter - severity should be critical");
        // access private filename
        try {
            fileName = ri.getClass().getDeclaredField("filename") ;
            fileName.setAccessible(true);
            assertNull(fileName.get(ri),"No parameter - filename should be null");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("No parameter - cannot access filename");
        }
        // Test constructor with the two parameters: severity and error
        ri = new ReportItem(ReportItem.Severity.IFIXEDITFORYOU,"Error to be fixed");
        assertEquals("Unknown function", ri.getFunction(),"Parameters severity and what - function should be unknown");
        assertEquals("Error to be fixed", ri.getWhat(),"Parameters severity and what - error should be what we want");
        assertEquals("No known fixes", ri.getHowto(),"Parameters severity and what - howto fix should be unknown");
        assertEquals("", ri.getStackTrace(),"Parameters severity and what - no stack trace without exception");
        assertEquals("", ri.getLocalisedMessage(),"Parameters severity and what - no message without exception");
        assertEquals(ReportItem.Severity.IFIXEDITFORYOU, ri.getSeverity(),"Parameters severity and what - severity should be IFIXEDITTFORYOU");
        // access private filename
        try {
            fileName = ri.getClass().getDeclaredField("filename") ;
            fileName.setAccessible(true);
            assertNull(fileName.get(ri),"Parameters severity and what - filename should be null");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Parameters severity and what - cannot access filename");
        }
        // Test constructor with three parameters: severity, exception and error
        ri = new ReportItem(ReportItem.Severity.CORRECT,new RuntimeException("No error here"),"Everything correct");
        assertEquals("Unknown function", ri.getFunction(),"Parameters severity, exception and what - function should be unknown");
        assertEquals("Everything correct", ri.getWhat(),"Parameters severity, exception and what - error should be what we want");
        assertEquals("No known fixes", ri.getHowto(),"Parameters severity, exception and what - howto fix should be unknown");
        assertTrue(ri.getStackTrace().startsWith("java.lang.RuntimeException: No error here"),"Parameters severity, exception and what - stack trace for the exception");
        assertEquals("No error here", ri.getLocalisedMessage(),"Parameters severity, exception and what - message for the exception");
        assertEquals(ReportItem.Severity.CORRECT, ri.getSeverity(), "Parameters severity, exception and what - severity should be CORRECT");
        // access private filename
        try {
            fileName = ri.getClass().getDeclaredField("filename") ;
            fileName.setAccessible(true);
            localAssertNull("Parameters severity, exception and what - filename should be null", fileName.get(ri));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Parameters severity, exception and what - cannot access filename");
        }
        // Test constructor with four parameters: severity, exception, filename and error
        ri = new ReportItem(ReportItem.Severity.UNKNOWN,new RuntimeException("No idea what's wrong"),"some_file.txt", "No idea");
        localAssertEquals("Parameters severity, exception, filename and what - function should be unknown","Unknown function", ri.getFunction());
        localAssertEquals("Parameters severity, exception, filename and what - error should be what we want","No idea", ri.getWhat());
        localAssertEquals("Parameters severity, exception, filename and what - howto fix should be unknown","No known fixes", ri.getHowto());
        localAssertTrue("Parameters severity, exception, filename and what - stack trace for the exception", ri.getStackTrace().startsWith("java.lang.RuntimeException: No idea what's wrong"));
        localAssertEquals("Parameters severity, exception, filename and what - message for the exception","No idea what's wrong", ri.getLocalisedMessage());
        localAssertEquals("Parameters severity, exception, filename and what - severity should be UNKNOWN",ReportItem.Severity.UNKNOWN, ri.getSeverity());
        // access private filename
        try {
            fileName = ri.getClass().getDeclaredField("filename") ;
            fileName.setAccessible(true);
            localAssertEquals("Parameters severity, exception, filename and what - filename should be some_file.txt", "some_file.txt", fileName.get(ri));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Parameters severity, exception, filename and what - cannot access filename");
        }
        // Test constructor with four parameters: severity, filename, error and function
        ri = new ReportItem(ReportItem.Severity.NOTE,"some_other_file.txt", "Just a note","fixing_function");
        localAssertEquals("Parameters severity, filename, error and function - function should be fixing_function","fixing_function", ri.getFunction());
        localAssertEquals("Parameters severity, filename, error and function - error should be what we want","Just a note", ri.getWhat());
        localAssertEquals("Parameters severity, filename, error and function - howto fix should be unknown","No known fixes", ri.getHowto());
        localAssertEquals("Parameters severity, filename, error and function - no stack trace without exception", "" , ri.getStackTrace());
        localAssertEquals("Parameters severity, filename, error and function - no message without exception","", ri.getLocalisedMessage());
        localAssertEquals("Parameters severity, filename, error and function - severity should be NOTE",ReportItem.Severity.NOTE, ri.getSeverity());
        // access private filename
        try {
            fileName = ri.getClass().getDeclaredField("filename") ;
            fileName.setAccessible(true);
            localAssertEquals("Parameters severity, filename, error and function - filename should be some_other_file.txt", "some_other_file.txt", fileName.get(ri));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Parameters severity, filename, error and function - cannot access filename");
        }
        // Test constructor with three parameters: severity, saxparseexception and error
        ri = new ReportItem(ReportItem.Severity.MISSING,new SAXParseException("Something's missing","public_id","system_id",23,42),"Something's missing");
        localAssertEquals("Parameters severity, saxparseexception and error - function should be unknown","Unknown function", ri.getFunction());
        localAssertEquals("Parameters severity, saxparseexception and error - error should be what we want","Something's missing", ri.getWhat());
        localAssertEquals("Parameters severity, saxparseexception and error - howto fix should be unknown","No known fixes", ri.getHowto());
        localAssertTrue("Parameters severity, saxparseexception and error - stack trace for the  exception",  ri.getStackTrace().startsWith("org.xml.sax.SAXParseExceptionpublicId: public_id; systemId: system_id; lineNumber: 23; columnNumber: 42; Something's missing"));
        localAssertEquals("Parameters severity, saxparseexception and error - message for the exception","Something's missing", ri.getLocalisedMessage());
        localAssertEquals("Parameters severity, saxparseexception and error - severity should be MISSING",ReportItem.Severity.MISSING, ri.getSeverity());
        // access private filename
        try {
            fileName = ri.getClass().getDeclaredField("filename") ;
            fileName.setAccessible(true);
            localAssertEquals("Parameters severity, saxparseexception and error - filename should be system_id", "system_id", fileName.get(ri));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Parameters severity, saxparseexception and error - cannot access filename");
        }
         // Test constructor with five parameters: severity, filename, error, function, howto
        ri = new ReportItem(ReportItem.Severity.CRITICAL,"another_filename.txt","That's bad", "another_function","This will fix it");
        localAssertEquals("Parameters severity, filename, error, function and howto - function should be another_function","another_function", ri.getFunction());
        localAssertEquals("Parameters severity, filename, error, function and howto - error should be what we want","That's bad", ri.getWhat());
        localAssertEquals("Parameters severity, filename, error, function and howto - howto fix should be what we want","This will fix it", ri.getHowto());
        localAssertEquals("Parameters severity, filename, error, function and howto - no stack trace without exception", "", ri.getStackTrace());
        localAssertEquals("Parameters severity, filename, error, function and howto - no message without exception","", ri.getLocalisedMessage());
        localAssertEquals("Parameters severity, filename, error, function and howto - severity should be CRITICAL",ReportItem.Severity.CRITICAL, ri.getSeverity());
        // access private filename
        try {
            fileName = ri.getClass().getDeclaredField("filename") ;
            fileName.setAccessible(true);
            localAssertEquals("Parameters severity, filename, error, function and howto - filename should be system_id", "another_filename.txt", fileName.get(ri));
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
            localAssertNull("First null exception", exception.get(ri));
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Error accessing field");
        }
        ri = new ReportItem(ReportItem.Severity.CRITICAL,(Throwable) null, "some what");
        try {
            Field exception = ri.getClass().getDeclaredField("e");
            exception.setAccessible(true);
            localAssertNull("Second null exception", exception.get(ri));
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Error accessing field");
        }
        ri = new ReportItem(ReportItem.Severity.CRITICAL, (Throwable) null, "some what");
        try {
            Field exception = ri.getClass().getDeclaredField("e");
            exception.setAccessible(true);
            localAssertNull("Third null exception", exception.get(ri));
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
            ReportItem ri = new ReportItem(s,"Test isGood");
            localAssertEquals("Good items match isGood", goodItems.contains(s), ri.isGood());
        }
    }

    @Test
    public void testIsBad() {
        logger.info("Run isBad test");
        // Set of all acceptable items
        Set<ReportItem.Severity> badItems = new HashSet<>(Arrays.asList(ReportItem.Severity.WARNING, ReportItem.Severity.CRITICAL, ReportItem.Severity.MISSING, ReportItem.Severity.UNKNOWN));
        // Check all possible values if they meet the expectation
        for (ReportItem.Severity s : ReportItem.Severity.values()) {
            ReportItem ri = new ReportItem(s,"Test isBad");
            localAssertEquals("Bad items match isBad", badItems.contains(s), ri.isBad());
        }
    }

    @Test
    public void testIsSevere() {
        logger.info("Run isSevere test");
        // Set of all acceptable items
        Set<ReportItem.Severity> severeItems = new HashSet<>(Arrays.asList(ReportItem.Severity.CRITICAL, ReportItem.Severity.MISSING, ReportItem.Severity.UNKNOWN));
        // Check all possible values if they meet the expectation
        for (ReportItem.Severity s : ReportItem.Severity.values()) {
            ReportItem ri = new ReportItem(s,"Test isSevere");
            localAssertEquals("Severe items match isSevere", severeItems.contains(s), ri.isSevere());
        }
    }

    @Test
    public void testIsFix() {
        logger.info("Run isFix test");
        // Set of all acceptable items
        Set<ReportItem.Severity> fixItems = new HashSet<>(Collections.singletonList(ReportItem.Severity.IFIXEDITFORYOU));
        // Check all possible values if they meet the expectation
        for (ReportItem.Severity s : ReportItem.Severity.values()) {
            ReportItem ri = new ReportItem(s,"Test isFix");
            localAssertEquals("Fix items match isFix", fixItems.contains(s), ri.isFix());
        }
    }

    @Test
    public void testGetLocation() {
        logger.info("Run getLocation test");
        ReportItem ri = new ReportItem();
        localAssertEquals("No location gives empty string", "", ri.getLocation());
        ri = new ReportItem(ReportItem.Severity.NOTE,"some_file.txt","","");
        localAssertEquals("A sax error gives the location", "some_file.txt", ri.getLocation());
        ri = new ReportItem(ReportItem.Severity.CRITICAL,new SAXParseException("","","some_file.xml",23,42),"");
        localAssertEquals("A sax error gives the location", "some_file.xml:23.42", ri.getLocation());
    }

    @Test
    public void testGetWhat() {
        logger.info("Run getWhat test");
        ReportItem ri = new ReportItem();
        localAssertEquals("No what", "Totally unknown error", ri.getWhat());
        ri = new ReportItem(ReportItem.Severity.NOTE,"");
        localAssertEquals("Empty what", "", ri.getWhat());
        ri = new ReportItem(ReportItem.Severity.NOTE,"what1");
        localAssertEquals("First what", "what1", ri.getWhat());
        ri = new ReportItem(ReportItem.Severity.NOTE,new Exception(),"what2");
        localAssertEquals("Second what", "what2", ri.getWhat());
        ri = new ReportItem(ReportItem.Severity.NOTE,new Exception(),"", "what3");
        localAssertEquals("Third what", "what3", ri.getWhat());
        ri = new ReportItem(ReportItem.Severity.NOTE,"","what4","");
        localAssertEquals("Fourth what", "what4", ri.getWhat());
        ri = new ReportItem(ReportItem.Severity.NOTE,new SAXParseException("",null),"what5");
        localAssertEquals("Fifth what", "what5", ri.getWhat());
        ri = new ReportItem(ReportItem.Severity.NOTE,"","what6","","");
        localAssertEquals("Sixth what", "what6", ri.getWhat());
    }

    @Test
    public void testGetHowto() {
        logger.info("Run getHowto test");
        ReportItem ri = new ReportItem();
        localAssertEquals("No howto", "No known fixes", ri.getHowto());
        ri = new ReportItem(ReportItem.Severity.WARNING, "some_file.txt", "some error", "someFunction", "");
        localAssertEquals("Empty howto", "", ri.getHowto());
        ri = new ReportItem(ReportItem.Severity.WARNING, "some_file.txt", "some error", "someFunction", "how to fix it");
        localAssertEquals("Some howto", "how to fix it", ri.getHowto());
    }

    @Test
    public void testGetFunction() {
        logger.info("Run getFunction test");
        ReportItem ri = new ReportItem();
        localAssertEquals("No function", "Unknown function", ri.getFunction());
        ri = new ReportItem(ReportItem.Severity.WARNING, "some_file.txt", "some error", "");
        localAssertEquals("Empty function", "", ri.getFunction());
        ri = new ReportItem(ReportItem.Severity.WARNING, "some_file.txt", "some error", "someFunction1");
        localAssertEquals("First function", "someFunction1", ri.getFunction());
        ri = new ReportItem(ReportItem.Severity.WARNING, "some_file.txt", "some error", "someFunction2", "some howto");
        localAssertEquals("Second function", "someFunction2", ri.getFunction());
    }

    @Test
    public void testGetLocalisedMessage() {
        logger.info("Run getLocalisedMessage test");
        ReportItem ri = new ReportItem();
        localAssertEquals("No message", "", ri.getLocalisedMessage());
        ri = new ReportItem(ReportItem.Severity.CRITICAL,(Throwable) null, "some what");
        localAssertEquals("Null exception", "", ri.getLocalisedMessage());
        ri = new ReportItem(ReportItem.Severity.CRITICAL, (Throwable) null, "some what");
        localAssertEquals("Null SAX exception", "", ri.getLocalisedMessage());
        ri = new ReportItem(ReportItem.Severity.CRITICAL,new RuntimeException("Some error occurred"), "some what");
        localAssertEquals("First exception", "Some error occurred", ri.getLocalisedMessage());
        ri = new ReportItem(ReportItem.Severity.CRITICAL, new RuntimeException("Some other error occurred"), "some what");
        localAssertEquals("Second exception", "Some other error occurred", ri.getLocalisedMessage());
        ri = new ReportItem(ReportItem.Severity.CRITICAL,new SAXParseException("Some SAX error occurred", "public_id", "system_id", 23, 24), "some what");
        localAssertEquals("SAX exception", "Some SAX error occurred", ri.getLocalisedMessage());
    }

    @Test
    public void testGetSummary() {
        logger.info("Run getSummary test");
        ReportItem ri = new ReportItem();
        localAssertEquals("Empty item", "    Totally unknown error", ri.getSummary());
        ri = new ReportItem(ReportItem.Severity.CRITICAL, "some what");
        localAssertEquals("Simple message", "    some what", ri.getSummary());
        ri = new ReportItem(ReportItem.Severity.CRITICAL, "some_file.txt", "some what", "someFunction");
        localAssertEquals("With filename", "    some_file.txt: some what", ri.getSummary());
        ri = new ReportItem(ReportItem.Severity.CRITICAL,new SAXParseException("Some SAX error occurred", "public_id", "system_id", 23, 24), "some other what");
        localAssertEquals("SAX exception", "    system_id:23.24: some other what", ri.getSummary());
    }

    @Test
    public void testToString() {
        logger.info("Run toString test");
        ReportItem ri = new ReportItem() ;
        localAssertEquals("Empty item", ": Totally unknown error. No known fixes. ", ri.toString());
        ri = new ReportItem(ReportItem.Severity.NOTE,"some what");
        localAssertEquals("Basic item", ": some what. No known fixes. ", ri.toString());
        ri = new ReportItem(ReportItem.Severity.NOTE,new SAXParseException("XML parse exception","public_id","system_id",23,42), "some XML problem");
        localAssertTrue("Item with XML exception", ri.toString().startsWith("system_id:23.42: some XML problem. No known fixes. XML parse exception\n" +
                "org.xml.sax.SAXParseException"));
    }

    @Test
    public void testGetStackTrace() {
        logger.info("Run getStackTrace test");
        ReportItem ri = new ReportItem() ;
        localAssertEquals("Empty item", "", ri.getStackTrace());
        ri = new ReportItem(ReportItem.Severity.NOTE,new RuntimeException("Runtime error"), "some runtime problem");
        localAssertTrue("Item with runtime exception", ri.getStackTrace().startsWith("java.lang.RuntimeException: Runtime error"));
        ri = new ReportItem(ReportItem.Severity.NOTE,new SAXParseException("XML parse exception","public_id","system_id",23,42), "some XML problem");
        localAssertTrue("Item with XML exception", ri.getStackTrace().startsWith("org.xml.sax.SAXParseExceptionpublicId: public_id; systemId: system_id; lineNumber: 23; columnNumber: 42; XML parse exception"));
    }

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
        localAssertEquals("Total of 1 messages: 1 critical errors, 0 warnings, 0 notes and 0 others.",summary) ;
        items = new ArrayList<>() ;
        items.add(new ReportItem(ReportItem.Severity.CRITICAL,"critical")) ;
        items.add(new ReportItem(ReportItem.Severity.WARNING,"warning")) ;
        items.add(new ReportItem(ReportItem.Severity.NOTE,"note")) ;
        items.add(new ReportItem(ReportItem.Severity.MISSING,"missing")) ;
        items.add(new ReportItem(ReportItem.Severity.CORRECT,"correct")) ;
        items.add(new ReportItem(ReportItem.Severity.IFIXEDITFORYOU,"ifixeditforyou")) ;
        items.add(new ReportItem(ReportItem.Severity.UNKNOWN,"unknown")) ;
        summary = ReportItem.generateSummary(items) ;
        localAssertEquals("Total of 7 messages: 2 critical errors, 1 warnings, 1 notes and 3 others.",summary) ;

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
