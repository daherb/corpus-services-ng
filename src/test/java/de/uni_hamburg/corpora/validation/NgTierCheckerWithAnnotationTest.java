package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.Report;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ozzy
 */
public class NgTierCheckerWithAnnotationTest {

    public NgTierCheckerWithAnnotationTest() {
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
     * Test of check method, of class NgTierCheckerWithAnnotation.
     */
    @Test
    public void testCheck() throws Exception {

        System.out.println("check");
        String corpusFolder = "src/test/java/de/uni_hamburg/corpora/resources/example";
        URL corpusURL = Paths.get(corpusFolder).toUri().toURL();
        Corpus corp = new Corpus(corpusURL);
        NgTierCheckerWithAnnotation instance = new NgTierCheckerWithAnnotation();
        Collection<CorpusData> cdc;
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
        NgTierCheckerWithAnnotation instance = new NgTierCheckerWithAnnotation();
        try {
            Collection<Class<? extends CorpusData>> result = instance.getIsUsableFor();
            assertNotNull(result);
        }
        catch (ClassNotFoundException e) {
            fail("Class not found");
        }
    }

}
