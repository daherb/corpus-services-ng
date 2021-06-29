package de.uni_hamburg.corpora;

import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.junit.Assert.*;

/**
 * @author bba1792 Dr. Herbert Lange
 * @version 20210628
 * Test cases for all methods defined in ReportItem class.
 */
public class ReportItemTest {
    Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    public ReportItemTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
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
        assertEquals("No parameter - function should be unknown","Unknown function", ri.getFunction());
        assertEquals("No parameter - error should be unknown","Totally unknown error", ri.getWhat());
        assertEquals("No parameter - howto fix should be unknown","No known fixes", ri.getHowto());
        assertEquals("No parameter - no stack trace without exception","", ri.getStackTrace());
        assertEquals("No parameter - no message without exception","", ri.getLocalisedMessage());
        assertEquals("No parameter - severity should be critical",ReportItem.Severity.CRITICAL, ri.getSeverity());
        // access private filename
        try {
            fileName = ri.getClass().getDeclaredField("filename") ;
            fileName.setAccessible(true);
            assertEquals("No parameter - filename should be null", null, fileName.get(ri));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("No parameter - cannot access filename");
        }
        // Test constructor with the two parameters: severity and error
        ri = new ReportItem(ReportItem.Severity.IFIXEDITFORYOU,"Error to be fixed");
        assertEquals("Parameters severity and what - function should be unknown","Unknown function", ri.getFunction());
        assertEquals("Parameters severity and what - error should be what we want","Error to be fixed", ri.getWhat());
        assertEquals("Parameters severity and what - howto fix should be unknown","No known fixes", ri.getHowto());
        assertEquals("Parameters severity and what - no stack trace without exception","", ri.getStackTrace());
        assertEquals("Parameters severity and what - no message without exception","", ri.getLocalisedMessage());
        assertEquals("Parameters severity and what - severity should be IFIXEDITTFORYOU",ReportItem.Severity.IFIXEDITFORYOU, ri.getSeverity());
        // access private filename
        try {
            fileName = ri.getClass().getDeclaredField("filename") ;
            fileName.setAccessible(true);
            assertEquals("Parameters severity and what - filename should be null", null, fileName.get(ri));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Parameters severity and what - cannot access filename");
        }
        // Test constructor with three parameters: severity, exception and error
        ri = new ReportItem(ReportItem.Severity.CORRECT,new RuntimeException("No error here"),"Everything correct");
        assertEquals("Parameters severity, exception and what - function should be unknown","Unknown function", ri.getFunction());
        assertEquals("Parameters severity, exception and what - error should be what we want","Everything correct", ri.getWhat());
        assertEquals("Parameters severity, exception and what - howto fix should be unknown","No known fixes", ri.getHowto());
        assertTrue("Parameters severity, exception and what - stack trace for the exception", ri.getStackTrace().startsWith("java.lang.RuntimeException: No error here"));
        assertEquals("Parameters severity, exception and what - message for the exception","No error here", ri.getLocalisedMessage());
        assertEquals("Parameters severity, exception and what - severity should be CORRECT",ReportItem.Severity.CORRECT, ri.getSeverity());
        // access private filename
        try {
            fileName = ri.getClass().getDeclaredField("filename") ;
            fileName.setAccessible(true);
            assertEquals("Parameters severity, exception and what - filename should be null", null, fileName.get(ri));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Parameters severity, exception and what - cannot access filename");
        }
        // Test constructor with four parameters: severity, exception, filename and error
        ri = new ReportItem(ReportItem.Severity.UNKNOWN,new RuntimeException("No idea what's wrong"),"some_file.txt", "No idea");
        assertEquals("Parameters severity, exception, filename and what - function should be unknown","Unknown function", ri.getFunction());
        assertEquals("Parameters severity, exception, filename and what - error should be what we want","No idea", ri.getWhat());
        assertEquals("Parameters severity, exception, filename and what - howto fix should be unknown","No known fixes", ri.getHowto());
        assertTrue("Parameters severity, exception, filename and what - stack trace for the exception", ri.getStackTrace().startsWith("java.lang.RuntimeException: No idea what's wrong"));
        assertEquals("Parameters severity, exception, filename and what - message for the exception","No idea what's wrong", ri.getLocalisedMessage());
        assertEquals("Parameters severity, exception, filename and what - severity should be UNKNOWN",ReportItem.Severity.UNKNOWN, ri.getSeverity());
        // access private filename
        try {
            fileName = ri.getClass().getDeclaredField("filename") ;
            fileName.setAccessible(true);
            assertEquals("Parameters severity, exception, filename and what - filename should be some_file.txt", "some_file.txt", fileName.get(ri));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Parameters severity, exception, filename and what - cannot access filename");
        }
        // Test constructor with four parameters: severity, filename, error and function
        ri = new ReportItem(ReportItem.Severity.NOTE,"some_other_file.txt", "Just a note","fixing_function");
        assertEquals("Parameters severity, filename, error and function - function should be fixing_function","fixing_function", ri.getFunction());
        assertEquals("Parameters severity, filename, error and function - error should be what we want","Just a note", ri.getWhat());
        assertEquals("Parameters severity, filename, error and function - howto fix should be unknown","No known fixes", ri.getHowto());
        assertEquals("Parameters severity, filename, error and function - no stack trace without exception", "" , ri.getStackTrace());
        assertEquals("Parameters severity, filename, error and function - no message without exception","", ri.getLocalisedMessage());
        assertEquals("Parameters severity, filename, error and function - severity should be NOTE",ReportItem.Severity.NOTE, ri.getSeverity());
        // access private filename
        try {
            fileName = ri.getClass().getDeclaredField("filename") ;
            fileName.setAccessible(true);
            assertEquals("Parameters severity, filename, error and function - filename should be some_other_file.txt", "some_other_file.txt", fileName.get(ri));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Parameters severity, filename, error and function - cannot access filename");
        }
        // Test constructor with three parameters: severity, saxparseexception and error
        ri = new ReportItem(ReportItem.Severity.MISSING,new SAXParseException("Something's missing","public_id","system_id",23,42),"Something's missing");
        assertEquals("Parameters severity, saxparseexception and error - function should be unknown","Unknown function", ri.getFunction());
        assertEquals("Parameters severity, saxparseexception and error - error should be what we want","Something's missing", ri.getWhat());
        assertEquals("Parameters severity, saxparseexception and error - howto fix should be unknown","No known fixes", ri.getHowto());
        assertTrue("Parameters severity, saxparseexception and error - stack trace for the  exception",  ri.getStackTrace().startsWith("org.xml.sax.SAXParseExceptionpublicId: public_id; systemId: system_id; lineNumber: 23; columnNumber: 42; Something's missing"));
        assertEquals("Parameters severity, saxparseexception and error - message for the exception","Something's missing", ri.getLocalisedMessage());
        assertEquals("Parameters severity, saxparseexception and error - severity should be MISSING",ReportItem.Severity.MISSING, ri.getSeverity());
        // access private filename
        try {
            fileName = ri.getClass().getDeclaredField("filename") ;
            fileName.setAccessible(true);
            assertEquals("Parameters severity, saxparseexception and error - filename should be system_id", "system_id", fileName.get(ri));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Parameters severity, saxparseexception and error - cannot access filename");
        }
         // Test constructor with five parameters: severity, filename, error, function, howto
        ri = new ReportItem(ReportItem.Severity.CRITICAL,"another_filename.txt","That's bad", "another_function","This will fix it");
        assertEquals("Parameters severity, filename, error, function and howto - function should be another_function","another_function", ri.getFunction());
        assertEquals("Parameters severity, filename, error, function and howto - error should be what we want","That's bad", ri.getWhat());
        assertEquals("Parameters severity, filename, error, function and howto - howto fix should be what we want","This will fix it", ri.getHowto());
        assertEquals("Parameters severity, filename, error, function and howto - no stack trace without exception", "", ri.getStackTrace());
        assertEquals("Parameters severity, filename, error, function and howto - no message without exception","", ri.getLocalisedMessage());
        assertEquals("Parameters severity, filename, error, function and howto - severity should be CRITICAL",ReportItem.Severity.CRITICAL, ri.getSeverity());
        // access private filename
        try {
            fileName = ri.getClass().getDeclaredField("filename") ;
            fileName.setAccessible(true);
            assertEquals("Parameters severity, filename, error, function and howto - filename should be system_id", "another_filename.txt", fileName.get(ri));
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
            assertEquals("First null exception", null, exception.get(ri));
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Error accessing field");
        }
        ri = new ReportItem(ReportItem.Severity.CRITICAL,(Throwable) null, "some what");
        try {
            Field exception = ri.getClass().getDeclaredField("e");
            exception.setAccessible(true);
            assertEquals("Second null exception", null, exception.get(ri));
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Error accessing field");
        }
        ri = new ReportItem(ReportItem.Severity.CRITICAL,null, "some what");
        try {
            Field exception = ri.getClass().getDeclaredField("e");
            exception.setAccessible(true);
            assertEquals("Third null exception", null, exception.get(ri));
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Error accessing field");
        }
    }

    @Test
    public void testIsGood() {
        logger.info("Run isGood test");
        // Set of all acceptable items
        Set<ReportItem.Severity> goodItems = new HashSet<>(Arrays.asList(new ReportItem.Severity[]{ReportItem.Severity.CORRECT, ReportItem.Severity.NOTE, ReportItem.Severity.IFIXEDITFORYOU}));
        // Check all possible values if they meet the expectation
        for (ReportItem.Severity s : ReportItem.Severity.values()) {
            ReportItem ri = new ReportItem(s,"Test isGood");
            assertEquals("Good items match isGood", goodItems.contains(s), ri.isGood());
        }
    }

    @Test
    public void testIsBad() {
        logger.info("Run isBad test");
        // Set of all acceptable items
        Set<ReportItem.Severity> badItems = new HashSet<>(Arrays.asList(new ReportItem.Severity[]{ReportItem.Severity.WARNING, ReportItem.Severity.CRITICAL, ReportItem.Severity.MISSING, ReportItem.Severity.UNKNOWN}));
        // Check all possible values if they meet the expectation
        for (ReportItem.Severity s : ReportItem.Severity.values()) {
            ReportItem ri = new ReportItem(s,"Test isBad");
            assertEquals("Bad items match isBad", badItems.contains(s), ri.isBad());
        }
    }

    @Test
    public void testIsSevere() {
        logger.info("Run isSevere test");
        // Set of all acceptable items
        Set<ReportItem.Severity> severeItems = new HashSet<>(Arrays.asList(new ReportItem.Severity[]{ReportItem.Severity.CRITICAL, ReportItem.Severity.MISSING, ReportItem.Severity.UNKNOWN}));
        // Check all possible values if they meet the expectation
        for (ReportItem.Severity s : ReportItem.Severity.values()) {
            ReportItem ri = new ReportItem(s,"Test isSevere");
            assertEquals("Severe items match isSevere", severeItems.contains(s), ri.isSevere());
        }
    }

    @Test
    public void testIsFix() {
        logger.info("Run isFix test");
        // Set of all acceptable items
        Set<ReportItem.Severity> fixItems = new HashSet<>(Arrays.asList(new ReportItem.Severity[]{ReportItem.Severity.IFIXEDITFORYOU}));
        // Check all possible values if they meet the expectation
        for (ReportItem.Severity s : ReportItem.Severity.values()) {
            ReportItem ri = new ReportItem(s,"Test isFix");
            assertEquals("Fix items match isFix", fixItems.contains(s), ri.isFix());
        }
    }

    @Test
    public void testGetLocation() {
        logger.info("Run getLocation test");
        ReportItem ri = new ReportItem();
        assertEquals("No location gives empty string", "", ri.getLocation());
        ri = new ReportItem(ReportItem.Severity.NOTE,"some_file.txt","","");
        assertEquals("A sax error gives the location", "some_file.txt", ri.getLocation());
        ri = new ReportItem(ReportItem.Severity.CRITICAL,new SAXParseException("","","some_file.xml",23,42),"");
        assertEquals("A sax error gives the location", "some_file.xml:23.42", ri.getLocation());
    }

    @Test
    public void testGetWhat() {
        logger.info("Run getWhat test");
        ReportItem ri = new ReportItem();
        assertEquals("No what", "Totally unknown error", ri.getWhat());
        ri = new ReportItem(ReportItem.Severity.NOTE,"");
        assertEquals("Empty what", "", ri.getWhat());
        ri = new ReportItem(ReportItem.Severity.NOTE,"what1");
        assertEquals("First what", "what1", ri.getWhat());
        ri = new ReportItem(ReportItem.Severity.NOTE,new Exception(),"what2");
        assertEquals("Second what", "what2", ri.getWhat());
        ri = new ReportItem(ReportItem.Severity.NOTE,new Exception(),"", "what3");
        assertEquals("Third what", "what3", ri.getWhat());
        ri = new ReportItem(ReportItem.Severity.NOTE,"","what4","");
        assertEquals("Fourth what", "what4", ri.getWhat());
        ri = new ReportItem(ReportItem.Severity.NOTE,new SAXParseException("",null),"what5");
        assertEquals("Fifth what", "what5", ri.getWhat());
        ri = new ReportItem(ReportItem.Severity.NOTE,"","what6","","");
        assertEquals("Sixth what", "what6", ri.getWhat());
    }

    @Test
    public void testGetHowto() {
        logger.info("Run getHowto test");
        ReportItem ri = new ReportItem();
        assertEquals("No howto", "No known fixes", ri.getHowto());
        ri = new ReportItem(ReportItem.Severity.WARNING, "some_file.txt", "some error", "someFunction", "");
        assertEquals("Empty howto", "", ri.getHowto());
        ri = new ReportItem(ReportItem.Severity.WARNING, "some_file.txt", "some error", "someFunction", "how to fix it");
        assertEquals("Some howto", "how to fix it", ri.getHowto());
    }

    @Test
    public void testGetFunction() {
        logger.info("Run getFunction test");
        ReportItem ri = new ReportItem();
        assertEquals("No function", "Unknown function", ri.getFunction());
        ri = new ReportItem(ReportItem.Severity.WARNING, "some_file.txt", "some error", "");
        assertEquals("Empty function", "", ri.getFunction());
        ri = new ReportItem(ReportItem.Severity.WARNING, "some_file.txt", "some error", "someFunction1");
        assertEquals("First function", "someFunction1", ri.getFunction());
        ri = new ReportItem(ReportItem.Severity.WARNING, "some_file.txt", "some error", "someFunction2", "some howto");
        assertEquals("Second function", "someFunction2", ri.getFunction());
    }

    @Test
    public void testGetLocalisedMessage() {
        logger.info("Run getLocalisedMessage test");
        ReportItem ri = new ReportItem();
        assertEquals("No message", "", ri.getLocalisedMessage());
        ri = new ReportItem(ReportItem.Severity.CRITICAL,(Throwable) null, "some what");
        assertEquals("Null exception", "", ri.getLocalisedMessage());
        ri = new ReportItem(ReportItem.Severity.CRITICAL, null, "some what");
        assertEquals("Null SAX exception", "", ri.getLocalisedMessage());
        ri = new ReportItem(ReportItem.Severity.CRITICAL,new RuntimeException("Some error occurred"), "some what");
        assertEquals("First exception", "Some error occurred", ri.getLocalisedMessage());
        ri = new ReportItem(ReportItem.Severity.CRITICAL, new RuntimeException("Some other error occurred"), "some what");
        assertEquals("Second exception", "Some other error occurred", ri.getLocalisedMessage());
        ri = new ReportItem(ReportItem.Severity.CRITICAL,new SAXParseException("Some SAX error occurred", "public_id", "system_id", 23, 24), "some what");
        assertEquals("SAX exception", "Some SAX error occurred", ri.getLocalisedMessage());
    }

    @Test
    public void testGetSummary() {
        logger.info("Run getSummary test");
        ReportItem ri = new ReportItem();
        assertEquals("Empty item", "    Totally unknown error", ri.getSummary());
        ri = new ReportItem(ReportItem.Severity.CRITICAL, "some what");
        assertEquals("Simple message", "    some what", ri.getSummary());
        ri = new ReportItem(ReportItem.Severity.CRITICAL, "some_file.txt", "some what", "someFunction");
        assertEquals("With filename", "    some_file.txt: some what", ri.getSummary());
        ri = new ReportItem(ReportItem.Severity.CRITICAL,new SAXParseException("Some SAX error occurred", "public_id", "system_id", 23, 24), "some other what");
        assertEquals("SAX exception", "    system_id:23.24: some other what", ri.getSummary());
    }

    @Test
    public void testToString() {
        logger.info("Run toString test");
        ReportItem ri = new ReportItem() ;
        assertEquals("Empty item", ": Totally unknown error. No known fixes. ", ri.toString());
        ri = new ReportItem(ReportItem.Severity.NOTE,"some what");
        assertEquals("Basic item", ": some what. No known fixes. ", ri.toString());
        ri = new ReportItem(ReportItem.Severity.NOTE,new SAXParseException("XML parse exception","public_id","system_id",23,42), "some XML problem");
        assertTrue("Item with XML exception", ri.toString().startsWith("system_id:23.42: some XML problem. No known fixes. XML parse exception\n" +
                "org.xml.sax.SAXParseException"));
    }

    @Test
    public void testGetStackTrace() {
        logger.info("Run getStackTrace test");
        ReportItem ri = new ReportItem() ;
        assertEquals("Empty item", "", ri.getStackTrace());
        ri = new ReportItem(ReportItem.Severity.NOTE,new RuntimeException("Runtime error"), "some runtime problem");
        assertTrue("Item with runtime exception", ri.getStackTrace().startsWith("java.lang.RuntimeException: Runtime error"));
        ri = new ReportItem(ReportItem.Severity.NOTE,new SAXParseException("XML parse exception","public_id","system_id",23,42), "some XML problem");
        assertTrue("Item with XML exception", ri.getStackTrace().startsWith("org.xml.sax.SAXParseExceptionpublicId: public_id; systemId: system_id; lineNumber: 23; columnNumber: 42; XML parse exception"));
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
        assertEquals("Total of 1 messages: 1 critical errors, 0 warnings, 0 notes and 0 others.",summary) ;
        items = new ArrayList<>() ;
        items.add(new ReportItem(ReportItem.Severity.CRITICAL,"critical")) ;
        items.add(new ReportItem(ReportItem.Severity.WARNING,"warning")) ;
        items.add(new ReportItem(ReportItem.Severity.NOTE,"note")) ;
        items.add(new ReportItem(ReportItem.Severity.MISSING,"missing")) ;
        items.add(new ReportItem(ReportItem.Severity.CORRECT,"correct")) ;
        items.add(new ReportItem(ReportItem.Severity.IFIXEDITFORYOU,"ifixeditforyou")) ;
        items.add(new ReportItem(ReportItem.Severity.UNKNOWN,"unknown")) ;
        summary = ReportItem.generateSummary(items) ;
        assertEquals("Total of 7 messages: 2 critical errors, 1 warnings, 1 notes and 3 others.",summary) ;

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
