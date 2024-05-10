package de.uni_hamburg.corpora;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
/**
 * Last updated
 * @author Herbert Lange
 * @version 20240510
 *
 * Unit tests for the Corpus class.
 */
public class CorpusTest {

    Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    public CorpusTest() {
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

    @Test
    public void testConstructor() {
        logger.info("Run constructor test");
        // We only test the constructor taking a collection of corpus data at the moment

        // Corpus c = new Corpus();
    }
}