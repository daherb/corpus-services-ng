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

import java.io.File;
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
public class GenerateAnnotationPanelTest {
    
    public GenerateAnnotationPanelTest() {
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
     * Test of check method, of class GenerateAnnotationPanel.
     */
    @Test
    public void testCheck() throws Exception {
            
            System.out.println("check");
            String corpusFolder = "src/test/java/de/uni_hamburg/corpora/resources/example";
            URL corpusURL = Paths.get(corpusFolder).toUri().toURL();
            Corpus corp = new Corpus(corpusURL);
            GenerateAnnotationPanel instance = new GenerateAnnotationPanel(new Properties());
            Collection<CorpusData> cdc;
            //what happens when we check exb files
            for (CorpusData cd : corp.getContentdata()){
                assertNotNull(instance.function(cd,false));
                //assertTrue(instance.CorpusDataIsAlreadyPretty(cd));
            }
            //what happens when we check coma files
            for (CorpusData cd : corp.getMetadata()){
                assertNotNull(instance.function(cd,false));
                //assertFalse(instance.CorpusDataIsAlreadyPretty(cd));
            }
            // Remove the file created for the dolgan corpus
            new File("src/test/java/de/uni_hamburg/corpora/resources/example/DolganCorpus/AnnotationSpecFromExbs.xml").delete();
    }


    /**
     * Test of getIsUsableFor method, of class GenerateAnnotationPanel.
     */
    @Test
    public void testGetIsUsableFor() {
        System.out.println("getIsUsableFor");
        GenerateAnnotationPanel instance = new GenerateAnnotationPanel(new Properties());
        //Collection<Class> expResult = null;
        Collection<Class<? extends CorpusData>> result = instance.getIsUsableFor();
        //no null object here
        assertNotNull(result);
    }
    
}
