/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.Report;
import org.junit.jupiter.api.*;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 * @author Ozzy
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240510
 */
public class ComaAddTiersFromExbsCorrectorTest {

    public ComaAddTiersFromExbsCorrectorTest() {
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
     * Test of getIsUsableFor method, of class ComaAddTiersFromExbsCorrector.
     */
    @Test
    public void testGetIsUsableFor() {
        System.out.println("getIsUsableFor");
        ComaAddTiersFromExbsCorrector instance = new ComaAddTiersFromExbsCorrector(new Properties());
        //Collection<Class> expResult = null;
        Collection<Class<? extends CorpusData>> result = instance.getIsUsableFor();
        //no null object here
        assertNotNull(result);
    }

    /**
     * Test of execute method, of class ComaAddTiersFromExbsCorrector.
     */
    @Test
    public void testExecute() throws Exception{
        System.out.println("execute");
        String corpusFolder = "src/test/java/de/uni_hamburg/corpora/resources/example";
        URL corpusURL = Paths.get(corpusFolder).toUri().toURL();
        Corpus corp = new Corpus(corpusURL);
        ComaAddTiersFromExbsCorrector instance = new ComaAddTiersFromExbsCorrector(new Properties());
        Collection<CorpusData> cdc;
        //what happens when we check coma files
        for (CorpusData cd : corp.getMetadata()) {
            assertNotNull(instance.execute(cd));
            //assertFalse(instance.CorpusDataIsAlreadyPretty(cd));
        }
    }

}
