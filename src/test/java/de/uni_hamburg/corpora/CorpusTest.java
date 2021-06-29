package de.uni_hamburg.corpora;

import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author bba1792 Dr. Herbert Lange
 * @version 20210629
 * Unit tests for the Corpus class.
 */
public class CorpusTest {

    Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    public CorpusTest() {
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

    @Test
    public void testConstructor() {
        logger.info("Run constructor test");
        // We only test the constructor taking a collection of corpus data at the moment

        // Corpus c = new Corpus();
    }
}