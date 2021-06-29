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
public class IAAFunctionalityTest {

    public IAAFunctionalityTest() {
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
     * Test of check method, of class IAAFunctionality.
     */
    @Test
    public void testCheck() throws Exception {
        System.out.println("check");
        //CorpusData cd = "src/test/java/de/uni_hamburg/corpora/resources/example";
        String corpusFolder = "src\\test\\java\\de\\uni_hamburg\\corpora\\resources\\example";
        URL corpusURL = Paths.get(corpusFolder).toUri().toURL();
        Corpus corp = new Corpus(corpusURL);
        IAAFunctionality instance = new IAAFunctionality();
        Collection<CorpusData> cdc;
        //what happens when we check exb files
        for (CorpusData cd : corp.getContentdata()) {
            assertNotNull(instance.function(cd,false));
        }

    }



    /**
     * Test of getIsUsableFor method, of class IAAFunctionality.
     */
    @Test
    public void testGetIsUsableFor() {
        System.out.println("getIsUsableFor");
        IAAFunctionality instance = new IAAFunctionality();
        //Collection<Class> expResult = null;
        //Collection<Class> result = instance.getIsUsableFor();
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
