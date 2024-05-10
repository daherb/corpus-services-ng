/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.Report;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Properties;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 * @author Ozzy
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240510
 */
public class ComaKmlForLocationsTest {

    public ComaKmlForLocationsTest() {
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
     * Test of check method, of class ComaKmlForLocations.
     */
    @Test
    public void testCheck() throws Exception {
        System.out.println("check");
        //CorpusData cd = "src/test/java/de/uni_hamburg/corpora/resources/example";
        String corpusFolder = "src\\test\\java\\de\\uni_hamburg\\corpora\\resources\\example\\DolganCorpus";
        URL corpusURL = Paths.get(corpusFolder).toUri().toURL();
        Corpus corp = new Corpus(corpusURL);
        ComaKmlForLocations instance = new ComaKmlForLocations(new Properties());
        instance.setKMLFilePath("src\\test\\java\\de\\uni_hamburg\\corpora\\resources\\example\\INEL_LangsRecolored.kml");
        Collection<CorpusData> cdc;
        //what happens when we check coma files
        for (CorpusData cd : corp.getMetadata()) {
            assertNotNull(instance.function(cd,false));
        }

    }

    /**
     * Test of fix method, of class ComaKmlForLocations.
     */
    @Test
    public void testFix() throws Exception {
        System.out.println("fix");
        //CorpusData cd = "src/test/java/de/uni_hamburg/corpora/resources/example";
        String corpusFolder = "src\\test\\java\\de\\uni_hamburg\\corpora\\resources\\example\\DolganCorpus";
        URL corpusURL = Paths.get(corpusFolder).toUri().toURL();
        Corpus corp = new Corpus(corpusURL);
        ComaKmlForLocations instance = new ComaKmlForLocations(new Properties());
        instance.setKMLFilePath("src\\test\\java\\de\\uni_hamburg\\corpora\\resources\\example\\INEL_LangsRecolored.kml");
        Collection<CorpusData> cdc;
        //what happens when we fix coma files
        for (CorpusData cd : corp.getMetadata()) {
            assertNotNull(instance.function(cd, true));
        }

    }

    /**
     * Test of getIsUsableFor method, of class ComaKmlForLocations.
     */
    @Test
    public void testGetIsUsableFor() {
        System.out.println("getIsUsableFor");
        ComaKmlForLocations instance = new ComaKmlForLocations(new Properties());
        //Collection<Class> expResult = null;
        //Collection<Class> result = instance.getIsUsableFor();
        Collection<Class<? extends CorpusData>> result = instance.getIsUsableFor();
        //no null object here
        assertNotNull(result);
    }
}
