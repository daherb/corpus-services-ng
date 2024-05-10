package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 * @author Ozzy
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240510
 */
public class NgTierCheckerWithAnnotationTest {

    public NgTierCheckerWithAnnotationTest() {
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
     * Test of check method, of class NgTierCheckerWithAnnotation.
     */
    @Test
    public void testCheck() throws Exception {
        System.out.println("check");
        String corpusFolder = "src/test/java/de/uni_hamburg/corpora/resources/example";
        URL corpusURL = Paths.get(corpusFolder).toUri().toURL();
        Corpus corp = new Corpus(corpusURL);
        NgTierCheckerWithAnnotation instance = new NgTierCheckerWithAnnotation(new Properties());
        //what happens when we check coma files
        for (CorpusData cd : corp.getMetadata()) {
            assertNotNull(instance.check(cd));
        }
        //what happens when we check annotation files
        for (CorpusData cd : corp.getAnnotationspecification()) {
            assertNotNull(instance.check(cd));
        }
    }

    /**
     * Test of getIsUsableFor method, of class NgTierCheckerWithAnnotation.
     */
    @Test
    public void testGetIsUsableFor() {
        System.out.println("getIsUsableFor");
        NgTierCheckerWithAnnotation instance = new NgTierCheckerWithAnnotation(new Properties());
        Collection<Class<? extends CorpusData>> result = instance.getIsUsableFor();
        assertNotNull(result);
    }

}
