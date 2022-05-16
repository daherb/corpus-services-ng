package de.uni_hamburg.corpora.utilities;

import org.junit.*;

import java.io.*;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import de.uni_hamburg.corpora.utilities.quest.DictionaryAutomaton;

import static org.junit.Assert.*;

/**
 * Class for a finite-state representation of a dictionary
 */
public class DictionaryAutomatonTest {
    File tmpDict;
    @Before
    public void setUp() throws Exception {
        // Write test file
        try {
            String[] lines = {"foo", "bar", "baz", "bla", "blubb" };
            tmpDict = Files.createTempFile(null,null).toFile();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpDict)));
            for (String l : lines) {
                bw.write(l);
                bw.newLine();
            }
            bw.close();
            tmpDict.deleteOnExit();
        } catch (IOException e) {
            fail("Exception when creating temp file");
        }
    }

    @After
    public void tearDown() throws Exception {

    }

    @BeforeClass
    public static void beforeClass() throws Exception {

    }

    @AfterClass
    public static void afterClass() throws Exception {

    }

    /**
     * Test readFileAsList function
     */
    @Test
    public void testReadFileAsList() {
        // Read temp file and check the results
        List<String> lines = DictionaryAutomaton.readFileAsList(tmpDict);
        assertNotEquals("List not null", lines, null);
        assertFalse("List not empty", lines.isEmpty());
        assertEquals("List has expected number of elements", 5, lines.size());
        assertEquals("Expected first element", lines.get(0), "foo");
        assertEquals("Expected last element", lines.get(lines.size()-1), "blubb");
    }

    /**
     * Test constructors
     */
    @Test
    public void testConstructor() {
        DictionaryAutomaton da1 = new DictionaryAutomaton(tmpDict);
        List<String> lines = DictionaryAutomaton.readFileAsList(tmpDict);
        DictionaryAutomaton da2 = new DictionaryAutomaton(lines);
        // TODO
    }

    /**
     * Test getInitialState
     */
    @Test
    public void testGetInitialState() {
        DictionaryAutomaton da = new DictionaryAutomaton(tmpDict);
        assertEquals("The artificial initial state" , "s0", da.getInitialState());
    }

    /**
     * Test isAcceptingState()
     */
    @Test
    public void testIsAcceptingState() {
        // TODO
    }

    /**
     * Test getAlphabet
     */
    @Test
    public void testGetAlphabet() {
        DictionaryAutomaton da = new DictionaryAutomaton(tmpDict);
        Set<Character> alphabet = da.getAlphabet();
        assertEquals("The expected alphabet", "[a, b, f, l, o, r, u, z]",
                alphabet.stream().sorted().collect(Collectors.toList()).toString());
    }

    /**
     * Test addWord
     */
    @Test
    public void testAddWord() {
        DictionaryAutomaton da = new DictionaryAutomaton(tmpDict);
        Set<Character> alphabet1 = new HashSet(da.getAlphabet());
        // Check that prior to adding it, the alphabet does not contain certain letters and does not accept the worc
        assertFalse("Letter w not in alphabet", alphabet1.contains('w'));
        assertFalse("The word is not accepted", da.match("wuseldusel"));
        da.addWord("wuseldusel");
        // Check that the alphabet gets extended appropriately
        Set<Character> alphabet2 = da.getAlphabet();
        assertFalse("Letter w still not in first alphabet", alphabet1.contains('w'));
        assertNotEquals("New alphabet is not the same", alphabet1, alphabet2);
        assertTrue("New alphabet is larger", alphabet2.size() > alphabet1.size());
        assertTrue("Letter w in second alphabet", alphabet2.contains('w'));
        // Check that the word is now accepted
        assertTrue("The word is now accepted", da.match("wuseldusel"));
        assertFalse("Some other random word is still not accepted",da.match("blafasel"));
    }

    @Test
    public void testMatch() {
        DictionaryAutomaton da = new DictionaryAutomaton(tmpDict);
        // Some words that are accepted
        assertTrue("First accepted word", da.match("bla"));
        assertTrue("Second accepted word", da.match("blubb"));
        // Some words that are not
        assertFalse("First non-accepted word", da.match("blu"));
        assertFalse("Second non-accepted word", da.match("blub"));
        assertFalse("Third non-accepted word", da.match("foobar"));
    }

    @Test
    public void testTransition() {
        DictionaryAutomaton da = new DictionaryAutomaton(tmpDict);
        // Testing all transitions for a complete word
        String state = da.getInitialState();
        String oldstate = state;
        state = da.transition(state, 'b');
        assertNotNull("First new state is not null", state);
        assertNotEquals("We are not in the initial state anymore", oldstate, state);
        oldstate = state;
        state = da.transition(state, 'l');
        assertNotNull("Second new state is not null", state);
        assertNotEquals("We are not in the same state anymore", oldstate, state);
        oldstate = state;
        state = da.transition(state, 'a');
        assertNotNull("Third new state is not null", state);
        assertTrue("After the end of the word we are in an accepting state", da.isAcceptingState(state));
        state = da.transition(state, 'w');
        assertNull("Incorrect transition leads to null state", state);
    }

    @Test
    public void testSegmentWord() {
        DictionaryAutomaton da = new DictionaryAutomaton(tmpDict);
        List<String> segments = da.segmentWord("foobar");
        assertNotNull("First segments are not null", segments);
        assertEquals("First segments have two elements", 2, segments.size());
        segments = da.segmentWord("foo");
        assertNotNull("Second segments are not null", segments);
        assertEquals("Second segments have one element", 1, segments.size());
        segments = da.segmentWord("foob");
        assertNull("Third segments are null", segments);
        segments = da.segmentWord("wuseldusel");
        assertNull("Fourth segments are null", segments);
    }

    @Test
    public void testCheckSegmentWord() {
         DictionaryAutomaton da = new DictionaryAutomaton(tmpDict);
         assertTrue("First word is segmentable", da.checkSegmentableWord("foobar"));
         assertTrue("Second word is segmentable", da.checkSegmentableWord("foo"));
         assertFalse("Third word is non-segmentable", da.checkSegmentableWord("foob"));
         assertFalse("Fourth word is non-segmentable", da.checkSegmentableWord("wuseldusel"));
    }

    @Test
    public void testStackToList() {
        // TODO
    }
}
