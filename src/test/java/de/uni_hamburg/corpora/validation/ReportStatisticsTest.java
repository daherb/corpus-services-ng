/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import org.junit.jupiter.api.*;

import de.uni_hamburg.corpora.Report;
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
public class ReportStatisticsTest {
    
    public ReportStatisticsTest() {
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
     * Test of check method, of class ReportStatistics.
     */
    @Test
    public void testCheck() throws Exception {
        System.out.println("check");
        //CorpusData cd = "src/test/java/de/uni_hamburg/corpora/resources/example";
        //String corpusFolder = "src/test/java/de/uni_hamburg/corpora/resources/example";
        String corpusFolder = "K:/Selkup/SelkupCorpus";
        URL corpusURL = Paths.get(corpusFolder).toUri().toURL();
        Corpus corp = new Corpus(corpusURL);
        ReportStatistics instance = new ReportStatistics(new Properties());
        Collection<CorpusData> cdc;
        //what happens when we check coma files
        for (CorpusData cd : corp.getMetadata()) {
            assertNotNull(instance.function(cd,false));
        }

    }



    /**
     * Test of getIsUsableFor method, of class ReportStatistics.
     */
    @Test
    public void testGetIsUsableFor() {
        System.out.println("getIsUsableFor");
        ReportStatistics instance = new ReportStatistics(new Properties());
        //Collection<Class> expResult = null;
        //Collection<Class> result = instance.getIsUsableFor();
        Collection<Class<? extends CorpusData>> result = instance.getIsUsableFor();
        //no null object here
        assertNotNull(result);
    }
    
}
