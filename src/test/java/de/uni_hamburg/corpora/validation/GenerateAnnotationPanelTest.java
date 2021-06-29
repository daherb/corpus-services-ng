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
public class GenerateAnnotationPanelTest {
    
    public GenerateAnnotationPanelTest() {
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
     * Test of check method, of class GenerateAnnotationPanel.
     */
    @Test
    public void testCheck() throws Exception {
            
            System.out.println("check");
            String corpusFolder = "src/test/java/de/uni_hamburg/corpora/resources/example";
            URL corpusURL = Paths.get(corpusFolder).toUri().toURL();
            Corpus corp = new Corpus(corpusURL);
            GenerateAnnotationPanel instance = new GenerateAnnotationPanel();
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
    }


    /**
     * Test of getIsUsableFor method, of class GenerateAnnotationPanel.
     */
    @Test
    public void testGetIsUsableFor() {
        System.out.println("getIsUsableFor");
        GenerateAnnotationPanel instance = new GenerateAnnotationPanel();
        //Collection<Class> expResult = null;
        try {
            Collection<Class<? extends CorpusData>> result = instance.getIsUsableFor();
            //no null object here
            assertNotNull(result);
        }
        catch (ClassNotFoundException e) {
            fail("Class not found");
        }
    }
    
}
