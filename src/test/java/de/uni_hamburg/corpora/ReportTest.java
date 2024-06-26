package de.uni_hamburg.corpora;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Last updated
 * @author Herbert Lange
 * @version 20240510
 *
 * Unit tests for the Report class.
 */
public class ReportTest {

    Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    Report comprehensiveReport ;

    public ReportTest() {
    }

    @BeforeAll
    public static void setUpClass() {
    }

    @AfterAll
    public static void tearDownClass() {
    }

    @BeforeEach
    public void setUp() {
        comprehensiveReport = new Report();
        comprehensiveReport.addCorrect("Correct","First correct");
        comprehensiveReport.addCorrect("Correct","some_file.txt","Second correct with description");
        comprehensiveReport.addCorrect("Correct", new EXMARaLDATranscriptionData(), "Third correct with corpus data");
        comprehensiveReport.addMissing("Missing","First missing");
        comprehensiveReport.addMissing("Missing", new EXMARaLDATranscriptionData(), "Second missing with corpus data");
        comprehensiveReport.addNote("Note","First note");
        comprehensiveReport.addNote("Note", new RuntimeException("Some runtime error"),"Second note with exception");
        comprehensiveReport.addNote("Note", new RuntimeException("Some other runtime error"),"Third note with exception and extrablah","More blah");
        comprehensiveReport.addNote("Note", new EXMARaLDATranscriptionData(), "Fourth note with corpus data");
        comprehensiveReport.addNote("Note","Fifth note with extrablah", "Even more blah");
        comprehensiveReport.addCritical("Critical","First critical");
        comprehensiveReport.addCritical("Critical","Second critical with extrablah", "And more blah");
        comprehensiveReport.addCritical("Critical", new RuntimeException("Some more runtime error"), "Third critical with exception");
        comprehensiveReport.addCritical("Critical", new RuntimeException("And some more runtime error"), "Fourth critical with exception and extra blah", "And even more blah");
        comprehensiveReport.addCritical("Critical",new EXMARaLDATranscriptionData(), "Fifth critical with corpus data");
        comprehensiveReport.addCritical("Critical at the root");
        comprehensiveReport.addWarning("Warning", "First warning");
        comprehensiveReport.addWarning("Warning", new EXMARaLDATranscriptionData(), "Second warning with corpus data");
        comprehensiveReport.addWarning("Warning", "Third warning with extrablah", "Blahblah");
        comprehensiveReport.addWarning("Warning", new RuntimeException("Something else went wrong"), "Fourth warning with exception and extrablah", "Blahblablah");
        comprehensiveReport.addException("Exception",new RuntimeException("Some runtime exception here"), "First exception");
        comprehensiveReport.addException("Exception",new RuntimeException("Some runtime exception here too"), "Second exception with extrablah", "Blahblahblarg");
        comprehensiveReport.addException("Exception", new RuntimeException("Somet exception here again"), new EXMARaLDATranscriptionData(), "Third exception with corpus data");
        comprehensiveReport.addException(new RuntimeException("Something went wrong at the root"), "Exception at the root");
        comprehensiveReport.addFix("Fix",new EXMARaLDATranscriptionData(), "Only fix");
        comprehensiveReport.addReportItem("Random",new ReportItem());
    }

    @AfterEach
    public void tearDown() {
    }

    @Test
    public void testGetOrCreateStatistic() {
        logger.info("Run getOrCreateStatistic test");
        Report r = new Report();
        // Get access to the private field statistics
        Class<?> privateReport = r.getClass();
        try {
            Field privateField = privateReport.getDeclaredField("statistics");
            privateField.setAccessible(true);
            Map<String, Collection<ReportItem>> privateStatistics = (Map<String, Collection<ReportItem>>) privateField.get(r);
            // After creation the report is empty
            assertTrue(privateStatistics.isEmpty(),"The statistics should be empty");
            // get access to the private method
            Method privateMethod = privateReport.getDeclaredMethod("getOrCreateStatistic",String.class);
            // Create new bucket testId
            privateMethod.setAccessible(true);
            privateMethod.invoke(r,"testId");
            // After creating a bucket the report is not empty any longer
            assertFalse(privateStatistics.isEmpty(),"The statistics should be non-empty");
            // And the key for the new bucket is contained
            assertTrue(privateStatistics.containsKey("testId"),"Key should be contained now");
        }
        catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testConstructor() {
        logger.info("Run constructor test");
        Report r = new Report();
        Field privateStatistics = null;
        try {
            privateStatistics = r.getClass().getDeclaredField("statistics");
            privateStatistics.setAccessible(true);
            assertTrue(((Map<String,Collection<ReportItem>>) privateStatistics.get(r)).isEmpty(), "An empty report contains an empty statistics");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testMerge() {
        logger.info("Run merge test");
        Report r1 = new Report() ;
        r1.addNote("MergeTest","Note 1");
        Report r2 = new Report() ;
        r2.addCritical("MergeTest", "Note 2");
        r1.merge(r2);
        r1.getFullReports();
        assertEquals("All reports\nMergeTest:\n: Note 1. No known fixes. \n: Note 2. No known fixes. \n", r1.getFullReports(), "Report should be combination of the two reports");
    }

    @Test
    public void testAddReportItem() {
        logger.info("Run addReportItem test");
        Report r = new Report();
        ReportItem ri = new ReportItem(ReportItem.Severity.NOTE, "some report item");
        try {
            Field privateStatistics = r.getClass().getDeclaredField("statistics");
            privateStatistics.setAccessible(true);
            Map<String, Collection<ReportItem>> map = (Map<String, Collection<ReportItem>>) privateStatistics.get(r);
            assertFalse(map.containsKey("Test"),"StatId key does not exist before adding") ;
            r.addReportItem("Test",ri);
            assertTrue(map.containsKey("Test"),"StatId key does exist after adding") ;
            assertTrue(map.get("Test").contains(ri),"Item exists in the collection") ;
            assertEquals(": some report item. No known fixes. ", map.get("Test").toArray()[0].toString(), "Item in the collection is the one we created");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAddCritical() {
        // logger.info("Run addCritical test");
        // TODO
    }

    @Test
    public void testAddFix() {
        // logger.info("Run addFix test");
        // TODO
    }

    @Test
    public void testAddWarning() {
        // logger.info("Run addWarning test");
        // TODO
    }

    @Test
    public void testAddMissing() {
        // logger.info("Run addMissing test");
        // TODO
    }

    @Test
    public void testAddCorrect() {
        // logger.info("Run addCorrect test");
        // TODO
    }

    @Test
    public void testAddNote() {
        // logger.info("Run addNote test");
        // TODO
    }

    @Test
    public void testAddException() {
        // logger.info("Run addException test");
        // TODO
    }

    @Test
    public void testGetSummaryLine() {
        logger.info("Run getSummaryLine test");
        String expectedSummary = "  Missing: 0 %: 0 OK, 2 bad, 0 warnings and 0 unknown. = 2 items.\n" +
                "  Warning: 0 %: 0 OK, 0 bad, 4 warnings and 0 unknown. = 4 items.\n" +
                "  Fix: 100 %: 1 OK, 0 bad, 0 warnings and 0 unknown. = 1 items.\n" +
                "  Random: 0 %: 0 OK, 1 bad, 0 warnings and 0 unknown. = 1 items.\n" +
                "  Note: 100 %: 5 OK, 0 bad, 0 warnings and 0 unknown. = 5 items.\n" +
                "  root: 0 %: 0 OK, 2 bad, 0 warnings and 0 unknown. = 2 items.\n" +
                "  Critical: 0 %: 0 OK, 5 bad, 0 warnings and 0 unknown. = 5 items.\n" +
                "  Correct: 100 %: 3 OK, 0 bad, 0 warnings and 0 unknown. = 3 items.\n" +
                "  Exception: 0 %: 0 OK, 3 bad, 0 warnings and 0 unknown. = 3 items.\n" +
                "  Total: 34 %: 9 OK, 13 bad, 4 warnings and 0 unknown. = 26 items.\n";
        assertEquals(expectedSummary, comprehensiveReport.getSummaryLines(), "The summary is what we expect");
    }

    @Test
    public void testGetAllAsSummaryLine() {
        // logger.info("Run getAllAsSummaryLine test");
        // TODO
    }

    @Test
    public void testGetSummaryLines() {
        // logger.info("Run getSummaryLines test");
        // TODO
    }

    @Test
    public void testGetErrorReport() {
        // logger.info("Run getErrorReport test");
        // TODO
    }

    @Test
    public void testGetWarningReport() {
        // logger.info("Run getWarningReport test");
        // TODO
    }

    @Test
    public void testGetErrorReports() {
        // logger.info("Run getErrorReports test");
        // TODO
    }

    @Test
    public void testGetWarningReports() {
        // logger.info("Run getWarningReports test");
        // TODO
    }

    @Test
    public void testGetFullReport() {
        // logger.info("Run getFullReport test");
        // TODO
    }

    @Test
    public void testGetFullReports() {
        // logger.info("Run getFullReports test");
        // TODO
    }

    @Test
    public void testGetRawStatistics() {
        logger.info("Run getRawStatistics test");
        Collection<ReportItem> rawStatistics = comprehensiveReport.getRawStatistics();
        // Just count the elements at the moment
        assertEquals(26, rawStatistics.size(), "Should contain all the elements we defined") ;
    }

    @Test
    public void testGetErrorStatistics() {
        logger.info("Run getErrorStatistics test");
        Collection<ReportItem> rawStatistics = comprehensiveReport.getRawStatistics();
        Collection<ReportItem> errorStatistics = comprehensiveReport.getErrorStatistics();
        assertTrue(rawStatistics.containsAll(errorStatistics), "All errors are in the raw statistics") ;
    }

    @Test
    public void testGetFixJson() {
        // logger.info("Run getFixJson test");
        // TODO
    }

    @Test
    public void testGetFixLine() {
        // logger.info("Run getFixJson test");
        // TODO
    }

}
