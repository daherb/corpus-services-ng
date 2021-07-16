/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.ELANData;
import org.junit.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Unit test for the RefcoChecker class
 *
 * @author bba1792 Dr. Herbert Lange
 * @version 20210715
 */
public class RefcoCheckerTest {

    public RefcoCheckerTest() {
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
     * Test of getDescription method, of class RefcoChecker
     */
    @Test
    public void testGetDescription() {
        RefcoChecker rc = new RefcoChecker();
        String description = rc.getDescription();
        assertNotNull("Description is not null", description);
    }

    /**
     * Test of check method, of class RefcoChecker.
     */
    @Test
    public void testCheck() throws Exception {
        // TODO
    }


    /**
     * Test of getIsUsableFor method, of class RefcoChecker.
     */
    @Test
    public void testGetIsUsableFor() {
        System.out.println("getIsUsableFor");
        RefcoChecker instance = new RefcoChecker();
        try {
            Collection<Class<? extends CorpusData>> result = instance.getIsUsableFor();
            //no null object here
            assertNotNull(result);
            assertTrue("Checker is usable for ELAN", result.contains(ELANData.class));
        }
        catch (ClassNotFoundException e) {
            fail("Class not found");
        }
    }

    /**
     * Test of setRefcoFileName method, of class RefcoChecker.
     */
    @Test
    public void testSetRefcoFileName() {

    }

    /**
     * Test of getCellText method, of class RefcoChecker.
     */
    @Test
    public void testGetCellText() {

    }

    /*
    safeGetText
    getTextInRow
    getInformationNotes
    readRefcoCriteria
    refCoGenericCheck
    refcoCorpusCheck
    checkTranscription
    checkUrl
    getTextsInTierByType
    getTextsInTierByID
    countWordsInTierByType
    countTranscribedWords
     */

    /**
     * Test of listToParamList method, of class RefcoChecker.
     */
    @Test
    public void testListToParamList() {
        String[] ss = { "x","1",".","a" } ;
        // Get first list from plain array
        ArrayList<String> l = new ArrayList<>(Arrays.asList(ss)) ;
        assertEquals("List as long as the plain array", ss.length,l.size());
        // Generate a Object list by means of streams
        List tmp = Arrays.stream(l.toArray()).collect(Collectors.toList());
        assertEquals("New list as long as the old one", l.size(),tmp.size());
        RefcoChecker rc = new RefcoChecker() ;
        try {
            // Convert back to String list using the method
            Method listToParamList = rc.getClass().getDeclaredMethod("listToParamList", Class.class, List.class) ;
            listToParamList.setAccessible(true);
            List<String> ll = (List<String>) listToParamList.invoke(rc,String.class, tmp);
            assertEquals("Final list as long as the original one", l.size(), ll.size());
            // Check the presence of all original elements in the new list
            for (String s : l) {
                assertTrue("The element of the original list is also in the new list", ll.contains(s));
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test of getChar method, of class RefcoChecker.
     */
    @Test
    public void testGetChars() {
        String s = "abc" ;
        String c = "." ;
        RefcoChecker rc = new RefcoChecker();
        try {
            Method getChar = rc.getClass().getDeclaredMethod("getChars",String.class);
            getChar.setAccessible(true);
            assertEquals("For a short string the char is the same as the string", new Character('.'),
                    ((List<Character>) getChar.invoke(rc, c)).get(0));
            for (int i = 0; i < s.length(); i++) {
                assertEquals("For a longer string the characters match",
                        new Character(s.charAt(i)), ((List<Character>) getChar.invoke(rc, s)).get(i));
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            fail("Unexpected exception");
        }


    }
}
