package de.uni_hamburg.corpora.utilities;

import java.io.*;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import de.uni_hamburg.corpora.utilities.quest.DictionaryAutomaton;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Class for a finite-state representation of a dictionary
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240510
 */
public class DictionaryAutomatonTest {
    File tmpDict;
    @BeforeEach
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

    @AfterEach
    public void tearDown() throws Exception {

    }

    @BeforeAll
    public static void beforeClass() throws Exception {

    }

    @AfterAll
    public static void afterClass() throws Exception {

    }

    /**
     * Test readFileAsList function
     */
    @Test
    public void testReadFileAsList() {
        // Read temp file and check the results
        List<String> lines = DictionaryAutomaton.readFileAsList(tmpDict);
        assertNotNull(lines, "List not null");
        assertFalse(lines.isEmpty(), "List not empty");
        assertEquals(5, lines.size(),"List has expected number of elements");
        assertEquals("foo",lines.get(0),  "Expected first element");
        assertEquals("blubb", lines.get(lines.size()-1), "Expected last element");
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
        assertEquals("s0", da.getInitialState(), "The artificial initial state");
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
        assertEquals("[a, b, f, l, o, r, u, z]",
                alphabet.stream().sorted().collect(Collectors.toList()).toString(), "The expected alphabet");
    }

    /**
     * Test addWord
     */
    @Test
    public void testAddWord() {
        DictionaryAutomaton da = new DictionaryAutomaton(tmpDict);
        Set<Character> alphabet1 = new HashSet(da.getAlphabet());
        // Check that prior to adding it, the alphabet does not contain certain letters and does not accept the worc
        assertFalse(alphabet1.contains('w'), "Letter w is in alphabet");
        assertFalse(da.match("wuseldusel"), "The word is accepted");
        da.addWord("wuseldusel");
        // Check that the alphabet gets extended appropriately
        Set<Character> alphabet2 = da.getAlphabet();
        assertFalse(alphabet1.contains('w'), "Letter w is nowin first alphabet");
        assertNotEquals(alphabet1, alphabet2,"New alphabet is the same");
        assertTrue(alphabet2.size() > alphabet1.size(), "New alphabet is not larger");
        assertTrue(alphabet2.contains('w'), "Letter w in not second alphabet");
        // Check that the word is now accepted
        assertTrue(da.match("wuseldusel"), "The word is still not accepted");
        assertFalse(da.match("blafasel"), "Some other random word is accepted");
    }

    @Test
    public void testMatch() {
        DictionaryAutomaton da = new DictionaryAutomaton(tmpDict);
        // Some words that are accepted
        assertTrue(da.match("bla"), "First word not accepted");
        assertTrue(da.match("blubb"), "Second word not accepted");
        // Some words that are not
        assertFalse(da.match("blu"), "First word accepted");
        assertFalse(da.match("blub"), "Second word accepted");
        assertFalse(da.match("foobar"), "Third word accepted");
    }

    @Test
    public void testTransition() {
        DictionaryAutomaton da = new DictionaryAutomaton(tmpDict);
        // Testing all transitions for a complete word
        String state = da.getInitialState();
        String oldstate = state;
        state = da.transition(state, 'b');
        assertNotNull(state, "First new state is null");
        assertNotEquals(oldstate, state, "We are still in the initial state");
        oldstate = state;
        state = da.transition(state, 'l');
        assertNotNull(state, "Second new state is null");
        assertNotEquals(oldstate, state, "We are still in the same state");
        oldstate = state;
        state = da.transition(state, 'a');
        assertNotNull(state, "Third new state is null");
        assertTrue(da.isAcceptingState(state), "After the end of the word we are not in an accepting state");
        state = da.transition(state, 'w');
        assertNull(state, "Incorrect transition does not lead to null state");
    }

    @Test
    public void testSegmentWord() {
        DictionaryAutomaton da = new DictionaryAutomaton(tmpDict);
        List<String> segments = da.segmentWord("foobar");
        assertNotNull(segments, "First segments are null");
        assertEquals(2, segments.size(), "First segments do not have two elements");
        segments = da.segmentWord("foo");
        assertNotNull(segments, "Second segments are null");
        assertEquals(1, segments.size(), "Second segments do not have one element");
        segments = da.segmentWord("foob");
        assertNull(segments, "Third segments are not null");
        segments = da.segmentWord("wuseldusel");
        assertNull(segments, "Fourth segments are not null");
    }

    @Test
    public void testCheckSegmentWord() {
         DictionaryAutomaton da = new DictionaryAutomaton(tmpDict);
         assertTrue(da.checkSegmentableWord("foobar"), "First word is not segmentable");
         assertTrue(da.checkSegmentableWord("foo"), "Second word is not segmentable");
         assertFalse(da.checkSegmentableWord("foob"), "Third word is segmentable");
         assertFalse(da.checkSegmentableWord("wuseldusel"), "Fourth word is segmentable");
    }

    @Test
    public void testStackToList() {
        // TODO
    }
}
