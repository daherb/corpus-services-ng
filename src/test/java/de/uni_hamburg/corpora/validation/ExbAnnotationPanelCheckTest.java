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
public class ExbAnnotationPanelCheckTest {
    
    public ExbAnnotationPanelCheckTest() {
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
     * Test of check method, of class ExbAnnotationPanelCheck.
     */
    @Test
    public void testCheck() throws Exception {
            
            System.out.println("check");
            String corpusFolder = "src/test/java/de/uni_hamburg/corpora/resources/example";
            URL corpusURL = Paths.get(corpusFolder).toUri().toURL();
            Corpus corp = new Corpus(corpusURL);
            ExbAnnotationPanelCheck instance = new ExbAnnotationPanelCheck(new Properties());
            Collection<CorpusData> cdc;
            //what happens when we check annotation files
            for (CorpusData cd : corp.getAnnotationspecification()){
                assertNotNull(instance.function(cd,false));
            }
            //what happens when we check exb files
            for (CorpusData cd : corp.getContentdata()){
                assertNotNull(instance.function(cd,false));
            }
    }

    /**
     * Test of getIsUsableFor method, of class ExbAnnotationPanelCheck.
     */
    @Test
    public void testGetIsUsableFor() {
        System.out.println("getIsUsableFor");
        ExbAnnotationPanelCheck instance = new ExbAnnotationPanelCheck(new Properties());
        //Collection<Class> expResult = null;
        Collection<Class<? extends CorpusData>> result = instance.getIsUsableFor();
        //no null object here
        assertNotNull(result);
    }
    
}
