package de.uni_hamburg.corpora.utilities.quest;



import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Basic implementation of a universal levenshtein automaton for k = 1
 * based on Schulz, Klaus and Mihov, Stoyan (2004): Fast approximate search in large dictionaries
 * https://www.cis.lmu.de/people/Schulz/Pub/fastapproxsearch.pdf
 *
 * @author bba1792 Dr. Herbert Lange
 * @version 20210730
 */
public class UniversalLevenshteinAutomatonK1 {

    /**
     * Function to generate a bit string from a character a pattern and the index in the pattern
     * @param c the significant character
     * @param pattern the pattern the bit string is based on
     * @param index the index within the pattern
     * @return the bit string or null
     */
    private static String toBitString(Character c, String pattern, int index) {
        // Pad the beginning of the string. For k=1 with one $
        String paddedPattern = "$" + pattern ;
        // Get the relevant substring, in case of k=1 at most 4 characters long (or until the end of the pattern)
        // and convert to a list of characters
        List<Character> tmp = paddedPattern.subSequence(index,Integer.min(paddedPattern.length(),index+4)).codePoints()
                        .mapToObj((pc) -> (char) pc).collect(Collectors.toList());
        // Compare each character with c and return 1 if it matches and 0 otherwise to form the bit string
        List<String> bs = tmp.stream().map((pc) -> (pc == c) ? "1" : "0").collect(Collectors.toList());
        // If the resulting bit string exists, concatenate the "bits" and return the string
        if (bs.stream().reduce(String::concat).isPresent())
            return bs.stream().reduce(String::concat).get();
        // Null in error case
        return null;
    }

    /**
     * Function to generate a list of bit strings based on a word and a pattern
     *
     * @param word the word to be compared to the pattern
     * @param pattern the pattern the bit strings are based on, i.e. another word
     * @return the list of bit strings generated by word and pattern
     */
    private List<String> toBitStrings(String word, String pattern) {
        ArrayList<String> result = new ArrayList<>();
        // For each character in word and its position generate a bit string
        for (int i =0 ; i < word.length() ; i++) {
                String bs = toBitString(word.charAt(i),pattern,i);
                // if the bit string exists, add it to the list
                if (bs != null)
                    result.add(bs);
            }
            return result ;
        }

    /**
     * Function to check the Levenshtein distance between two words and return the list of all states passed on the way
     *
     * @param word1 the first word
     * @param word2 the second word
     * @return the list of states passed, can contain null to indicate failure matching strings
     */
    private List<String> matchStates(String word1, String word2) {
        // Our Levenshtein automaton for k=1
        ULevenshteinAutomatonK1 ul = new ULevenshteinAutomatonK1();
        // Get all the bit strings
        List<String> bss = toBitStrings(word1,word2);
        // Return list of states
        List<String> states = new ArrayList<>();
        // Start through the UL automaton
        String state = ul.initialState ;
        // Continue for each bit string
        for (String bs : bss) {
            String newState = ul.transition(state,bs);
            // Add new state to the list, this could be null and indicate failure matching
            states.add(newState);
            // Stop if we cannot find a next state
            if (newState == null)
                break ;
            else
                state = newState ;
        }
        return states;
    }

    /**
     * Function to check for two words if they have a Levenshtein distance of less than k=1
     *
     * @param word1 the first word
     * @param word2 the second word
     * @return if the words are similar
     */
    public boolean match(String word1, String word2) {
        List<String> states = matchStates(word1,word2);
        return !states.contains(null);
    }

    /**
     * Class representing the current state when matching in universal Levenshtein and dictionary automaton in parallel
     */
    static class State {
        // Character index
        int index ;
        // The partial word matched against the pattern
        String word ;
        // State in the universal Levenshtein automaton
        String uLevenshteinState;
        // State in the dictionary automaton
        String dictionaryState;

        // Constructor setting all parameters
        public State(int index, String word, String dictionaryState, String uLevenshteinState) {
            this.index = index;
            this.word = word;
            this.uLevenshteinState = uLevenshteinState;
            this.dictionaryState = dictionaryState;
        }
    }

    /**
     * Function to find all similar words (Levenshtein distance less or equal k=1) in a dictionary based on a pattern
     *
     * @param pattern the word to be compared to the dictionary
     * @param da the automaton representing the dictionary
     * @return the list of all similar words in the dictionary
     */
    public static List<String> matchDictionary(String pattern, DictionaryAutomaton da) {
        List<String> matched = new ArrayList<>();
        ULevenshteinAutomatonK1 ula = new ULevenshteinAutomatonK1();
        Set<Character>  alphabet = da.getAlphabet();
        Stack<State> stack = new Stack<>();
        stack.push(new State(0,"",da.getInitialState(),ula.initialState));
        while (!stack.isEmpty()) {
            State current = stack.pop();
            for (Character c : alphabet) {
                String bitString = toBitString(c,pattern,current.index);
                String newDictState = da.transition(current.dictionaryState,c);
                String newULState = ula.transition(current.uLevenshteinState,bitString);
                //System.out.println(c + " - " + bitString + " - " + current.dictionaryState + " - " + newDictState + " - " + newULState);
                if (newDictState != null && newULState != null) {
                    String newWord = current.word + c;
                    stack.push(new State(current.index + 1 , newWord, newDictState, newULState)) ;
                    if (da.isAcceptingState(newDictState) && ula.acceptingStates.contains(newULState))
                        matched.add(newWord);
                }
            }
        }
        return matched ;
    }

    /**
     * Class representing the universal Levenshtein automaton for k=1
     */
    static class ULevenshteinAutomatonK1 {
        HashMap<String,HashMap<Pattern,String>> transitions = new HashMap<>() ;
        Set<String > acceptingStates = new HashSet<>() ;
        String initialState ;

        /**
         * Function to expand a bit string pattern used in the automaton to a pattern of a regular expression
         * @param s the bit pattern
         * @return the regular expression pattern
         */
        private Pattern toPattern(String s) {
                                   // Expand optional underscore (_) to optional 0 or 1
            return Pattern.compile(s.replace("(_)","[01]?")
                    // Expand _ to either 1 or 1
                    .replace("_","[01]")) ;
        }

        /**
         * Default constructor to build the universal Levenshtein automaton for k=1 following the definition
         * given in the paper
         */
        public ULevenshteinAutomatonK1() {
            // All transitions for each state
            HashMap<Pattern,String> i0 = new HashMap<>();
            i0.put(toPattern("_1__"),"I^0") ;
            i0.put(toPattern("_00(_)"),"(I-1)^1,I^1");
            i0.put(toPattern("_01_"),"(I-1)^1,I^1,(I+1)^1");
            i0.put(toPattern("_01"),"(M-2)^1,(M-1)^1,M^1");
            i0.put(toPattern("_0"),"(M-1)^1,M^1");
            i0.put(toPattern("_1_"),"(M-1)^0");
            i0.put(toPattern("_1"),"M^0");
            i0.put(toPattern("_"),"M^1") ;
            HashMap<Pattern,String> i_11i1 = new HashMap<>();
            i_11i1.put(toPattern("1_(_)"),"(I-1)^1,I^1");
            i_11i1.put(toPattern("10(_)(_)"),"(I-1)^1");
            i_11i1.put(toPattern("01_(_)"),"I^1");
            i_11i1.put(toPattern("01"),"M^1");
            i_11i1.put(toPattern("11"),"(M-1)^1,M^1");
            HashMap<Pattern,String> i_11 = new HashMap<>();
            i_11.put(toPattern("1_(_)(_)"),"(I-1)^1");
            i_11.put(toPattern("1"),"M^1");
            HashMap<Pattern,String> i1 = new HashMap<>();
            i1.put(toPattern("_1_(_)"),"I^1");
            i1.put(toPattern("_1"),"M^1");
            HashMap<Pattern, String> i_11i1i11 = new HashMap<>();
            i_11i1i11.put(toPattern("111_"),"(I-1)^1,I^1,(I+1)^1");
            i_11i1i11.put(toPattern("110(_)"),"(I-1)^1,I^1");
            i_11i1i11.put(toPattern("100(_)"),"(I-1)^1");
            i_11i1i11.put(toPattern("101_"),"(I-1)^1,(I+1)^1");
            i_11i1i11.put(toPattern("001_"),"(I+1)^1");
            i_11i1i11.put(toPattern("011_"),"I^1,(I+1)^1");
            i_11i1i11.put(toPattern("011"),"(M-1)^1,M^1");
            i_11i1i11.put(toPattern("101"),"(M-2)^1,M^1");
            i_11i1i11.put(toPattern("111"),"(M-2)^1,(M-1)^1,M^1");
            HashMap<Pattern,String> i_11i11 = new HashMap<>();
            i_11i11.put(toPattern("1_1_"),"(I-1)^1,(I+1)^1");
            i_11i11.put(toPattern("1_0(_)"),"(I-1)^1");
            i_11i11.put(toPattern("0_1_"),"(I+1)^1");
            i_11i11.put(toPattern("0_1"),"M^1") ;
            i_11i11.put(toPattern("1_1"),"(M-2)^1,M^1") ;
            HashMap<Pattern,String> i11 = new HashMap<>();
            i11.put(toPattern("__1_"),"(I+1)^1");
            i11.put(toPattern("__1"),"M^1");
            HashMap<Pattern,String> i1i11 = new HashMap<>();
            i1i11.put(toPattern("_11_"),"I^1,(I+1)^1");
            i1i11.put(toPattern("_10_"),"I^1");
            i1i11.put(toPattern("_01_"),"(I+1)^1");
            i1i11.put(toPattern("_10"),"I^1");
            i1i11.put(toPattern("_01"),"M^1");
            i1i11.put(toPattern("_11"),"(M-1)^1,M^1");
            HashMap<Pattern,String> m_21m1 = new HashMap<>();
            m_21m1.put(toPattern("1_"),"(I-1)^1");
            HashMap<Pattern,String> m_21m_11m1 = new HashMap<>();
            m_21m_11m1.put(toPattern("10"),"(I-1)^1");
            m_21m_11m1.put(toPattern("01"),"M^1");
            m_21m_11m1.put(toPattern("11"),"(M-1),M^1");
            HashMap<Pattern,String> m_10 = new HashMap<>();
            m_10.put(toPattern("_0"),"(M-1)^1,M^1");
            m_10.put(toPattern("_1"),"M^0");
            HashMap<Pattern,String> m_11m1 = new HashMap<>();
            m_11m1.put(toPattern("1"),"M^1");
            m_11m1.put(toPattern("_1"),"M^1");
            HashMap<Pattern,String> m0 = new HashMap<>();
            m0.put(toPattern("_"),"M^1");
            // Push all states and associated transitions into the map
            transitions.put("I^0",i0) ;
            transitions.put("(I-1)^1,I^1",i_11i1);
            transitions.put("(I-1)^1", i_11);
            transitions.put("I^1", i1);
            transitions.put("(I-1)^1,I^1,(I+1)^1",i_11i1i11);
            transitions.put("(I-1)^1,(I+1)^1",i_11i11);
            transitions.put("(I+1)^1", i11);
            transitions.put("I^1,(I+1)^1",i1i11);
            transitions.put("(M-2)^1,M^1",m_21m1);
            transitions.put("(M-2)^1,(M-1^1,M^1",m_21m_11m1);
            transitions.put("(M-1)^0",m_10);
            transitions.put("(M-1)^1,M^1",m_11m1);
            transitions.put("M^0",m0);
            // Initial and accepting states
            initialState = "I^0" ;
            acceptingStates.add("(M-2)^1,M^1");
            acceptingStates.add("(M-2)^1,(M-1)^1,M^1");
            acceptingStates.add("(M-1)^0");
            acceptingStates.add("(M-1)^1,M^1");
            acceptingStates.add("M^0");
            acceptingStates.add("M^1");
        }

        /**
         * Function to follow a transition from one state to a new state depending on the input
         *
         * @param state the state to start from
         * @param input the input to determine the transition
         * @return the new state if the transition exists, and null otherwise
         */
        public String transition(String state, String input) {
            HashMap<Pattern,String> currentTransitions = transitions.get(state);
            if (currentTransitions != null) {
                for (Pattern p : currentTransitions.keySet())
                    if (p.matcher(input).matches())
                        return currentTransitions.get(p);
            }
            return null ;
        }
    }
}