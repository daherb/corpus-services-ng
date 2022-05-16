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
public class CmdiCheckerTest {
    
    public CmdiCheckerTest() {
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
     * Test of check method, of class CmdiChecker.
     */
    @Test
public void testCheck() throws Exception {
            
            System.out.println("check");
            String corpusFolder = "src/test/java/de/uni_hamburg/corpora/resources/example";
            URL corpusURL = Paths.get(corpusFolder).toUri().toURL();
            Corpus corp = new Corpus(corpusURL);
            CmdiChecker instance = new CmdiChecker(new Properties());
            Collection<CorpusData> cdc;
            //what happens when we check cmdi files
            for (CorpusData cd : corp.getCmdidata()){
                assertNotNull(instance.function(cd,false));
                //assertFalse(instance.CorpusDataIsAlreadyPretty(cd));
            }
    }


    /**
     * Test of getIsUsableFor method, of class CmdiChecker.
     */
    @Test
    public void testGetIsUsableFor() {
        System.out.println("getIsUsableFor");
        CmdiChecker instance = new CmdiChecker(new Properties());
        //Collection<Class> expResult = null;
        Collection<Class<? extends CorpusData>> result = instance.getIsUsableFor();
        //no null object here
        assertNotNull(result);
    }
    
}
