/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.utilities.PrettyPrinter;

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
 * @author fsnv625
 */
public class PrettyPrintDataTest {
    
    public PrettyPrintDataTest() {
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
     * Test of check method, of class PrettyPrintData.
     */
    @Test
    public void testCheck() throws Exception {
            
            System.out.println("check");
            String corpusFolder = "src/test/java/de/uni_hamburg/corpora/resources/example";
            URL corpusURL = Paths.get(corpusFolder).toUri().toURL();
            Corpus corp = new Corpus(corpusURL);
            PrettyPrintData instance = new PrettyPrintData();
            //what happens when we check coma files
            Collection<CorpusData> cdc;
            //what happens when we check coma files
            for (CorpusData cd : corp.getMetadata()){
                assertNotNull(instance.function(cd,false));
                //shouldn't be pretty printed yet
                //assertFalse(instance.CorpusDataIsAlreadyPretty(cd));
            }
            //what happens when we check exb files
            for (CorpusData cd : corp.getContentdata()){
                assertNotNull(instance.function(cd,false));
                //shouldn't be pretty printed yet
                //assertTrue(instance.CorpusDataIsAlreadyPretty(cd));
            }
            //what happens when we check annotation files
            for (CorpusData cd : corp.getAnnotationspecification()){
                assertNotNull(instance.function(cd,false));
                //shouldn't be pretty printed yet
                //assertFalse(instance.CorpusDataIsAlreadyPretty(cd));
            }
    }

    /**
     * Test of fix method, of class PrettyPrintData.
     */
    @Test
    public void testFix() throws Exception {

            System.out.println("fix");
            String corpusFolder = "src/test/java/de/uni_hamburg/corpora/resources/example";
            PrettyPrinter pp = new PrettyPrinter();
            URL corpusURL = Paths.get(corpusFolder).toUri().toURL();
            Corpus corp = new Corpus(corpusURL);
            PrettyPrintData instance = new PrettyPrintData();
            //what happens when we check coma files
            Collection<CorpusData> cdc;
            //what happens when we check coma files
            for (CorpusData cd : corp.getMetadata()){
                assertNotNull(instance.function(cd,true));
                //don't know if pretty printed or not yet
                //assertFalse(instance.CorpusDataIsAlreadyPretty(cd));
                instance.function(cd,true);
                //but now it should
                assertTrue(instance.CorpusDataIsAlreadyPretty(cd));
                //should be the same when pretty printed multiple times
                String prettyCorpusData = pp.indent(cd.toUnformattedString(), "event");
                String prettyCorpusDataSecond = pp.indent(prettyCorpusData, "event");
                assertEquals(prettyCorpusData, prettyCorpusDataSecond);
                String prettyCorpusDataThird = pp.indent(prettyCorpusDataSecond, "event");
                assertEquals(prettyCorpusData, prettyCorpusDataThird);
            }
            //what happens when we check exb files
            for (CorpusData cd : corp.getContentdata()){
                assertNotNull(instance.function(cd,true));
                //don't know if pretty printed or not yet
                //assertFalse(instance.CorpusDataIsAlreadyPretty(cd));
                //instance.function(cd,true);
                //but now it should
                //assertTrue(instance.CorpusDataIsAlreadyPretty(cd));
                //should be the same when pretty printed multiple times
                String prettyCorpusData = pp.indent(cd.toUnformattedString(), "event");
                String prettyCorpusDataSecond = pp.indent(prettyCorpusData, "event");
                assertEquals(prettyCorpusData, prettyCorpusDataSecond);
                String prettyCorpusDataThird = pp.indent(prettyCorpusDataSecond, "event");
                assertEquals(prettyCorpusData, prettyCorpusDataThird);
            }
            //what happens when we check annotation files
            for (CorpusData cd : corp.getAnnotationspecification()){
                assertNotNull(instance.function(cd,true));
                //don't know if pretty printed or not yet
                //assertFalse(instance.CorpusDataIsAlreadyPretty(cd));
                //instance.function(cd,true);
                //but now it should
                //assertTrue(instance.CorpusDataIsAlreadyPretty(cd));
                //should be the same when pretty printed multiple times
                String prettyCorpusData = pp.indent(cd.toUnformattedString(), "event");
                String prettyCorpusDataSecond = pp.indent(prettyCorpusData, "event");
                assertEquals(prettyCorpusData, prettyCorpusDataSecond);
                String prettyCorpusDataThird = pp.indent(prettyCorpusDataSecond, "event");
                assertEquals(prettyCorpusData, prettyCorpusDataThird);
            }
            
            //the following needs to be in the CorpusData class probably       
            //System.out.println(TypeConverter.String2JdomDocument(exbString).toString());
            //System.out.println(TypeConverter.String2JdomDocument(cd.toUnformattedString()).toString());
            //assertEquals(TypeConverter.String2JdomDocument(exbString), TypeConverter.String2JdomDocument(cd.toUnformattedString())); 
            //URL url1 = Paths.get("C:\\Users\\fsnv625\\Desktop\\test1.xml").toUri().toURL();
            //URL url2 = Paths.get("C:\\Users\\fsnv625\\Desktop\\test2.xml").toUri().toURL();

            
        } 
    

    /**
     * Test of getIsUsableFor method, of class PrettyPrintData.
    * */ 
    @Test
    public void testGetIsUsableFor() {
        System.out.println("getIsUsableFor");
        PrettyPrintData instance = new PrettyPrintData();
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
